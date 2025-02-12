package middle.llvm.llvmir.value.instruction.basic;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

import java.util.ArrayList;

/**
 * @Description CallInst
 */

public class CallInst extends Instruction {
    private String functionName;
    private ValueType returnType;
    private ArrayList<Value> args;
    private Value result;

    public CallInst(ValueType returnType, String functionName,
                    ArrayList<Value> args, Value result) {
        super(returnType, InstType.Call);
        this.functionName = functionName;
        this.returnType = returnType;
        this.args = args;
        this.result = result;
    }

    public Value getResult() {
        return result;
    }

    public String getFunctionName() {
        return functionName;
    }

    public ArrayList<Value> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        StringBuilder inst = new StringBuilder();
        if (result != null) {
            inst.append(result).append(" = ");
        }
        inst.append("call ").append(returnType).append(" @").append(functionName).append("(");
        for (int i = 0; i < args.size(); i++) {
            inst.append(args.get(i).getType()).append(" ").append(args.get(i));
            if (i != args.size() - 1) {
                inst.append(", ");
            }
        }
        inst.append(")");
        return inst.toString();
    }
}
