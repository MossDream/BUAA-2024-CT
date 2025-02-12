package middle.llvm.llvmir.type;

import java.util.ArrayList;

/**
 * @Description UnknownType
 */

public class UnknownType extends ValueType {
    private static final UnknownType unknownType = new UnknownType();

    private UnknownType() {
    }

    public static UnknownType getInstance() {
        return unknownType;
    }

}
