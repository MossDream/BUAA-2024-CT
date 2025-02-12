package middle.llvm.llvmir.value.instruction.memory;

import middle.llvm.llvmir.type.UnknownType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

/**
 * @Description StoreInst
 */

public class StoreInst extends Instruction {
    private Value content;
    private Value position;

    public StoreInst(Value content, Value position) {
        super(UnknownType.getInstance(), InstType.Store);
        this.content = content;
        this.position = position;
    }

    public Value getContent() {
        return content;
    }

    public Value getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "store " + content.getType().toString() + " "
                + content.toString() + ", " +
                position.getType().toString() + " " + position.toString();
    }
}
