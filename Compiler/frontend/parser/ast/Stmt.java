package frontend.parser.ast;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.Lexer;
import frontend.lexer.Token;

/**
 * @Description Stmt
 */

public class Stmt {
    private AssignStmt assignStmt;
    private AddExp expStmt;
    private IfStmt ifStmt;
    private Block blockStmt;
    private ForStmt forStmt;
    private BreakStmt breakStmt;
    private ContinueStmt continueStmt;
    private ReturnStmt returnStmt;
    private PrintfStmt printfStmt;

    private int returnLineNum;
    private int breakLineNum;
    private int continueLineNum;
    private int printfLineNum;

    private Type type;

    public enum Type {
        ASSIGN, EXP, IF, BLOCK, FOR, BREAK, CONTINUE, RETURN, PRINTF
    }

    public Stmt() {
        returnLineNum = 0;
        breakLineNum = 0;
        continueLineNum = 0;
        printfLineNum = 0;
    }

    public Type getType() {
        return type;
    }

    public int getReturnLineNum() {
        return returnLineNum;
    }

    public int getBreakLineNum() {
        return breakLineNum;
    }

    public int getContinueLineNum() {
        return continueLineNum;
    }

    public int getPrintfLineNum() {
        return printfLineNum;
    }

    public AddExp getExp() {
        return expStmt;
    }

    public AssignStmt getAssignStmt() {
        return assignStmt;
    }

    public Block getBlockStmt() {
        return blockStmt;
    }

    public PrintfStmt getPrintfStmt() {
        return printfStmt;
    }

    public ReturnStmt getReturnStmt() {
        return returnStmt;
    }

    public IfStmt getIfStmt() {
        return ifStmt;
    }

    public ForStmt getForStmt() {
        return forStmt;
    }

    public boolean hasReturnStmt() {
        boolean hasReturn = false;
        if (type == Type.BLOCK) {
            for (BlockItem blockItem : blockStmt.getBlockItems()) {
                if (!blockItem.isDecl()) {
                    hasReturn = blockItem.getStmt().hasReturnStmt();
                    if (hasReturn) {
                        break;
                    }
                }
            }
        } else if (type == Type.RETURN) {
            hasReturn = true;
        }
        return hasReturn;
    }

    public boolean hasReturnExp() {
        if (!hasReturnStmt()) {
            return false;
        }
        if (type == Type.BLOCK) {
            for (BlockItem blockItem : blockStmt.getBlockItems()) {
                if (!blockItem.isDecl()) {
                    if (blockItem.getStmt().hasReturnExp()) {
                        return true;
                    }
                }
            }
        } else if (type == Type.RETURN) {
            return returnStmt.getExp() != null;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("--> <Stmt> begin\n");
        switch (type) {
            case ASSIGN:
                sb.append(assignStmt.toString());
                break;
            case EXP:
                if (expStmt != null) {
                    sb.append(expStmt.toString());
                    sb.append("<Exp>\n");
                }
                sb.append("SEMICN ;\n");
                break;
            case IF:
                sb.append(ifStmt.toString());
                break;
            case BLOCK:
                sb.append(blockStmt.toString());
                break;
            case FOR:
                sb.append(forStmt.toString());
                break;
            case BREAK:
                sb.append(breakStmt.toString());
                break;
            case CONTINUE:
                sb.append(continueStmt.toString());
                break;
            case RETURN:
                sb.append(returnStmt.toString());
                break;
            case PRINTF:
                sb.append(printfStmt.toString());
                break;
        }
        sb.append("<Stmt>\n");
        return sb.toString();
    }

    public void parse() {
        Token token = Lexer.nextToken();
        if (token.isIf()) {
            Lexer.rollback(1);
            ifStmt = new IfStmt();
            ifStmt.parse();
            type = Type.IF;
        } else if (token.isLbrace()) {
            Lexer.rollback(1);
            blockStmt = new Block();
            blockStmt.parse();
            type = Type.BLOCK;
        } else if (token.isFor()) {
            Lexer.rollback(1);
            forStmt = new ForStmt();
            forStmt.parse();
            type = Type.FOR;
        } else if (token.isBreak()) {
            breakLineNum = token.getLineNum();
            Lexer.rollback(1);
            breakStmt = new BreakStmt();
            breakStmt.parse();
            type = Type.BREAK;
        } else if (token.isContinue()) {
            continueLineNum = token.getLineNum();
            Lexer.rollback(1);
            continueStmt = new ContinueStmt();
            continueStmt.parse();
            type = Type.CONTINUE;
        } else if (token.isReturn()) {
            returnLineNum = token.getLineNum();
            Lexer.rollback(1);
            returnStmt = new ReturnStmt();
            returnStmt.parse();
            type = Type.RETURN;
        } else if (token.isPrintf()) {
            printfLineNum = token.getLineNum();
            Lexer.rollback(1);
            printfStmt = new PrintfStmt();
            printfStmt.parse();
            type = Type.PRINTF;
        } else if (token.isId()) {
            Token nextToken = Lexer.nextToken();
            Lexer.rollback(2);
            if (nextToken.isLparent()) {
                expStmt = new AddExp();
                expStmt.parse();
                type = Type.EXP;
                if (!Lexer.nextToken().isSemi()) {
                    // 缺少分号
                    ErrorChecker.addError(
                            ErrorType.MISSING_SEMICOLON,
                            Lexer.getLastLineNum()
                    );
                    Lexer.rollback(1);
                }
            } else {
                int origin = Lexer.getIndex();
                LVal lval = new LVal();
                lval.parse();
                if (Lexer.nextToken().isAssign()) {
                    Lexer.rollback(1);
                    assignStmt = new AssignStmt(lval);
                    assignStmt.parse();
                    type = Type.ASSIGN;
                } else {
                    Lexer.setIndex(origin);
                    expStmt = new AddExp();
                    expStmt.parse();
                    type = Type.EXP;
                    if (!Lexer.nextToken().isSemi()) {
                        // 缺少分号
                        ErrorChecker.addError(
                                ErrorType.MISSING_SEMICOLON,
                                Lexer.getLastLineNum()
                        );
                        Lexer.rollback(1);
                    }
                }
            }
        } else if (token.isSemi()) {
            type = Type.EXP;
            expStmt = null;
        } else {
            Lexer.rollback(1);
            expStmt = new AddExp();
            expStmt.parse();
            type = Type.EXP;
            if (!Lexer.nextToken().isSemi()) {
                // 缺少分号
                ErrorChecker.addError(
                        ErrorType.MISSING_SEMICOLON,
                        Lexer.getLastLineNum()
                );
                Lexer.rollback(1);
            }
        }
    }
}
