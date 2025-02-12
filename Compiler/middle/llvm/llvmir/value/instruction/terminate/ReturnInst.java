package middle.llvm.llvmir.value.instruction.terminate;

import middle.llvm.llvmir.type.VoidType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

/**
 * @Description ReturnInst
 */

public class ReturnInst extends Instruction {
    private Value returnValue;

    public ReturnInst(Value returnValue) {
        super(returnValue.getType(), InstType.Return);
        this.returnValue = returnValue;
    }

    public Value getReturnValue() {
        return returnValue;
    }

    @Override
    public String toString() {
        if(returnValue.getType() instanceof VoidType){
            return "ret void";
        } else {
            return "ret " + returnValue.getType().toString() + " " + returnValue.toString();
        }
    }
}
