package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Seq
 */

public class Seq extends MipsInst {
    private String rd;
    private String rs;
    private String rtOrImm;

    public Seq(String rd, String rs, String rtOrImm) {
        this.rd = rd;
        this.rs = rs;
        this.rtOrImm = rtOrImm;
    }

    @Override
    public String toString() {
        return "seq " + rd + ", " + rs + ", " + rtOrImm;
    }
}
