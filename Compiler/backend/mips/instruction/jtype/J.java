package backend.mips.instruction.jtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description J
 */

public class J extends MipsInst {
    private String targetLabel;

    public J(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    @Override
    public String toString() {
        return "j " + targetLabel;
    }
}
