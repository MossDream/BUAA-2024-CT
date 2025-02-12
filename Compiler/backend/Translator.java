package backend;

import backend.mips.block.MipsBlock;
import backend.mips.function.MipsFunction;
import backend.mips.globallabel.GlobalLabel;
import backend.mips.instruction.MipsInst;
import backend.mips.instruction.jtype.J;
import backend.mips.instruction.jtype.Jal;
import backend.mips.instruction.jtype.Jr;
import backend.mips.instruction.ltype.*;
import backend.mips.instruction.rtype.*;
import backend.mips.module.MipsModule;
import backend.utils.MacroBuilder;
import backend.utils.RegisterFile;
import backend.utils.VirtualReg;
import middle.llvm.llvmir.type.*;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.basicblock.BasicBlock;
import middle.llvm.llvmir.value.function.Function;
import middle.llvm.llvmir.value.globalvar.GlobalVar;
import middle.llvm.llvmir.value.globalvar.GlobalVarRef;
import middle.llvm.llvmir.value.instruction.Instruction;
import middle.llvm.llvmir.value.instruction.basic.*;
import middle.llvm.llvmir.value.instruction.memory.AllocateInst;
import middle.llvm.llvmir.value.instruction.memory.GetElePtrInst;
import middle.llvm.llvmir.value.instruction.memory.LoadInst;
import middle.llvm.llvmir.value.instruction.memory.StoreInst;
import middle.llvm.llvmir.value.instruction.terminate.BranchInst;
import middle.llvm.llvmir.value.instruction.terminate.ReturnInst;
import middle.llvm.llvmir.value.item.Literal;
import middle.llvm.llvmir.value.item.LocalVar;
import middle.llvm.llvmir.value.module.Module;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @Description Translator
 */

public class Translator {
    private Module module;
    private MipsModule mipsModule;
    private boolean isOptOpen;

    public Translator(Module module, boolean isOptOpen) {
        this.module = module;
        this.isOptOpen = isOptOpen;
        mipsModule = new MipsModule();
    }

