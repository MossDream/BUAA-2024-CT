package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;

/**
 * @Description LVal
 */

public class LVal {
    private Token id;
    private AddExp exp;

    public LVal() {
        exp = null;
    }

    public Token getId() {
        return id;
    }

    public AddExp getExp() {
        return exp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <LVal> begin\n");
        sb.append(id.toString()).append("\n");
        if (exp != null) {
            sb.append("LBRACK [\n");
            sb.append(exp.toString());
            sb.append("<Exp>\n");
            sb.append("RBRACK ]\n");
        }
        sb.append("<LVal>\n");
        return sb.toString();
    }

    public void parse() {
        id = Lexer.nextToken();
        Token token = Lexer.nextToken();
        if (token.isLbrack()) {
            exp = new AddExp();
            exp.parse();
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
