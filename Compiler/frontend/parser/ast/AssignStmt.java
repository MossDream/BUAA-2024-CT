package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;

/**
 * @Description AssignStmt
 */

public class AssignStmt {
    private LVal lval;
    private AddExp exp;

    public AssignStmt(LVal lval) {
        this.lval = lval;
        exp = null;
    }

    public AssignStmt(LVal lval, AddExp exp) {
        this.lval = lval;
        this.exp = exp;
        this.type = Type.COMMON;
    }

    public LVal getLVal() {
        return lval;
    }

    public AddExp getExp() {
        return exp;
    }

    public AssignStmt.Type getType() {
        return type;
    }

    public enum Type {
        COMMON, GETINT, GETCHAR
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <AssignStmt> begin\n");
        sb.append(lval.toString());
        sb.append("ASSIGN =\n");
        if (exp != null) {
            sb.append(exp.toString());
            sb.append("<Exp>\n");
        } else {
            if (type == Type.GETINT) {
                sb.append("GETINTTK getint\n");
            } else {
                sb.append("GETCHARTK getchar\n");
            }
            sb.append("LPARENT (\n");
            sb.append("RPARENT )\n");
        }
        sb.append("SEMICN ;\n");
        return sb.toString();
    }

    private Type type;

    public void parse() {
        // 读赋值符号
        Lexer.nextToken();
        // 读表达式
        Token token = Lexer.nextToken();
        if (token.isGetInt() || token.isGetChar()) {
            if (token.isGetInt()) {
                type = Type.GETINT;
            } else {
                type = Type.GETCHAR;
            }
            Lexer.nextToken();
            if (!Lexer.nextToken().isRparent()) {
                // 缺少右小括号，语法错误
                ErrorChecker.addError(
                        ErrorType.MISSING_RPARENT,
                        Lexer.getLastLineNum()
                );
                Lexer.rollback(1);
            }
            if (!Lexer.nextToken().isSemi()) {
                // 缺少分号，语法错误
                ErrorChecker.addError(
                        ErrorType.MISSING_SEMICOLON,
                        Lexer.getLastLineNum()
                );
                Lexer.rollback(1);
            }
        } else {
            Lexer.rollback(1);
            type = Type.COMMON;
            exp = new AddExp();
            exp.parse();
            if (!Lexer.nextToken().isSemi()) {
                // 缺少分号，语法错误
                ErrorChecker.addError(
                        ErrorType.MISSING_SEMICOLON,
                        Lexer.getLastLineNum()
                );
                Lexer.rollback(1);
            }
        }
    }
}
