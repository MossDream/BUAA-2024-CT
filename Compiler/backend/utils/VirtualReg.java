package backend.utils;

/**
 * @Description VirtualReg
 */

public class VirtualReg {
    // 编号（以LLVM函数为基本单位），参数寄存器统一为-1。
    private int id;
    // 生存期起始位置所在指令的索引
    private int defPos;
    // 生存期结束位置所在指令的索引
    private int lastUsePos;

    public enum VRegType {
        // 变量或者数组元素值
        VarValue,
        // 变量地址
        VarAddr,
        // 数组起始地址
        ArrayAddr,
        // 数组起始地址的地址
        AddrOfArrayAddr,
        // 数组元素地址
        ArrayEleAddr,
        // 函数形参
        Param,
    }

    private VRegType type;

    public VRegType getType() {
        return type;
    }

    // 是否已经分配物理寄存器或栈上位置
    private boolean isAllocated;
    // 所分配的物理寄存器编号
    private int mappingRegId;
    // 若无法分配物理寄存器，则分配到栈上，记录其相对于$sp的偏移量
    private int offsetToFp;

    // 非普通寄存器（VarValue）的情况下，需要记录更多信息
    // 如果代表本地数组地址，需要记录数组大小和数组在栈上的起始位置，仍然是相对于$sp的偏移量
    private int arraySize;
    private int arrayPosToFp;
    // 如果代表变量地址，需要记录其对应变量在栈上的位置，仍然是相对于$sp的偏移量
    private int varPosToFp;
    // 如果代表数组元素地址，需要记录其对应数组元素绝对地址在栈上的位置，仍然是相对于$sp的偏移量
    private int eleAddrPosToFp;
    // 如果代表变量地址（参数相关）、本地数组地址（参数相关）或数组地址的地址（必定参数相关），需要记录其对应第几个参数寄存器
    private int paramIndex;
    // 如果是参数寄存器，记录实参值在栈上的位置或者在哪个参数寄存器中
    private String argRegName;
    private int argPosToFp;

    public VirtualReg(int id, VRegType type) {
        this.id = id;
        this.type = type;
        this.defPos = -1;
        this.lastUsePos = -1;
        this.mappingRegId = -1;
        this.offsetToFp = 0;
        this.isAllocated = false;
        this.arraySize = 0;
        this.arrayPosToFp = 0;
        this.varPosToFp = 0;
        this.eleAddrPosToFp = 0;
        this.paramIndex = -1;
        this.argRegName = null;
        this.argPosToFp = 0;
    }

    public void setDefPos(int defPos) {
        this.defPos = defPos;
    }

    public void setLastUsePos(int lastUsePos) {
        this.lastUsePos = lastUsePos;
    }

    public void setAllocated(boolean allocated) {
        isAllocated = allocated;
    }

    public void setMappingRegId(int mappingRegId) {
        this.mappingRegId = mappingRegId;
        setAllocated(true);
    }

    public void setOffsetToFp(int offsetToFp) {
        this.offsetToFp = offsetToFp;
        setAllocated(true);
    }

    public void setArrayPosToFp(int arrayPosToFp) {
        this.arrayPosToFp = arrayPosToFp;
    }

    public void setArraySize(int arraySize) {
        this.arraySize = arraySize;
    }

    public void setVarPosToFp(int varPosToFp) {
        this.varPosToFp = varPosToFp;
    }

    public void setEleAddrPosToFp(int eleAddrPosToFp) {
        this.eleAddrPosToFp = eleAddrPosToFp;
    }

    public void setParamIndex(int paramIndex) {
        this.paramIndex = paramIndex;
    }

    public void setArgRegName(String argRegName) {
        this.argRegName = argRegName;
    }

    public void setArgPosToFp(int argPosToFp) {
        this.argPosToFp = argPosToFp;
    }

    public int getId() {
        return id;
    }

    public int getDefPos() {
        return defPos;
    }

    public int getLastUsePos() {
        return lastUsePos;
    }

    public int getMappingRegId() {
        return mappingRegId;
    }

    public String getMappingRegName() {
        return RegisterFile.getRegName(mappingRegId);
    }

    public int getOffsetToFp() {
        return offsetToFp;
    }

    public int getArraySize() {
        return arraySize;
    }

    public int getArrayPosToFp() {
        return arrayPosToFp;
    }

    public int getVarPosToFp() {
        return varPosToFp;
    }

    public int getEleAddrPosToFp() {
        return eleAddrPosToFp;
    }

    public int getParamIndex() {
        return paramIndex;
    }

    public String getArgRegName() {
        return argRegName;
    }

    public int getArgPosToFp() {
        return argPosToFp;
    }

    public boolean isAllocated() {
        return isAllocated;
    }

}
