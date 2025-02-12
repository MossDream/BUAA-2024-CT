package middle.llvm.llvmir.value.item;

import middle.llvm.llvmir.type.ArrayType;
import middle.llvm.llvmir.type.IntegerType;
import middle.llvm.llvmir.value.Value;

import java.util.ArrayList;

/**
 * @Description Literal
 */

public class Literal extends Value {

    // value是字面量的值，可以是字符串、整数、字符以及zeroinitializer
    //类似{ 2，3 }这样的初始值，将被逐个存储在ArrayList中
    private ArrayList<String> value;
    private boolean isZeroInitializer;

    public Literal(int num, int bitWidth) {
        super(new IntegerType(bitWidth));
        this.value = new ArrayList<>();
        this.value.add(String.valueOf(num));
        isZeroInitializer = false;
    }

    public Literal(int arraySize, ArrayList<Integer> array, int bitWidth) {
        // i32、i8 类型数组字面量 例如{2，3}
        super(new ArrayType(new IntegerType(bitWidth), arraySize));
        this.value = new ArrayList<>();
        for (int i : array) {
            this.value.add(String.valueOf(i));
        }
        if (arraySize == 0) {
            isZeroInitializer = true;
        } else {
            isZeroInitializer = false;
        }
    }

    public boolean isInteger() {
        return getType() instanceof IntegerType;
    }

    public boolean isArray() {
        return getType() instanceof ArrayType;
    }

    public boolean isZeroInitializer() {
        return isZeroInitializer;
    }

    public int getIntElem(int index) {
        if (isZeroInitializer) {
            return 0;
        }
        return Integer.parseInt(value.get(index));
    }

    public int getInt() {
        return Integer.parseInt(value.get(0));
    }

    public ArrayList<String> getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder literal = new StringBuilder();
        if (isZeroInitializer) {
            literal.append("zeroinitializer");
        } else {
            if (isInteger()) {
                literal.append(value.get(0));
            } else {
                ArrayType arrayType = (ArrayType) getType();
                literal.append("[");
                for (int i = 0; i < value.size(); i++) {
                    literal.append(arrayType.getBaseType()).append(" ");
                    literal.append(value.get(i));
                    if (i != value.size() - 1) {
                        literal.append(", ");
                    }
                }
                literal.append("]");
            }
        }
        return literal.toString();
    }
}
