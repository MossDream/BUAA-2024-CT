package backend.utils;

import backend.mips.instruction.MipsInst;
import backend.mips.instruction.ltype.Li;
import backend.mips.instruction.ltype.Move;
import middle.llvm.llvmir.type.ArrayType;
import middle.llvm.llvmir.type.IntegerType;
import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.Instruction;
import middle.llvm.llvmir.value.instruction.basic.*;
import middle.llvm.llvmir.value.instruction.memory.AllocateInst;
import middle.llvm.llvmir.value.instruction.memory.GetElePtrInst;
import middle.llvm.llvmir.value.instruction.memory.LoadInst;
import middle.llvm.llvmir.value.instruction.memory.StoreInst;
import middle.llvm.llvmir.value.instruction.terminate.BranchInst;
import middle.llvm.llvmir.value.instruction.terminate.ReturnInst;
import middle.llvm.llvmir.value.item.LocalVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * @Description RegisterFile
 */

public class RegisterFile {
    private static final ArrayList<String> registers = new ArrayList<>() {{
        Arrays.asList("$zero", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
                "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
                "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
                "$t8", "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra").forEach(this::add);
    }};

    public static void initRegFile() {
        indexToCheck = 8;
        for (int i = 0; i < 32; i++) {
            if (i == 0 || i == 1 || (i >= 26 && i <= 31)) {
                regStates.set(i, RegState.SAVED);
            } else {
                regStates.set(i, RegState.FREE);
            }
        }
        offsetToFp = 0;
    }

    public static String getRegName(int id) {
        return registers.get(id);
    }

    // 记录各个寄存器分配情况，从$t0开始到$s7结束
    private static int indexToCheck = 8;

    public enum RegState {
        FREE, USED, SAVED
    }

    private static ArrayList<RegState> regStates = new ArrayList<>() {{
        for (int i = 0; i < 32; i++) {
            if (i == 0 || i == 1 || (i >= 26 && i <= 31)) {
                add(RegState.SAVED);
            } else {
                add(RegState.FREE);
            }
        }
    }};

    // 记录存储绝对位置与$fp之间的偏移量
    private static int offsetToFp = 0;

    public static int getOffsetToFp() {
        return offsetToFp;
    }

    public static void increaseOffsetToFp(int size) {
        offsetToFp += size;
    }

    // 尝试释放某个虚拟寄存器所占用的物理寄存器
    public static void tryToFreeReg(int vRegId, int index) {
        VirtualReg virtualReg = virtualRegs.get(vRegId);
        if (virtualReg.isAllocated() && virtualReg.getLastUsePos() == index) {
            int mappingRegId = virtualReg.getMappingRegId();
            if (mappingRegId != -1) {
                regStates.set(mappingRegId, RegState.FREE);
            } else {
                return;
            }
            virtualReg.setAllocated(false);
        }
    }

    // 为虚拟寄存器分配合适的物理寄存器或将其分配到栈上
    private static int getFreeReg() {
        // 使用savedToCheck，在8-25之间循环寻找空闲寄存器
        for (int i = 0; i < 18; i++) {
            if (regStates.get(indexToCheck) == RegState.FREE) {
                regStates.set(indexToCheck, RegState.USED);
                return indexToCheck;
            }
            indexToCheck++;
            if (indexToCheck == 26) {
                indexToCheck = 8;
            }
        }
        return -1;
    }

    private static ArrayList<MipsInst> allocateToStack(VirtualReg vReg, String regOrImm) {
        ArrayList<MipsInst> instructions = new ArrayList<>();
        vReg.setOffsetToFp(offsetToFp);
        if (regOrImm != null) {
            if (regOrImm.startsWith("$")) {
                instructions.add(new Move("$v1", regOrImm));
            } else {
                instructions.add(new Li("$v1", regOrImm));
            }
            instructions.addAll(MacroBuilder.push("$v1"));
        } else {
            instructions.addAll(MacroBuilder.pushOnly());
        }
        return instructions;
    }

    // 在分配到栈上时，传入某个reg或imm表示其初值，没有初值传入null
    // 强制规定reg必须使用 $v1 寄存器
    public static ArrayList<MipsInst> allocateToRegOrStack(int vRegId, String regOrImm) {
        VirtualReg virtualReg = virtualRegs.get(vRegId);
        if (virtualReg.isAllocated()) {
            // 返回null表示该虚拟寄存器已经分配或者成功分配了物理寄存器
            return null;
        }
        // 先考虑寄存器，再考虑栈上分配
        int reg = getFreeReg();
        if (reg != -1) {
            virtualReg.setMappingRegId(reg);
            return null;
        } else {
            return allocateToStack(virtualReg, regOrImm);
        }
    }

    // 虚拟寄存器表
    private static HashMap<Integer, VirtualReg> virtualRegs = new HashMap<>();
    // 按lastUsePos降序排列的虚拟寄存器表
    private static ArrayList<VirtualReg> allVirtualRegs = new ArrayList<>();

    public static VirtualReg getVirtualReg(int id) {
        return virtualRegs.get(id);
    }

    public static ArrayList<VirtualReg> getAllVirtualRegs() {
        return allVirtualRegs;
    }

