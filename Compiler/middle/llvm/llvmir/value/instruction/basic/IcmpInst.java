package middle.llvm.llvmir.value.instruction.basic;

import middle.llvm.llvmir.type.IntegerType;
import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

/**
 * @Description IcmpInst
 */

public class IcmpInst extends Instruction {
    private Type cmpType;
    private ValueType opType;
    private Value op1;
    private Value op2;
    private Value result;

    public enum Type {
        EQ("eq"),
        NE("ne"),
        SGT("sgt"),
        SGE("sge"),
        SLT("slt"),
        SLE("sle");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public IcmpInst(ValueType opType, Type cmpType, Value op1, Value op2, Value result) {
        super(new IntegerType(1), InstType.Icmp);
        this.cmpType = cmpType;
        this.opType = opType;
        this.op1 = op1;
        this.op2 = op2;
        this.result = result;
    }

    public Value getResult() {
        return result;
    }

    public Value getOp1() {
        return op1;
    }

    public Value getOp2() {
        return op2;
    }

    public Type getCmpType() {
        return cmpType;
    }

    public String toString() {
        return result + " = icmp " + cmpType + " " + opType + " " + op1 + ", " + op2;
    }
}
