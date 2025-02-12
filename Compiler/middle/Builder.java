package middle;

import frontend.lexer.Token;
import frontend.lexer.TokenType;
import frontend.parser.ast.*;
import middle.llvm.llvmir.type.*;
import middle.llvm.llvmir.value.Value;
import middle.llvm.llvmir.value.basicblock.BasicBlock;
import middle.llvm.llvmir.value.function.Function;
import middle.llvm.llvmir.value.globalvar.GlobalVar;
import middle.llvm.llvmir.value.globalvar.GlobalVarRef;
import middle.llvm.llvmir.value.instruction.Instruction;
import middle.llvm.llvmir.value.instruction.basic.*;
import middle.llvm.llvmir.value.instruction.memory.AllocateInst;
import middle.llvm.llvmir.value.instruction.memory.GetElePtrInst;
import middle.llvm.llvmir.value.instruction.memory.LoadInst;
import middle.llvm.llvmir.value.instruction.memory.StoreInst;
import middle.llvm.llvmir.value.instruction.terminate.BranchInst;
import middle.llvm.llvmir.value.instruction.terminate.ReturnInst;
import middle.llvm.llvmir.value.item.Literal;
import middle.llvm.llvmir.value.item.LocalVar;
import middle.llvm.symbol.FuncSymbol;
import middle.llvm.symbol.SymbolTable;
import middle.llvm.llvmir.value.module.Module;
import middle.llvm.symbol.VarSymbol;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

/**
 * @Description Builder
 */

public class Builder {
    private static int symbolTableIndex = 1;
    private static SymbolTable globalTable = new SymbolTable(null, symbolTableIndex++);
    private static SymbolTable currentTable = globalTable;
    private static Module module = new Module(UnknownType.getInstance());

    public static Module getModule() {
        return Builder.module;
    }

