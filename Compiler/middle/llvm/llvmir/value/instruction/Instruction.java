package middle.llvm.llvmir.value.instruction;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;

/**
 * @Description Instruction
 */

public class Instruction extends Value {
    private InstType instType;

    public Instruction(ValueType valueType, InstType instType) {
        super(valueType);
        this.instType = instType;
    }

    public InstType getInstType() {
        return instType;
    }

    public Value getResult() {
        throw new RuntimeException("指令返回值未定义");
    }
}
