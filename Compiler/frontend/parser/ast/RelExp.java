package frontend.parser.ast;

import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description RelExp
 */

public class RelExp {
    ArrayList<AddExp> addExps;
    ArrayList<Token> relOps;

    public RelExp() {
        addExps = new ArrayList<>();
        relOps = new ArrayList<>();
    }

    public ArrayList<AddExp> getAddExps() {
        return addExps;
    }

    public ArrayList<Token> getRelOps() {
        return relOps;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <RelExp> begin\n");
        int index = 0;
        for (AddExp addExp : addExps) {
            sb.append(addExp.toString());
            sb.append("<RelExp>\n");
            if (index < relOps.size()) {
                sb.append(relOps.get(index).toString()).append("\n");
            }
            index++;
        }
        return sb.toString();
    }

    public void parse() {
        AddExp addExp = new AddExp();
        addExp.parse();
        addExps.add(addExp);
        Token token = Lexer.nextToken();
        while (token.isRelOp()) {
            relOps.add(token);
            addExp = new AddExp();
            addExp.parse();
            addExps.add(addExp);
            token = Lexer.nextToken();
        }
        Lexer.rollback(1);
    }
}