    public static void printResultToFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter writer = new FileWriter(file);
        writer.write(module.toString());
        writer.close();
    }

    public static void build(CompUnit root) {
        for (Decl decl : root.getDecls()) {
            ArrayList<GlobalVar> globalVars = buildGlobalVars(decl);
            module.addGlobalVariables(globalVars);
        }
        for (FuncDef funcDef : root.getFuncDefs()) {
            Function function = buildFunction(funcDef);
            module.addFunction(function);
        }
        if (root.getMainFuncDef() != null) {
            Function function = buildFunction(root.getMainFuncDef());
            module.addFunction(function);
        }
    }

    private static int computeUnaryOps(ArrayList<Token> unaryOps, int value) {
        // 倒序处理，处理负号和逻辑非两种运算符
        for (int i = unaryOps.size() - 1; i >= 0; i--) {
            if (unaryOps.get(i).getValue().equals("-")) {
                value = -value;
            } else if (unaryOps.get(i).getValue().equals("!")) {
                value = value == 0 ? 1 : 0;
            }
        }
        return value;
    }

    private static Integer compute(UnaryExp unaryExp, boolean useInitValue) {
        ArrayList<Token> unaryOps = unaryExp.getUnaryOps();
        // 如果是PrimaryExp
        if (!unaryExp.isFuncCall()) {
            PrimaryExp primaryExp = unaryExp.getPrimaryExp();
            switch (primaryExp.getType()) {
                case NUM:
                    Token num = primaryExp.getNumber();
                    return computeUnaryOps(unaryOps, Integer.parseInt(num.getValue()));
                case CHAR:
                    Token ch = primaryExp.getCharacter();
                    String charStr = ch.getValue();
                    return computeUnaryOps(unaryOps, getCharValue(charStr, 0));
                case EXP:
                    int result;
                    Integer res = compute(primaryExp.getExp(), useInitValue);
                    if (res == null) {
                        return null;
                    } else {
                        result = computeUnaryOps(unaryOps, res);
                    }
                    return result;
                case LVAL:
                    LVal lVal = primaryExp.getLVal();
                    String name = lVal.getId().getValue();
                    VarSymbol varSymbol = currentTable.findVar(name);
                    if (!useInitValue && !varSymbol.isConst()) {
                        return null;
                    }
                    Value initValue = varSymbol.getInitValue();
                    if (initValue == null) {
                        return null;
                    }
                    if (initValue instanceof Literal literal) {
                        //Literal要求是i32或者i32数组，i32数组还要求LVal是这个数组的某个元素
                        if (literal.isInteger()) {
                            return computeUnaryOps(unaryOps, literal.getInt());
                        } else if (literal.isArray() && lVal.getExp() != null) {
                            int index;
                            Integer inx = compute(lVal.getExp(), true);
                            if (inx == null) {
                                return null;
                            } else {
                                index = inx;
                            }
                            return computeUnaryOps(unaryOps, literal.getIntElem(index));
                        }
                    }
                    return null;
                default:
                    return null;
            }
        }
        return null;
    }

    private static Integer compute(MulExp mulExp, boolean useInitValue) {
        int result;
        Integer res = compute(mulExp.getUnaryExps().get(0), useInitValue);
        if (res == null) {
            return null;
        } else {
            result = res;
        }
        int index = 1;
        for (Token mulOp : mulExp.getMulOps()) {
            int value;
            Integer val = compute(mulExp.getUnaryExps().get(index), useInitValue);
            if (val == null) {
                return null;
            } else {
                value = val;
            }
            if (mulOp.getValue().equals("*")) {
                result *= value;
            } else if (mulOp.getValue().equals("/")) {
                result /= value;
            } else {
                result %= value;
            }
            index++;
        }
        return result;
    }


    private static Integer compute(AddExp exp, boolean useInitValue) {
        //仅限于计算常量表达式，否则返回null
        int result;
        Integer res = compute(exp.getMulExps().get(0), useInitValue);
        if (res == null) {
            return null;
        } else {
            result = res;
        }
        int index = 1;
        for (Token addOp : exp.getAddOps()) {
            int value;
            Integer val = compute(exp.getMulExps().get(index), useInitValue);
            if (val == null) {
                return null;
            } else {
                value = val;
            }
            if (addOp.getValue().equals("+")) {
                result += value;
            } else {
                result -= value;
            }
            index++;
        }
        return result;
    }

    private static VarSymbol.Type getVarSymbolType(Decl decl, Def def) {
        VarSymbol.Type type;
        TokenType basicType = decl.getBasicType();
        if (basicType == TokenType.CHAR) {
            if (def.getExp() != null) {
                type = VarSymbol.Type.CHARARRAY;
            } else {
                type = VarSymbol.Type.CHAR;
            }
        } else {
            if (def.getExp() != null) {
                type = VarSymbol.Type.INTARRAY;
            } else {
                type = VarSymbol.Type.INT;
            }

        }
        return type;
    }

    private static ValueType getValueType(VarSymbol.Type type, Def def) {
        ValueType valueType;
        if (def.getExp() == null) {
            if (type == VarSymbol.Type.INT) {
                valueType = new IntegerType(32);
            } else {
                valueType = new IntegerType(8);
            }
        } else {
            int size;
            Integer sz = compute(def.getExp(), true);
            if (sz == null) {
                throw new RuntimeException("数组大小计算错误");
            } else {
                size = sz;
            }
            if (type == VarSymbol.Type.INTARRAY) {
                valueType = new ArrayType(new IntegerType(32), size);
            } else {
                valueType = new ArrayType(new IntegerType(8), size);
            }
        }
        return valueType;
    }

    private static ArrayList<GlobalVar> buildGlobalVars(Decl decl) {
        ArrayList<GlobalVar> globalVars = new ArrayList<>();
        for (Def def : decl.getDefs()) {
            //加入符号表中
            VarSymbol.Type type = getVarSymbolType(decl, def);
            boolean isConst = decl.isConst();
            String name = def.getId().getValue();
            VarSymbol varSymbol = new VarSymbol(name, isConst, type);
            currentTable.addVar(varSymbol);
            // 处理初始值
            Value initValue;
            if (def.getInitVal() != null) {
                InitVal initVal = def.getInitVal();
                int size = 0;
                if (def.getExp() != null) {
                    Integer sz = compute(def.getExp(), true);
                    if (sz == null) {
                        throw new RuntimeException("数组大小计算错误");
                    } else {
                        size = sz;
                    }
                }
                if (def.getExp() != null) {
                    // 数组
                    int bitWidth = 32;
                    if (type == VarSymbol.Type.CHARARRAY) {
                        bitWidth = 8;
                    }
                    if (initVal.getStrToken() == null) {
                        ArrayList<Integer> nums = new ArrayList<>();
                        boolean allZero = true;
                        for (AddExp exp : initVal.getExps()) {
                            int num;
                            Integer number = compute(exp, true);
                            // 如果无法计算，置为0
                            num = Objects.requireNonNullElse(number, 0);
                            if (num != 0) {
                                allZero = false;
                            }
                            nums.add(num);
                        }
                        if (!allZero) {
                            // 补充可能缺少的元素，置为0
                            while (nums.size() < size) {
                                nums.add(0);
                            }
                            initValue = new Literal(size, nums, bitWidth);
                        } else {
                            initValue = new Literal(0, new ArrayList<>(), bitWidth);
                        }
                    } else {
                        String str = initVal.getStrToken().getValue();
                        ArrayList<Integer> values = parseString(str, size);
                        initValue = new Literal(size, values, 8);
                    }
                } else {
                    int num;
                    Integer number = compute(initVal.getExps().get(0), true);
                    // 如果无法计算，置为0
                    num = Objects.requireNonNullElse(number, 0);
                    if (type == VarSymbol.Type.INT) {
                        initValue = new Literal(num, 32);
                    } else {
                        initValue = new Literal(num, 8);
                    }
                }
            } else {
                //如果没有初始值，那么默认为0
                if (varSymbol.isArray()) {
                    if (type == VarSymbol.Type.INTARRAY) {
                        initValue = new Literal(0, new ArrayList<>(), 32);
                    } else {
                        initValue = new Literal(0, new ArrayList<>(), 8);
                    }
                } else {
                    if (type == VarSymbol.Type.INT) {
                        initValue = new Literal(0, 32);
                    } else {
                        initValue = new Literal(0, 8);
                    }
                }
            }
            //向符号表中加入初始值
            varSymbol.setInitValue(initValue);
            //确定类型
            ValueType valueType = getValueType(type, def);
            //加入全局变量中
            GlobalVar globalVar = new GlobalVar(name, valueType, isConst);
            globalVar.setInitValue(initValue);
            globalVars.add(globalVar);
            varSymbol.setGlobalVar(globalVar);
        }
        return globalVars;
    }

    private static boolean isEscapeChar(int value) {
        return value == '\n' || value == 7 || value == '\b'
                || value == '\t' || value == 11 || value == '\f'
                || value == '\\' || value == '\'' || value == '\"'
                || value == 0;
    }

    private static int getCharValue(String str, int index) {
        int value = 0;
        if (index < str.length() - 2) {
            char c = str.charAt(index + 1);
            if (c != '\\') {
                value = c;
            } else {
                switch (str.charAt(index + 2)) {
                    case 'n' -> value = '\n';
                    case 'a' -> value = 7;
                    case 'b' -> value = '\b';
                    case 't' -> value = '\t';
                    case 'v' -> value = 11;
                    case 'f' -> value = '\f';
                    case '\\' -> value = '\\';
                    case '\'' -> value = '\'';
                    case '\"' -> value = '\"';
                    default -> {
                    }
                }
            }
        }
        return value;
    }

    private static ArrayList<Integer> parseString(String str, int size) {
        ArrayList<Integer> values = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int value = getCharValue(str, i);
            if (isEscapeChar(value)) {
                i++;
            }
            values.add(value);
        }
        if (values.size() < size) {
            for (int i = values.size(); i < size; i++) {
                values.add(0);
            }
        }
        return values;
    }

    private static Function buildFunction(FuncDef funcDef) {
        //返回类型加入符号表
        FuncSymbol.Type returnType;
        ValueType returnValueType;
        if (funcDef.getFuncType() == TokenType.CHAR) {
            returnType = FuncSymbol.Type.CHAR;
            returnValueType = new IntegerType(8);
        } else if (funcDef.getFuncType() == TokenType.INT) {
            returnType = FuncSymbol.Type.INT;
            returnValueType = new IntegerType(32);
        } else {
            returnType = FuncSymbol.Type.VOID;
            returnValueType = VoidType.getInstance();
        }
        String name = funcDef.getId().getValue();
        FuncSymbol funcSymbol = new FuncSymbol(name, returnType);
        currentTable.addFunc(funcSymbol);

        //进入新作用域
        SymbolTable preTable = currentTable;
        currentTable = new SymbolTable(preTable, symbolTableIndex++);

        // 参数加入符号表
        ArrayList<ValueType> paramTypes = new ArrayList<>();
        ArrayList<VarSymbol> paramSymbols = new ArrayList<>();
        int paramIndex = 0;
        for (Param param : funcDef.getParams()) {
            String varName = param.getId().getValue();
            ValueType valueType;
            VarSymbol.Type type;
            if (param.getBasicType() == TokenType.CHAR) {
                if (param.isArray()) {
                    type = VarSymbol.Type.CHARARRAY;
                    valueType = new PointerType(new IntegerType(8));
                } else {
                    type = VarSymbol.Type.CHAR;
                    valueType = new IntegerType(8);
                }
            } else {
                if (param.isArray()) {
                    type = VarSymbol.Type.INTARRAY;
                    valueType = new PointerType(new IntegerType(32));
                } else {
                    type = VarSymbol.Type.INT;
                    valueType = new IntegerType(32);
                }
            }
            VarSymbol varSymbol = new VarSymbol(varName, false, type);
            varSymbol.setParamIndex(paramIndex++);
            varSymbol.setParamType(valueType);
            currentTable.addVar(varSymbol);
            funcSymbol.addParamType(type);
            funcSymbol.addParamValueType(valueType);
            paramTypes.add(valueType);
            paramSymbols.add(varSymbol);
        }

        //构建函数头
        Function function = new Function(name, new FunctionType(returnValueType, paramTypes));

        //构建函数体
        ArrayList<BasicBlock> basicBlocks;
        BasicBlock buildingBlock = new BasicBlock(LabelType.getInstance());
        // 提前加载参数
        BasicBlock entryBlock = new BasicBlock("entry", LabelType.getInstance());
        if (!paramSymbols.isEmpty()) {
            ArrayList<Instruction> instructions = entryBlock.getInstructions();
            for (VarSymbol varSymbol : paramSymbols) {
                if (varSymbol.isArray()) {
                    ValueType allocatedType = varSymbol.getParamType();
                    Value allocatedResult = new LocalVar(new PointerType(allocatedType));
                    Instruction allocateInst = new AllocateInst(allocatedType, allocatedResult);
                    Value param = new Value("%p" + varSymbol.getParamIndex(), allocatedType);
                    Instruction storeInst = new StoreInst(param, allocatedResult);
                    Value result = new LocalVar(allocatedType);
                    LoadInst loadInst = new LoadInst(result, allocatedType, allocatedResult);
                    varSymbol.setLocalVar(result);
                    instructions.add(allocateInst);
                    instructions.add(storeInst);
                    instructions.add(loadInst);
                } else {
                    // 如果是参数，需要分配内存，然后存值，最后才能Load
                    // Allocate
                    ValueType allocatedType = varSymbol.getParamType();
                    Value allocatedResult = new LocalVar(new PointerType(allocatedType));
                    varSymbol.setLocalVar(allocatedResult);
                    Instruction allocateInst = new AllocateInst(allocatedType, allocatedResult);
                    instructions.add(allocateInst);
                    // Store
                    // 没有为参数单独建类，视为父类Value
                    Value param = new Value("%p" + varSymbol.getParamIndex(), allocatedType);
                    Instruction storeInst = new StoreInst(param, allocatedResult);
                    instructions.add(storeInst);
                }
            }
        }

        basicBlocks = buildBasicBlocks(funcDef.getBlock(), buildingBlock, funcSymbol, true,
                null, null);
        if (!paramSymbols.isEmpty()) {
            entryBlock.addInstruction(new BranchInst(basicBlocks.get(0).getName()));
            // 将entryBlock放在第一个
            basicBlocks.add(0, entryBlock);
        }
        function.addBasicBlocks(basicBlocks);

        // 检查如果某个基本块为空，加入br指令，目标是下一个基本块
        for (int i = 0; i < basicBlocks.size() - 1; i++) {
            BasicBlock current = basicBlocks.get(i);
            BasicBlock next = basicBlocks.get(i + 1);
            if (current.getInstructions().isEmpty()) {
                current.addInstruction(new BranchInst(next.getName()));
            }
        }
        currentTable = preTable;
        LocalVar.resetNameIndex();
        BasicBlock.resetLabelNameIndex();
        return function;
    }

    private static ArrayList<Instruction> buildLocalVars(Decl decl) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        ArrayList<Instruction> initInstructions = new ArrayList<>();
        for (Def def : decl.getDefs()) {
            //加入符号表中
            VarSymbol.Type type = getVarSymbolType(decl, def);
            boolean isConst = decl.isConst();
            String name = def.getId().getValue();
            VarSymbol varSymbol = new VarSymbol(name, isConst, type);
            currentTable.addVar(varSymbol);
            // 分配内存allocate
            ValueType allocatedType = getValueType(type, def);
            Value allocatedResult = new LocalVar(new PointerType(allocatedType));
            Instruction allocateInst = new AllocateInst(allocatedType, allocatedResult);
            instructions.add(allocateInst);
            varSymbol.setLocalVar(allocatedResult);
            // 处理初始值
            Value initValue = null;
            if (def.getInitVal() != null) {
                InitVal initVal = def.getInitVal();
                if (def.getExp() != null) {
                    // 数组
                    int index = 1;
                    Value prevGepResult = null;
                    ValueType elementType = ((ArrayType) allocatedType).getBaseType();
                    int size = ((ArrayType) allocatedType).getSize();
                    ArrayList<Integer> values = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        int value = 0;
                        boolean isConstValue = true;
                        initInstructions.clear();
                        if (initVal.getStrToken() == null) {
                            if (i < initVal.getExps().size()) {
                                Integer val = compute(initVal.getExps().get(i), decl.isConst() ? true : false);
                                // 对于常量如果直接计算初始值，报错
                                if (decl.isConst() && val == null) {
                                    throw new RuntimeException("常量数组元素初始值计算错误");
                                } else if (!decl.isConst() && val == null) {
                                    // 需要生成关于AddExp的指令
                                    isConstValue = false;
                                    initInstructions.clear();
                                    initInstructions.addAll(buildFromAddExp(initVal.getExps().get(i), elementType));
                                } else {
                                    value = val;
                                }
                            }
                        } else {
                            String str = initVal.getStrToken().getValue();
                            value = getCharValue(str, i);
                            if (isEscapeChar(value)) {
                                i++;
                            }
                        }
                        // 先计算存储位置GEP，再存储到内存中Store
                        // GEP（第一条GEP指令比较特殊）
                        GetElePtrInst gepInst;
                        Value gepResult = new LocalVar(new PointerType(elementType));
                        if (index == 1) {
                            gepInst = new GetElePtrInst(allocatedType, allocatedResult,
                                    gepResult);
                            if (type == VarSymbol.Type.INTARRAY) {
                                gepInst.addIndex(new Literal(0, 32));
                                gepInst.addIndex(new Literal(0, 32));
                            } else {
                                gepInst.addIndex(new Literal(0, 8));
                                gepInst.addIndex(new Literal(0, 8));
                            }
                        } else {
                            gepInst = new GetElePtrInst(elementType, prevGepResult,
                                    gepResult);
                            if (type == VarSymbol.Type.INTARRAY) {
                                gepInst.addIndex(new Literal(1, 32));
                            } else {
                                gepInst.addIndex(new Literal(1, 8));
                            }
                        }
                        prevGepResult = gepResult;
                        instructions.add(gepInst);
                        // Store
                        Instruction storeInst;
                        Literal literal;
                        if (type == VarSymbol.Type.INTARRAY) {
                            literal = new Literal(value, 32);
                        } else {
                            literal = new Literal(value, 8);
                        }
                        if (isConstValue) {
                            storeInst = new StoreInst(literal, gepResult);
                        } else {
                            instructions.addAll(initInstructions);
                            Value result = getLastResult(initInstructions);
                            storeInst = new StoreInst(result, gepResult);
                        }
                        instructions.add(storeInst);
                        values.add(value);
                        index++;
                    }
                    if (type == VarSymbol.Type.INTARRAY) {
                        initValue = new Literal(size, values, 32);
                    } else {
                        initValue = new Literal(size, values, 8);
                    }
                } else {
                    // 单个变量
                    int content = 0;
                    Integer cont = compute(initVal.getExps().get(0), decl.isConst() ? true : false);
                    boolean hasInitValue = true;
                    if (decl.isConst() && cont == null) {
                        throw new RuntimeException("常量初始值计算错误");
                    } else if (!decl.isConst() && cont == null) {
                        hasInitValue = false;
                        initInstructions.clear();
                        initInstructions.addAll(buildFromAddExp(initVal.getExps().get(0), allocatedType));
                    } else {
                        content = cont;
                    }
                    Literal literal;
                    if (type == VarSymbol.Type.INT) {
                        literal = new Literal(content, 32);
                    } else {
                        literal = new Literal(content, 8);
                    }
                    initValue = hasInitValue ? literal : null;
                    // 存储到内存中Store
                    Instruction storeInst;
                    if (hasInitValue) {
                        storeInst = new StoreInst(literal, allocatedResult);
                    } else {
                        instructions.addAll(initInstructions);
                        Value result = getLastResult(initInstructions);
                        storeInst = new StoreInst(result, allocatedResult);
                    }
                    instructions.add(storeInst);
                }
            }
            //向符号表中加入初始值
            varSymbol.setInitValue(initValue);
        }
        return instructions;
    }

    private static Instruction buildZextOrTrunc(Value src, ValueType srcType, ValueType destType) {
        // 仅支持int与char类型的转换
        Instruction inst;
        if (srcType instanceof IntegerType && destType instanceof IntegerType) {
            int srcBitWidth = ((IntegerType) srcType).getBitWidth();
            int destBitWidth = ((IntegerType) destType).getBitWidth();
            if (srcBitWidth < destBitWidth) {
                Value result = new LocalVar(destType);
                inst = new ZextInst(src, result, destType);
            } else if (srcBitWidth > destBitWidth) {
                Value result = new LocalVar(destType);
                inst = new TruncInst(src, result, destType);
            } else {
                inst = null;
            }
        } else {
            inst = null;
        }
        return inst;
    }

    private static ArrayList<Instruction> buildFromLVal(LVal lVal, ValueType resultType) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        String varName = lVal.getId().getValue();
        VarSymbol varSymbol = currentTable.findVar(varName);
        // 分数组引用和普通变量
        if (lVal.getExp() != null) {
            // 数组引用
            AddExp addExp = lVal.getExp();
            // 先使用GEP指令计算地址，再使用Load指令取值
            Instruction gepInst;
            ValueType elementType;
            if (varSymbol.isGlobal()) {
                elementType = varSymbol.getGlobalVar().getType();
                GlobalVarRef globalVarRef = new GlobalVarRef("@" + varName, new PointerType(elementType));
                gepInst = new GetElePtrInst(elementType, globalVarRef,
                        new LocalVar(new PointerType(((ArrayType) elementType).getBaseType())));
            } else {
                // 如果是参数，这里element是PointType，不是ArrayType
                elementType = ((PointerType) varSymbol.getLocalVar().getType()).getBaseType();
                if (!varSymbol.isParam()) {
                    if (elementType instanceof ArrayType) {
                        gepInst = new GetElePtrInst(elementType, varSymbol.getLocalVar(),
                                new LocalVar(new PointerType(((ArrayType) elementType).getBaseType())));
                    } else {
                        gepInst = new GetElePtrInst(elementType, varSymbol.getLocalVar(),
                                new LocalVar(elementType));
                    }
                } else {
                    gepInst = new GetElePtrInst(elementType, varSymbol.getLocalVar(),
                            new LocalVar(new PointerType(elementType)));
                }
            }
            // 添加索引
            ValueType indexType;
            if (varSymbol.isParam()) {
                indexType = elementType;
            } else {
                indexType = ((ArrayType) elementType).getBaseType();
            }
            int bitWidth = ((IntegerType) indexType).getBitWidth();
            if (!varSymbol.isParam()) {
                ((GetElePtrInst) gepInst).addIndex(new Literal(0, bitWidth));
            }
            // 计算索引偏移量
            Integer offset = compute(addExp, false);
            if (offset == null) {
                // 无法计算，递归处理
                ArrayList<Instruction> indexInstructions = buildFromAddExp(addExp, indexType);
                instructions.addAll(indexInstructions);
                Value index = getLastResult(indexInstructions);
                ((GetElePtrInst) gepInst).addIndex(index);
            } else {
                ((GetElePtrInst) gepInst).addIndex(new Literal(offset, bitWidth));
            }
            instructions.add(gepInst);
            // Load指令
            ValueType loadType = ((PointerType) gepInst.getType()).getBaseType();
            Value result = new LocalVar(loadType);
            LoadInst loadInst = new LoadInst(result, loadType, gepInst.getResult());
            instructions.add(loadInst);
        } else {
            // 普通变量
            Value var;
            if (varSymbol.isGlobal()) {
                ValueType valueType = varSymbol.getGlobalVar().getType();
                if (valueType instanceof ArrayType) {
                    ValueType baseType = ((ArrayType) valueType).getBaseType();
                    int bitWidth = ((IntegerType) baseType).getBitWidth();
                    Value pos = new LocalVar(new PointerType(baseType));
                    GlobalVarRef globalVarRef = new GlobalVarRef("@" + varName, new PointerType(valueType));
                    GetElePtrInst gepInst = new GetElePtrInst(valueType, globalVarRef, pos);
                    gepInst.addIndex(new Literal(0, bitWidth));
                    gepInst.addIndex(new Literal(0, bitWidth));
                    instructions.add(gepInst);
                } else {
                    GlobalVarRef globalVarRef = new GlobalVarRef("@" + varName,
                            new PointerType(valueType));
                    Value result = new LocalVar(valueType);
                    LoadInst loadInst = new LoadInst(result, valueType, globalVarRef);
                    instructions.add(loadInst);
                }
            } else {
                var = varSymbol.getLocalVar();
                ValueType valueType = ((PointerType) var.getType()).getBaseType();
                if (valueType instanceof ArrayType) {
                    // 本地数组变量
                    ValueType baseType = ((ArrayType) valueType).getBaseType();
                    int bitWidth = ((IntegerType) baseType).getBitWidth();
                    Value pos = new LocalVar(new PointerType(baseType));
                    GetElePtrInst gepInst = new GetElePtrInst(valueType, var, pos);
                    gepInst.addIndex(new Literal(0, bitWidth));
                    gepInst.addIndex(new Literal(0, bitWidth));
                    instructions.add(gepInst);
                } else if (varSymbol.isArray()) {
                    // 参数数组变量，特别注意，由于参数已经预加载，此处可以直接引用
                    LiteralInst literalInst = new LiteralInst(var);
                    instructions.add(literalInst);
                } else {
                    Value result = new LocalVar(valueType);
                    LoadInst loadInst = new LoadInst(result, valueType, var);
                    instructions.add(loadInst);
                }
            }
        }
        return instructions;
    }

    private static ArrayList<Instruction> buildFromUnaryOps(ArrayList<Token> unaryOps, Value result) {
        ValueType i32 = new IntegerType(32);
        // 倒序处理，处理负号和逻辑非两种运算符
        Value tmp = result;
        ArrayList<Instruction> instructions = new ArrayList<>();
        Instruction inst = buildZextOrTrunc(tmp, tmp.getType(), i32);
        if (inst != null) {
            instructions.add(inst);
            tmp = getLastResult(instructions);
        }
        for (int i = unaryOps.size() - 1; i >= 0; i--) {
            if (unaryOps.get(i).getValue().equals("-")) {
                Value res = new LocalVar(i32);
                BinaryInst binaryInst = new BinaryInst(BinaryInst.OpType.SUB, new Literal(0, 32), tmp, res);
                instructions.add(binaryInst);
                tmp = getLastResult(instructions);
            } else if (unaryOps.get(i).getValue().equals("!")) {
                // 和0比较，等于0则为1，否则则为0
                Value res = new LocalVar(new IntegerType(1));
                IcmpInst icmpInst = new IcmpInst(i32, IcmpInst.Type.EQ, tmp, new Literal(0,
                        32), res);
                instructions.add(icmpInst);
                // Zext扩展
                Value zextRes = new LocalVar(i32);
                ZextInst zextInst = new ZextInst(res, zextRes, i32);
                instructions.add(zextInst);
                tmp = getLastResult(instructions);
            }
        }
        return instructions;
    }

    private static ArrayList<Instruction> buildFromUnaryExp(UnaryExp unaryExp, ValueType resultType) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        // 如果是FuncCall
        if (unaryExp.isFuncCall()) {
            FuncCall funcCall = unaryExp.getFuncCall();
            String funcName = funcCall.getId().getValue();
            FuncSymbol funcSymbol = currentTable.findFunc(funcName);
            ValueType returnType;
            if (funcSymbol.getReturnValue() != null) {
                returnType = funcSymbol.getReturnValue().getType();
            } else {
                returnType = VoidType.getInstance();
            }
            ArrayList<Value> args = new ArrayList<>();
            int index = 0;
            for (AddExp addExp : funcCall.getArgs()) {
                Integer result = compute(addExp, false);
                if (result == null) {
                    //返回值不是能直接计算的常量表达式，递归处理
                    ArrayList<Instruction> argInstructions = buildFromAddExp(addExp, funcSymbol.getParamValueType(index));
                    instructions.addAll(argInstructions);
                    args.add(getLastResult(argInstructions));
                } else {
                    IntegerType intType = (IntegerType) (funcSymbol.getParamValueType(index));
                    args.add(new Literal(result, intType.getBitWidth()));
                }
                index++;
            }
            Instruction callInst;
            if (returnType instanceof VoidType || resultType instanceof VoidType) {
                callInst = new CallInst(returnType, funcName, args, null);
            } else {
                callInst = new CallInst(returnType, funcName, args, new LocalVar(returnType));
            }
            instructions.add(callInst);
            // 处理正负号和逻辑非
            Value result = getLastResult(instructions);
            if (!(resultType instanceof VoidType)) {
                instructions.addAll(buildFromUnaryOps(unaryExp.getUnaryOps(), result));
            }
        } else {
            // 如果是PrimaryExp
            PrimaryExp primaryExp = unaryExp.getPrimaryExp();
            switch (primaryExp.getType()) {
                case NUM:
                    if (resultType instanceof VoidType) {
                        break;
                    }
                    Token num = primaryExp.getNumber();
                    int value = Integer.parseInt(num.getValue());
                    value = computeUnaryOps(unaryExp.getUnaryOps(), value);
                    Literal literal = new Literal(value, 32);
                    instructions.add(new LiteralInst(literal));
                    break;
                case CHAR:
                    if (resultType instanceof VoidType) {
                        break;
                    }
                    Token ch = primaryExp.getCharacter();
                    String charStr = ch.getValue();
                    int charValue = getCharValue(charStr, 0);
                    charValue = computeUnaryOps(unaryExp.getUnaryOps(), charValue);
                    Literal charLiteral = new Literal(charValue, 32);
                    instructions.add(new LiteralInst(charLiteral));
                    break;
                case LVAL:
                    if (resultType instanceof VoidType) {
                        break;
                    }
                    LVal lVal = primaryExp.getLVal();
                    instructions.addAll(buildFromLVal(lVal, resultType));
                    // 处理正负号和逻辑非
                    Value result = getLastResult(instructions);
                    instructions.addAll(buildFromUnaryOps(unaryExp.getUnaryOps(), result));
                    break;
                case EXP:
                    instructions.addAll(buildFromAddExp(primaryExp.getExp(), resultType));
                    if (resultType instanceof VoidType) {
                        break;
                    }
                    Value expResult = getLastResult(instructions);
                    instructions.addAll(buildFromUnaryOps(unaryExp.getUnaryOps(), expResult));
                    break;
                default:
                    break;
            }
        }
        return instructions;
    }

    private static ArrayList<Instruction> buildFromMulExp(MulExp mulExp, ValueType resultType) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        ArrayList<Token> mulOps = mulExp.getMulOps();
        ArrayList<UnaryExp> unaryExps = mulExp.getUnaryExps();
        // 如果返回类型为Void，简化处理
        if (resultType instanceof VoidType) {
            for (UnaryExp unaryExp : unaryExps) {
                instructions.addAll(buildFromUnaryExp(unaryExp, resultType));
            }
            return instructions;
        }
        if (mulOps.isEmpty()) {
            UnaryExp unaryExp = unaryExps.get(0);
            instructions.addAll(buildFromUnaryExp(unaryExp, resultType));
        } else {
            //一个或者多个乘除模运算
            ArrayList<Value> values = new ArrayList<>();
            ArrayList<Instruction> leftInstructions = buildFromUnaryExp(unaryExps.get(0), resultType);
            instructions.addAll(leftInstructions);
            Value left = getLastResult(leftInstructions);
            values.add(left);
            for (int i = 0; i < mulOps.size(); i++) {
                ArrayList<Instruction> rightInstructions = buildFromUnaryExp(unaryExps.get(i + 1), resultType);
                instructions.addAll(rightInstructions);
                Value right = getLastResult(rightInstructions);
                values.add(right);
                ValueType i32 = new IntegerType(32);
                Value result = new LocalVar(i32);
                BinaryInst.OpType opType;
                if (mulOps.get(i).getValue().equals("*")) {
                    opType = BinaryInst.OpType.MUL;
                } else if (mulOps.get(i).getValue().equals("/")) {
                    opType = BinaryInst.OpType.SDIV;
                } else {
                    opType = BinaryInst.OpType.SREM;
                }
                // left的取值实际上是上一轮的result
                Instruction l = buildZextOrTrunc(values.get(i), values.get(i).getType(), i32);
                if (l != null) {
                    instructions.add(l);
                    values.set(i, getLastResult(instructions));
                }
                Instruction r = buildZextOrTrunc(values.get(i + 1), values.get(i + 1).getType(), i32);
                if (r != null) {
                    instructions.add(r);
                    values.set(i + 1, getLastResult(instructions));
                }
                BinaryInst binaryInst = new BinaryInst(opType, values.get(i), values.get(i + 1), result);
                instructions.add(binaryInst);
                //将本轮的result作为下一轮的left
                values.remove(i + 1);
                values.add(result);
            }
        }
        return instructions;
    }

    private static ArrayList<Instruction> buildFromAddExp(AddExp addExp, ValueType resultType) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        ArrayList<Token> addOps = addExp.getAddOps();
        ArrayList<MulExp> mulExps = addExp.getMulExps();
        // 如果返回类型为Void，简化处理
        if (resultType instanceof VoidType) {
            for (MulExp mulExp : mulExps) {
                instructions.addAll(buildFromMulExp(mulExp, resultType));
            }
            return instructions;
        }
        if (addOps.isEmpty()) {
            MulExp mulExp = mulExps.get(0);
            instructions.addAll(buildFromMulExp(mulExp, resultType));
        } else {
            //一个或者多个加减法
            ArrayList<Value> values = new ArrayList<>();
            ArrayList<Instruction> leftInstructions = buildFromMulExp(mulExps.get(0), resultType);
            instructions.addAll(leftInstructions);
            Value left = getLastResult(leftInstructions);
            values.add(left);
            for (int i = 0; i < addOps.size(); i++) {
                ArrayList<Instruction> rightInstructions = buildFromMulExp(mulExps.get(i + 1), resultType);
                instructions.addAll(rightInstructions);
                Value right = getLastResult(rightInstructions);
                values.add(right);
                ValueType i32 = new IntegerType(32);
                Value result = new LocalVar(i32);
                BinaryInst.OpType opType;
                if (addOps.get(i).getValue().equals("+")) {
                    opType = BinaryInst.OpType.ADD;
                } else {
                    opType = BinaryInst.OpType.SUB;
                }
                // left的取值实际上是上一轮的result
                Instruction l = buildZextOrTrunc(values.get(i), values.get(i).getType(), i32);
                if (l != null) {
                    instructions.add(l);
                    values.set(i, getLastResult(instructions));
                }
                Instruction r = buildZextOrTrunc(values.get(i + 1), values.get(i + 1).getType(), i32);
                if (r != null) {
                    instructions.add(r);
                    values.set(i + 1, getLastResult(instructions));
                }
                BinaryInst binaryInst = new BinaryInst(opType, values.get(i), values.get(i + 1), result);
                instructions.add(binaryInst);
                //将本轮的result作为下一轮的left
                values.remove(i + 1);
                values.add(result);
            }
        }
        if (!instructions.isEmpty()) {
            Value last = getLastResult(instructions);
            Instruction lastInst = buildZextOrTrunc(last, last.getType(), resultType);
            if (lastInst != null) {
                instructions.add(lastInst);
            }
        }
        return instructions;
    }

    private static Value getLastResult(ArrayList<Instruction> instructions) {
        if (instructions.isEmpty()) {
            throw new RuntimeException("没有指令，无法返回值");
        }
        Instruction lastInst = instructions.get(instructions.size() - 1);
        return lastInst.getResult();
    }

    private static Value buildReturnValue(ReturnStmt returnStmt, FuncSymbol funcSymbol,
                                          BasicBlock buildingBlock) {
        Value returnValue;
        if (returnStmt.getExp() != null) {
            AddExp addExp = returnStmt.getExp();
            Integer result = compute(addExp, false);
            if (result == null) {
                //返回值不是能直接计算的常量表达式，递归处理
                ValueType resultType;
                if (funcSymbol.getReturnType() == FuncSymbol.Type.INT) {
                    resultType = new IntegerType(32);
                } else {
                    resultType = new IntegerType(8);
                }
                ArrayList<Instruction> binaryInstructions = buildFromAddExp(addExp, resultType);
                buildingBlock.addInstructions(binaryInstructions);
                // 最后一条指令（return）的结果即为返回值
                returnValue = getLastResult(binaryInstructions);
            } else {
                if (funcSymbol.getReturnType() == FuncSymbol.Type.INT) {
                    returnValue = new Literal(result, 32);
                } else {
                    returnValue = new Literal(result, 8);
                }
            }
        } else {
            returnValue = new Value(VoidType.getInstance());
        }
        return returnValue;
    }

    private static ArrayList<Instruction> buildingFromAssignStmt(AssignStmt assignStmt) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        LVal lVal = assignStmt.getLVal();
        ValueType resultType;
        VarSymbol varSymbol = currentTable.findVar(lVal.getId().getValue());
        if (varSymbol.isArray()) {
            if (varSymbol.getType() == VarSymbol.Type.INTARRAY) {
                if (lVal.getExp() == null) {
                    resultType = new PointerType(new IntegerType(32));
                } else {
                    resultType = new IntegerType(32);
                }
            } else {
                if (lVal.getExp() == null) {
                    resultType = new PointerType(new IntegerType(8));
                } else {
                    resultType = new IntegerType(8);
                }
            }
        } else {
            if (varSymbol.getType() == VarSymbol.Type.INT) {
                resultType = new IntegerType(32);
            } else {
                resultType = new IntegerType(8);
            }
        }
        switch (assignStmt.getType()) {
            case COMMON:
                instructions.addAll(buildFromAddExp(assignStmt.getExp(), resultType));
                break;
            case GETINT:
                LocalVar result1 = new LocalVar(new IntegerType(32));
                CallInst callInst1 = new CallInst(new IntegerType(32), "getint", new ArrayList<>(), result1);
                instructions.add(callInst1);
                break;
            case GETCHAR:
                LocalVar result2 = new LocalVar(new IntegerType(32));
                CallInst callInst2 = new CallInst(new IntegerType(32), "getchar", new ArrayList<>(), result2);
                instructions.add(callInst2);
                // 强制转换为i8
                LocalVar result3 = new LocalVar(new IntegerType(8));
                TruncInst truncInst = new TruncInst(result2, result3, new IntegerType(8));
                instructions.add(truncInst);
                break;
            default:
                break;
        }
        // Store指令
        StoreInst storeInst;
        Value result = getLastResult(instructions);
        ArrayList<Instruction> lValInstructions = buildFromLVal(lVal, resultType);
        // lVal的最后一条指令的必定是Load，但这里不需要Load，只需要往Load的地址中Store
        Instruction lastInst = lValInstructions.remove(lValInstructions.size() - 1);
        instructions.addAll(lValInstructions);
        Value storeAddr = ((LoadInst) lastInst).getPosition();
        storeInst = new StoreInst(result, storeAddr);
        instructions.add(storeInst);
        return instructions;
    }

    private static ArrayList<Instruction> buildFromRelExp(RelExp relExp) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        ArrayList<Token> relOps = relExp.getRelOps();
        ArrayList<AddExp> addExps = relExp.getAddExps();
        if (relOps.isEmpty()) {
            AddExp addExp = addExps.get(0);
            // 默认AddExp的结果类型为32位整数
            instructions.addAll(buildFromAddExp(addExp, new IntegerType(32)));
        } else {
            //一个或者多个关系运算
            ArrayList<Value> values = new ArrayList<>();
            // 默认AddExp的结果类型为32位整数
            ArrayList<Instruction> leftInstructions = buildFromAddExp(addExps.get(0), new IntegerType(32));
            instructions.addAll(leftInstructions);
            Value left = getLastResult(leftInstructions);
            values.add(left);
            for (int i = 0; i < relOps.size(); i++) {
                ArrayList<Instruction> rightInstructions = buildFromAddExp(addExps.get(i + 1), new IntegerType(32));
                instructions.addAll(rightInstructions);
                Value right = getLastResult(rightInstructions);
                values.add(right);
                Value result = new LocalVar(new IntegerType(1));
                IcmpInst.Type icmpType;
                if (relOps.get(i).getValue().equals("<")) {
                    icmpType = IcmpInst.Type.SLT;
                } else if (relOps.get(i).getValue().equals(">")) {
                    icmpType = IcmpInst.Type.SGT;
                } else if (relOps.get(i).getValue().equals("<=")) {
                    icmpType = IcmpInst.Type.SLE;
                } else {
                    icmpType = IcmpInst.Type.SGE;
                }
                // left的取值实际上是上一轮的result，如果result不是i32，需要转换
                left = values.get(i);
                Value finalLeft = left;
                if (left.getType() instanceof IntegerType && ((IntegerType) left.getType()).getBitWidth() != 32) {
                    finalLeft = new LocalVar(new IntegerType(32));
                    ZextInst zextInst = new ZextInst(left, finalLeft, new IntegerType(32));
                    instructions.add(zextInst);
                }
                IcmpInst icmpInst = new IcmpInst(new IntegerType(32),
                        icmpType, finalLeft, values.get(i + 1), result);
                instructions.add(icmpInst);
                //将本轮的result作为下一轮的left
                values.remove(i + 1);
                values.add(result);
            }
        }
        return instructions;
    }

    private static ArrayList<Instruction> buildFromEqExp(EqExp eqExp) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        ArrayList<Token> eqOps = eqExp.getEqOps();
        ArrayList<RelExp> relExps = eqExp.getRelExps();
        if (eqOps.isEmpty()) {
            RelExp relExp = relExps.get(0);
            instructions.addAll(buildFromRelExp(relExp));
        } else {
            //一个或者多个等于或者不等于运算
            ArrayList<Value> values = new ArrayList<>();
            ArrayList<Instruction> leftInstructions = buildFromRelExp(relExps.get(0));
            instructions.addAll(leftInstructions);
            Value left = getLastResult(leftInstructions);
            values.add(left);
            for (int i = 0; i < eqOps.size(); i++) {
                ArrayList<Instruction> rightInstructions = buildFromRelExp(relExps.get(i + 1));
                instructions.addAll(rightInstructions);
                Value right = getLastResult(rightInstructions);
                values.add(right);
                Value result = new LocalVar(new IntegerType(1));
                IcmpInst.Type icmpType;
                if (eqOps.get(i).getValue().equals("==")) {
                    icmpType = IcmpInst.Type.EQ;
                } else {
                    icmpType = IcmpInst.Type.NE;
                }
                left = values.get(i);
                Value finalLeft = left;
                right = values.get(i + 1);
                Value finalRight = right;
                if (left.getType() instanceof IntegerType && ((IntegerType) left.getType()).getBitWidth() != 32) {
                    finalLeft = new LocalVar(new IntegerType(32));
                    ZextInst zextInst = new ZextInst(left, finalLeft, new IntegerType(32));
                    instructions.add(zextInst);
                }
                if (right.getType() instanceof IntegerType && ((IntegerType) right.getType()).getBitWidth() != 32) {
                    finalRight = new LocalVar(new IntegerType(32));
                    ZextInst zextInst = new ZextInst(right, finalRight, new IntegerType(32));
                    instructions.add(zextInst);
                }
                IcmpInst icmpInst = new IcmpInst(new IntegerType(32), icmpType, finalLeft, finalRight, result);
                values.remove(i + 1);
                values.add(result);
                instructions.add(icmpInst);
            }
        }
        return instructions;
    }

    // 返回0表示false，返回1表示true，返回-1表示非常数表达式
    private static int buildFromLAndExp(LAndExp lAndExp, BasicBlock buildingBlock,
                                        ArrayList<BasicBlock> basicBlocks,
                                        String labelWhenTrue, String nextLabel) {
        ArrayList<EqExp> eqExps = lAndExp.getEqExps();
        boolean isTrue = true;
        for (EqExp eqExp : eqExps) {
            ArrayList<Instruction> instructions = buildFromEqExp(eqExp);
            Value result = getLastResult(instructions);
            if (result instanceof Literal) {
                Literal literal = (Literal) result;
                if (literal.getInt() == 0) {
                    // 短路，
                    buildingBlock.addInstruction(new BranchInst(nextLabel));
                    return 0;
                } else {
                    if (eqExps.indexOf(eqExp) == eqExps.size() - 1) {
                        buildingBlock.addInstruction(new BranchInst(labelWhenTrue));
                    }
                    continue;
                }
            }
            buildingBlock.addInstructions(instructions);
            // 根据result：i1变量条件跳转
            isTrue = false;
            LocalVar flag = new LocalVar(new IntegerType(1));
            // 如果result不是i32类型，需要转换
            Value finalResult = result;
            if (result.getType() instanceof IntegerType && ((IntegerType) result.getType()).getBitWidth() != 32) {
                finalResult = new LocalVar(new IntegerType(32));
                ZextInst zextInst = new ZextInst(result, finalResult, new IntegerType(32));
                buildingBlock.addInstruction(zextInst);
            }
            IcmpInst icmpInst = new IcmpInst(new IntegerType(32), IcmpInst.Type.NE,
                    finalResult, new Literal(0, 32), flag);
            buildingBlock.addInstruction(icmpInst);
            // 下一块，继续计算下一个EqExp
            BranchInst branchInst;
            // 是最后一个EqExp，分别跳转到labelWhenFalse和labelWhenTrue
            if (eqExps.indexOf(eqExp) == eqExps.size() - 1) {
                branchInst = new BranchInst(flag, labelWhenTrue, nextLabel);
                buildingBlock.addInstruction(branchInst);
            } else {
                BasicBlock nextBlock = new BasicBlock(LabelType.getInstance());
                branchInst = new BranchInst(flag, nextBlock.getName(), nextLabel);
                buildingBlock.addInstruction(branchInst);
                buildingBlock = nextBlock;
                basicBlocks.add(buildingBlock);
            }
        }
        if (isTrue) {
            return 1;
        }
        return -1;
    }

    private static BasicBlock buildFromIfStmt(IfStmt ifStmt, BasicBlock buildingBlock,
                                              ArrayList<BasicBlock> basicBlocks, FuncSymbol funcSymbol,
                                              boolean needNewBlock,
                                              //用于for循环
                                              String falseBlockName, String updateBlockName) {
        LOrExp lOrExp = ifStmt.getCondition();
        ArrayList<LAndExp> lAndExps = lOrExp.getLAndExps();
        boolean elseUsed = (ifStmt.getElseStmt() != null);
        boolean thenUsed = false;
        boolean nextUsed = true;
        BasicBlock trueBlock = new BasicBlock(LabelType.getInstance());
        BasicBlock falseBlock = new BasicBlock(LabelType.getInstance());
        String nextLabel = falseBlock.getName();
        BasicBlock nextBlock = falseBlock;
        for (LAndExp lAndExp : lAndExps) {
            if (lAndExps.indexOf(lAndExp) != lAndExps.size() - 1 && nextUsed) {
                nextBlock = new BasicBlock(LabelType.getInstance());
                nextLabel = nextBlock.getName();
                nextUsed = false;
            }
            if (lAndExps.indexOf(lAndExp) == lAndExps.size() - 1) {
                nextLabel = falseBlock.getName();
            }
            int result = buildFromLAndExp(lAndExp, buildingBlock, basicBlocks,
                    trueBlock.getName(), nextLabel);
            if (result != -1) {
                if (result == 1) {
                    // 短路，直接跳转
                    BranchInst branchInst = new BranchInst(trueBlock.getName());
                    buildingBlock.addInstruction(branchInst);
                    elseUsed = false;
                    thenUsed = true;
                    break;
                } else {
                    if (lAndExps.indexOf(lAndExp) != lAndExps.size() - 1) {
                        // 下一个LAndExp
                        nextUsed = true;
                        buildingBlock = nextBlock;
                        basicBlocks.add(buildingBlock);
                    }
                    continue;
                }
            }
            thenUsed = true;
            buildingBlock = nextBlock;
            if (lAndExps.indexOf(lAndExp) != lAndExps.size() - 1) {
                // 下一个LAndExp
                nextUsed = true;
                buildingBlock = nextBlock;
                basicBlocks.add(buildingBlock);
            }
        }
        // if后的Block构建
        BasicBlock blockAfterIf = new BasicBlock(LabelType.getInstance());
        BranchInst branchInst = new BranchInst(blockAfterIf.getName());
        // trueBlock构建
        buildingBlock = trueBlock;
        basicBlocks.add(buildingBlock);
        if (thenUsed) {
            buildingBlock = buildFromStmt(ifStmt.getThenStmt(), buildingBlock, basicBlocks, funcSymbol, needNewBlock
                    , falseBlockName, updateBlockName);
        }
        buildingBlock.addInstruction(branchInst);
        // falseBlock构建
        buildingBlock = falseBlock;
        basicBlocks.add(buildingBlock);
        if (elseUsed) {
            buildingBlock = buildFromStmt(ifStmt.getElseStmt(), buildingBlock, basicBlocks, funcSymbol, needNewBlock
                    , falseBlockName, updateBlockName);
        }
        buildingBlock.addInstruction(branchInst);
        buildingBlock = blockAfterIf;
        basicBlocks.add(buildingBlock);
        return buildingBlock;
    }

    private static BasicBlock buildFromForStmt(ForStmt forStmt, BasicBlock buildingBlock,
                                               ArrayList<BasicBlock> basicBlocks, FuncSymbol funcSymbol,
                                               boolean needNewBlock) {
        BasicBlock conditionBlock = new BasicBlock(LabelType.getInstance());
        BasicBlock trueBlock = new BasicBlock(LabelType.getInstance());
        BasicBlock falseBlock = new BasicBlock(LabelType.getInstance());
        BasicBlock updateBlock = new BasicBlock(LabelType.getInstance());
        // 处理初始化语句
        if (forStmt.getInit() != null) {
            ForBlock init = forStmt.getInit();
            AssignStmt initAssign = new AssignStmt(init.getLVal(), init.getExp());
            buildingBlock.addInstructions(buildingFromAssignStmt(initAssign));
        }
        buildingBlock.addInstruction(new BranchInst(conditionBlock.getName()));
        // 处理更新语句
        buildingBlock = updateBlock;
        basicBlocks.add(buildingBlock);
        if (forStmt.getUpdate() != null) {
            ForBlock update = forStmt.getUpdate();
            AssignStmt updateAssign = new AssignStmt(update.getLVal(), update.getExp());
            updateBlock.addInstructions(buildingFromAssignStmt(updateAssign));
        }
        updateBlock.addInstruction(new BranchInst(conditionBlock.getName()));
        // 处理条件语句
        buildingBlock = conditionBlock;
        basicBlocks.add(buildingBlock);
        boolean thenUsed = false;
        int zeroCount = 0;
        if (forStmt.getCondition() != null) {
            LOrExp lOrExp = forStmt.getCondition();
            ArrayList<LAndExp> lAndExps = lOrExp.getLAndExps();
            boolean nextUsed = true;
            String nextLabel = falseBlock.getName();
            BasicBlock nextBlock = falseBlock;
            for (LAndExp lAndExp : lAndExps) {
                if (lAndExps.indexOf(lAndExp) != lAndExps.size() - 1 && nextUsed) {
                    nextBlock = new BasicBlock(LabelType.getInstance());
                    nextLabel = nextBlock.getName();
                    nextUsed = false;
                }
                if (lAndExps.indexOf(lAndExp) == lAndExps.size() - 1) {
                    nextLabel = falseBlock.getName();
                }
                int result = buildFromLAndExp(lAndExp, buildingBlock, basicBlocks,
                        trueBlock.getName(), nextLabel);
                if (result != -1) {
                    if (result == 1) {
                        // 短路，直接跳转
                        BranchInst branchInst = new BranchInst(trueBlock.getName());
                        buildingBlock.addInstruction(branchInst);
                        thenUsed = true;
                        break;
                    } else {
                        zeroCount++;
                        continue;
                    }
                }
                thenUsed = true;
                buildingBlock = nextBlock;
                if (lAndExps.indexOf(lAndExp) != lAndExps.size() - 1) {
                    // 下一个LAndExp
                    nextUsed = true;
                    buildingBlock = nextBlock;
                    basicBlocks.add(buildingBlock);
                }
            }
            if (zeroCount == lAndExps.size()) {
                // 条件全为0，直接跳转到falseBlock
                BranchInst branchInst = new BranchInst(falseBlock.getName());
                conditionBlock.addInstruction(branchInst);
            }
        } else {
            thenUsed = true;
            BranchInst branchInst = new BranchInst(trueBlock.getName());
            buildingBlock.addInstruction(branchInst);
        }
        // trueBlock（循环体）构建
        buildingBlock = trueBlock;
        basicBlocks.add(buildingBlock);
        if (thenUsed) {
            buildingBlock = buildFromStmt(forStmt.getBody(), buildingBlock, basicBlocks, funcSymbol, needNewBlock
                    , falseBlock.getName(), updateBlock.getName());
        }
        buildingBlock.addInstruction(new BranchInst(updateBlock.getName()));
        // falseBlock（for之后的块）构建
        buildingBlock = falseBlock;
        basicBlocks.add(buildingBlock);
        return buildingBlock;
    }

    private static ArrayList<Instruction> buildFromPrintfStmt(PrintfStmt printfStmt) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        String format = printfStmt.getFormat().getValue();
        StringBuilder content = new StringBuilder();
        int length = 1;
        int index = 0;
        for (int i = 0; i < format.length() - 2; i++) {
            int charValue = getCharValue(format, i);
            if (charValue != '%') {
                if (charValue == '\n') {
                    content.append("\\0A");
                    i++;
                } else {
                    content.append((char) charValue);
                }
                length++;
            } else {
                charValue = getCharValue(format, ++i);
                if (charValue != 'd' && charValue != 'c') {
                    content.append('%');
                    if (charValue == '\n') {
                        content.append("\\0A");
                        i++;
                    } else {
                        content.append((char) charValue);
                    }
                    length += 2;
                    continue;
                }
                if (content.length() != 0) {
                    String str = content.toString();
                    module.addStringConstant(str, length);
                    String strName = module.getStringConstantName(str);
                    PrintInst printInst = new PrintInst("putstr", strName, length);
                    instructions.add(printInst);
                }
                // 处理%的格式输出，只支持%d和%c格式
                content = new StringBuilder();
                length = 1;
                AddExp arg = printfStmt.getArgs().get(index++);
                Integer res = compute(arg, false);
                if (res != null) {
                    Literal literal = new Literal(res, charValue == 'd' ? 32 : 8);
                    PrintInst printInst1 = new PrintInst(charValue == 'd' ? "putint" : "putch", literal);
                    instructions.add(printInst1);
                } else {
                    ValueType resultType;
                    if (charValue == 'd') {
                        resultType = new IntegerType(32);
                    } else {
                        resultType = new IntegerType(8);
                    }
                    ArrayList<Instruction> argInstructions = buildFromAddExp(arg, resultType);
                    PrintInst printInst2 = new PrintInst(charValue == 'd' ? "putint" : "putch", getLastResult(argInstructions));
                    instructions.addAll(argInstructions);
                    instructions.add(printInst2);
                }
            }
        }
        if (content.length() != 0) {
            String str = content.toString();
            module.addStringConstant(str, length);
            String strName = module.getStringConstantName(str);
            PrintInst printInst = new PrintInst("putstr", strName, length);
            instructions.add(printInst);
        }
        return instructions;
    }

    private static BasicBlock buildFromStmt(Stmt stmt, BasicBlock buildingBlock,
                                            ArrayList<BasicBlock> basicBlocks, FuncSymbol funcSymbol,
                                            boolean needNewBlock,
                                            String falseBlockName, String updateBlockName) {
        switch (stmt.getType()) {
            case RETURN:
                ReturnStmt returnStmt = stmt.getReturnStmt();
                Value returnValue = buildReturnValue(returnStmt, funcSymbol, buildingBlock);
                // 向符号表中添加该函数的返回值
                funcSymbol.setReturnValue(returnValue);
                ReturnInst returnInst = new ReturnInst(returnValue);
                buildingBlock.addInstruction(returnInst);
                // 该基本块结束
                if (needNewBlock) {
                    buildingBlock = new BasicBlock(LabelType.getInstance());
                    basicBlocks.add(buildingBlock);
                }
                break;
            case EXP:
                if (stmt.getExp() != null) {
                    AddExp addExp = stmt.getExp();
                    Integer result = compute(addExp, false);
                    if (result == null) {
                        // Exp的返回值并不会被使用，因此指定返回值为Void，此时只进行函数调用，其他操作省略
                        buildingBlock.addInstructions(buildFromAddExp(addExp, VoidType.getInstance()));
                    }
                }
                break;
            case BLOCK:
                //进入新作用域
                SymbolTable preTable = currentTable;
                currentTable = new SymbolTable(preTable, symbolTableIndex++);
                Block innerBlock = stmt.getBlockStmt();
                ArrayList<BasicBlock> innerBlocks = buildBasicBlocks(innerBlock, buildingBlock, funcSymbol, false,
                        falseBlockName, updateBlockName);
                // innerBlocks的第一个基本块是当前基本块，不必重复加入
                innerBlocks.remove(0);
                basicBlocks.addAll(innerBlocks);
                // 此时buildingBlock应该是innerBlocks的最后一个基本块
                if (!innerBlocks.isEmpty()) {
                    buildingBlock = innerBlocks.get(innerBlocks.size() - 1);
                }
                currentTable = preTable;
                break;
            case ASSIGN:
                AssignStmt assignStmt = stmt.getAssignStmt();
                buildingBlock.addInstructions(buildingFromAssignStmt(assignStmt));
                break;
            case IF:
                IfStmt ifStmt = stmt.getIfStmt();
                buildingBlock = buildFromIfStmt(ifStmt, buildingBlock, basicBlocks, funcSymbol, needNewBlock,
                        falseBlockName, updateBlockName);
                break;
            case PRINTF:
                PrintfStmt printfStmt = stmt.getPrintfStmt();
                buildingBlock.addInstructions(buildFromPrintfStmt(printfStmt));
                break;
            case FOR:
                ForStmt forStmt = stmt.getForStmt();
                buildingBlock = buildFromForStmt(forStmt, buildingBlock, basicBlocks, funcSymbol, needNewBlock);
                break;
            case BREAK:
                BranchInst branchInst1 = new BranchInst(falseBlockName);
                buildingBlock.addInstruction(branchInst1);
                break;
            case CONTINUE:
                BranchInst branchInst2 = new BranchInst(updateBlockName);
                buildingBlock.addInstruction(branchInst2);
                break;
            default:
                break;
        }
        return buildingBlock;
    }

    private static ArrayList<BasicBlock> buildBasicBlocks(Block block, BasicBlock buildingBlock,
                                                          FuncSymbol funcSymbol, boolean isInFuncBlock,
                                                          //用于for循环
                                                          String falseBlockName, String updateBlockName) {
        ArrayList<BasicBlock> basicBlocks = new ArrayList<>();
        basicBlocks.add(buildingBlock);
        boolean needNewBlock = true;
        for (BlockItem blockItem : block.getBlockItems()) {
            // 如果是最后一个BlockItem，不需要新建一个基本块
            if (block.getBlockItems().indexOf(blockItem) == block.getBlockItems().size() - 1 && isInFuncBlock) {
                needNewBlock = false;
            }
            if (blockItem.isDecl()) {
                buildingBlock.addInstructions(
                        buildLocalVars(blockItem.getDecl()));
            } else {
                Stmt stmt = blockItem.getStmt();
                buildingBlock = buildFromStmt(stmt, buildingBlock, basicBlocks, funcSymbol, needNewBlock
                        , falseBlockName, updateBlockName);
            }
            if (block.getBlockItems().indexOf(blockItem) == block.getBlockItems().size() - 1 && isInFuncBlock) {
                if (funcSymbol.getReturnType() == FuncSymbol.Type.VOID) {
                    if (buildingBlock.getInstructions().isEmpty() ||
                            !(buildingBlock.getInstructions().get(buildingBlock.getInstructions().size() - 1) instanceof ReturnInst)) {
                        ReturnInst returnInst = new ReturnInst(new Value(VoidType.getInstance()));
                        buildingBlock.addInstruction(returnInst);
                    }
                }
            }
        }
        if (isInFuncBlock && block.getBlockItems().isEmpty()) {
            if (funcSymbol.getReturnType() == FuncSymbol.Type.VOID) {
                ReturnInst returnInst = new ReturnInst(new Value(VoidType.getInstance()));
                buildingBlock.addInstruction(returnInst);
            }
        }
        return basicBlocks;
    }
}
