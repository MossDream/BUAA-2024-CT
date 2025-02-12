package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Andi
 */

public class Andi extends MipsInst {
    private String rd;
    private String rs;
    private String imm;

    public Andi(String rd, String rs, String imm) {
        this.rd = rd;
        this.rs = rs;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return "andi " + rd + ", " + rs + ", " + imm;
    }
}
