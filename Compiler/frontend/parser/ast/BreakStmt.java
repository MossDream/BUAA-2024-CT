package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;

/**
 * @Description BreakStmt
 */

public class BreakStmt {
    public BreakStmt() {

    }

    @Override
    public String toString() {
        return "BREAKTK break\n" +
                "SEMICN ;\n";
    }

    public void parse() {
        Lexer.nextToken();
        if (!Lexer.nextToken().isSemi()) {
            // 缺少分号
            ErrorChecker.addError(
                    ErrorType.MISSING_SEMICOLON,
                    Lexer.getLastLineNum()
            );
            Lexer.rollback(1);
        }
    }
}
