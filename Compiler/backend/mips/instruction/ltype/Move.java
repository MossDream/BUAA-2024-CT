package backend.mips.instruction.ltype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Move
 */

public class Move extends MipsInst {
    private String rd;
    private String rs;

    public Move(String rd, String rs) {
        this.rd = rd;
        this.rs = rs;
    }

    public String getRd() {
        return rd;
    }

    @Override
    public String toString() {
        return "move " + rd + ", " + rs;
    }
}
