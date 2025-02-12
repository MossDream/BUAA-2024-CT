package frontend.parser.ast;

import frontend.lexer.Lexer;
import frontend.lexer.Token;

/**
 * @Description BlockItem
 */

public class BlockItem {
    private Decl decl;
    private Stmt stmt;
    private boolean isDecl;
    private boolean isDeclConst;

    public BlockItem() {
        this.isDecl = false;
        this.isDeclConst = false;
    }

    public Decl getDecl() {
        return decl;
    }

    public Stmt getStmt() {
        return stmt;
    }

    public boolean isDecl() {
        return isDecl;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <BlockItem> begin\n");
        if (isDecl) {
            sb.append(decl.toString());
        } else {
            sb.append(stmt.toString());
        }
        return sb.toString();
    }

    public void parse() {
        Token token = Lexer.nextToken();
        Token nextToken = Lexer.nextToken();
        Lexer.rollback(2);
        if (token.isConst()) {
            Lexer.nextToken();
            isDecl = true;
            isDeclConst = true;
            decl = new Decl(true);
            decl.parse();
        } else if (token.isBasicType() && nextToken.isId()) {
            isDecl = true;
            isDeclConst = false;
            decl = new Decl(false);
            decl.parse();
        } else {
            stmt = new Stmt();
            stmt.parse();
        }
    }
}
