package middle.llvm.llvmir.value.globalvar;

import middle.llvm.llvmir.type.ArrayType;
import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;

/**
 * @Description GlobalVar
 */

public class GlobalVar extends Value {

    private boolean isConst;
    private Value initValue;

    public GlobalVar(String name, ValueType type, boolean isConst) {
        super(name, type);
        this.isConst = isConst;
    }

    public void setInitValue(Value initValue) {
        this.initValue = initValue;
    }

    public Value getInitValue() {
        return initValue;
    }

    public String getName() {
        return "@" + super.getName();
    }

    @Override
    public String toString() {
        StringBuilder globalVar = new StringBuilder();
        globalVar.append(getName()).append(" = dso_local global ");
        if (getType() instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) getType();
            globalVar.append(arrayType.toString()).append(" ");
            globalVar.append(initValue.toString()).append("\n");
        } else {
            globalVar.append(getType()).append(" ");
            globalVar.append(initValue.toString()).append("\n");
        }
        return globalVar.toString();
    }
}
