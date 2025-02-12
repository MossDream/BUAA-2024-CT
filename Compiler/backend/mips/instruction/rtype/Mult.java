package backend.mips.instruction.rtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Mult
 */

public class Mult extends MipsInst {
    private String rs;
    private String rt;

    public Mult(String rs, String rt) {
        this.rs = rs;
        this.rt = rt;
    }

    @Override
    public String toString() {
        return "mult " + rs + ", " + rt;
    }
}
