package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Subu
 */

public class Subu extends MipsInst {
    private String rd;
    private String rs;
    private String rt;

    public Subu(String rd, String rs, String rt) {
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
    }

    @Override
    public String toString() {
        return "subu " + rd + ", " + rs + ", " + rt;
    }
}
