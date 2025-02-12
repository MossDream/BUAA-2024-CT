package middle.llvm.llvmir.type;

import java.util.ArrayList;

/**
 * @Description ArrayType
 */

public class ArrayType extends ValueType {
    // baseType是数组的基本类型，size是数组的大小，即元素个数
    // baseType仍然可以是ArrayType
    private ValueType baseType;
    private int size;

    public ArrayType(ValueType baseType, int size) {
        this.baseType = baseType;
        this.size = size;
    }

    public ValueType getBaseType() {
        return baseType;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "[" + size + " x " + baseType.toString() + "]";
    }
}
