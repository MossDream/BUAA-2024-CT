package middle.llvm.llvmir.value.item;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;

/**
 * @Description LocalVar
 */

public class LocalVar extends Value {
    private static int nameIndex = 0;

    public LocalVar(ValueType type) {
        super(type);
        setName("%lv" + nameIndex);
        nameIndex++;
    }

    public int getNameIndex() {
        return Integer.parseInt(getName().substring(3));
    }

    public static void resetNameIndex() {
        nameIndex = 0;
    }

    @Override
    public String toString() {
        return getName();
    }
}
