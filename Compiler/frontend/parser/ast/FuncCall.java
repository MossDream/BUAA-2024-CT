package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description FuncCall
 */

public class FuncCall {
    private Token id;
    private ArrayList<AddExp> args;
    //针对语义评测时错误重叠问题
    private boolean isChecked;

    public FuncCall() {
        args = new ArrayList<>();
        isChecked = false;
    }

    public Token getId() {
        return id;
    }

    public int getArgsNum() {
        return args.size();
    }

    public ArrayList<AddExp> getArgs() {
        return args;
    }

    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <FuncCall> begin\n");
        sb.append(id.toString()).append("\n");
        sb.append("LPARENT (\n");
        for (AddExp arg : args) {
            sb.append(arg.toString());
            sb.append("<Exp>\n");
            if (args.indexOf(arg) != args.size() - 1) {
                sb.append("COMMA ,\n");
            }
        }
        if (args.size() > 0) {
            sb.append("<FuncRParams>\n");
        }
        sb.append("RPARENT )\n");
        return sb.toString();
    }

    public void parse() {
        id = Lexer.nextToken();
        // 读左小括号
        Lexer.nextToken();
        // 读参数
        Token token = Lexer.nextToken();
        if (token.isRparent()) {
            return;
        }
        Lexer.rollback(1);
        do {
            AddExp exp = new AddExp();
            exp.parse();
            args.add(exp);
            token = Lexer.nextToken();
        } while (token.isComma());
        if (!token.isRparent()) {
            // 缺少右小括号，语法错误
            ErrorChecker.addError(
                    ErrorType.MISSING_RPARENT,
                    Lexer.getLastLineNum()
            );
            Lexer.rollback(1);
            isChecked = true;
        }
    }
}
