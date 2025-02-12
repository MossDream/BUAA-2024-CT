package backend.mips.instruction.ltype;

import backend.mips.instruction.MipsInst;

/**
 * @Description Lw
 */

public class Lw extends MipsInst {
    private String rt;
    private String offsetImm;
    private String offsetReg;
    private String baseReg;
    private String label;
    private boolean useLabel;

    public Lw(String rt, String offsetImm, String baseReg) {
        this.rt = rt;
        this.offsetImm = offsetImm;
        this.baseReg = baseReg;
        this.useLabel = false;
    }

    public Lw(String rt, String offsetImm, String offsetReg, String label) {
        this.rt = rt;
        this.offsetImm = offsetImm;
        this.offsetReg = offsetReg;
        this.label = label;
        this.useLabel = true;
    }

    public String getRt() {
        return rt;
    }

    @Override
    public String toString() {
        if (useLabel) {
            if (offsetReg.equals("null")) {
                if (offsetImm.equals("0")) {
                    return "lw " + rt + ", " + label;
                } else {
                    return "lw " + rt + ", " + label + "+" + offsetImm;
                }
            } else {
                if (offsetImm.equals("0")) {
                    return "lw " + rt + ", " + label + "(" + offsetReg + ")";
                } else {
                    return "lw " + rt + ", " + label + "+" + offsetImm + "(" + offsetReg + ")";
                }
            }
        } else {
            return "lw " + rt + ", " + offsetImm + "(" + baseReg + ")";
        }
    }
}
