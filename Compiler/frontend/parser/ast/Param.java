package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;
import frontend.lexer.TokenType;

/**
 * @Description Param
 */

public class Param {
    private TokenType basicType;
    private Token id;
    private boolean isArray;

    public Param() {
        this.isArray = false;
    }

    public TokenType getBasicType() {
        return basicType;
    }

    public Token getId() {
        return id;
    }

    public boolean isArray() {
        return isArray;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <Param> begin\n");
        if (basicType == TokenType.INT) {
            sb.append("INTTK int\n");
        } else {
            sb.append("CHARTK char\n");
        }
        sb.append(id.toString()).append("\n");
        if (isArray) {
            sb.append("LBRACK [\n");
            sb.append("RBRACK ]\n");
        }
        sb.append("<FuncFParam>\n");
        return sb.toString();
    }

    public void parse() {
        basicType = Lexer.nextToken().getType();
        id = Lexer.nextToken();
        Token token = Lexer.nextToken();
        if (token.isLbrack()) {
            isArray = true;
            token = Lexer.nextToken();
            if (!token.isRbrack()) {
                // 缺少右中括号，语法错误
                ErrorChecker.addError(
                        ErrorType.MISSING_RBRACK,
                        Lexer.getLastLineNum()
                );
                Lexer.rollback(1);
            }
        } else {
            Lexer.rollback(1);
        }
    }
}