    // 更新虚拟寄存器的use信息
    public static void updateVRegUsed(int id, int usedIndex) {
        VirtualReg reg = virtualRegs.get(id);
        reg.setLastUsePos(usedIndex);
    }

    // 扫描整个LLVM函数的所有指令，初始化虚拟寄存器表
    public static void initVirtualRegs(ArrayList<Instruction> instructions) {
        virtualRegs.clear();
        for (Instruction inst : instructions) {
            int index = instructions.indexOf(inst);
            switch (inst.getInstType()) {
                case Allocate -> initFromAllocateInst(inst, index);
                case Load -> initFromLoadInst(inst, index);
                case Store -> initFromStoreInst(inst, index);
                case Binary -> initFromBinaryInst(inst, index);
                case Icmp -> initFromCompareInst(inst, index);
                case Branch -> initFromBranchInst(inst, index);
                case Call -> initFromCallInst(inst, index);
                case Return -> initFromReturnInst(inst, index);
                case Print -> initFromPrintInst(inst, index);
                case GetElementPtr -> initFromGepInst(inst, index);
                case Zext -> initFromZextInst(inst, index);
                case Trunc -> initFromTruncInst(inst, index);
                default -> {
                }
            }
        }
        allVirtualRegs.clear();
        for (VirtualReg reg : virtualRegs.values()) {
            allVirtualRegs.add(reg);
        }
    }

    private static void initFromAllocateInst(Instruction inst, int index) {
        AllocateInst allocateInst = (AllocateInst) inst;
        ValueType allocatedType = allocateInst.getAllocatedType();
        LocalVar result = (LocalVar) allocateInst.getResult();
        int id = result.getNameIndex();
        // 该指令的结果必定是新的虚拟寄存器
        VirtualReg virtualReg;
        if (allocatedType instanceof IntegerType) {
            virtualReg = new VirtualReg(id, VirtualReg.VRegType.VarAddr);
        } else if (allocatedType instanceof ArrayType) {
            virtualReg = new VirtualReg(id, VirtualReg.VRegType.ArrayAddr);
            virtualReg.setArraySize(((ArrayType) allocatedType).getSize());
        } else {
            virtualReg = new VirtualReg(id, VirtualReg.VRegType.AddrOfArrayAddr);
        }
        virtualReg.setDefPos(index);
        virtualReg.setLastUsePos(index);
        virtualRegs.put(id, virtualReg);
    }

    private static void initFromLoadInst(Instruction inst, int index) {
        LoadInst loadInst = (LoadInst) inst;
        // pos已经定义，res是新的虚拟寄存器
        LocalVar result = (LocalVar) loadInst.getResult();
        int id = result.getNameIndex();
        VirtualReg resReg;
        if (loadInst.getLoadType() instanceof IntegerType) {
            resReg = new VirtualReg(id, VirtualReg.VRegType.VarValue);
        } else {
            resReg = new VirtualReg(id, VirtualReg.VRegType.ArrayAddr);
        }
        resReg.setDefPos(index);
        resReg.setLastUsePos(index);
        virtualRegs.put(id, resReg);
        Value position = loadInst.getPosition();
        if (position instanceof LocalVar) {
            int posId = ((LocalVar) position).getNameIndex();
            RegisterFile.updateVRegUsed(posId, index);
        }
    }

    private static void initFromStoreInst(Instruction inst, int index) {
        StoreInst storeInst = (StoreInst) inst;
        // 用虚拟寄存器表示的content和position都已存在（函数参数特殊处理）
        Value content = storeInst.getContent();
        if (content instanceof LocalVar) {
            int contentId = ((LocalVar) content).getNameIndex();
            RegisterFile.updateVRegUsed(contentId, index);
        }
        Value position = storeInst.getPosition();
        if (position instanceof LocalVar) {
            int posId = ((LocalVar) position).getNameIndex();
            RegisterFile.updateVRegUsed(posId, index);
        }
    }

    private static void initFromBinaryInst(Instruction inst, int index) {
        BinaryInst binaryInst = (BinaryInst) inst;
        // res是新的虚拟寄存器，left和right已存在
        LocalVar result = (LocalVar) binaryInst.getResult();
        VirtualReg resReg = new VirtualReg(result.getNameIndex(), VirtualReg.VRegType.VarValue);
        resReg.setDefPos(index);
        resReg.setLastUsePos(index);
        virtualRegs.put(result.getNameIndex(), resReg);
        Value left = binaryInst.getLeft();
        if (left instanceof LocalVar) {
            int leftId = ((LocalVar) left).getNameIndex();
            RegisterFile.updateVRegUsed(leftId, index);
        }
        Value right = binaryInst.getRight();
        if (right instanceof LocalVar) {
            int rightId = ((LocalVar) right).getNameIndex();
            RegisterFile.updateVRegUsed(rightId, index);
        }
    }

