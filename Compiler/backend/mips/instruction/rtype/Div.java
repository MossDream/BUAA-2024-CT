package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Div
 */

public class Div extends MipsInst {
    private String rs;
    private String rt;

    public Div(String rs, String rt) {
        this.rs = rs;
        this.rt = rt;
    }

    @Override
    public String toString() {
        return "div " + rs + ", " + rt;
    }
}
