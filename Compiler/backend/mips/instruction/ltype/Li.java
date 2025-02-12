package backend.mips.instruction.ltype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Li
 */

public class Li extends MipsInst {
    private String rd;
    private String imm;

    public Li(String rd, String imm) {
        this.rd = rd;
        this.imm = imm;
    }

    public String getImm() {
        return imm;
    }

    @Override
    public String toString() {
        return "li " + rd + ", " + imm;
    }
}
