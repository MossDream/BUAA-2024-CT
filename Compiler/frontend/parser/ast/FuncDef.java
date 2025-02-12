package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;
import frontend.lexer.TokenType;

import java.util.ArrayList;

/**
 * @Description FuncDef
 */

public class FuncDef {
    private TokenType funcType;
    private Token id;
    private ArrayList<Param> params;
    private Block block;
    private boolean isMain;

    public FuncDef() {
        params = new ArrayList<>();
        isMain = false;
    }

    public TokenType getFuncType() {
        return funcType;
    }

    public Token getId() {
        return id;
    }

    public ArrayList<Param> getParams() {
        return params;
    }

    public Block getBlock() {
        return block;
    }

    public boolean isMain() {
        return isMain;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <FuncDef> begin\n");
        if (isMain) {
            sb.append("INTTK int\n");
        } else {
            switch (funcType) {
                case VOID:
                    sb.append("VOIDTK void\n");
                    break;
                case INT:
                    sb.append("INTTK int\n");
                    break;
                case CHAR:
                    sb.append("CHARTK char\n");
                    break;
                default:
                    break;
            }
            sb.append("<FuncType>\n");
        }
        sb.append(id.toString()).append("\n");
        sb.append("LPARENT (\n");
        for (Param param : params) {
            sb.append(param.toString());
            if (params.indexOf(param) != params.size() - 1) {
                sb.append("COMMA ,\n");
            }
        }
        if (!isMain && params.size() > 0) {
            sb.append("<FuncFParams>\n");
        }
        sb.append("RPARENT )\n");
        sb.append(block.toString());
        sb.append(isMain ? "<MainFuncDef>\n" : "<FuncDef>\n");
        return sb.toString();
    }

    public void parse() {
        funcType = Lexer.nextToken().getType();
        id = Lexer.nextToken();
        if (id.getType() == TokenType.MAIN) {
            isMain = true;
            Lexer.nextToken();
            if (!Lexer.nextToken().isRparent()) {
                // 缺少右小括号，语法错误
                ErrorChecker.addError(
                        ErrorType.MISSING_RPARENT,
                        Lexer.getLastLineNum()
                );
                Lexer.rollback(1);
            }
        } else {
            // 读左小括号
            Lexer.nextToken();
            // 读参数
            Token token = Lexer.nextToken();
            if (!token.isRparent()) {
                Lexer.rollback(1);
                Token specifier = Lexer.nextToken();
                // 对无参数且缺少右小括号的情况进行处理
                if (!specifier.isLbrace()) {
                    Lexer.rollback(1);
                    do {
                        Param param = new Param();
                        param.parse();
                        params.add(param);
                        token = Lexer.nextToken();
                    } while (token.isComma());
                }
                if (!token.isRparent()) {
                    // 缺少右小括号，语法错误
                    ErrorChecker.addError(
                            ErrorType.MISSING_RPARENT,
                            Lexer.getLastLineNum()
                    );
                    Lexer.rollback(1);
                }
            }
        }
        block = new Block();
        block.parse();
    }
}
