package backend.mips.globallabel;

/**
 * @Description GlobalLabel
 */

public class GlobalLabel {
    private String name;
    // 三种情况： .word .byte .asciiz
    private String type;
    private String initValue;

    public GlobalLabel(String name, String type, String initValue) {
        this.name = name;
        this.type = type;
        this.initValue = initValue;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "\t" + name + ": " + type + " " + initValue + "\n";
    }
}