    private static void initFromCompareInst(Instruction inst, int index) {
        IcmpInst icmpInst = (IcmpInst) inst;
        // res是新的虚拟寄存器，op1和op2已存在
        LocalVar result = (LocalVar) icmpInst.getResult();
        VirtualReg resReg = new VirtualReg(result.getNameIndex(), VirtualReg.VRegType.VarValue);
        resReg.setDefPos(index);
        resReg.setLastUsePos(index);
        virtualRegs.put(result.getNameIndex(), resReg);
        Value op1 = icmpInst.getOp1();
        if (op1 instanceof LocalVar) {
            int op1Id = ((LocalVar) op1).getNameIndex();
            RegisterFile.updateVRegUsed(op1Id, index);
        }
        Value op2 = icmpInst.getOp2();
        if (op2 instanceof LocalVar) {
            int op2Id = ((LocalVar) op2).getNameIndex();
            RegisterFile.updateVRegUsed(op2Id, index);
        }
    }

    private static void initFromBranchInst(Instruction inst, int index) {
        BranchInst branchInst = (BranchInst) inst;
        // condition已存在
        Value condition = branchInst.getCondition();
        if (condition instanceof LocalVar) {
            int conditionId = ((LocalVar) condition).getNameIndex();
            RegisterFile.updateVRegUsed(conditionId, index);
        }
    }

    private static void initFromCallInst(Instruction inst, int index) {
        CallInst callInst = (CallInst) inst;
        // res是新的虚拟寄存器（可能为null），args已存在
        Value result = callInst.getResult();
        if (result != null && result instanceof LocalVar) {
            int resultId = ((LocalVar) result).getNameIndex();
            VirtualReg resReg = new VirtualReg(resultId, VirtualReg.VRegType.VarValue);
            resReg.setDefPos(index);
            resReg.setLastUsePos(index);
            virtualRegs.put(resultId, resReg);
        }
        for (Value arg : callInst.getArgs()) {
            if (arg instanceof LocalVar) {
                int argId = ((LocalVar) arg).getNameIndex();
                RegisterFile.updateVRegUsed(argId, index);
            }
        }

    }

    private static void initFromReturnInst(Instruction inst, int index) {
        ReturnInst returnInst = (ReturnInst) inst;
        // returnValue已存在
        Value returnValue = returnInst.getReturnValue();
        if (returnValue instanceof LocalVar) {
            int returnId = ((LocalVar) returnValue).getNameIndex();
            RegisterFile.updateVRegUsed(returnId, index);
        }
    }

    private static void initFromPrintInst(Instruction inst, int index) {
        PrintInst printInst = (PrintInst) inst;
        // 必须是putch或putint，printValue已存在
        Value printValue = printInst.getPrintValue();
        if (printValue != null && printValue instanceof LocalVar) {
            int printId = ((LocalVar) printValue).getNameIndex();
            RegisterFile.updateVRegUsed(printId, index);
        }
    }

    private static void initFromGepInst(Instruction inst, int index) {
        GetElePtrInst gepInst = (GetElePtrInst) inst;
        // res是新的虚拟寄存器，basePointer已存在，indices已存在
        LocalVar result = (LocalVar) gepInst.getResult();
        VirtualReg resReg = new VirtualReg(result.getNameIndex(), VirtualReg.VRegType.ArrayEleAddr);
        resReg.setDefPos(index);
        resReg.setLastUsePos(index);
        virtualRegs.put(result.getNameIndex(), resReg);
        Value basePointer = gepInst.getBasePointer();
        if (basePointer instanceof LocalVar) {
            int basePointerId = ((LocalVar) basePointer).getNameIndex();
            RegisterFile.updateVRegUsed(basePointerId, index);
        }
        for (Value inx : gepInst.getIndices()) {
            if (inx instanceof LocalVar) {
                int indexId = ((LocalVar) inx).getNameIndex();
                RegisterFile.updateVRegUsed(indexId, index);
            }
        }
    }

    private static void initFromZextInst(Instruction inst, int index) {
        ZextInst zextInst = (ZextInst) inst;
        // res是新的虚拟寄存器，src已存在
        LocalVar result = (LocalVar) zextInst.getResult();
        VirtualReg resReg = new VirtualReg(result.getNameIndex(), VirtualReg.VRegType.VarValue);
        resReg.setDefPos(index);
        resReg.setLastUsePos(index);
        virtualRegs.put(result.getNameIndex(), resReg);
        Value src = zextInst.getSrc();
        if (src instanceof LocalVar) {
            int srcId = ((LocalVar) src).getNameIndex();
            RegisterFile.updateVRegUsed(srcId, index);
        }
    }

    private static void initFromTruncInst(Instruction inst, int index) {
        TruncInst truncInst = (TruncInst) inst;
        // res是新的虚拟寄存器，src已存在
        Value src = truncInst.getSrc();
        if (src instanceof LocalVar) {
            int srcId = ((LocalVar) src).getNameIndex();
            RegisterFile.updateVRegUsed(srcId, index);
        }
        LocalVar result = (LocalVar) truncInst.getResult();
        VirtualReg resReg = new VirtualReg(result.getNameIndex(), VirtualReg.VRegType.VarValue);
        resReg.setDefPos(index);
        resReg.setLastUsePos(index);
        virtualRegs.put(result.getNameIndex(), resReg);
    }
}
