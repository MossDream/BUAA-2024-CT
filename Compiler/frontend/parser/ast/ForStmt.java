package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;

/**
 * @Description ForStmt
 */

public class ForStmt {
    private ForBlock init;
    private LOrExp condition;
    private ForBlock update;
    private Stmt body;

    public ForStmt() {
        init = null;
        condition = null;
        update = null;
    }

    public ForBlock getInit() {
        return init;
    }

    public LOrExp getCondition() {
        return condition;
    }

    public ForBlock getUpdate() {
        return update;
    }

    public Stmt getBody() {
        return body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <ForStmt> begin\n");
        sb.append("FORTK for\n");
        sb.append("LPARENT (\n");
        if (init != null) {
            sb.append(init.toString());
        }
        sb.append("SEMICN ;\n");
        if (condition != null) {
            sb.append(condition.toString());
            sb.append("<Cond>\n");
        }
        sb.append("SEMICN ;\n");
        if (update != null) {
            sb.append(update.toString());
        }
        sb.append("RPARENT )\n");
        sb.append(body.toString());
        return sb.toString();
    }

    public void parse() {
        Lexer.nextToken();
        Lexer.nextToken();
        Token token = Lexer.nextToken();
        if (!token.isSemi()) {
            Lexer.rollback(1);
            init = new ForBlock();
            init.parse();
            if (!Lexer.nextToken().isSemi()) {
                //缺少分号
                ErrorChecker.addError(
                        ErrorType.MISSING_SEMICOLON,
                        Lexer.getLastLineNum()
                );
                Lexer.rollback(1);
            }
        }
        token = Lexer.nextToken();
        if (!token.isSemi()) {
            Lexer.rollback(1);
            condition = new LOrExp();
            condition.parse();
            if (!Lexer.nextToken().isSemi()) {
                //缺少分号
                ErrorChecker.addError(
                        ErrorType.MISSING_SEMICOLON,
                        Lexer.getLastLineNum()
                );
                Lexer.rollback(1);
            }
        }
        token = Lexer.nextToken();
        if (!token.isRparent()) {
            Lexer.rollback(1);
            update = new ForBlock();
            update.parse();
            if (!Lexer.nextToken().isRparent()) {
                //缺少右小括号
                ErrorChecker.addError(
                        ErrorType.MISSING_RPARENT,
                        Lexer.getLastLineNum()
                );
                Lexer.rollback(1);
            }
        }
        body = new Stmt();
        body.parse();
    }
}
