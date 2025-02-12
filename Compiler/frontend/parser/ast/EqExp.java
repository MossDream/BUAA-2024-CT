package frontend.parser.ast;

import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description EqExp
 */

public class EqExp {
    ArrayList<RelExp> relExps;
    ArrayList<Token> eqOps;

    public EqExp() {
        relExps = new ArrayList<>();
        eqOps = new ArrayList<>();
    }

    public ArrayList<RelExp> getRelExps() {
        return relExps;
    }

    public ArrayList<Token> getEqOps() {
        return eqOps;
    }

    public boolean hasNot() {
        UnaryExp head = relExps.get(0).getAddExps().get(0).
                getMulExps().get(0).getUnaryExps().get(0);
        return head.hasNot();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <EqExp> begin\n");
        int index = 0;
        for (RelExp relExp : relExps) {
            sb.append(relExp.toString());
            sb.append("<EqExp>\n");
            if (index < eqOps.size()) {
                sb.append(eqOps.get(index).toString()).append("\n");
            }
            index++;
        }
        return sb.toString();
    }

    public void parse() {
        RelExp relExp = new RelExp();
        relExp.parse();
        relExps.add(relExp);
        Token token = Lexer.nextToken();
        while (token.isEq()) {
            eqOps.add(token);
            relExp = new RelExp();
            relExp.parse();
            relExps.add(relExp);
            token = Lexer.nextToken();
        }
        Lexer.rollback(1);
    }
}
