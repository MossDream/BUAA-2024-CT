package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Sra
 */

public class Sra extends MipsInst {
    private String rd;
    private String rs;
    private String imm;

    public Sra(String rd, String rs, String imm) {
        this.rd = rd;
        this.rs = rs;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return "sra " + rd + ", " + rs + ", " + imm;
    }
}
