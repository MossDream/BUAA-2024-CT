package backend.mips.instruction;

/**
 * @Description SysCall
 */

public class SysCall extends MipsInst {
    // 单例模式
    private static SysCall instance = new SysCall();

    private SysCall() {
    }

    public static SysCall getInstance() {
        return instance;
    }

    @Override
    public String toString() {
        return "syscall";
    }
}
