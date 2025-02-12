package middle.llvm.llvmir.type;

import java.util.ArrayList;

/**
 * @Description PointerType
 */

public class PointerType extends ValueType {
    private ValueType baseType;

    public PointerType(ValueType baseType) {
        this.baseType = baseType;
    }

    public ValueType getBaseType() {
        return baseType;
    }

    @Override
    public String toString() {
        return baseType.toString() + "*";
    }
}
