package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Sll
 */

public class Sll extends MipsInst {
    private String rd;
    private String rs;
    private String imm;

    public Sll(String rd, String rs, String imm) {
        this.rd = rd;
        this.rs = rs;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return "sll " + rd + ", " + rs + ", " + imm;
    }

}
