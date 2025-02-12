package middle.llvm.llvmir.value;

import middle.llvm.llvmir.type.ValueType;

import java.util.ArrayList;

/**
 * @Description Value
 */

public class Value {
    private String name;
    private ValueType type;
    private ArrayList<Value> users;
    private ArrayList<Value> uses;

    public Value(String name, ValueType type) {
        this.name = name;
        this.type = type;
        this.users = new ArrayList<>();
        this.uses = new ArrayList<>();
    }

    public Value(ValueType type) {
        this.name = "";
        this.type = type;
        this.users = new ArrayList<>();
        this.uses = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public ValueType getType() {
        return type;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    public ArrayList<Value> getUsers() {
        return users;
    }

    public ArrayList<Value> getUses() {
        return uses;
    }

    public void addUser(Value user) {
        users.add(user);
    }

    public void addUse(Value use) {
        uses.add(use);
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
