package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;

/**
 * @Description PrimaryExp
 */

public class PrimaryExp {
    private Token number;
    private Token character;
    private LVal lval;
    private AddExp exp;

    private Type type;

    public enum Type {
        NUM, CHAR, LVAL, EXP, NULL
    }

    public PrimaryExp() {
    }

    public boolean isLVal() {
        boolean isLVal = false;
        if(type == Type.LVAL) {
            isLVal = true;
        } else if (type == Type.EXP) {
            isLVal = exp.isLVal();
        }
        return isLVal;
    }

    public LVal getLVal() {
        return lval;
    }

    public Type getType() {
        return type;
    }

    public AddExp getExp() {
        return exp;
    }

    public Token getNumber() {
        return number;
    }

    public Token getCharacter() {
        return character;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <PrimaryExp> begin\n");
        switch (type) {
            case NUM:
                sb.append(number.toString()).append("\n");
                sb.append("<Number>\n");
                break;
            case CHAR:
                sb.append(character.toString()).append("\n");
                sb.append("<Character>\n");
                break;
            case LVAL:
                sb.append(lval.toString());
                break;
            case EXP:
                sb.append("LPARENT (\n");
                sb.append(exp.toString());
                sb.append("<Exp>\n");
                sb.append("RPARENT )\n");
                break;
            default:
                break;
        }
        sb.append("<PrimaryExp>\n");
        return sb.toString();
    }

    public void parse() {
        Token token = Lexer.nextToken();
        if (token.isNumber()) {
            number = token;
            type = Type.NUM;
        } else if (token.isCharacter()) {
            character = token;
            type = Type.CHAR;
        } else if (token.isLparent()) {
            exp = new AddExp();
            exp.parse();
            token = Lexer.nextToken();
            if (!token.isRparent()) {
                // 缺少右小括号，语法错误
                ErrorChecker.addError(
                        ErrorType.MISSING_RPARENT,
                        Lexer.getLastLineNum()
                );
                Lexer.rollback(1);
            }
            type = Type.EXP;
        } else if (token.isId()) {
            Lexer.rollback(1);
            lval = new LVal();
            lval.parse();
            type = Type.LVAL;
        } else {
            Lexer.rollback(1);
            type = Type.NULL;
        }
    }
}
