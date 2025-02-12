package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Slt
 */

public class Slt extends MipsInst {
    private String rd;
    private String rs;
    private String rtOrImm;

    public Slt(String rd, String rs, String rtOrImm) {
        this.rd = rd;
        this.rs = rs;
        this.rtOrImm = rtOrImm;
    }

    @Override
    public String toString() {
        return "slt " + rd + ", " + rs + ", " + rtOrImm;
    }
}
