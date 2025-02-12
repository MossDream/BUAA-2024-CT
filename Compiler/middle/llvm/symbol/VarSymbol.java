package middle.llvm.symbol;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;

/**
 * @Description VarSymbol
 */

public class VarSymbol {
    private String name;
    private boolean isConst;
    private Type type;
    private Value initValue;
    private Value localVar;
    private Value globalVar;
    private boolean isGlobal;
    private int paramIndex;
    private ValueType paramType;

    public enum Type {
        INT, CHAR, INTARRAY, CHARARRAY, NOTARRAY
    }

    public VarSymbol(String name, boolean isConst, Type type) {
        this.name = name;
        this.isConst = isConst;
        this.type = type;
        initValue = null;
        localVar = null;
        globalVar = null;
        isGlobal = false;
        paramIndex = -1;
        paramType = null;
    }

    public boolean isParam() {
        return paramIndex != -1;
    }

    public void setInitValue(Value initValue) {
        this.initValue = initValue;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public boolean isArray() {
        return type == Type.INTARRAY || type == Type.CHARARRAY;
    }

    public boolean isConst() {
        return isConst;
    }

    public Value getInitValue() {
        return initValue;
    }

    public void setLocalVar(Value localVar) {
        this.localVar = localVar;
    }

    public void setGlobalVar(Value globalVar) {
        this.globalVar = globalVar;
        isGlobal = true;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public Value getLocalVar() {
        return localVar;
    }

    public Value getGlobalVar() {
        return globalVar;
    }

    public void setParamIndex(int paramIndex) {
        this.paramIndex = paramIndex;
    }

    public int getParamIndex() {
        return paramIndex;
    }

    public void setParamType(ValueType paramType) {
        this.paramType = paramType;
    }

    public ValueType getParamType() {
        return paramType;
    }

    @Override
    public String toString() {
        StringBuilder varSymbol = new StringBuilder();
        String typeStr = "";
        switch (type) {
            case INT -> typeStr = (isConst ? "ConstInt" : "Int");
            case CHAR -> typeStr = (isConst ? "ConstChar" : "Char");
            case INTARRAY -> typeStr = (isConst ? "ConstIntArray" : "IntArray");
            case CHARARRAY -> typeStr = (isConst ? "ConstCharArray" : "CharArray");
            default -> {
            }
        }
        varSymbol.append(name).append(" ").append(typeStr);
        return varSymbol.toString();
    }

}
