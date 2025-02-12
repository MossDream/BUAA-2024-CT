package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Srl
 */

public class Srl extends MipsInst {
    private String rd;
    private String rs;
    private String imm;

    public Srl(String rd, String rs, String imm) {
        this.rd = rd;
        this.rs = rs;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return "srl " + rd + ", " + rs + ", " + imm;
    }
}
