package frontend.parser.ast;

import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description CompUnit
 */

public class CompUnit {
    private ArrayList<Decl> decls;
    private ArrayList<FuncDef> funcDefs;
    private FuncDef mainFuncDef;

    public CompUnit() {
        decls = new ArrayList<>();
        funcDefs = new ArrayList<>();
    }

    public ArrayList<Decl> getDecls() {
        return decls;
    }

    public ArrayList<FuncDef> getFuncDefs() {
        return funcDefs;
    }

    public FuncDef getMainFuncDef() {
        return mainFuncDef;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <CompUnit> begin\n");
        for (Decl decl : decls) {
            sb.append(decl.toString());
        }
        for (FuncDef funcDef : funcDefs) {
            sb.append(funcDef.toString());
        }
        if (mainFuncDef != null) {
            sb.append(mainFuncDef.toString());
        }
        sb.append("<CompUnit>");
        return sb.toString();
    }

    public void parse() {
        if (Lexer.isEnd()) {
            return;
        }
        Token token = Lexer.nextToken();
        while (token.isFuncType() || token.isConst()) {
            if (token.isConst()) {
                // 一定是常变量声明
                Decl decl = new Decl(true);
                decl.parse();
                decls.add(decl);
            } else if (Lexer.nextToken().isIdOrMain()) {
                // 函数定义或者变量声明
                if (Lexer.nextToken().isLparent()) {
                    // 函数定义
                    Lexer.rollback(3);
                    FuncDef funcDef = new FuncDef();
                    funcDef.parse();
                    if (funcDef.isMain()) {
                        mainFuncDef = funcDef;
                    } else {
                        funcDefs.add(funcDef);
                    }
                } else {
                    Lexer.rollback(3);
                    Decl decl = new Decl(false);
                    decl.parse();
                    decls.add(decl);
                }
            }
            if (Lexer.isEnd()) {
                break;
            }
            token = Lexer.nextToken();
        }
    }
}
