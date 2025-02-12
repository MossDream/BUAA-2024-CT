package middle.llvm.llvmir.type;

import java.util.ArrayList;

/**
 * @Description IntegerType
 */

public class IntegerType extends ValueType {
    private int bitWidth;

    public IntegerType(int bitWidth) {
        if (bitWidth != 1 && bitWidth != 8 && bitWidth != 32) {
            throw new IllegalArgumentException("Invalid bit width for integer type: " + bitWidth);
        }
        this.bitWidth = bitWidth;
    }

    public int getBitWidth() {
        return bitWidth;
    }

    @Override
    public String toString() {
        return "i" + bitWidth;
    }
}
