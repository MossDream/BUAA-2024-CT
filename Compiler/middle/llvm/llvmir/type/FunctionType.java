package middle.llvm.llvmir.type;

import java.util.ArrayList;

/**
 * @Description FunctionType
 */

public class FunctionType extends ValueType {
    private ValueType returnType;
    private ArrayList<ValueType> paramTypes;

    public FunctionType(ValueType returnType, ArrayList<ValueType> paramTypes) {
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    public ValueType getReturnType() {
        return returnType;
    }

    public ArrayList<ValueType> getParamTypes() {
        return paramTypes;
    }
}
