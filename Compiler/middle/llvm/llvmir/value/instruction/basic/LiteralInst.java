package middle.llvm.llvmir.value.instruction.basic;

import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

/**
 * @Description LiteralInst
 * 一种特殊的指令，实质上就是一个字面量或全局非数组变量Value，LLVM IR中对其是直接引用的，故没有该指令
 * 本编译器的递归设计需要该指令
 */

public class LiteralInst extends Instruction {
    // 该指令的唯一操作数和返回值
    private Value value;

    public LiteralInst(Value value) {
        super(value.getType(), InstType.Literal);
        this.value = value;
    }

    public Value getResult() {
        return value;
    }

    @Override
    public String toString() {
        // 无需生成LLVM IR代码
        return "";
    }
}
