package backend.mips.instruction.ltype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Mfhi
 */

public class Mfhi extends MipsInst {
    private String rd;

    public Mfhi(String rd) {
        this.rd = rd;
    }

    @Override
    public String toString() {
        return "mfhi " + rd;
    }
}
