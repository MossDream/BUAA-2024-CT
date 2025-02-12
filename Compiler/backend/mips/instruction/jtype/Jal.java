package backend.mips.instruction.jtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Jal
 */

public class Jal extends MipsInst {
    private String targetLabel;

    public Jal(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    @Override
    public String toString() {
        return "jal " + targetLabel;
    }
}
