package middle.llvm.llvmir.value.instruction;

/**
 * @Description InstType
 */

public enum InstType {
    Binary,
    Literal,
    Branch,
    Icmp,
    Return,
    Call,
    Store,
    Load,
    Allocate,
    GetElementPtr,
    Zext,
    Trunc,
    Print,
}
