package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;

/**
 * @Description Def
 */

public class Def {
    private Token id;
    private AddExp exp;
    private InitVal initVal;
    private boolean isConst;

    public Def(boolean isConst) {
        this.isConst = isConst;
        exp = null;
        initVal = null;
    }

    public Token getId() {
        return id;
    }

    public AddExp getExp() {
        return exp;
    }

    public InitVal getInitVal() {
        return initVal;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <Def> begin\n");
        sb.append(id.toString()).append("\n");
        if (exp != null) {
            sb.append("LBRACK [\n");
            sb.append(exp.toString());
            sb.append("<ConstExp>\n");
            sb.append("RBRACK ]\n");
        }
        if (initVal != null) {
            sb.append("ASSIGN =\n");
            sb.append(initVal.toString());
        }
        sb.append(isConst ? "<ConstDef>\n" : "<VarDef>\n");
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
            token = Lexer.nextToken();
        }
        if (token.isAssign()) {
            initVal = new InitVal(isConst);
            initVal.parse();
        } else {
            Lexer.rollback(1);
        }
    }
}
