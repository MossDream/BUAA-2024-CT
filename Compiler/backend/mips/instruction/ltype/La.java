package backend.mips.instruction.ltype;

import backend.mips.instruction.MipsInst;

/**
 * @Description La
 */

public class La extends MipsInst {
    private String rt;
    private String label;

    public La(String rt, String label) {
        this.rt = rt;
        this.label = label;
    }

    @Override
    public String toString() {
        return "la " + rt + ", " + label;
    }
}
