package middle.llvm.llvmir.value.function;

import middle.llvm.llvmir.type.ArrayType;
import middle.llvm.llvmir.type.FunctionType;
import middle.llvm.llvmir.type.PointerType;
import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.basicblock.BasicBlock;
import middle.llvm.llvmir.value.instruction.Instruction;

import java.util.ArrayList;

/**
 * @Description Function
 */

public class Function extends Value {

    private ArrayList<BasicBlock> basicBlocks;
    private int paramNameIndex;

    public Function(String name, ValueType type) {
        super(name, type);
        basicBlocks = new ArrayList<>();
        paramNameIndex = 0;
    }

    public ArrayList<Instruction> getAllInstructions() {
        ArrayList<Instruction> instructions = new ArrayList<>();
        for (BasicBlock basicBlock : basicBlocks) {
            instructions.addAll(basicBlock.getInstructions());
        }
        return instructions;
    }

    public ArrayList<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public void addBasicBlocks(ArrayList<BasicBlock> basicBlocks) {
        this.basicBlocks.addAll(basicBlocks);
    }

    public int getParamNum() {
        return ((FunctionType) getType()).getParamTypes().size();
    }

    @Override
    public String toString() {
        StringBuilder function = new StringBuilder();
        function.append("define dso_local ");
        FunctionType functionType = (FunctionType) getType();
        function.append(functionType.getReturnType())
                .append(" @").append(getName()).append("(");
        ArrayList<ValueType> paramTypes = functionType.getParamTypes();
        paramNameIndex = 0;
        for (int i = 0; i < paramTypes.size(); i++) {
            ValueType paramType = paramTypes.get(i);
            function.append(paramType).append(" %p").append(paramNameIndex++);
            if (i != paramTypes.size() - 1) {
                function.append(", ");
            }
        }
        function.append(") {\n");
        for (BasicBlock basicBlock : basicBlocks) {
            function.append(basicBlock);
        }
        function.append("}\n");
        return function.toString();
    }
}
