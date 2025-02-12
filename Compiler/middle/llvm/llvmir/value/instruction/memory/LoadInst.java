package middle.llvm.llvmir.value.instruction.memory;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

/**
 * @Description LoadInst
 */

public class LoadInst extends Instruction {
    private Value result;
    private ValueType loadType;
    private Value position;

    public LoadInst(Value result, ValueType loadType, Value position) {
        super(result.getType(), InstType.Load);
        this.result = result;
        this.loadType = loadType;
        this.position = position;
    }

    public Value getResult() {
        return result;
    }

    public Value getPosition() {
        return position;
    }

    public ValueType getLoadType() {
        return loadType;
    }

    @Override
    public String toString() {
        return result + " = load " + loadType + ", " + position.getType() + " " + position;
    }
}
