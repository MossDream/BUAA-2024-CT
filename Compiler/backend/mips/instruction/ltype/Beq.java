package backend.mips.instruction.ltype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Beq
 */

public class Beq extends MipsInst {
    private String rs;
    private String rtOrImm;
    private String targetLabel;

    public Beq(String rs, String rtOrImm, String targetLabel) {
        this.rs = rs;
        this.rtOrImm = rtOrImm;
        this.targetLabel = targetLabel;
    }

    @Override
    public String toString() {
        return "beq " + rs + ", " + rtOrImm + ", " + targetLabel;
    }
}
