package frontend.parser.ast;

import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description InitVal
 */

public class InitVal {
    private ArrayList<AddExp> exps;
    private Token strToken;
    private boolean haveBrace;
    private boolean isConst;

    public InitVal(boolean isConst) {
        exps = new ArrayList<>();
        haveBrace = false;
        strToken = null;
        this.isConst = isConst;
    }

    public ArrayList<AddExp> getExps() {
        return exps;
    }

    public Token getStrToken() {
        return strToken;
    }

    public boolean haveBrace() {
        return haveBrace;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <InitVal> begin\n");
        if (haveBrace) {
            sb.append("LBRACE {\n");
        }
        if (strToken != null) {
            sb.append(strToken.toString()).append("\n");
        } else {
            for (AddExp exp : exps) {
                sb.append(exp.toString());
                if (isConst) {
                    sb.append("<ConstExp>\n");
                } else {
                    sb.append("<Exp>\n");
                }
                if (exps.indexOf(exp) != exps.size() - 1) {
                    sb.append("COMMA ,\n");
                }
            }
        }
        if (haveBrace) {
            sb.append("RBRACE }\n");
        }
        sb.append(isConst ? "<ConstInitVal>\n" : "<InitVal>\n");
        return sb.toString();
    }

    public void parse() {
        Token token = Lexer.nextToken();
        if (token.isLbrace()) {
            haveBrace = true;
            token = Lexer.nextToken();
            while (!token.isRbrace()) {
                Lexer.rollback(1);
                AddExp exp = new AddExp();
                exp.parse();
                exps.add(exp);
                token = Lexer.nextToken();
                if (token.isComma()) {
                    token = Lexer.nextToken();
                }
            }
        } else if (token.isStrConst()) {
            strToken = token;
        } else {
            Lexer.rollback(1);
            AddExp exp = new AddExp();
            exp.parse();
            exps.add(exp);
        }
    }
}
