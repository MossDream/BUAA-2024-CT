package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Addu
 */

public class Addu extends MipsInst {
    private String rd;
    private String rs;
    private String rt;

    public Addu(String rd, String rs, String rt) {
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
    }

    @Override
    public String toString() {
        return "addu " + rd + ", " + rs + ", " + rt;
    }
}
