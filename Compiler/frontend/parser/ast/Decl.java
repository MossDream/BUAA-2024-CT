package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;
import frontend.lexer.TokenType;

import java.util.ArrayList;

/**
 * @Description Decl
 */

public class Decl {
    private TokenType basicType;
    private ArrayList<Def> defs;
    private boolean isConst;

    public Decl(boolean isConst) {
        defs = new ArrayList<>();
        this.isConst = isConst;
    }

    public TokenType getBasicType() {
        return basicType;
    }

    public ArrayList<Def> getDefs() {
        return defs;
    }

    public boolean isConst() {
        return isConst;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <Decl> begin\n");
        sb.append(isConst ? "CONSTTK const\n" : "");
        if(basicType == TokenType.INT) {
            sb.append("INTTK int\n");
        } else {
            sb.append("CHARTK char\n");
        }
        for (Def def : defs) {
            sb.append(def.toString());
            // 如果不是最后一个Def，添加逗号
            if (defs.indexOf(def) != defs.size() - 1) {
                sb.append("COMMA ,\n");
            }
        }
        sb.append("SEMICN ;\n");
        sb.append(isConst ? "<ConstDecl>\n" : "<VarDecl>\n");
        return sb.toString();
    }

    public void parse() {
        // 读BasicType
        Token token = Lexer.nextToken();
        basicType = token.getType();
        do {
            Def def = new Def(isConst);
            def.parse();
            defs.add(def);
            token = Lexer.nextToken();
        } while (token.isComma());
        if (!token.isSemi()) {
            // 缺少分号，语法错误
            ErrorChecker.addError(
                    ErrorType.MISSING_SEMICOLON,
                    Lexer.getLastLineNum()
            );
            Lexer.rollback(1);
        }
    }
}
