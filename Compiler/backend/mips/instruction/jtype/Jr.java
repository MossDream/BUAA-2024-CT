package backend.mips.instruction.jtype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Jr
 */

public class Jr extends MipsInst {

    public Jr() {
    }

    @Override
    public String toString() {
        return "jr $ra";
    }
}
