package backend.mips.block;

import backend.mips.instruction.MipsInst;

import java.util.ArrayList;

/**
 * @Description MipsBlock
 */

public class MipsBlock {
    // 标签由所在函数标签和LLVM基本块名拼接而成
    private String blockLabel;
    private ArrayList<MipsInst> instructions;

    public MipsBlock(String blockLabel) {
        this.blockLabel = blockLabel;
        instructions = new ArrayList<>();
    }

    public void addInst(MipsInst inst) {
        instructions.add(inst);
    }

    public void addAllInst(ArrayList<MipsInst> insts) {
        instructions.addAll(insts);
    }

    public void clearInsts() {
        instructions.clear();
    }

    public String getBlockLabel() {
        return blockLabel;
    }

    public ArrayList<MipsInst> getInsts() {
        return instructions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" ").append(blockLabel).append(":\n");
        for (MipsInst inst : instructions) {
            sb.append("\t").append(inst.toString()).append("\n");
        }
        return sb.toString();
    }
}
