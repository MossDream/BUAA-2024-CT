package middle.llvm.llvmir.value.instruction.memory;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

import java.util.ArrayList;

/**
 * @Description GetElePtrInst
 */

public class GetElePtrInst extends Instruction {
    private Value result;
    private ValueType elementType;
    private Value basePointer;
    private ArrayList<Value> indices;

    public GetElePtrInst(ValueType elementType, Value basePointer, Value result) {
        super(result.getType(), InstType.GetElementPtr);
        this.elementType = elementType;
        this.basePointer = basePointer;
        this.indices = new ArrayList<>();
        this.result = result;
    }

    public void addIndex(Value index) {
        indices.add(index);
    }

    public Value getResult() {
        return result;
    }

    public Value getBasePointer() {
        return basePointer;
    }

    public ArrayList<Value> getIndices() {
        return indices;
    }

    @Override
    public String toString() {
        StringBuilder inst = new StringBuilder();
        inst.append(result).append(" = getelementptr inbounds ").append(elementType).
                append(", ").append(basePointer.getType()).append(" ").append(basePointer);
        for (Value index : indices) {
            inst.append(", ").append(index.getType()).append(" ").append(index);
        }
        return inst.toString();
    }
}
