package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Addiu
 */

public class Addiu extends MipsInst {
    private String rd;
    private String rs;
    private String imm;

    public Addiu(String rd, String rs, String imm) {
        this.rd = rd;
        this.rs = rs;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return "addiu " + rd + ", " + rs + ", " + imm;
    }
}
