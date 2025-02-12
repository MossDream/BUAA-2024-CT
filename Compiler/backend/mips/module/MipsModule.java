package backend.mips.module;

import backend.mips.function.MipsFunction;
import backend.mips.globallabel.GlobalLabel;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @Description MipsModule
 */

public class MipsModule {
    // name -> label
    private HashMap<String, GlobalLabel> globalLabels;
    private ArrayList<MipsFunction> functions;

    public MipsModule() {
        this.globalLabels = new HashMap<>();
        this.functions = new ArrayList<>();
    }

    public void addGlobalLabel(GlobalLabel globalLabel) {
        globalLabels.put(globalLabel.getName(), globalLabel);
    }

    public void addFunction(MipsFunction function) {
        functions.add(function);
    }

    public MipsFunction getFunction(String name) {
        for (MipsFunction function : functions) {
            if (function.getName().equals(name)) {
                return function;
            }
        }
        return null;
    }

    public ArrayList<MipsFunction> getFunctions() {
        return functions;
    }

    @Override
    public String toString() {
        StringBuilder mipsModule = new StringBuilder();
        // data segment
        mipsModule.append(".data\n");
        for (GlobalLabel globalLabel : globalLabels.values()) {
            mipsModule.append(globalLabel.toString());
        }
        // text segment
        mipsModule.append(".text\n");
        mipsModule.append("\tli $fp, 0x10008000\n");
        mipsModule.append("\tj main\n");
        for (MipsFunction function : functions) {
            mipsModule.append(function.toString());
        }
        return mipsModule.toString();
    }
}
