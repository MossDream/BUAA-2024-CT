package frontend.lexer;

import java.util.Objects;

/**
 * @Description Token
 */

public class Token {
    private TokenType type;
    private String value;
    private int lineNum;

    public Token(TokenType type, String value, int lineNum) {
        this.type = type;
        this.value = value;
        this.lineNum = lineNum;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getLineNum() {
        return lineNum;
    }

    public boolean isConst() {
        return type == TokenType.CONST;
    }

    public boolean isBasicType() {
        return (type == TokenType.INT || type == TokenType.CHAR);
    }

    public boolean isFuncType() {
        return (type == TokenType.VOID || isBasicType());
    }

    public boolean isId() {
        return type == TokenType.IDSY;
    }

    public boolean isIdOrMain() {
        return isId() || type == TokenType.MAIN;
    }

    public boolean isLparent() {
        return type == TokenType.LPARENSY;
    }

    public boolean isRparent() {
        return type == TokenType.RPARENSY;
    }

    public boolean isSemi() {
        return type == TokenType.SEMISY;
    }

    public boolean isComma() {
        return type == TokenType.COMMASY;
    }

    public boolean isLbrack() {
        return type == TokenType.LBRACKSY;
    }

    public boolean isRbrack() {
        return type == TokenType.RBRACKSY;
    }

    public boolean isLbrace() {
        return type == TokenType.LBRACESY;
    }

    public boolean isRbrace() {
        return type == TokenType.RBRACESY;
    }

    public boolean isStrConst() {
        return type == TokenType.STRINGSY;
    }

    public boolean isAssign() {
        return type == TokenType.ASSIGNSY;
    }

    public boolean isAdd() {
        return (type == TokenType.PLUSSY || type == TokenType.MINUSSY);
    }

    public boolean isMul() {
        return (type == TokenType.MULTSY || type == TokenType.DIVISY || type == TokenType.MODSY);
    }

    public boolean isNumber() {
        return type == TokenType.INTSY;
    }

    public boolean isCharacter() {
        return type == TokenType.CHARSY;
    }

    public boolean isUnaryOp() {
        return (type == TokenType.PLUSSY || type == TokenType.MINUSSY || type == TokenType.NOTSY);
    }

    public boolean isOr() {
        return type == TokenType.ORSY;
    }

    public boolean isAnd() {
        return type == TokenType.ANDSY;
    }

    public boolean isEq() {
        return (type == TokenType.EQUSY || type == TokenType.NOTEQSY);
    }

    public boolean isNot() {
        return type == TokenType.NOTSY;
    }

    public boolean isRelOp() {
        return (type == TokenType.LESSSY || type == TokenType.LESSEQSY
                || type == TokenType.GREATERSY || type == TokenType.GREATEREQSY);
    }

    public boolean isIf() {
        return type == TokenType.IF;
    }

    public boolean isFor() {
        return type == TokenType.FOR;
    }

    public boolean isBreak() {
        return type == TokenType.BREAK;
    }

    public boolean isContinue() {
        return type == TokenType.CONTINUE;
    }

    public boolean isReturn() {
        return type == TokenType.RETURN;
    }

    public boolean isPrintf() {
        return type == TokenType.PRINTF;
    }

    public boolean isGetInt() {
        return type == TokenType.GETINT;
    }

    public boolean isGetChar() {
        return type == TokenType.GETCHAR;
    }

    public boolean isElse() {
        return type == TokenType.ELSE;
    }

    @Override
    public String toString() {
        return type + " " + value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        Token token = (Token) obj;
        return type == token.type && value.equals(token.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }
}
