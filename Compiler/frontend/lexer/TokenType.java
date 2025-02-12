package frontend.lexer;

/**
 * @Description TokenType
 */

public enum TokenType {
    // 保留字
    CONST("CONSTTK"),
    MAIN("MAINTK"),
    IF("IFTK"),
    INT("INTTK"),
    CHAR("CHARTK"),
    VOID("VOIDTK"),
    ELSE("ELSETK"),
    FOR("FORTK"),
    CONTINUE("CONTINUETK"),
    BREAK("BREAKTK"),
    RETURN("RETURNTK"),
    GETINT("GETINTTK"),
    GETCHAR("GETCHARTK"),
    PRINTF("PRINTFTK"),
    // 标识符
    IDSY("IDENFR"),
    // 字面值
    INTSY("INTCON"),
    CHARSY("CHRCON"),
    STRINGSY("STRCON"),
    // 运算符
    PLUSSY("PLUS"),
    MINUSSY("MINU"),
    MULTSY("MULT"),
    DIVISY("DIV"),
    MODSY("MOD"),
    ANDSY("AND"),
    ORSY("OR"),
    NOTSY("NOT"),
    LESSSY("LSS"),
    LESSEQSY("LEQ"),
    GREATERSY("GRE"),
    GREATEREQSY("GEQ"),
    EQUSY("EQL"),
    NOTEQSY("NEQ"),
    // 分隔符
    LPARENSY("LPARENT"),
    RPARENSY("RPARENT"),
    LBRACKSY("LBRACK"),
    RBRACKSY("RBRACK"),
    LBRACESY("LBRACE"),
    RBRACESY("RBRACE"),
    COMMASY("COMMA"),
    SEMISY("SEMICN"),
    ASSIGNSY("ASSIGN"),
    // 无法识别的单词
    ERROR("ERROR"),;

    private final String type;

    TokenType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
