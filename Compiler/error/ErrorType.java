package error;

/**
 * @Description ErrorType
 */

public enum ErrorType {
    // 词法错误
    // 非法的符号，特指单个&或者|
    ILLEGAL_SYMBOL("a"),
    // 语法错误
    // 缺少分号
    MISSING_SEMICOLON("i"),
    // 缺少右小括号
    MISSING_RPARENT("j"),
    // 缺少右中括号
    MISSING_RBRACK("k"),
    // 语义错误
    // 重定义
    REDEFINITION("b"),
    // 未定义
    UNDEFINED("c"),
    // 函数参数个数不匹配
    PARAMS_NUM_MISMATCH("d"),
    // 函数参数类型不匹配
    PARAM_TYPE_MISMATCH("e"),
    // 多余return语句
    REDUNDANT_RETURN("f"),
    // 缺少return语句
    MISSING_RETURN("g"),
    // 改变常量的值
    CHANGE_CONST("h"),
    // printf表达式与格式字符数量不匹配
    PRINTF_MISMATCH("l"),
    // 非循环块内break或continue
    BREAK_CONTINUE("m");


    private String message;

    ErrorType(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
