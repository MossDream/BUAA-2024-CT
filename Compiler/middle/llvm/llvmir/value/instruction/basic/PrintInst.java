package middle.llvm.llvmir.value.instruction.basic;

import middle.llvm.llvmir.type.UnknownType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.instruction.InstType;
import middle.llvm.llvmir.value.instruction.Instruction;

/**
 * @Description PrintInst
 */

public class PrintInst extends Instruction {
    private String printType;
    private String stringName;
    private int stringLength;
    private Value printValue;

    public PrintInst(String printType, String stringName, int stringLength) {
        super(UnknownType.getInstance(), InstType.Print);
        this.printType = printType;
        this.stringName = stringName;
        this.stringLength = stringLength;
    }

    public PrintInst(String printType, Value printValue) {
        super(UnknownType.getInstance(), InstType.Print);
        this.printType = printType;
        this.printValue = printValue;
    }

    public Value getPrintValue() {
        return printValue;
    }

    public String getPrintType() {
        return printType;
    }

    public String getStringName() {
        return stringName;
    }

    @Override
    public String toString() {
        if (printType.equals("putstr")) {
            return "call void @putstr(i8* getelementptr inbounds ([" + stringLength + " x i8], [" + stringLength + " x i8]* " + stringName + ", i64 0, i64 0))";
        } else if (printType.equals("putint")) {
            return "call void @putint(i32 " + printValue + ")";
        } else if (printType.equals("putch")) {
            return "call void @putch(i8 " + printValue + ")";
        }
        return null;
    }
}
