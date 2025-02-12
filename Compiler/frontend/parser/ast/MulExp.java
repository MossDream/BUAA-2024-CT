package frontend.parser.ast;

import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description MulExp
 */

public class MulExp {
    private ArrayList<UnaryExp> unaryExps;
    private ArrayList<Token> mulOps;

    public MulExp() {
        unaryExps = new ArrayList<>();
        mulOps = new ArrayList<>();
    }

    public ArrayList<UnaryExp> getUnaryExps() {
        return unaryExps;
    }

    public ArrayList<Token> getMulOps() {
        return mulOps;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <MulExp> begin\n");
        int index = 0;
        for (UnaryExp unaryExp : unaryExps) {
            sb.append(unaryExp.toString());
            sb.append("<MulExp>\n");
            if (index < mulOps.size()) {
                sb.append(mulOps.get(index).toString()).append("\n");
            }
            index++;
        }
        return sb.toString();
    }

    public void parse() {
        UnaryExp unaryExp = new UnaryExp();
        unaryExp.parse();
        unaryExps.add(unaryExp);
        Token token = Lexer.nextToken();
        while (token.isMul()) {
            mulOps.add(token);
            unaryExp = new UnaryExp();
            unaryExp.parse();
            unaryExps.add(unaryExp);
            token = Lexer.nextToken();
        }
        Lexer.rollback(1);
    }
}
