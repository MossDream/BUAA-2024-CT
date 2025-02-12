package frontend.parser.ast;

import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description AddExp
 */

public class AddExp {
    private ArrayList<MulExp> mulExps;
    private ArrayList<Token> addOps;

    public AddExp() {
        mulExps = new ArrayList<>();
        addOps = new ArrayList<>();
    }

    public ArrayList<MulExp> getMulExps() {
        return mulExps;
    }

    public ArrayList<Token> getAddOps() {
        return addOps;
    }

    public boolean isLVal() {
        UnaryExp head = mulExps.get(0).getUnaryExps().get(0);
        if (head.isFuncCall()) {
            return false;
        }
        if (head.getPrimaryExp() == null) {
            return false;
        }
        return head.getPrimaryExp().isLVal();
    }

    public String getHeadName() {
        if (!this.isLVal()) {
            return null;
        }
        PrimaryExp primaryExp = mulExps.get(0).getUnaryExps().get(0).
                getPrimaryExp();
        if (primaryExp.getType() == PrimaryExp.Type.LVAL) {
            return primaryExp.getLVal().getId().getValue();
        } else {
            return primaryExp.getExp().getHeadName();
        }
    }

    public boolean isReferenced() {
        if (!this.isLVal()) {
            return false;
        }
        PrimaryExp primaryExp = mulExps.get(0).getUnaryExps().get(0).
                getPrimaryExp();
        if (primaryExp.getType() == PrimaryExp.Type.LVAL) {
            return primaryExp.getLVal().getExp() != null;
        } else {
            return primaryExp.getExp().isReferenced();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <AddExp> begin\n");
        int index = 0;
        for (MulExp mulExp : mulExps) {
            sb.append(mulExp.toString());
            sb.append("<AddExp>\n");
            if (index < addOps.size()) {
                sb.append(addOps.get(index).toString()).append("\n");
            }
            index++;
        }
        return sb.toString();
    }

    public void parse() {
        MulExp mulExp = new MulExp();
        mulExp.parse();
        mulExps.add(mulExp);
        Token token = Lexer.nextToken();
        while (token.isAdd()) {
            addOps.add(token);
            mulExp = new MulExp();
            mulExp.parse();
            mulExps.add(mulExp);
            token = Lexer.nextToken();
        }
        Lexer.rollback(1);
    }
}
