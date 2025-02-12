package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Sne
 */

public class Sne extends MipsInst {
    private String rd;
    private String rs;
    private String rtOrImm;

    public Sne(String rd, String rs, String rtOrImm) {
        this.rd = rd;
        this.rs = rs;
        this.rtOrImm = rtOrImm;
    }

    @Override
    public String toString() {
        return "sne " + rd + ", " + rs + ", " + rtOrImm;
    }
}
