package middle.llvm.llvmir.type;

import java.util.ArrayList;

/**
 * @Description VoidType
 */

public class VoidType extends ValueType {

    public static final VoidType voidType = new VoidType();

    private VoidType() {
    }

    public static VoidType getInstance() {
        return voidType;
    }

    @Override
    public String toString() {
        return "void";
    }

}
