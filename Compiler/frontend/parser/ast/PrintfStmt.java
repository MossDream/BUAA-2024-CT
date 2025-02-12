package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;

import java.util.ArrayList;

/**
 * @Description PrintfStmt
 */

public class PrintfStmt {
    private Token format;
    private ArrayList<AddExp> args;

    public PrintfStmt() {
        args = new ArrayList<>();
    }

    public Token getFormat() {
        return format;
    }

    public ArrayList<AddExp> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <PrintfStmt> begin\n");
        sb.append("PRINTFTK printf\n");
        sb.append("LPARENT (\n");
        sb.append(format.toString()).append("\n");
        for (AddExp addExp : args) {
            sb.append("COMMA ,\n");
            sb.append(addExp.toString());
            sb.append("<Exp>\n");
        }
        sb.append("RPARENT )\n");
        sb.append("SEMICN ;\n");
        return sb.toString();
    }

    public void parse() {
        Lexer.nextToken();
        Lexer.nextToken();
        format = Lexer.nextToken();
        Token token = Lexer.nextToken();
        while (token.isComma()) {
            AddExp addExp = new AddExp();
            addExp.parse();
            args.add(addExp);
            token = Lexer.nextToken();
        }
        if (!token.isRparent()) {
            //缺少右小括号
            ErrorChecker.addError(
                    ErrorType.MISSING_RPARENT,
                    Lexer.getLastLineNum()
            );
            Lexer.rollback(1);
        }
        if (!Lexer.nextToken().isSemi()) {
            //缺少分号
            ErrorChecker.addError(
                    ErrorType.MISSING_SEMICOLON,
                    Lexer.getLastLineNum()
            );
            Lexer.rollback(1);
        }
    }
}
