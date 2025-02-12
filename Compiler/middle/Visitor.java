package middle;

import error.ErrorChecker;
import error.ErrorType;
import frontend.lexer.TokenType;
import frontend.parser.ast.*;
import middle.llvm.symbol.FuncSymbol;
import middle.llvm.symbol.SymbolTable;
import middle.llvm.symbol.VarSymbol;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * @Description Visitor
 */

public class Visitor {

    private static int symbolTableIndex = 1;
    private static SymbolTable globalTable = new SymbolTable(null, symbolTableIndex++);
    private static SymbolTable currentTable = globalTable;
    private static TreeMap<Integer, ArrayList<String>> visitResult = new TreeMap<>();


    private static void addResult(int curIndex, String result) {
        if (!visitResult.containsKey(curIndex)) {
            visitResult.put(curIndex, new ArrayList<>());
        }
        visitResult.get(curIndex).add(result);
    }

    public static void printResultToFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter writer = new FileWriter(file);
        for (ArrayList<String> list : visitResult.values()) {
            for (String str : list) {
                writer.write(str + "\n");
            }
        }
        writer.close();
    }

    public static void visit(CompUnit root) {
        for (Decl decl : root.getDecls()) {
            visitDecl(decl);
        }
        for (FuncDef funcDef : root.getFuncDefs()) {
            visitFuncDef(funcDef);
        }
        if (root.getMainFuncDef() != null) {
            visitFuncDef(root.getMainFuncDef());
        }
    }

    private static void visitDecl(Decl decl) {
        for (Def def : decl.getDefs()) {
            visitDef(def, decl.isConst(), decl.getBasicType());
        }
    }

    private static void visitDef(Def def, boolean isConst, TokenType basicType) {
        String name = def.getId().getValue();
        if (currentTable.containsVar(name) || currentTable.containsFunc(name)) {
            ErrorChecker.addError(ErrorType.REDEFINITION, def.getId().getLineNum());
        } else {
            VarSymbol.Type type;
            if (basicType == TokenType.INT) {
                if (def.getExp() != null) {
                    type = VarSymbol.Type.INTARRAY;
                } else {
                    type = VarSymbol.Type.INT;
                }
            } else {
                if (def.getExp() != null) {
                    type = VarSymbol.Type.CHARARRAY;
                } else {
                    type = VarSymbol.Type.CHAR;
                }
            }
            VarSymbol varSymbol = new VarSymbol(name, isConst, type);
            currentTable.addVar(varSymbol);
            int curIndex = currentTable.getIndex();
            addResult(curIndex, curIndex + " " + varSymbol);
        }
        // 处理（数组）[]内的表达式
        if (def.getExp() != null) {
            visitExp(def.getExp());
        }
        // 处理初始值
        if (def.getInitVal() != null) {
            InitVal initVal = def.getInitVal();
            if (initVal.getStrToken() == null) {
                for (AddExp exp : initVal.getExps()) {
                    visitExp(exp);
                }
            }
        }
    }

    private static void visitFuncDef(FuncDef funcDef) {
        String name = funcDef.getId().getValue();
        if (currentTable.containsFunc(name) || currentTable.containsVar(name)) {
            ErrorChecker.addError(ErrorType.REDEFINITION, funcDef.getId().getLineNum());
        }
        FuncSymbol.Type returnType;
        if (funcDef.getFuncType() == TokenType.INT) {
            returnType = FuncSymbol.Type.INT;
        } else if (funcDef.getFuncType() == TokenType.CHAR) {
            returnType = FuncSymbol.Type.CHAR;
        } else {
            returnType = FuncSymbol.Type.VOID;
        }
        FuncSymbol funcSymbol = new FuncSymbol(name, returnType);
        if (!name.equals("main")) {
            int curIndex = currentTable.getIndex();
            addResult(curIndex, curIndex + " " + funcSymbol);
        }
        if (!currentTable.containsFunc(name)) {
            currentTable.addFunc(funcSymbol);
        }
        Block block = funcDef.getBlock();
        visitFuncBlock(block, funcDef.getParams(), funcSymbol);
    }

    private static void visitParam(Param param, FuncSymbol funcSymbol) {
        String name = param.getId().getValue();
        VarSymbol.Type type;
        if (param.getBasicType() == TokenType.INT) {
            if (param.isArray()) {
                type = VarSymbol.Type.INTARRAY;
            } else {
                type = VarSymbol.Type.INT;
            }
        } else {
            if (param.isArray()) {
                type = VarSymbol.Type.CHARARRAY;
            } else {
                type = VarSymbol.Type.CHAR;
            }
        }
        if (currentTable.containsVar(name) || currentTable.containsFunc(name)) {
            ErrorChecker.addError(ErrorType.REDEFINITION, param.getId().getLineNum());
        } else {
            VarSymbol varSymbol = new VarSymbol(name, false, type);
            int curIndex = currentTable.getIndex();
            addResult(curIndex, curIndex + " " + varSymbol);
            currentTable.addVar(varSymbol);
        }
        funcSymbol.addParamType(type);
    }

    private static void visitBlock(Block block, boolean isVoid,
                                   boolean needCheck, boolean isLoop) {
        boolean hasReturn = false;
        boolean hasReturnExp = false;
        int returnLineNum = 0;
        for (BlockItem blockItem : block.getBlockItems()) {
            if (blockItem.isDecl()) {
                visitDecl(blockItem.getDecl());
            } else {
                Stmt stmt = blockItem.getStmt();
                if (stmt.hasReturnStmt()) {
                    hasReturn = true;
                    if (stmt.hasReturnExp()) {
                        hasReturnExp = true;
                    }
                    returnLineNum = stmt.getReturnLineNum();
                    if (isVoid && returnLineNum != 0) {
                        if (hasReturn && hasReturnExp) {
                            ErrorChecker.addError(ErrorType.REDUNDANT_RETURN, returnLineNum);
                        }
                    }
                }
                visitStmt(stmt, isLoop, isVoid);
            }
        }
        if (!isVoid) {
            if (needCheck && !hasReturn) {
                ErrorChecker.addError(ErrorType.MISSING_RETURN, block.getEndLineNum());
            }
        }
    }

    private static void visitFuncCall(FuncCall funcCall) {
        String name = funcCall.getId().getValue();
        int idLineNum = funcCall.getId().getLineNum();
        FuncSymbol funcSymbol = currentTable.findFunc(name);
        if (funcSymbol == null) {
            ErrorChecker.addError(ErrorType.UNDEFINED, idLineNum);
        } else {
            for (AddExp arg : funcCall.getArgs()) {
                visitExp(arg);
            }
            if (!funcCall.isChecked() &&
                    funcCall.getArgsNum() != funcSymbol.getParamNum()) {
                ErrorChecker.addError(ErrorType.PARAMS_NUM_MISMATCH, idLineNum);
            } else if (!funcCall.isChecked()) {
                for (int i = 0; i < funcCall.getArgsNum(); i++) {
                    AddExp arg = funcCall.getArgs().get(i);
                    VarSymbol.Type paramType = funcSymbol.getParamTypes().get(i);
                    VarSymbol.Type argType;
                    String headName = arg.getHeadName();
                    if (headName != null) {
                        boolean isReferenced = arg.isReferenced();
                        VarSymbol varSymbol = currentTable.findVar(headName);
                        if (varSymbol != null) {
                            argType = varSymbol.getType();
                            if (varSymbol.isArray() && isReferenced) {
                                if (argType == VarSymbol.Type.INTARRAY) {
                                    argType = VarSymbol.Type.INT;
                                } else {
                                    argType = VarSymbol.Type.CHAR;
                                }
                            }
                            if (argType == VarSymbol.Type.INTARRAY || argType == VarSymbol.Type.CHARARRAY) {
                                if (paramType != argType) {
                                    ErrorChecker.addError(ErrorType.PARAM_TYPE_MISMATCH, idLineNum);
                                }
                            } else if (paramType == VarSymbol.Type.INTARRAY || paramType == VarSymbol.Type.CHARARRAY) {
                                ErrorChecker.addError(ErrorType.PARAM_TYPE_MISMATCH, idLineNum);
                            }
                        }
                    } else {
                        if (paramType == VarSymbol.Type.INTARRAY || paramType == VarSymbol.Type.CHARARRAY) {
                            ErrorChecker.addError(ErrorType.PARAM_TYPE_MISMATCH, idLineNum);
                        }
                    }
                }
            }
        }
    }

    private static void visitLVal(LVal lVal) {
        String name = lVal.getId().getValue();
        VarSymbol varSymbol = currentTable.findVar(name);
        if (varSymbol == null) {
            ErrorChecker.addError(ErrorType.UNDEFINED, lVal.getId().getLineNum());
        } else {
            if (lVal.getExp() != null) {
                visitExp(lVal.getExp());
            }
        }
    }

    private static void visitPrimaryExp(PrimaryExp exp) {
        switch (exp.getType()) {
            case LVAL:
                LVal lVal = exp.getLVal();
                visitLVal(lVal);
                break;
            case EXP:
                visitExp(exp.getExp());
                break;
            default:
                break;
        }
    }

    private static void visitUnaryExp(UnaryExp exp) {
        if (exp.isFuncCall()) {
            visitFuncCall(exp.getFuncCall());
        } else {
            visitPrimaryExp(exp.getPrimaryExp());
        }
    }

    private static void visitMulExp(MulExp exp) {
        for (UnaryExp unaryExp : exp.getUnaryExps()) {
            visitUnaryExp(unaryExp);
        }
    }

    private static void visitExp(AddExp exp) {
        for (MulExp mulExp : exp.getMulExps()) {
            visitMulExp(mulExp);
        }
    }

    private static void visitRelExp(RelExp exp) {
        for (AddExp addExp : exp.getAddExps()) {
            visitExp(addExp);
        }
    }

    private static void visitEqExp(EqExp exp) {
        for (RelExp relExp : exp.getRelExps()) {
            visitRelExp(relExp);
        }
    }

    private static void visitLAndExp(LAndExp exp) {
        for (EqExp eqExp : exp.getEqExps()) {
            visitEqExp(eqExp);
        }
    }

    private static void visitLOrExp(LOrExp exp) {
        for (LAndExp lAndExp : exp.getLAndExps()) {
            visitLAndExp(lAndExp);
        }
    }

    private static void visitForBlock(ForBlock forBlock) {
        visitLVal(forBlock.getLVal());
        visitExp(forBlock.getExp());
        String name = forBlock.getLVal().getId().getValue();
        VarSymbol varSymbol = currentTable.findVar(name);
        if (varSymbol != null && varSymbol.isConst()) {
            ErrorChecker.addError(ErrorType.CHANGE_CONST, forBlock.getLVal().getId().getLineNum());
        }
    }

    private static void visitStmt(Stmt stmt, boolean isLoop, boolean isVoid) {
        switch (stmt.getType()) {
            case BLOCK:
                SymbolTable preTable = currentTable;
                currentTable = new SymbolTable(preTable, symbolTableIndex++);
                visitBlock(stmt.getBlockStmt(), isVoid, false, isLoop);
                currentTable = preTable;
                break;
            case EXP:
                if (stmt.getExp() != null) {
                    visitExp(stmt.getExp());
                }
                break;
            case ASSIGN:
                AssignStmt assignStmt = stmt.getAssignStmt();
                LVal lVal = assignStmt.getLVal();
                visitLVal(lVal);
                String name = lVal.getId().getValue();
                VarSymbol varSymbol = currentTable.findVar(name);
                if (varSymbol != null && varSymbol.isConst()) {
                    ErrorChecker.addError(ErrorType.CHANGE_CONST, lVal.getId().getLineNum());
                }
                if (assignStmt.getExp() != null) {
                    visitExp(assignStmt.getExp());
                }
                break;
            case BREAK:
                if (!isLoop) {
                    ErrorChecker.addError(ErrorType.BREAK_CONTINUE, stmt.getBreakLineNum());
                }
                break;
            case CONTINUE:
                if (!isLoop) {
                    ErrorChecker.addError(ErrorType.BREAK_CONTINUE, stmt.getContinueLineNum());
                }
                break;
            case IF:
                IfStmt ifStmt = stmt.getIfStmt();
                visitLOrExp(ifStmt.getCondition());
                visitStmt(ifStmt.getThenStmt(), isLoop, isVoid);
                if (ifStmt.getElseStmt() != null) {
                    visitStmt(ifStmt.getElseStmt(), isLoop, isVoid);
                }
                break;
            case FOR:
                ForStmt forStmt = stmt.getForStmt();
                if (forStmt.getInit() != null) {
                    visitForBlock(forStmt.getInit());
                }
                if (forStmt.getCondition() != null) {
                    visitLOrExp(forStmt.getCondition());
                }
                if (forStmt.getUpdate() != null) {
                    visitForBlock(forStmt.getUpdate());
                }
                visitStmt(forStmt.getBody(), true, isVoid);
                break;
            case PRINTF:
                PrintfStmt printfStmt = stmt.getPrintfStmt();
                int expCount = printfStmt.getArgs().size();
                // 计数格式字符数量
                int formatCount = 0;
                String format = printfStmt.getFormat().getValue();
                for (int i = 0; i < format.length(); i++) {
                    if (format.charAt(i) == '%') {
                        if (i + 1 < format.length() &&
                                (format.charAt(i + 1) == 'd' || format.charAt(i + 1) == 'c')) {
                            formatCount++;
                        }
                    }
                }
                if (expCount != formatCount) {
                    ErrorChecker.addError(ErrorType.PRINTF_MISMATCH, stmt.getPrintfLineNum());
                }
                for (AddExp addExp : printfStmt.getArgs()) {
                    visitExp(addExp);
                }
                break;
            case RETURN:
                ReturnStmt returnStmt = stmt.getReturnStmt();
                if (returnStmt.getExp() != null) {
                    visitExp(returnStmt.getExp());
                }
                break;
            default:
                break;
        }
    }

    private static void visitFuncBlock(Block block, ArrayList<Param> params, FuncSymbol funcSymbol) {
        SymbolTable preTable = currentTable;
        currentTable = new SymbolTable(preTable, symbolTableIndex++);
        // 处理参数
        for (Param param : params) {
            visitParam(param, funcSymbol);
        }
        funcSymbol.setParamNum(params.size());
        // 处理函数体
        visitBlock(block, funcSymbol.getReturnType() == FuncSymbol.Type.VOID,
                true, false);
        currentTable = preTable;
    }
}
