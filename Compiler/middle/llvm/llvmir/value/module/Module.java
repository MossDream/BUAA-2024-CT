package middle.llvm.llvmir.value.module;

import middle.llvm.llvmir.type.ValueType;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.globalvar.GlobalVar;
import middle.llvm.llvmir.value.function.Function;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @Description Module
 */

public class Module extends Value {
    ArrayList<Function> functions;
    ArrayList<GlobalVar> globalVariables;
    // content -> index
    private HashMap<String, Integer> stringConstants;
    // content -> length
    private HashMap<String, Integer> stringLengths;
    private int stringIndex;

    public Module(ValueType type) {
        super(type);
        functions = new ArrayList<>();
        globalVariables = new ArrayList<>();
        stringConstants = new HashMap<>();
        stringLengths = new HashMap<>();
        stringIndex = 0;
    }

    public ArrayList<Function> getFunctions() {
        return functions;
    }

    public ArrayList<GlobalVar> getGlobalVariables() {
        return globalVariables;
    }

    public HashMap<String, Integer> getStringConstants() {
        return stringConstants;
    }

    public void addStringConstant(String content, int length) {
        if (!stringConstants.containsKey(content)) {
            stringConstants.put(content, stringIndex++);
            stringLengths.put(content, length);
        }
    }

    public String getStringConstantName(String content) {
        return "@.str." + stringConstants.get(content);
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    public void addGlobalVariables(ArrayList<GlobalVar> globalVariables) {
        this.globalVariables.addAll(globalVariables);
    }

    @Override
    public String toString() {
        StringBuilder module = new StringBuilder();
        String declarations = "declare i32 @getint()\n" +
                "declare i32 @getchar()\n" +
                "declare void @putint(i32)\n" +
                "declare void @putch(i8)\n" +
                "declare void @putstr(i8*)\n\n";
        module.append(declarations);
        String stringConstants = "";
        for (String content : this.stringConstants.keySet()) {
            stringConstants += "@.str." + this.stringConstants.get(content)
                    + " = private unnamed_addr constant [" + (stringLengths.get(content)) + " x i8] c\"" + content + "\\00\"\n";
        }
        module.append(stringConstants).append("\n");
        for (GlobalVar globalVar : globalVariables) {
            module.append(globalVar.toString());
        }
        module.append("\n");
        for (Function function : functions) {
            module.append(function.toString()).append("\n");
        }
        return module.toString();
    }
}
