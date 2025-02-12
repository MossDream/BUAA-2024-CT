package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;

/**
 * @Description ReturnStmt
 */

public class ReturnStmt {
    private AddExp exp;

    public ReturnStmt() {
        exp = null;
    }

    public AddExp getExp() {
        return exp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <ReturnStmt> begin\n");
        sb.append("RETURNTK return\n");
        if (exp != null) {
            sb.append(exp.toString());
            sb.append("<Exp>\n");
        }
        sb.append("SEMICN ;\n");
        return sb.toString();
    }

    public void parse() {
        Lexer.nextToken();
        Token token = Lexer.nextToken();
        if (token.isSemi()) {
            return;
        }
        Lexer.rollback(1);
        exp = new AddExp();
        exp.parse();
        if (!Lexer.nextToken().isSemi()) {
            //缺少分号
            ErrorChecker.addError(
                    ErrorType.MISSING_SEMICOLON,
                    Lexer.getLastLineNum()
            );
            Lexer.rollback(1);
        }
    }
}
