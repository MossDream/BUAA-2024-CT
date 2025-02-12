package middle.llvm.llvmir.value.instruction.terminate;

import middle.llvm.llvmir.type.UnknownType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

/**
 * @Description BranchInst
 */

public class BranchInst extends Instruction {
    private Value condition;
    private String trueLabel;
    private String falseLabel;
    private String targetLabel;

    public BranchInst(Value condition, String trueLabel, String falseLabel) {
        super(UnknownType.getInstance(), InstType.Branch);
        this.condition = condition;
        this.trueLabel = trueLabel;
        this.falseLabel = falseLabel;
        this.targetLabel = null;
    }

    public BranchInst(String targetLabel) {
        super(UnknownType.getInstance(), InstType.Branch);
        this.condition = null;
        this.trueLabel = null;
        this.falseLabel = null;
        this.targetLabel = targetLabel;
    }

    public Value getCondition() {
        return condition;
    }

    public String getTrueLabel() {
        return trueLabel;
    }

    public String getFalseLabel() {
        return falseLabel;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public String toString() {
        if (condition != null) {
            return "br i1 " + condition + ", label %" + trueLabel + ", label %" + falseLabel;
        } else {
            return "br label %" + targetLabel;
        }
    }
}
