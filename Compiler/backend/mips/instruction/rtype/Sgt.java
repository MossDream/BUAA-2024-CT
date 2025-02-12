package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Sgt
 */

public class Sgt extends MipsInst {
    private String rd;
    private String rs;
    private String rtOrImm;

    public Sgt(String rd, String rs, String rtOrImm) {
        this.rd = rd;
        this.rs = rs;
        this.rtOrImm = rtOrImm;
    }

    @Override
    public String toString() {
        return "sgt " + rd + ", " + rs + ", " + rtOrImm;
    }
}
