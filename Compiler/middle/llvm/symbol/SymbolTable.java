package middle.llvm.symbol;

import java.util.HashMap;

/**
 * @Description SymbolTable
 */

public class SymbolTable {
    private SymbolTable father;
    private HashMap<String, VarSymbol> varTable;
    private HashMap<String, FuncSymbol> funcTable;
    private int index;

    public SymbolTable(SymbolTable father, int index) {
        this.father = father;
        this.varTable = new HashMap<>();
        this.funcTable = new HashMap<>();
        this.index = index;
    }

    public SymbolTable getFather() {
        return father;
    }

    public int getIndex() {
        return index;
    }

    public void addVar(VarSymbol var) {
        varTable.put(var.getName(), var);
    }

    public void addFunc(FuncSymbol func) {
        funcTable.put(func.getName(), func);
    }

    public boolean containsVar(String name) {
        return varTable.containsKey(name);
    }

    public boolean containsFunc(String name) {
        return funcTable.containsKey(name);
    }

    public VarSymbol getVar(String name) {
        return varTable.get(name);
    }

    public VarSymbol findVar(String name) {
        SymbolTable table = this;
        while (table != null) {
            if (table.containsVar(name)) {
                return table.getVar(name);
            }
            table = table.getFather();
        }
        return null;
    }

    public FuncSymbol getFunc(String name) {
        return funcTable.get(name);
    }

    public FuncSymbol findFunc(String name) {
        SymbolTable table = this;
        while (table != null) {
            if (table.containsFunc(name)) {
                return table.getFunc(name);
            }
            table = table.getFather();
        }
        return null;
    }
}
