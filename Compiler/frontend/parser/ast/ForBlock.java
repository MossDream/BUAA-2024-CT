package frontend.parser.ast;

import frontend.lexer.Lexer;

/**
 * @Description ForBlock
 */

public class ForBlock {
    private LVal lval;
    private AddExp exp;

    public ForBlock() {

    }

    public LVal getLVal() {
        return lval;
    }

    public AddExp getExp() {
        return exp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <ForBlock> begin\n");
        sb.append(lval.toString());
        sb.append("ASSIGN =\n");
        sb.append(exp.toString());
        sb.append("<Exp>\n");
        sb.append("<ForStmt>\n");
        return sb.toString();
    }

    public void parse() {
        lval = new LVal();
        lval.parse();
        Lexer.nextToken();
        exp = new AddExp();
        exp.parse();
    }
}
