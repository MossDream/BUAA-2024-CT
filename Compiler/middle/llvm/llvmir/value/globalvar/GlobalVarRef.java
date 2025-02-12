package middle.llvm.llvmir.value.globalvar;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;

/**
 * @Description GlobalVarRef
 */

public class GlobalVarRef extends Value {
    private String name;
    private ValueType refType;

    public GlobalVarRef(String name, ValueType refType) {
        super(name, refType);
        this.name = name;
        this.refType = refType;
    }

    public ValueType getRefType() {
        return refType;
    }

    @Override
    public String toString() {
        return name;
    }
}
