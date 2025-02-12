package backend.mips.instruction.ltype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Mflo
 */

public class Mflo extends MipsInst {
    private String rd;

    public Mflo(String rd) {
        this.rd = rd;
    }

    public String getRd() {
        return rd;
    }

    @Override
    public String toString() {
        return "mflo " + rd;
    }
}
