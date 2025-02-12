package middle.llvm.llvmir.value.basicblock;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.Instruction;
import middle.llvm.llvmir.value.instruction.basic.LiteralInst;
import middle.llvm.llvmir.value.instruction.terminate.BranchInst;
import middle.llvm.llvmir.value.instruction.terminate.ReturnInst;

import java.util.ArrayList;

/**
 * @Description BasicBlock
 */

public class BasicBlock extends Value {

    private static int labelNameIndex = 0;

    private int nameIndex;

    private ArrayList<Instruction> instructions;

    public BasicBlock(ValueType type) {
        super(type);
        setName("lb" + labelNameIndex);
        this.nameIndex = labelNameIndex;
        labelNameIndex++;
        instructions = new ArrayList<>();
    }

    public BasicBlock(String name, ValueType type) {
        super(type);
        setName(name);
        instructions = new ArrayList<>();
    }

    public int getNameIndex() {
        return nameIndex;
    }

    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }

    public void addInstructions(ArrayList<Instruction> instructions) {
        this.instructions.addAll(instructions);
    }

    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
    }

    public static void resetLabelNameIndex() {
        labelNameIndex = 0;
    }

    public static int getLabelNameIndex() {
        return labelNameIndex;
    }

    @Override
    public String toString() {
        StringBuilder basicBlock = new StringBuilder();
        basicBlock.append(getName()).append(":\n");
        for (Instruction instruction : instructions) {
            if (!(instruction instanceof LiteralInst)) {
                basicBlock.append("\t");
            }
            basicBlock.append(instruction.toString());
            if (!(instruction instanceof LiteralInst)) {
                basicBlock.append("\n");
            }
        }
        return basicBlock.toString();
    }
}
