package middle.llvm.llvmir.value.instruction.basic;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

/**
 * @Description TruncInst
 */

public class TruncInst extends Instruction {
    private Value result;
    private Value src;
    private ValueType destType;

    public TruncInst(Value src, Value result, ValueType destType) {
        super(destType, InstType.Trunc);
        this.destType = destType;
        this.src = src;
        this.result = result;
    }

    public Value getResult() {
        return result;
    }

    public ValueType getDestType() {
        return destType;
    }

    public Value getSrc() {
        return src;
    }

    public String toString() {
        return result + " = trunc " + src.getType().toString() + " " + src + " to " + destType.toString();
    }
}
