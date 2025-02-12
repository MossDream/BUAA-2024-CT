package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Sge
 */

public class Sge extends MipsInst {
    private String rd;
    private String rs;
    private String rtOrImm;

    public Sge(String rd, String rs, String rtOrImm) {
        this.rd = rd;
        this.rs = rs;
        this.rtOrImm = rtOrImm;
    }

    @Override
    public String toString() {
        return "sge " + rd + ", " + rs + ", " + rtOrImm;
    }
}
