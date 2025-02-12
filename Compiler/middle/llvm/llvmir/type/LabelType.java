package middle.llvm.llvmir.type;

import java.util.ArrayList;

/**
 * @Description LabelType
 */

public class LabelType extends ValueType {

    public static final LabelType labelType = new LabelType();

    private LabelType() {
    }

    public static LabelType getInstance() {
        return labelType;
    }

}
