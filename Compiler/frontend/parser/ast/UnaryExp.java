package frontend.parser.ast;

import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description UnaryExp
 */

public class UnaryExp {

    private PrimaryExp primaryExp;
    private FuncCall funcCall;
    private ArrayList<Token> unaryOps;
    private boolean isfuncCall;

    public UnaryExp() {
        unaryOps = new ArrayList<>();
        isfuncCall = false;
    }

    public PrimaryExp getPrimaryExp() {
        return primaryExp;
    }

    public FuncCall getFuncCall() {
        return funcCall;
    }

    public boolean isFuncCall() {
        return isfuncCall;
    }

    public ArrayList<Token> getUnaryOps() {
        return unaryOps;
    }

    public boolean hasNot() {
        boolean hasNot = false;
        for (Token unaryOp : unaryOps) {
            if (unaryOp.isNot()) {
                hasNot = !hasNot;
            }
        }
        return hasNot;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <UnaryExp> begin\n");
        for (Token unaryOp : unaryOps) {
            sb.append(unaryOp.toString()).append("\n");
            sb.append("<UnaryOp>\n");
        }
        if (isfuncCall) {
            sb.append(funcCall.toString());
        } else {
            sb.append(primaryExp.toString());
        }
        for (int i = 0; i < unaryOps.size(); i++) {
            sb.append("<UnaryExp>\n");
        }
        sb.append("<UnaryExp>\n");
        return sb.toString();
    }

    public void parse() {
        Token token = Lexer.nextToken();
        while (token.isUnaryOp()) {
            unaryOps.add(token);
            token = Lexer.nextToken();
        }
        Token nextToken = Lexer.nextToken();
        Lexer.rollback(2);
        if (token.isId() && nextToken.isLparent()) {
            isfuncCall = true;
            funcCall = new FuncCall();
            funcCall.parse();
        } else {
            primaryExp = new PrimaryExp();
            primaryExp.parse();
        }
    }


}
