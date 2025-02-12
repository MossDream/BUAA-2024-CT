package frontend.parser.ast;

import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description LAndExp
 */

public class LAndExp {
    private ArrayList<EqExp> eqExps;
    private ArrayList<Token> andOps;

    public LAndExp() {
        eqExps = new ArrayList<>();
        andOps = new ArrayList<>();
    }

    public ArrayList<EqExp> getEqExps() {
        return eqExps;
    }

    public ArrayList<Token> getAndOps() {
        return andOps;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <LAndExp> begin\n");
        int index = 0;
        for (EqExp eqExp : eqExps) {
            sb.append(eqExp.toString());
            sb.append("<LAndExp>\n");
            if (index < andOps.size()) {
                sb.append(andOps.get(index).toString()).append("\n");
            }
            index++;
        }
        return sb.toString();
    }

    public void parse() {
        EqExp eqExp = new EqExp();
        eqExp.parse();
        eqExps.add(eqExp);
        Token token = Lexer.nextToken();
        while (token.isAnd()) {
            andOps.add(token);
            eqExp = new EqExp();
            eqExp.parse();
            eqExps.add(eqExp);
            token = Lexer.nextToken();
        }
        Lexer.rollback(1);
    }
}
