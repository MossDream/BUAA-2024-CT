package backend.mips.instruction.ltype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Not
 */

public class Not extends MipsInst {
    private String rd;
    private String rs;

    public Not(String rd, String rs) {
        this.rd = rd;
        this.rs = rs;
    }

    @Override
    public String toString() {
        return "not " + rd + ", " + rs;
    }
}
