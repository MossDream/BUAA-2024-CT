package middle.llvm.llvmir.value.instruction.basic;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

/**
 * @Description ZextInst
 */

public class ZextInst extends Instruction {
    private Value result;
    private Value src;
    private ValueType destType;

    public ZextInst(Value src, Value result, ValueType destType) {
        super(destType, InstType.Zext);
        this.destType = destType;
        this.src = src;
        this.result = result;
    }

    public Value getResult() {
        return result;
    }

    public Value getSrc() {
        return src;
    }

    @Override
    public String toString() {
        return result + " = zext " + src.getType().toString() + " " + src + " to " + destType.toString();
    }
}
