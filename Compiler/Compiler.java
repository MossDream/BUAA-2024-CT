import backend.Translator;
import error.ErrorChecker;
import frontend.parser.Parser;
import frontend.preprocess.Extractor;
import frontend.lexer.Lexer;
import middle.Builder;
import middle.Visitor;

import java.io.IOException;

/**
 * @Description Compiler </br>
 * 前端：词法分析（源程序提取器、语法分析器、保留字检查器）</br>
 * 错误处理：（错误检查器）</br>
 */

public class Compiler {
    public static void main(String[] args) throws IOException {
        // 读取源程序文本文件
        String content = Extractor.extractSourceCode();

        // 词法分析
        Lexer.lexicalAnalysis(content);

        // 语法分析
        Parser.parse();
        Parser.printResultToFile("parser.txt");

        // 语义分析
        Visitor.visit(Parser.getCompUnit());
        Visitor.printResultToFile("symbol.txt");
        ErrorChecker.printErrorToFile("error.txt");

        if (!ErrorChecker.hasError()) {
            // 中间代码生成
            Builder.build(Parser.getCompUnit());
            Builder.printResultToFile("llvm_ir.txt");

            // 目标代码生成与优化
            Translator translator = new Translator(Builder.getModule(), false);
            Translator translatorWithOpt = new Translator(Builder.getModule(), true);
            translator.translate();
            translator.printResultToFile("mips_non_opt.txt");
            translatorWithOpt.translate();
            translatorWithOpt.printResultToFile("mips.txt");
        }
    }
}
