package backend.mips.instruction.ltype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Sw
 */

public class Sw extends MipsInst {
    private String rt;
    private String offsetImm;
    private String offsetReg;
    private String baseReg;
    private String label;
    private boolean useLabel;

    public Sw(String rt, String offsetImm, String baseReg) {
        this.rt = rt;
        this.offsetImm = offsetImm;
        this.baseReg = baseReg;
        this.useLabel = false;
    }

    public Sw(String rt, String offsetImm, String offsetReg, String label) {
        this.rt = rt;
        this.offsetImm = offsetImm;
        this.offsetReg = offsetReg;
        this.label = label;
        this.useLabel = true;
    }

    @Override
    public String toString() {
        if (useLabel) {
            if (offsetReg.equals("null")) {
                if (offsetImm.equals("0")) {
                    return "sw " + rt + ", " + label;
                } else {
                    return "sw " + rt + ", " + label + "+" + offsetImm;
                }
            } else {
                if (offsetImm.equals("0")) {
                    return "sw " + rt + ", " + label + "(" + offsetReg + ")";
                } else {
                    return "sw " + rt + ", " + label + "+" + offsetImm + "(" + offsetReg + ")";
                }
            }
        } else {
            return "sw " + rt + ", " + offsetImm + "(" + baseReg + ")";
        }
    }
}
