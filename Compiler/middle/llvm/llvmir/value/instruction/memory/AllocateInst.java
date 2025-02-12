package middle.llvm.llvmir.value.instruction.memory;

import middle.llvm.llvmir.type.PointerType;
import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.Instruction;
import middle.llvm.llvmir.value.instruction.InstType;

/**
 * @Description AllocateInst
 */

public class AllocateInst extends Instruction {
    private ValueType allocatedType;
    private Value result;

    public AllocateInst(ValueType allocatedType, Value result) {
        super(new PointerType(allocatedType), InstType.Allocate);
        this.allocatedType = allocatedType;
        this.result = result;
    }

    public ValueType getAllocatedType() {
        return allocatedType;
    }

    public Value getResult() {
        return result;
    }

    @Override
    public String toString() {
        return result + " = alloca " + allocatedType;
    }
}
