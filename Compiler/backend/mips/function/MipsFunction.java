package backend.mips.function;

import backend.mips.block.MipsBlock;
import backend.mips.instruction.MipsInst;
import backend.utils.VirtualReg;

import java.util.ArrayList;

/**
 * @Description MipsFunction
 */

public class MipsFunction {
    private String funcName;
    private ArrayList<MipsBlock> blocks;
    private ArrayList<VirtualReg> paramVirtualRegs;

    public MipsFunction(String funcName) {
        this.funcName = funcName;
        blocks = new ArrayList<>();
        paramVirtualRegs = new ArrayList<>();
    }

    public void addBlock(MipsBlock block) {
        blocks.add(block);
    }

    public void addParamVirtualReg(VirtualReg virtualReg) {
        paramVirtualRegs.add(virtualReg);
    }

    public VirtualReg getParamVirtualReg(int index) {
        return paramVirtualRegs.get(index);
    }

    public String getName() {
        return funcName;
    }

    public ArrayList<MipsBlock> getBlocks() {
        return blocks;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(funcName).append(":\n");
        for (MipsBlock block : blocks) {
            sb.append(block.toString());
        }
        return sb.toString();
    }
}
