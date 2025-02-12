package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Sle
 */

public class Sle extends MipsInst {
    private String rd;
    private String rs;
    private String rtOrImm;

    public Sle(String rd, String rs, String rtOrImm) {
        this.rd = rd;
        this.rs = rs;
        this.rtOrImm = rtOrImm;
    }

    @Override
    public String toString() {
        return "sle " + rd + ", " + rs + ", " + rtOrImm;
    }
}