    public void printResultToFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter writer = new FileWriter(file);
        writer.write(mipsModule.toString());
        writer.close();
    }

    public void translate() {
        translateToDataSegment();
        translateToTextSegment();
    }

    private void translateToDataSegment() {
        HashMap<String, Integer> stringConstants = module.getStringConstants();
        for (String key : stringConstants.keySet()) {
            String name = module.getStringConstantName(key);
            // 将key中可能出现的\0A字符串替换成\n
            String initValue = key.replace("\\0A", "\\n");
            mipsModule.addGlobalLabel(new GlobalLabel(name.substring(1), ".asciiz", "\"" + initValue + "\""));
        }
        ArrayList<GlobalVar> globalVariables = module.getGlobalVariables();
        for (GlobalVar globalVar : globalVariables) {
            // 不带@的变量名
            String name = globalVar.getName().substring(1);
            // 确定类型
            String type = ".word";
            int arraySize = 0;
            ValueType valueType = globalVar.getType();
            if (valueType instanceof ArrayType) {
                ArrayType arrayType = (ArrayType) valueType;
                arraySize = arrayType.getSize();
            }
            // 确定初始值
            Value initValue = globalVar.getInitValue();
            Literal literal = (Literal) initValue;
            if (literal.isZeroInitializer()) {
                mipsModule.addGlobalLabel(new GlobalLabel(name, type, "0:" + arraySize));
            } else if (literal.isArray()) {
                StringBuilder initValueBuilder = new StringBuilder();
                for (String value : literal.getValue()) {
                    initValueBuilder.append(value);
                    if (literal.getValue().indexOf(value) != literal.getValue().size() - 1) {
                        initValueBuilder.append(", ");
                    }
                }
                mipsModule.addGlobalLabel(new GlobalLabel(name, type, initValueBuilder.toString()));
            } else {
                mipsModule.addGlobalLabel(new GlobalLabel(name, type, literal.getInt() + ""));
            }
        }
    }

    private void translateAllocateInst(Instruction inst, ArrayList<MipsInst> mipsInsts,
                                       boolean isEntryBlock) {
        AllocateInst allocateInst = (AllocateInst) inst;
        LocalVar res = (LocalVar) allocateInst.getResult();
        VirtualReg allocatedRes = RegisterFile.getVirtualReg(res.getNameIndex());
        VirtualReg.VRegType type = allocatedRes.getType();
        if (type == VirtualReg.VRegType.ArrayAddr) {
            // 在栈上为本地数组分配空间
            allocatedRes.setArrayPosToFp(RegisterFile.getOffsetToFp());
            int size = allocatedRes.getArraySize();
            for (int i = 0; i < size; i++) {
                mipsInsts.addAll(MacroBuilder.pushOnly());
            }
        } else if (type == VirtualReg.VRegType.VarAddr && !isEntryBlock) {
            // 在栈上为本地变量分配空间
            allocatedRes.setVarPosToFp(RegisterFile.getOffsetToFp());
            mipsInsts.addAll(MacroBuilder.pushOnly());
        }
    }

    private void translateLoadInst(Instruction inst, ArrayList<MipsInst> mipsInsts, String functionName) {
        LoadInst loadInst = (LoadInst) inst;
        Value position = loadInst.getPosition();
        int resRegId = ((LocalVar) loadInst.getResult()).getNameIndex();
        VirtualReg res = RegisterFile.getVirtualReg(resRegId);
        if (res.getDefPos() < res.getLastUsePos()) {
            if (position instanceof GlobalVarRef) {
                String labelName = position.getName().substring(1);
                ArrayList<MipsInst> insts = RegisterFile.allocateToRegOrStack(resRegId, null);
                if (insts != null) {
                    mipsInsts.addAll(insts);
                    mipsInsts.add(new Lw("$v1", "0", "null", labelName));
                    mipsInsts.add(new Sw("$v1", res.getOffsetToFp() + "", "$fp"));
                } else {
                    mipsInsts.add(new Lw(res.getMappingRegName(), "0", "null", labelName));
                }
            } else {
                LocalVar pos = (LocalVar) position;
                VirtualReg posReg = RegisterFile.getVirtualReg(pos.getNameIndex());
                switch (posReg.getType()) {
                    case VarAddr -> {
                        if (posReg.getParamIndex() != -1) {
                            // 本地参数变量地址
                            MipsFunction mipsFunction = mipsModule.getFunction(functionName);
                            VirtualReg paramReg = mipsFunction.getParamVirtualReg(posReg.getParamIndex());
                            if (paramReg.getArgRegName() != null) {
                                mipsInsts.add(new Move("$v1", paramReg.getArgRegName()));
                            } else {
                                mipsInsts.add(new Lw("$v1", paramReg.getArgPosToFp() + "", "$fp"));
                            }
                        } else {
                            mipsInsts.add(new Lw("$v1", posReg.getVarPosToFp() + "", "$fp"));
                        }
                        ArrayList<MipsInst> insts = RegisterFile.allocateToRegOrStack(resRegId, null);
                        if (insts != null) {
                            mipsInsts.addAll(insts);
                            mipsInsts.add(new Sw("$v1", res.getOffsetToFp() + "", "$fp"));
                        } else {
                            mipsInsts.add(new Move(res.getMappingRegName(), "$v1"));
                        }
                    }
                    case ArrayEleAddr -> {
                        ArrayList<MipsInst> insts = RegisterFile.allocateToRegOrStack(resRegId, null);
                        if (insts != null) {
                            mipsInsts.addAll(insts);
                            mipsInsts.add(new Lw("$v1", posReg.getEleAddrPosToFp() + "", "$fp"));
                            mipsInsts.add(new Lw("$v1", "0", "$v1"));
                            mipsInsts.add(new Sw("$v1", res.getOffsetToFp() + "", "$fp"));
                        } else {
                            mipsInsts.add(new Lw("$v1", posReg.getEleAddrPosToFp() + "", "$fp"));
                            mipsInsts.add(new Lw(res.getMappingRegName(), "0", "$v1"));
                        }
                    }
                    case AddrOfArrayAddr -> res.setParamIndex(posReg.getParamIndex());
                }
            }
        }
    }

    private void translateStoreInst(Instruction inst, int index, ArrayList<MipsInst> mipsInsts,
                                    String functionName) {
        StoreInst storeInst = (StoreInst) inst;
        Value content = storeInst.getContent();
        Value position = storeInst.getPosition();
        if (position instanceof GlobalVarRef) {
            String labelName = position.getName().substring(1);
            if (content instanceof Literal) {
                int value = ((Literal) content).getInt();
                mipsInsts.add(new Li("$v1", value + ""));
                mipsInsts.add(new Sw("$v1", "0", "null", labelName));
            } else {
                VirtualReg contentReg = RegisterFile.getVirtualReg(((LocalVar) content).getNameIndex());
                if (contentReg.getMappingRegId() != -1) {
                    mipsInsts.add(new Sw(contentReg.getMappingRegName(), "0", "null", labelName));
                } else {
                    mipsInsts.add(new Lw("$v1", contentReg.getOffsetToFp() + "", "$fp"));
                    mipsInsts.add(new Sw("$v1", "0", "null", labelName));
                }
            }
            return;
        }
        VirtualReg posReg = RegisterFile.getVirtualReg(((LocalVar) position).getNameIndex());
        if (content.getName().startsWith("%p")) {
            int paramIndex = Integer.parseInt(content.getName().substring(2));
            posReg.setParamIndex(paramIndex);
        } else {
            switch (posReg.getType()) {
                case VarAddr -> {
                    if (content instanceof Literal) {
                        int value = ((Literal) content).getInt();
                        mipsInsts.add(new Li("$v1", value + ""));
                        if (posReg.getParamIndex() != -1) {
                            MipsFunction mipsFunction = mipsModule.getFunction(functionName);
                            VirtualReg paramReg = mipsFunction.getParamVirtualReg(posReg.getParamIndex());
                            if (paramReg.getArgRegName() != null) {
                                mipsInsts.add(new Move(paramReg.getArgRegName(), "$v1"));
                            } else {
                                mipsInsts.add(new Sw("$v1", paramReg.getArgPosToFp() + "", "$fp"));
                            }
                        } else {
                            mipsInsts.add(new Sw("$v1", posReg.getVarPosToFp() + "", "$fp"));
                        }
                    } else {
                        VirtualReg contentReg = RegisterFile.getVirtualReg(((LocalVar) content).getNameIndex());
                        if (contentReg.getMappingRegId() != -1) {
                            if (posReg.getParamIndex() != -1) {
                                MipsFunction mipsFunction = mipsModule.getFunction(functionName);
                                VirtualReg paramReg = mipsFunction.getParamVirtualReg(posReg.getParamIndex());
                                if (paramReg.getArgRegName() != null) {
                                    mipsInsts.add(new Move(paramReg.getArgRegName(), contentReg.getMappingRegName()));
                                } else {
                                    mipsInsts.add(new Sw(contentReg.getMappingRegName(), paramReg.getArgPosToFp() + "", "$fp"));
                                }
                            } else {
                                mipsInsts.add(new Sw(contentReg.getMappingRegName(), posReg.getVarPosToFp() + "", "$fp"));
                            }
                        } else {
                            mipsInsts.add(new Lw("$v1", contentReg.getOffsetToFp() + "", "$fp"));
                            if (posReg.getParamIndex() != -1) {
                                MipsFunction mipsFunction = mipsModule.getFunction(functionName);
                                VirtualReg paramReg = mipsFunction.getParamVirtualReg(posReg.getParamIndex());
                                if (paramReg.getArgRegName() != null) {
                                    mipsInsts.add(new Move(paramReg.getArgRegName(), "$v1"));
                                } else {
                                    mipsInsts.add(new Sw("$v1", paramReg.getArgPosToFp() + "", "$fp"));
                                }
                            } else {
                                mipsInsts.add(new Sw("$v1", posReg.getVarPosToFp() + "", "$fp"));
                            }
                        }
                    }
                }
                case ArrayEleAddr -> {
                    if (content instanceof Literal) {
                        int value = ((Literal) content).getInt();
                        mipsInsts.add(new Li("$v1", value + ""));
                        mipsInsts.add(new Lw("$v0", posReg.getEleAddrPosToFp() + "", "$fp"));
                        mipsInsts.add(new Sw("$v1", "0", "$v0"));
                    } else {
                        VirtualReg contentReg = RegisterFile.getVirtualReg(((LocalVar) content).getNameIndex());
                        if (contentReg.getMappingRegId() != -1) {
                            mipsInsts.add(new Lw("$v1", posReg.getEleAddrPosToFp() + "", "$fp"));
                            mipsInsts.add(new Sw(contentReg.getMappingRegName(), "0", "$v1"));
                        } else {
                            mipsInsts.add(new Lw("$v1", contentReg.getOffsetToFp() + "", "$fp"));
                            mipsInsts.add(new Lw("$v0", posReg.getEleAddrPosToFp() + "", "$fp"));
                            mipsInsts.add(new Sw("$v1", "0", "$v0"));
                        }
                    }
                }
            }
        }
        if (content instanceof LocalVar) {
            int id = ((LocalVar) content).getNameIndex();
            RegisterFile.tryToFreeReg(id, index);
        }
    }

    private void calcEleAddr(Value inx, ArrayList<MipsInst> mipsInsts) {
        // 基地址默认存储在$v1中，绝对地址结果也存储在$v1中
        if (inx instanceof Literal) {
            int value = ((Literal) inx).getInt();
            mipsInsts.add(new Li("$v0", value + ""));
            mipsInsts.add(new Sll("$v0", "$v0", "2"));
            mipsInsts.add(new Addu("$v1", "$v1", "$v0"));
        } else {
            VirtualReg inxReg = RegisterFile.getVirtualReg(((LocalVar) inx).getNameIndex());
            if (inxReg.getMappingRegId() != -1) {
                mipsInsts.add(new Sll("$v0", inxReg.getMappingRegName(), "2"));
                mipsInsts.add(new Addu("$v1", "$v1", "$v0"));
            } else {
                mipsInsts.add(new Lw("$v0", inxReg.getOffsetToFp() + "", "$fp"));
                mipsInsts.add(new Sll("$v0", "$v0", "2"));
                mipsInsts.add(new Addu("$v1", "$v1", "$v0"));
            }
        }
    }

    private void translateGepInst(Instruction inst, int index, ArrayList<MipsInst> mipsInsts, String functionName) {
        GetElePtrInst gepInst = (GetElePtrInst) inst;
        Value basePointer = gepInst.getBasePointer();
        ArrayList<Value> indices = gepInst.getIndices();
        Value inx = indices.get(indices.size() - 1);
        VirtualReg res = RegisterFile.getVirtualReg(((LocalVar) gepInst.getResult()).getNameIndex());
        if (basePointer instanceof GlobalVarRef) {
            String labelName = basePointer.getName().substring(1);
            mipsInsts.add(new La("$v1", labelName));
            calcEleAddr(inx, mipsInsts);
            res.setEleAddrPosToFp(RegisterFile.getOffsetToFp());
            mipsInsts.addAll(MacroBuilder.push("$v1"));
        } else {
            VirtualReg baseReg = RegisterFile.getVirtualReg(((LocalVar) basePointer).getNameIndex());
            switch (baseReg.getType()) {
                case ArrayAddr -> {
                    if (baseReg.getParamIndex() != -1) {
                        // 本地参数数组地址
                        MipsFunction mipsFunction = mipsModule.getFunction(functionName);
                        VirtualReg paramReg = mipsFunction.getParamVirtualReg(baseReg.getParamIndex());
                        if (paramReg.getArgRegName() != null) {
                            mipsInsts.add(new Move("$v1", paramReg.getArgRegName()));
                        } else {
                            mipsInsts.add(new Lw("$v1", paramReg.getArgPosToFp() + "", "$fp"));
                        }
                    } else {
                        // 计算绝对地址
                        mipsInsts.add(new Addiu("$v1", "$fp", baseReg.getArrayPosToFp() + ""));
                    }
                    calcEleAddr(inx, mipsInsts);
                    res.setEleAddrPosToFp(RegisterFile.getOffsetToFp());
                    mipsInsts.addAll(MacroBuilder.push("$v1"));
                }
                case ArrayEleAddr -> {
                    mipsInsts.add(new Lw("$v1", baseReg.getEleAddrPosToFp() + "", "$fp"));
                    calcEleAddr(inx, mipsInsts);
                    res.setEleAddrPosToFp(RegisterFile.getOffsetToFp());
                    mipsInsts.addAll(MacroBuilder.push("$v1"));
                }
            }
        }
        if (basePointer instanceof LocalVar) {
            int id = ((LocalVar) basePointer).getNameIndex();
            RegisterFile.tryToFreeReg(id, index);
        }
        for (Value i : indices) {
            if (i instanceof LocalVar) {
                int id = ((LocalVar) i).getNameIndex();
                RegisterFile.tryToFreeReg(id, index);
            }
        }
    }

    private void translateCallInst(Instruction inst, int index, ArrayList<MipsInst> mipsInsts,
                                   String functionName) {
        CallInst callInst = (CallInst) inst;
        String callFunctionName = callInst.getFunctionName();
        if (callFunctionName.equals("getint") || callFunctionName.equals("getchar")) {
            if (callFunctionName.equals("getint")) {
                mipsInsts.addAll(MacroBuilder.readInt("$v1"));
            } else {
                mipsInsts.addAll(MacroBuilder.readChar("$v1"));
            }
            VirtualReg res = RegisterFile.getVirtualReg(((LocalVar) callInst.getResult()).getNameIndex());
            if (res.getDefPos() < res.getLastUsePos()) {
                ArrayList<MipsInst> insts = RegisterFile.allocateToRegOrStack(res.getId(), null);
                if (insts != null) {
                    mipsInsts.addAll(insts);
                    mipsInsts.add(new Sw("$v1", res.getOffsetToFp() + "", "$fp"));
                } else {
                    mipsInsts.add(new Move(res.getMappingRegName(), "$v1"));
                }
            }
            return;
        }
        // 调用者保存寄存器
        int recordOffsetToSp = RegisterFile.getOffsetToFp();
        mipsInsts.addAll(MacroBuilder.push("$ra"));
        // 三个参数寄存器
        int argRegNum = callInst.getArgs().size() > 3 ? 3 : callInst.getArgs().size();
        for (int i = 0; i < argRegNum; i++) {
            mipsInsts.addAll(MacroBuilder.push(RegisterFile.getRegName(5 + i)));
        }
        // 所有符合条件的虚拟寄存器，所映射的物理寄存器值全部存入栈中
        for (VirtualReg reg : RegisterFile.getAllVirtualRegs()) {
            if (reg.getDefPos() < index && reg.getLastUsePos() > index) {
                if (reg.isAllocated() && reg.getMappingRegId() != -1) {
                    String regName = reg.getMappingRegName();
                    mipsInsts.addAll(MacroBuilder.push(regName));
                }
            }
        }
        // 装载参数
        int argNum = 0;
        for (Value arg : callInst.getArgs()) {
            if (arg instanceof Literal) {
                mipsInsts.add(new Li("$v1", ((Literal) arg).getInt() + ""));
            } else {
                VirtualReg argReg = RegisterFile.getVirtualReg(((LocalVar) arg).getNameIndex());
                switch (argReg.getType()) {
                    case VarValue -> {
                        if (argReg.getMappingRegId() != -1) {
                            mipsInsts.add(new Move("$v1", argReg.getMappingRegName()));
                        } else {
                            mipsInsts.add(new Lw("$v1", argReg.getOffsetToFp() + "", "$fp"));
                        }
                    }
                    case ArrayAddr -> {
                        // 必定为本地参数数组地址
                        MipsFunction mipsFunction = mipsModule.getFunction(functionName);
                        VirtualReg paramReg = mipsFunction.getParamVirtualReg(argReg.getParamIndex());
                        if (paramReg.getArgRegName() != null) {
                            mipsInsts.add(new Move("$v1", paramReg.getArgRegName()));
                        } else {
                            mipsInsts.add(new Lw("$v1", paramReg.getArgPosToFp() + "", "$fp"));
                        }
                    }
                    case ArrayEleAddr -> mipsInsts.add(new Lw("$v1", argReg.getEleAddrPosToFp() + "", "$fp"));
                }
            }
            MipsFunction mipsCallFunction = mipsModule.getFunction(callFunctionName);
            VirtualReg paramReg = mipsCallFunction.getParamVirtualReg(argNum);
            if (argNum < 3) {
                paramReg.setArgRegName("$a" + (argNum + 1));
                mipsInsts.add(new Move(RegisterFile.getRegName(5 + argNum), "$v1"));
            } else {
                mipsInsts.add(new Sw("$v1", 0x550 + paramReg.getArgPosToFp() + "", "$fp"));
            }
            argNum++;
        }
        // 调用函数
        // $fp需要增加一定值以开辟空间，应对递归调用
        mipsInsts.add(new Addiu("$fp", "$fp", 0x550 + ""));
        mipsInsts.add(new Jal(callFunctionName));
        mipsInsts.add(new Addiu("$fp", "$fp", (-0x550) + ""));
        // 获取返回值（存储在$v1中）
        if (callInst.getResult() != null) {
            VirtualReg res = RegisterFile.getVirtualReg(((LocalVar) callInst.getResult()).getNameIndex());
            if (res.getDefPos() < res.getLastUsePos()) {
                ArrayList<MipsInst> insts = RegisterFile.allocateToRegOrStack(res.getId(), null);
                if (insts != null) {
                    mipsInsts.addAll(insts);
                    mipsInsts.add(new Sw("$v1", res.getOffsetToFp() + "", "$fp"));
                } else {
                    mipsInsts.add(new Move(res.getMappingRegName(), "$v1"));
                }
            }
        }
        // 恢复调用者保存的寄存器
        mipsInsts.add(new Lw("$ra", recordOffsetToSp + "", "$fp"));
        recordOffsetToSp += 4;
        for (int i = 0; i < argRegNum; i++) {
            mipsInsts.add(new Lw(RegisterFile.getRegName(5 + i), recordOffsetToSp + "", "$fp"));
            recordOffsetToSp += 4;
        }
        for (VirtualReg reg : RegisterFile.getAllVirtualRegs()) {
            if (reg.getDefPos() < index && reg.getLastUsePos() > index) {
                if (reg.isAllocated() && reg.getMappingRegId() != -1) {
                    String regName = reg.getMappingRegName();
                    mipsInsts.add(new Lw(regName, recordOffsetToSp + "", "$fp"));
                    recordOffsetToSp += 4;
                }
            }
        }
        // 释放参数
        for (Value arg : callInst.getArgs()) {
            if (arg instanceof LocalVar) {
                int id = ((LocalVar) arg).getNameIndex();
                RegisterFile.tryToFreeReg(id, index);
            }
        }
    }


    private boolean loadValueToReg(Value value, String desReg, ArrayList<MipsInst> mipsInsts) {
        // 将value的值加载到desReg中，value只能是立即数或普通虚拟寄存器
        boolean instAdded = false;
        if (value instanceof Literal) {
            int val = ((Literal) value).getInt();
            mipsInsts.add(new Li(desReg, val + ""));
            instAdded = true;
        } else {
            VirtualReg reg = RegisterFile.getVirtualReg(((LocalVar) value).getNameIndex());
            if (reg.getMappingRegId() != -1) {
                String regName = reg.getMappingRegName();
                if (!regName.equals(desReg)) {
                    mipsInsts.add(new Move(desReg, regName));
                    instAdded = true;
                }
            } else {
                mipsInsts.add(new Lw(desReg, reg.getOffsetToFp() + "", "$fp"));
                instAdded = true;
            }
        }
        return instAdded;
    }

    private boolean optimizeMulti(ArrayList<MipsInst> insts, boolean isLeftImm, boolean isRightImm, String targetReg,
                                  boolean hasLoadInst, String leftOrRightReg, int leftOrRightImm) {
        // 优化2：乘法优化
        int index = insts.size() - 1;
        if (isLeftImm && isRightImm) {
            // 乘法操作的左、右操作数都是立即数，可以立即计算结果
            MipsInst loadLeft = insts.get(index - 1);
            int left = Integer.parseInt(((Li) loadLeft).getImm());
            MipsInst loadRight = insts.get(index);
            int right = Integer.parseInt(((Li) loadRight).getImm());
            int result = left * right;
            MipsInst li = new Li(targetReg, String.valueOf(result));
            insts.set(index - 1, li);
            insts.remove(index);
            return true;
        } else if (isLeftImm || isRightImm) {
            // imm为1、-1或0时，特殊处理
            if (leftOrRightImm == 1) {
                MipsInst move = new Move(targetReg, leftOrRightReg);
                if (isLeftImm) {
                    if (hasLoadInst) {
                        insts.add(move);
                        insts.remove(index - 1);
                    } else {
                        insts.set(index, move);
                    }
                } else {
                    insts.set(index, move);
                }
                return true;
            } else if (leftOrRightImm == -1) {
                MipsInst sub = new Subu(targetReg, "$zero", leftOrRightReg);
                if (isLeftImm) {
                    if (hasLoadInst) {
                        insts.add(sub);
                        insts.remove(index - 1);
                    } else {
                        insts.set(index, sub);
                    }
                } else {
                    insts.set(index, sub);
                }
                return true;
            } else if (leftOrRightImm == 0) {
                MipsInst li = new Move(targetReg, "$zero");
                if (isLeftImm) {
                    if (hasLoadInst) {
                        insts.add(li);
                        insts.remove(index - 1);
                    } else {
                        insts.set(index, li);
                    }
                } else {
                    insts.set(index, li);
                }
                return true;
            }
            // 乘数为2的幂次方时，可以用移位操作替代乘法操作
            // 如果imm是负数，先取正值
            boolean isNegative = false;
            if (leftOrRightImm < 0) {
                isNegative = true;
                leftOrRightImm = -leftOrRightImm;
            }
            if ((leftOrRightImm & (leftOrRightImm - 1)) == 0) {
                int shift = 0;
                while (leftOrRightImm > 1) {
                    leftOrRightImm >>= 1;
                    shift++;
                }
                MipsInst sll = new Sll(targetReg, leftOrRightReg, String.valueOf(shift));
                if (isLeftImm) {
                    if (hasLoadInst) {
                        insts.add(sll);
                        insts.remove(index - 1);
                    } else {
                        insts.set(index, sll);
                    }
                } else {
                    insts.set(index, sll);
                }
                if (isNegative) {
                    // 如果imm是负数，需要将结果取负
                    MipsInst sub = new Subu(targetReg, "$zero", targetReg);
                    insts.add(sub);
                }
                return true;
            }
        }
        return false;
    }

    public static long[] choose_multiplier(int d, int prec) {
        long l = (long) Math.ceil((Math.log(d) / Math.log(2)));
        long sh = l;
        long m_low = (long) Math.floor(Math.pow(2, 32 + l) / d);
        long m_high = (long) Math.floor((Math.pow(2, 32 + l) + Math.pow(2, 32 + l - prec)) / d);
        while ((Math.floor(m_low >> 1) < Math.floor(m_high >> 1)) && sh > 0) {
            m_low = (long) Math.floor(m_low >> 1);
            m_high = (long) Math.floor(m_high >> 1);
            sh = sh - 1;
        }
        return new long[]{m_high, sh};
    }

    private boolean optimizeDiv(ArrayList<MipsInst> insts, boolean isLeftImm, boolean isRightImm, String targetReg,
                                String leftOrRightReg, int leftOrRightImm) {
        // 优化3：除法优化
        int index = insts.size() - 1;
        if (isLeftImm && isRightImm) {
            // 除法操作的左、右操作数都是立即数，可以立即计算结果
            MipsInst loadLeft = insts.get(index - 1);
            int left = Integer.parseInt(((Li) loadLeft).getImm());
            MipsInst loadRight = insts.get(index);
            int right = Integer.parseInt(((Li) loadRight).getImm());
            int result = left / right;
            MipsInst li = new Li(targetReg, String.valueOf(result));
            insts.set(index - 1, li);
            insts.remove(index);
            return true;
        } else if (isRightImm) {
            // 除法操作的右操作数是立即数，可以用乘法和移位操作替代除法操作
            // 分成两种情况：除数的绝对值为2的幂次方和除数不为2的幂次方
            // imm为1或者-1时，特殊处理
            if (leftOrRightImm == 1) {
                MipsInst move = new Move(targetReg, leftOrRightReg);
                insts.set(index, move);
                return true;
            } else if (leftOrRightImm == -1) {
                MipsInst sub = new Subu(targetReg, "$zero", leftOrRightReg);
                insts.set(index, sub);
                return true;
            }
            boolean isNegative = false;
            if (leftOrRightImm < 0) {
                isNegative = true;
                leftOrRightImm = -leftOrRightImm;
            }
            int padding = leftOrRightImm - 1;
            if ((leftOrRightImm & (leftOrRightImm - 1)) == 0) {
                // 除数为2的幂次方时，可以用移位操作替代除法操作
                int shift = 0;
                while (leftOrRightImm > 1) {
                    leftOrRightImm >>= 1;
                    shift++;
                }
                // 需要根据除法表达式（除数、被除数）的正负来决定是否加上padding
                // 如果整个表达式有负号，需要加上padding
                MipsInst sra1 = new Sra("$v1", leftOrRightReg, "31");
                insts.set(index, sra1);
                if (isNegative) {
                    insts.add(new Not("$v1", "$v1"));
                }
                MipsInst andi = new Andi("$v1", "$v1", String.valueOf(padding));
                MipsInst add = new Addu(targetReg, leftOrRightReg, "$v1");
                MipsInst sra2 = new Sra(targetReg, targetReg, String.valueOf(shift));
                insts.add(andi);
                insts.add(add);
                insts.add(sra2);
            } else {
                // 除数不为2的幂次方时，用乘法和移位操作替代除法操作
                // 计算出合适的magic number和shift number
                // 除数不为2的幂次方时，用乘法和移位操作替代除法操作
                // 计算出合适的magic number和shift number
                long[] result = choose_multiplier(leftOrRightImm, 31);
                long m = result[0];
                long sh = result[1];
                MipsInst li;
                if (m >= (1L << 31)) {
                    li = new Li("$v1", String.valueOf((int) (m - Math.pow(2, 32))));
                } else {
                    li = new Li("$v1", String.valueOf(m));
                }
                insts.set(index, li);
                MipsInst mult = new Mult(leftOrRightReg, "$v1");
                insts.add(mult);
                MipsInst mfhi = new Mfhi(targetReg);
                insts.add(mfhi);
                if (m >= (1L << 31)) {
                    insts.add(new Addu(targetReg, targetReg, leftOrRightReg));
                }
                MipsInst srl = new Srl("$v1", leftOrRightReg, String.valueOf(31));
                insts.add(srl);
                if (isNegative) {
                    insts.add(new Not("$v1", "$v1"));
                }
                MipsInst sra = new Sra(targetReg, targetReg, String.valueOf(sh));
                MipsInst add = new Addu(targetReg, targetReg, "$v1");
                insts.add(sra);
                insts.add(add);
            }
            return true;
        }
        return false;
    }

    private boolean optimizeMod(ArrayList<MipsInst> insts, boolean isLeftImm, boolean isRightImm, String targetReg,
                                String leftOrRightReg, int leftOrRightImm) {
        // 优化4：取模优化
        int index = insts.size() - 1;
        if (isLeftImm && isRightImm) {
            // 取模操作的左、右操作数都是立即数，可以立即计算结果
            MipsInst loadLeft = insts.get(index - 1);
            int left = Integer.parseInt(((Li) loadLeft).getImm());
            MipsInst loadRight = insts.get(index);
            int right = Integer.parseInt(((Li) loadRight).getImm());
            int result = left % right;
            MipsInst li = new Li(targetReg, String.valueOf(result));
            insts.set(index - 1, li);
            insts.remove(index);
            return true;
        } else if (isRightImm) {
            // 取模操作的右操作数是立即数，可以用按位操作来代替取模操作
            // 同时要求：模数为1、-1或2的幂次方正值
            if (leftOrRightImm == 1 || leftOrRightImm == -1) {
                MipsInst move = new Move(targetReg, "$zero");
                insts.set(index, move);
                return true;
            }
            if (leftOrRightImm < 0 || (leftOrRightImm & (leftOrRightImm - 1)) != 0) {
                return false;
            }
            int andNum = leftOrRightImm - 1;
            // 被除数先算术右移31位
            MipsInst sra = new Sra("$v1", leftOrRightReg, "31");
            // 再逻辑右移32-shift位
            int shift = 0;
            while (leftOrRightImm > 1) {
                leftOrRightImm >>= 1;
                shift++;
            }
            MipsInst srl = new Srl("$v1", "$v1", String.valueOf(32 - shift));
            MipsInst add = new Addu(leftOrRightReg, leftOrRightReg, "$v1");
            // 与操作
            MipsInst andi = new Andi(targetReg, leftOrRightReg, String.valueOf(andNum));
            MipsInst sub = new Subu(targetReg, targetReg, "$v1");
            insts.set(index, sra);
            insts.add(srl);
            insts.add(add);
            insts.add(andi);
            insts.add(sub);
            return true;
        }
        return false;
    }

    private boolean optimizeConstCompute(ArrayList<MipsInst> insts, boolean isLeftImm, boolean isRightImm, String targetReg,
                                         String opType) {
        // 优化5：常数计算优化
        int index = insts.size() - 1;
        if (isLeftImm && isRightImm) {
            // 除法操作的左、右操作数都是立即数，可以立即计算结果
            MipsInst loadLeft = insts.get(index - 1);
            int left = Integer.parseInt(((Li) loadLeft).getImm());
            MipsInst loadRight = insts.get(index);
            int right = Integer.parseInt(((Li) loadRight).getImm());
            int result = 0;
            switch (opType) {
                case "add" -> result = left + right;
                case "sub" -> result = left - right;
                case "seq" -> result = left == right ? 1 : 0;
                case "sne" -> result = left != right ? 1 : 0;
                case "slt" -> result = left < right ? 1 : 0;
                case "sle" -> result = left <= right ? 1 : 0;
                case "sgt" -> result = left > right ? 1 : 0;
                case "sge" -> result = left >= right ? 1 : 0;
            }
            MipsInst li = new Li(targetReg, String.valueOf(result));
            insts.set(index - 1, li);
            insts.remove(index);
            return true;
        }
        return false;
    }

    private void translateBinaryInst(Instruction inst, int index, ArrayList<MipsInst> mipsInsts) {
        BinaryInst binaryInst = (BinaryInst) inst;
        VirtualReg res = RegisterFile.getVirtualReg(((LocalVar) binaryInst.getResult()).getNameIndex());
        Value left = binaryInst.getLeft();
        String leftDesReg = "$v1";
        boolean isLeftImm = false;
        int leftImm = 0;
        if (left instanceof LocalVar) {
            int id = ((LocalVar) left).getNameIndex();
            VirtualReg leftReg = RegisterFile.getVirtualReg(id);
            if (leftReg.getMappingRegId() != -1) {
                leftDesReg = leftReg.getMappingRegName();
            }
        } else {
            leftImm = ((Literal) left).getInt();
            isLeftImm = true;
        }
        Value right = binaryInst.getRight();
        String rightDesReg = "$v0";
        int rightImm = 0;
        boolean isRightImm = false;
        if (right instanceof LocalVar) {
            int id = ((LocalVar) right).getNameIndex();
            VirtualReg rightReg = RegisterFile.getVirtualReg(id);
            if (rightReg.getMappingRegId() != -1) {
                rightDesReg = rightReg.getMappingRegName();
            }
        } else {
            isRightImm = true;
            rightImm = ((Literal) right).getInt();
        }
        String leftOrRightReg = isLeftImm ? rightDesReg : leftDesReg;
        int leftOrRightImm = isLeftImm ? leftImm : rightImm;
        if (res.getDefPos() < res.getLastUsePos()) {
            boolean leftAdded = loadValueToReg(left, leftDesReg, mipsInsts);
            boolean rightAdded = loadValueToReg(right, rightDesReg, mipsInsts);
            boolean hasLoadInst = isLeftImm ? rightAdded : leftAdded;
            boolean isInStack = false;
            ArrayList<MipsInst> insts = RegisterFile.allocateToRegOrStack(res.getId(), null);
            if (insts != null) {
                mipsInsts.addAll(insts);
                isInStack = true;
            }
            switch (binaryInst.getOpType()) {
                case ADD -> {
                    String opType = "add";
                    String targetReg = isInStack ? "$v1" : res.getMappingRegName();
                    boolean optimized = false;
                    if (isOptOpen) {
                        optimized = optimizeConstCompute(mipsInsts, isLeftImm, isRightImm, targetReg, opType);
                    }
                    if (isInStack) {
                        if (!optimized) {
                            mipsInsts.add(new Addu("$v1", leftDesReg, rightDesReg));
                        }
                        mipsInsts.add(new Sw(targetReg, res.getOffsetToFp() + "", "$fp"));
                    } else {
                        if (!optimized) {
                            mipsInsts.add(new Addu(targetReg, leftDesReg, rightDesReg));
                        }
                    }
                }
                case SUB -> {
                    String opType = "sub";
                    String targetReg = isInStack ? "$v1" : res.getMappingRegName();
                    boolean optimized = false;
                    if (isOptOpen) {
                        optimized = optimizeConstCompute(mipsInsts, isLeftImm, isRightImm, targetReg, opType);
                    }
                    if (isInStack) {
                        if (!optimized) {
                            mipsInsts.add(new Subu("$v1", leftDesReg, rightDesReg));
                        }
                        mipsInsts.add(new Sw(targetReg, res.getOffsetToFp() + "", "$fp"));
                    } else {
                        if (!optimized) {
                            mipsInsts.add(new Subu(targetReg, leftDesReg, rightDesReg));
                        }
                    }
                }
                case MUL -> {
                    String targetReg = isInStack ? "$v1" : res.getMappingRegName();
                    boolean optimized = false;
                    if (isOptOpen) {
                        optimized = optimizeMulti(mipsInsts, isLeftImm, isRightImm, targetReg,
                                hasLoadInst, leftOrRightReg, leftOrRightImm);
                    }
                    if (isInStack) {
                        if (!optimized) {
                            mipsInsts.add(new Mult(leftDesReg, rightDesReg));
                            mipsInsts.add(new Mflo("$v1"));
                        }
                        mipsInsts.add(new Sw(targetReg, res.getOffsetToFp() + "", "$fp"));
                    } else {
                        if (!optimized) {
                            mipsInsts.add(new Mult(leftDesReg, rightDesReg));
                            mipsInsts.add(new Mflo(targetReg));
                        }
                    }
                }
                case SDIV -> {
                    String targetReg = isInStack ? "$v1" : res.getMappingRegName();
                    boolean optimized = false;
                    if (isOptOpen) {
                        optimized = optimizeDiv(mipsInsts, isLeftImm, isRightImm, targetReg,
                                leftOrRightReg, leftOrRightImm);
                    }
                    if (isInStack) {
                        if (!optimized) {
                            mipsInsts.add(new Div(leftDesReg, rightDesReg));
                            mipsInsts.add(new Mflo("$v1"));
                        }
                        mipsInsts.add(new Sw(targetReg, res.getOffsetToFp() + "", "$fp"));
                    } else {
                        if (!optimized) {
                            mipsInsts.add(new Div(leftDesReg, rightDesReg));
                            mipsInsts.add(new Mflo(targetReg));
                        }
                    }
                }
                case SREM -> {
                    String targetReg = isInStack ? "$v1" : res.getMappingRegName();
                    boolean optimized = false;
                    if (isOptOpen) {
                        optimized = optimizeMod(mipsInsts, isLeftImm, isRightImm, targetReg,
                                leftOrRightReg, leftOrRightImm);
                    }
                    if (isInStack) {
                        if (!optimized) {
                            mipsInsts.add(new Div(leftDesReg, rightDesReg));
                            mipsInsts.add(new Mfhi("$v1"));
                        }
                        mipsInsts.add(new Sw(targetReg, res.getOffsetToFp() + "", "$fp"));
                    } else {
                        if (!optimized) {
                            mipsInsts.add(new Div(leftDesReg, rightDesReg));
                            mipsInsts.add(new Mfhi(targetReg));
                        }
                    }
                }
            }
        }
        if (left instanceof LocalVar) {
            int id = ((LocalVar) left).getNameIndex();
            RegisterFile.tryToFreeReg(id, index);
        }
        if (right instanceof LocalVar) {
            int id = ((LocalVar) right).getNameIndex();
            RegisterFile.tryToFreeReg(id, index);
        }
    }

    private void translateBranchInst(Instruction inst, int index, ArrayList<MipsInst> mipsInsts,
                                     String functionName) {
        BranchInst branchInst = (BranchInst) inst;
        Value condition = branchInst.getCondition();
        if (condition != null) {
            VirtualReg conditionReg = RegisterFile.getVirtualReg(((LocalVar) condition).getNameIndex());
            String trueLabel = functionName + "_" + branchInst.getTrueLabel();
            String falseLabel = functionName + "_" + branchInst.getFalseLabel();
            if (conditionReg.getMappingRegId() != -1) {
                mipsInsts.add(new Beq(conditionReg.getMappingRegName(), "1", trueLabel));
                mipsInsts.add(new Beq(conditionReg.getMappingRegName(), "$zero", falseLabel));
            } else {
                mipsInsts.add(new Lw("$v1", conditionReg.getOffsetToFp() + "", "$fp"));
                mipsInsts.add(new Beq("$v1", "1", trueLabel));
                mipsInsts.add(new Beq("$v1", "$zero", falseLabel));
            }
        } else {
            mipsInsts.add(new J(functionName + "_" + branchInst.getTargetLabel()));
        }
        if (condition != null) {
            int id = ((LocalVar) condition).getNameIndex();
            RegisterFile.tryToFreeReg(id, index);
        }
    }

    private void translateCompareInst(Instruction inst, int index, ArrayList<MipsInst> mipsInsts) {
        IcmpInst icmpInst = (IcmpInst) inst;
        VirtualReg res = RegisterFile.getVirtualReg(((LocalVar) icmpInst.getResult()).getNameIndex());
        Value op1 = icmpInst.getOp1();
        String op1DesReg = "$v1";
        boolean isOp1Imm = false;
        if (op1 instanceof LocalVar) {
            int id = ((LocalVar) op1).getNameIndex();
            VirtualReg op1Reg = RegisterFile.getVirtualReg(id);
            if (op1Reg.getMappingRegId() != -1) {
                op1DesReg = op1Reg.getMappingRegName();
            }
        }else{
            isOp1Imm = true;
        }
        Value op2 = icmpInst.getOp2();
        String op2DesReg = "$v0";
        boolean isOp2Imm = false;
        if (op2 instanceof LocalVar) {
            int id = ((LocalVar) op2).getNameIndex();
            VirtualReg op2Reg = RegisterFile.getVirtualReg(id);
            if (op2Reg.getMappingRegId() != -1) {
                op2DesReg = op2Reg.getMappingRegName();
            }
        }else{
            isOp2Imm = true;
        }
        if (res.getDefPos() < res.getLastUsePos()) {
            loadValueToReg(op1, op1DesReg, mipsInsts);
            loadValueToReg(op2, op2DesReg, mipsInsts);
            boolean isInStack = false;
            ArrayList<MipsInst> insts = RegisterFile.allocateToRegOrStack(res.getId(), null);
            if (insts != null) {
                mipsInsts.addAll(insts);
                isInStack = true;
            }
            switch (icmpInst.getCmpType()) {
                case EQ -> {
                    String opType = "seq";
                    String targetReg = isInStack ? "$v1" : res.getMappingRegName();
                    boolean optimized = false;
                    if (isOptOpen) {
                        optimized = optimizeConstCompute(mipsInsts, isOp1Imm, isOp2Imm, targetReg, opType);
                    }
                    if (isInStack) {
                        if (!optimized) {
                            mipsInsts.add(new Seq("$v1", op1DesReg, op2DesReg));
                        }
                        mipsInsts.add(new Sw(targetReg, res.getOffsetToFp() + "", "$fp"));
                    } else {
                        if (!optimized) {
                            mipsInsts.add(new Seq(targetReg, op1DesReg, op2DesReg));
                        }
                    }
                }
                case NE -> {
                    String opType = "sne";
                    String targetReg = isInStack ? "$v1" : res.getMappingRegName();
                    boolean optimized = false;
                    if (isOptOpen) {
                        optimized = optimizeConstCompute(mipsInsts, isOp1Imm, isOp2Imm, targetReg, opType);
                    }
                    if (isInStack) {
                        if (!optimized) {
                            mipsInsts.add(new Sne("$v1", op1DesReg, op2DesReg));
                        }
                        mipsInsts.add(new Sw(targetReg, res.getOffsetToFp() + "", "$fp"));
                    } else {
                        if (!optimized) {
                            mipsInsts.add(new Sne(targetReg, op1DesReg, op2DesReg));
                        }
                    }
                }
                case SGT -> {
                    String opType = "sgt";
                    String targetReg = isInStack ? "$v1" : res.getMappingRegName();
                    boolean optimized = false;
                    if (isOptOpen) {
                        optimized = optimizeConstCompute(mipsInsts, isOp1Imm, isOp2Imm, targetReg, opType);
                    }
                    if (isInStack) {
                        if (!optimized) {
                            mipsInsts.add(new Sgt("$v1", op1DesReg, op2DesReg));
                        }
                        mipsInsts.add(new Sw(targetReg, res.getOffsetToFp() + "", "$fp"));
                    } else {
                        if (!optimized) {
                            mipsInsts.add(new Sgt(targetReg, op1DesReg, op2DesReg));
                        }
                    }
                }
                case SGE -> {
                    String opType = "sge";
                    String targetReg = isInStack ? "$v1" : res.getMappingRegName();
                    boolean optimized = false;
                    if (isOptOpen) {
                        optimized = optimizeConstCompute(mipsInsts, isOp1Imm, isOp2Imm, targetReg, opType);
                    }
                    if (isInStack) {
                        if (!optimized) {
                            mipsInsts.add(new Sge("$v1", op1DesReg, op2DesReg));
                        }
                        mipsInsts.add(new Sw(targetReg, res.getOffsetToFp() + "", "$fp"));
                    } else {
                        if (!optimized) {
                            mipsInsts.add(new Sge(targetReg, op1DesReg, op2DesReg));
                        }
                    }
                }
                case SLT -> {
                    String opType = "slt";
                    String targetReg = isInStack ? "$v1" : res.getMappingRegName();
                    boolean optimized = false;
                    if (isOptOpen) {
                        optimized = optimizeConstCompute(mipsInsts, isOp1Imm, isOp2Imm, targetReg, opType);
                    }
                    if (isInStack) {
                        if (!optimized) {
                            mipsInsts.add(new Slt("$v1", op1DesReg, op2DesReg));
                        }
                        mipsInsts.add(new Sw(targetReg, res.getOffsetToFp() + "", "$fp"));
                    } else {
                        if (!optimized) {
                            mipsInsts.add(new Slt(targetReg, op1DesReg, op2DesReg));
                        }
                    }
                }
                case SLE -> {
                    String opType = "sle";
                    String targetReg = isInStack ? "$v1" : res.getMappingRegName();
                    boolean optimized = false;
                    if (isOptOpen) {
                        optimized = optimizeConstCompute(mipsInsts, isOp1Imm, isOp2Imm, targetReg, opType);
                    }
                    if (isInStack) {
                        if (!optimized) {
                            mipsInsts.add(new Sle("$v1", op1DesReg, op2DesReg));
                        }
                        mipsInsts.add(new Sw(targetReg, res.getOffsetToFp() + "", "$fp"));
                    } else {
                        if (!optimized) {
                            mipsInsts.add(new Sle(targetReg, op1DesReg, op2DesReg));
                        }
                    }
                }
            }
        }
        if (op1 instanceof LocalVar) {
            int id = ((LocalVar) op1).getNameIndex();
            RegisterFile.tryToFreeReg(id, index);
        }
        if (op2 instanceof LocalVar) {
            int id = ((LocalVar) op2).getNameIndex();
            RegisterFile.tryToFreeReg(id, index);
        }
    }

    private void translateReturnInst(Instruction inst, int index, ArrayList<MipsInst> mipsInsts,
                                     String funcName) {
        if (funcName.equals("main")) {
            mipsInsts.addAll(MacroBuilder.exit());
            return;
        }
        ReturnInst returnInst = (ReturnInst) inst;
        Value returnValue = returnInst.getReturnValue();
        if (!(returnValue.getType() instanceof VoidType)) {
            loadValueToReg(returnValue, "$v1", mipsInsts);
        }
        mipsInsts.add(new Jr());
        if (returnValue instanceof LocalVar) {
            int id = ((LocalVar) returnValue).getNameIndex();
            RegisterFile.tryToFreeReg(id, index);
        }
    }

    private void translatePrintInst(Instruction inst, int index, ArrayList<MipsInst> mipsInsts) {
        PrintInst printInst = (PrintInst) inst;
        Value printValue = printInst.getPrintValue();
        switch (printInst.getPrintType()) {
            case "putstr" -> {
                String stringName = printInst.getStringName().substring(1);
                mipsInsts.add(new La("$v1", stringName));
                mipsInsts.addAll(MacroBuilder.printString("$v1"));
            }
            case "putint" -> {
                loadValueToReg(printValue, "$v1", mipsInsts);
                mipsInsts.addAll(MacroBuilder.printInt("$v1"));
            }
            case "putch" -> {
                loadValueToReg(printValue, "$v1", mipsInsts);
                mipsInsts.addAll(MacroBuilder.printChar("$v1"));
            }
        }
        if (printValue != null && printValue instanceof LocalVar) {
            int id = ((LocalVar) printValue).getNameIndex();
            RegisterFile.tryToFreeReg(id, index);
        }
    }

    private void translateZextInst(Instruction inst, int index, ArrayList<MipsInst> mipsInsts) {
        // mips中没有zext指令，直接将源操作数寄存器和结果寄存器映射到同一位置即可
        ZextInst zextInst = (ZextInst) inst;
        VirtualReg res = RegisterFile.getVirtualReg(((LocalVar) zextInst.getResult()).getNameIndex());
        Value src = zextInst.getSrc();
        if (src instanceof LocalVar) {
            VirtualReg srcReg = RegisterFile.getVirtualReg(((LocalVar) src).getNameIndex());
            if (srcReg.isAllocated()) {
                if (srcReg.getMappingRegId() != -1) {
                    res.setMappingRegId(srcReg.getMappingRegId());
                } else {
                    res.setOffsetToFp(srcReg.getOffsetToFp());
                }
            }
        } else if (src instanceof Literal) {
            // src是立即数
            if (res.getDefPos() < res.getLastUsePos()) {
                int value = ((Literal) src).getInt();
                ArrayList<MipsInst> insts = RegisterFile.allocateToRegOrStack(res.getId(), value + "");
                if (insts != null) {
                    mipsInsts.addAll(insts);
                } else {
                    mipsInsts.add(new Li(res.getMappingRegName(), value + ""));
                }
            }
        }
    }

    private void translateTruncInst(Instruction inst, int index, ArrayList<MipsInst> mipsInsts) {
        TruncInst truncInst = (TruncInst) inst;
        VirtualReg res = RegisterFile.getVirtualReg(((LocalVar) truncInst.getResult()).getNameIndex());
        Value src = truncInst.getSrc();
        ValueType destType = truncInst.getDestType();
        int destBitWidth = ((IntegerType) destType).getBitWidth();
        if (res.getDefPos() < res.getLastUsePos()) {
            ArrayList<MipsInst> insts = RegisterFile.allocateToRegOrStack(res.getId(), null);
            // 将截断后的值保存到$v1中
            if (src instanceof LocalVar) {
                VirtualReg srcReg = RegisterFile.getVirtualReg(((LocalVar) src).getNameIndex());
                if (srcReg.getMappingRegId() != -1) {
                    mipsInsts.add(new Move("$v1", srcReg.getMappingRegName()));
                } else {
                    mipsInsts.add(new Lw("$v1", srcReg.getOffsetToFp() + "", "$fp"));
                }
                // 用andi指令进行截断
                mipsInsts.add(new Andi("$v1", "$v1", ((1 << destBitWidth) - 1) + ""));
            } else if (src instanceof Literal) {
                int value = ((Literal) src).getInt();
                int truncValue = value & ((1 << destBitWidth) - 1);
                mipsInsts.add(new Li("$v1", truncValue + ""));
            }
            if (insts != null) {
                mipsInsts.addAll(insts);
                mipsInsts.add(new Sw("$v1", res.getOffsetToFp() + "", "$fp"));
            } else {
                mipsInsts.add(new Move(res.getMappingRegName(), "$v1"));
            }
        }
        if (src instanceof LocalVar) {
            int id = ((LocalVar) src).getNameIndex();
            RegisterFile.tryToFreeReg(id, index);
        }
    }

    private ArrayList<MipsInst> translateToMipsInsts(Instruction inst, int index, boolean isEntryBlock,
                                                     String functionName) {
        ArrayList<MipsInst> mipsInsts = new ArrayList<>();
        switch (inst.getInstType()) {
            case Allocate -> translateAllocateInst(inst, mipsInsts, isEntryBlock);
            case Load -> translateLoadInst(inst, mipsInsts, functionName);
            case Store -> translateStoreInst(inst, index, mipsInsts, functionName);
            case GetElementPtr -> translateGepInst(inst, index, mipsInsts, functionName);
            case Call -> translateCallInst(inst, index, mipsInsts, functionName);
            case Binary -> translateBinaryInst(inst, index, mipsInsts);
            case Branch -> translateBranchInst(inst, index, mipsInsts, functionName);
            case Icmp -> translateCompareInst(inst, index, mipsInsts);
            case Return -> translateReturnInst(inst, index, mipsInsts, functionName);
            case Print -> translatePrintInst(inst, index, mipsInsts);
            case Zext -> translateZextInst(inst, index, mipsInsts);
            case Trunc -> translateTruncInst(inst, index, mipsInsts);
        }
        return mipsInsts;
    }

    private void translateToMipsBlock(BasicBlock basicBlock, MipsBlock mipsBlock,
                                      ArrayList<Instruction> instructions, String functionName) {
        boolean isEntryBlock = basicBlock.getName().equals("entry");
        for (Instruction instruction : basicBlock.getInstructions()) {
            int index = checkIndex(instructions, instruction);
            mipsBlock.addAllInst(translateToMipsInsts(instruction, index, isEntryBlock, functionName));
        }
    }

    private void translateToMipsFunction(Function function, ArrayList<Instruction> instructions) {
        String functionName = function.getName();
        MipsFunction mipsFunction = mipsModule.getFunction(functionName);
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            MipsBlock mipsBlock = new MipsBlock(functionName + "_" + basicBlock.getName());
            translateToMipsBlock(basicBlock, mipsBlock, instructions, functionName);
            // 优化1：去除多余的jump指令
            if (isOptOpen && mipsBlock.getBlockLabel().endsWith("entry")) {
                mipsBlock.clearInsts();
            }
            mipsFunction.addBlock(mipsBlock);
        }
    }

    private static int checkIndex(ArrayList<Instruction> instructions, Instruction inst) {
        return instructions.indexOf(inst);
    }

    private void translateToTextSegment() {
        RegisterFile.initRegFile();
        ArrayList<Function> functions = module.getFunctions();
        // 为mips函数建立参数虚拟寄存器表
        for (Function function : functions) {
            MipsFunction mipsFunction = new MipsFunction(function.getName());
            for (int i = 0; i < function.getParamNum(); i++) {
                VirtualReg paramReg = new VirtualReg(i, VirtualReg.VRegType.Param);
                // 从$a1开始
                if (i < 3) {
                    paramReg.setArgRegName("$a" + (i + 1));
                } else {
                    paramReg.setArgPosToFp(RegisterFile.getOffsetToFp());
                    RegisterFile.increaseOffsetToFp(4);
                }
                mipsFunction.addParamVirtualReg(paramReg);
            }
            mipsModule.addFunction(mipsFunction);
        }
        for (Function function : functions) {
            ArrayList<Instruction> instructions = function.getAllInstructions();
            RegisterFile.initVirtualRegs(instructions);
            translateToMipsFunction(function, instructions);
        }
    }

}
