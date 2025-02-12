package middle.llvm.llvmir.value.instruction.basic;

import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

/**
 * @Description BinaryInst
 */

public class BinaryInst extends Instruction {
    private OpType opType;
    private Value left;
    private Value right;
    private Value result;

    public BinaryInst(OpType opType, Value left, Value right, Value result) {
        super(result.getType(), InstType.Binary);
        this.opType = opType;
        this.left = left;
        this.right = right;
        this.result = result;
    }

    public Value getResult() {
        return result;
    }

    public OpType getOpType() {
        return opType;
    }

    public Value getLeft() {
        return left;
    }

    public Value getRight() {
        return right;
    }

    public enum OpType {
        ADD("add"), SUB("sub"), MUL("mul"), SDIV("sdiv"), SREM("srem");

        private String op;

        OpType(String op) {
            this.op = op;
        }

        @Override
        public String toString() {
            return op;
        }
    }

    @Override
    public String toString() {
        return result + " = " + opType + " " + left.getType() + " " + left + ", " + right;
    }
}
