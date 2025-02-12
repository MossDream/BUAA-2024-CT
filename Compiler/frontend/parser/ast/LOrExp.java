package frontend.parser.ast;

import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description LOrExp
 */

public class LOrExp {
    private ArrayList<LAndExp> LAndExps;
    private ArrayList<Token> orOps;

    public LOrExp() {
        this.LAndExps = new ArrayList<>();
        this.orOps = new ArrayList<>();
    }

    public ArrayList<LAndExp> getLAndExps() {
        return LAndExps;
    }

    public ArrayList<Token> getOrOps() {
        return orOps;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <LOrExp> begin\n");
        int index = 0;
        for (LAndExp LAndExp : LAndExps) {
            sb.append(LAndExp.toString());
            sb.append("<LOrExp>\n");
            if (index < orOps.size()) {
                sb.append(orOps.get(index).toString()).append("\n");
            }
            index++;
        }
        return sb.toString();
    }

    public void parse() {
        LAndExp LAndExp = new LAndExp();
        LAndExp.parse();
        LAndExps.add(LAndExp);
        Token token = Lexer.nextToken();
        while (token.isOr()) {
            orOps.add(token);
            LAndExp = new LAndExp();
            LAndExp.parse();
            LAndExps.add(LAndExp);
            token = Lexer.nextToken();
        }
        Lexer.rollback(1);
    }
}
