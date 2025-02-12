package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;

/**
 * @Description IfStmt
 */

public class IfStmt {
    private LOrExp condition;
    private Stmt thenStmt;
    private Stmt elseStmt;

    public IfStmt() {
        elseStmt = null;
    }

    public LOrExp getCondition() {
        return condition;
    }

    public Stmt getThenStmt() {
        return thenStmt;
    }

    public Stmt getElseStmt() {
        return elseStmt;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <IfStmt> begin\n");
        sb.append("IFTK if\n");
        sb.append("LPARENT (\n");
        sb.append(condition.toString());
        sb.append("<Cond>\n");
        sb.append("RPARENT )\n");
        sb.append(thenStmt.toString());
        if (elseStmt != null) {
            sb.append("ELSETK else\n");
            sb.append(elseStmt.toString());
        }
        return sb.toString();
    }

    public void parse() {
        Lexer.nextToken();
        Lexer.nextToken();
        condition = new LOrExp();
        condition.parse();
        if (!Lexer.nextToken().isRparent()) {
            // 缺少右小括号
            ErrorChecker.addError(
                    ErrorType.MISSING_RPARENT,
                    Lexer.getLastLineNum()
            );
            Lexer.rollback(1);
        }
        thenStmt = new Stmt();
        thenStmt.parse();
        Token token = Lexer.nextToken();
        if (token.isElse()) {
            elseStmt = new Stmt();
            elseStmt.parse();
        } else {
            Lexer.rollback(1);
        }
    }
}
