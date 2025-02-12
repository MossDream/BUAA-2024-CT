package backend.utils;

import backend.mips.instruction.MipsInst;
import backend.mips.instruction.SysCall;
import backend.mips.instruction.ltype.Li;
import backend.mips.instruction.ltype.Move;
import backend.mips.instruction.ltype.Sw;

import java.util.ArrayList;

/**
 * @Description MacroBuilder
 * 将一些常用的宏定义封装,调用对应的静态方法即可生成相应的mips指令序列
 */

public class MacroBuilder {

    public static ArrayList<MipsInst> exit() {
        ArrayList<MipsInst> instructions = new ArrayList<>();
        instructions.add(new Li("$v0", "10"));
        instructions.add(SysCall.getInstance());
        return instructions;
    }

    public static ArrayList<MipsInst> readInt(String destReg) {
        ArrayList<MipsInst> instructions = new ArrayList<>();
        instructions.add(new Li("$v0", "5"));
        instructions.add(SysCall.getInstance());
        instructions.add(new Move(destReg, "$v0"));
        return instructions;
    }

    public static ArrayList<MipsInst> readChar(String destReg) {
        ArrayList<MipsInst> instructions = new ArrayList<>();
        instructions.add(new Li("$v0", "12"));
        instructions.add(SysCall.getInstance());
        instructions.add(new Move(destReg, "$v0"));
        return instructions;
    }

    public static ArrayList<MipsInst> printInt(String srcReg) {
        ArrayList<MipsInst> instructions = new ArrayList<>();
        instructions.add(new Li("$v0", "1"));
        instructions.add(new Move("$a0", srcReg));
        instructions.add(SysCall.getInstance());
        return instructions;
    }

    public static ArrayList<MipsInst> printChar(String srcReg) {
        ArrayList<MipsInst> instructions = new ArrayList<>();
        instructions.add(new Li("$v0", "11"));
        instructions.add(new Move("$a0", srcReg));
        instructions.add(SysCall.getInstance());
        return instructions;
    }

    public static ArrayList<MipsInst> printString(String srcReg) {
        ArrayList<MipsInst> instructions = new ArrayList<>();
        instructions.add(new Li("$v0", "4"));
        instructions.add(new Move("$a0", srcReg));
        instructions.add(SysCall.getInstance());
        return instructions;
    }

    public static ArrayList<MipsInst> push(String srcReg) {
        ArrayList<MipsInst> instructions = new ArrayList<>();
        instructions.add(new Sw(srcReg, RegisterFile.getOffsetToFp() + "", "$fp"));
        RegisterFile.increaseOffsetToFp(4);
        return instructions;
    }

    public static ArrayList<MipsInst> pushOnly() {
        ArrayList<MipsInst> instructions = new ArrayList<>();
        RegisterFile.increaseOffsetToFp(4);
        return instructions;
    }
}
