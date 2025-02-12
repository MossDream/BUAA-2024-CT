package middle.llvm.symbol;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;

import java.util.ArrayList;

/**
 * @Description FuncSymbol
 */

public class FuncSymbol {
    private String name;
    private Type returnType;
    private ArrayList<VarSymbol.Type> paramTypes;
    private ArrayList<ValueType> paramValueTypes;
    private int paramNum;
    private Value returnValue;

    public enum Type {
        INT, CHAR, VOID
    }

    public FuncSymbol(String name, Type returnType) {
        this.name = name;
        this.returnType = returnType;
        this.paramTypes = new ArrayList<>();
        this.paramValueTypes = new ArrayList<>();
        paramNum = 0;
        returnValue = null;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void addParamType(VarSymbol.Type type) {
        paramTypes.add(type);
    }

    public String getName() {
        return name;
    }

    public ArrayList<VarSymbol.Type> getParamTypes() {
        return paramTypes;
    }

    public int getParamNum() {
        return paramNum;
    }

    public void setParamNum(int paramNum) {
        this.paramNum = paramNum;
    }

    public Value getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Value returnValue) {
        this.returnValue = returnValue;
    }

    public ValueType getParamValueType(int index) {
        return paramValueTypes.get(index);
    }

    public void addParamValueType(ValueType valueType) {
        paramValueTypes.add(valueType);
    }

    @Override
    public String toString() {
        StringBuilder funcSymbol = new StringBuilder();
        String returnTypeStr =
                (returnType == Type.INT ? "IntFunc" :
                        returnType == Type.CHAR ? "CharFunc" : "VoidFunc");
        funcSymbol.append(name).append(" ").append(returnTypeStr);
        return funcSymbol.toString();
    }

}
