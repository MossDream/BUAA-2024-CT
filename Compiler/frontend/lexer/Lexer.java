package frontend.lexer;

import error.ErrorChecker;
import error.ErrorType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @Description Lexer
 */

public class Lexer {
    private static ArrayList<Token> result = new ArrayList<>();

    private static int index = 0;

    public static Token nextToken() {
        if (index < result.size()) {
            return result.get(index++);
        }
        return null;
    }

    public static void rollback(int step) {
        if (index - step >= 0) {
            index -= step;
        } else {
            index = 0;
        }
    }

    public static int getIndex() {
        return index;
    }

    public static void setIndex(int index) {
        Lexer.index = index;
    }

    public static int getLastLineNum() {
        return result.get(index - 2).getLineNum();
    }

    public static boolean isEnd() {
        return index >= result.size();
    }

    public static void lexicalAnalysis(String sourceCode) {
        // 词法分析：以K-V二元组为基本结果形式，K是类别码，V是单词值
        // 逐个字符扫描源程序文本
        ArrayList<Character> buffer = new ArrayList<>();
        int lineNum = 1;
        for (int i = 0; i < sourceCode.length(); i++) {
            buffer.clear();
            char ch = sourceCode.charAt(i);
            // 空格、制表符、换行符、回车符
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                if (ch == '\n') {
                    lineNum++;
                }
                continue;
            }
            // 标识符或者保留关键字
            if (ch == '_' || Character.isLetter(ch)) {
                do {
                    buffer.add(ch);
                    i++;
                    if (i < sourceCode.length()) {
                        ch = sourceCode.charAt(i);
                    } else {
                        break;
                    }
                } while (ch == '_' || Character.isLetterOrDigit(ch));
                // 回退一个字符
                i--;
                // 判断保留字
                StringBuilder word = new StringBuilder();
                for (char c : buffer) {
                    word.append(c);
                }
                String wordStr = word.toString();
                if (ReservedWordsChecker.isReservedWord(wordStr)) {
                    result.add(new Token(ReservedWordsChecker.getTokenType(wordStr),
                            wordStr, lineNum));
                } else {
                    result.add(new Token(TokenType.IDSY, wordStr, lineNum));
                }
            } else if (Character.isDigit(ch)) {
                // 无前导0的整数字面值
                do {
                    buffer.add(ch);
                    i++;
                    if (i < sourceCode.length()) {
                        ch = sourceCode.charAt(i);
                    } else {
                        break;
                    }
                } while (Character.isDigit(ch));
                i--;
                StringBuilder number = new StringBuilder();
                for (char c : buffer) {
                    number.append(c);
                }
                String numberStr = number.toString();
                result.add(new Token(TokenType.INTSY, numberStr, lineNum));
            } else if (ch == '=') {
                // 判断赋值符号
                if (i + 1 < sourceCode.length() && sourceCode.charAt(i + 1) == '=') {
                    result.add(new Token(TokenType.EQUSY, "==", lineNum));
                    i++;
                } else {
                    result.add(new Token(TokenType.ASSIGNSY, "=", lineNum));
                }
            } else if (ch == '+') {
                result.add(new Token(TokenType.PLUSSY, "+", lineNum));
            } else if (ch == '-') {
                result.add(new Token(TokenType.MINUSSY, "-", lineNum));
            } else if (ch == '*') {
                result.add(new Token(TokenType.MULTSY, "*", lineNum));
            } else if (ch == '%') {
                result.add(new Token(TokenType.MODSY, "%", lineNum));
            } else if (ch == '&') {
                if (i + 1 < sourceCode.length() && sourceCode.charAt(i + 1) == '&') {
                    result.add(new Token(TokenType.ANDSY, "&&", lineNum));
                    i++;
                } else {
                    result.add(new Token(TokenType.ANDSY, "&&", lineNum));
                    ErrorChecker.addError(ErrorType.ILLEGAL_SYMBOL, lineNum);
                }
            } else if (ch == '|') {
                if (i + 1 < sourceCode.length() && sourceCode.charAt(i + 1) == '|') {
                    result.add(new Token(TokenType.ORSY, "||", lineNum));
                    i++;
                } else {
                    result.add(new Token(TokenType.ORSY, "||", lineNum));
                    ErrorChecker.addError(ErrorType.ILLEGAL_SYMBOL, lineNum);
                }
            } else if (ch == '<') {
                if (i + 1 < sourceCode.length() && sourceCode.charAt(i + 1) == '=') {
                    result.add(new Token(TokenType.LESSEQSY, "<=", lineNum));
                    i++;
                } else {
                    result.add(new Token(TokenType.LESSSY, "<", lineNum));
                }
            } else if (ch == '>') {
                if (i + 1 < sourceCode.length() && sourceCode.charAt(i + 1) == '=') {
                    result.add(new Token(TokenType.GREATEREQSY, ">=", lineNum));
                    i++;
                } else {
                    result.add(new Token(TokenType.GREATERSY, ">", lineNum));
                }
            } else if (ch == '!') {
                if (i + 1 < sourceCode.length() && sourceCode.charAt(i + 1) == '=') {
                    result.add(new Token(TokenType.NOTEQSY, "!=", lineNum));
                    i++;
                } else {
                    result.add(new Token(TokenType.NOTSY, "!", lineNum));
                }
            } else if (ch == '[') {
                result.add(new Token(TokenType.LBRACKSY, "[", lineNum));
            } else if (ch == ']') {
                result.add(new Token(TokenType.RBRACKSY, "]", lineNum));
            } else if (ch == '{') {
                result.add(new Token(TokenType.LBRACESY, "{", lineNum));
            } else if (ch == '}') {
                result.add(new Token(TokenType.RBRACESY, "}", lineNum));
            } else if (ch == '(') {
                result.add(new Token(TokenType.LPARENSY, "(", lineNum));
            } else if (ch == ')') {
                result.add(new Token(TokenType.RPARENSY, ")", lineNum));
            } else if (ch == ',') {
                result.add(new Token(TokenType.COMMASY, ",", lineNum));
            } else if (ch == ';') {
                result.add(new Token(TokenType.SEMISY, ";", lineNum));
            } else if (ch == '/') {
                // 两种注释：以/*开头, 以*/结尾；或者以//开头，以换行符结尾
                if (i + 1 < sourceCode.length() && sourceCode.charAt(i + 1) == '*') {
                    i += 2;
                    while (i + 1 < sourceCode.length() && !(sourceCode.charAt(i) == '*' && sourceCode.charAt(i + 1) == '/')) {
                        if (sourceCode.charAt(i) == '\n') {
                            lineNum++;
                        }
                        i++;
                    }
                    i++;
                } else if (i + 1 < sourceCode.length() && sourceCode.charAt(i + 1) == '/') {
                    i += 2;
                    while (i < sourceCode.length() && sourceCode.charAt(i) != '\n') {
                        i++;
                    }
                    if (i < sourceCode.length() && sourceCode.charAt(i) == '\n') {
                        lineNum++;
                    }
                } else {
                    result.add(new Token(TokenType.DIVISY, "/", lineNum));
                }
            } else if (ch == '\'') {
                i++;
                if (i < sourceCode.length()) {
                    char c = sourceCode.charAt(i);
                    // 判断可能的转义字符
                    if (i + 1 < sourceCode.length() && c == '\\') {
                        String finalString = "";
                        char c1 = sourceCode.charAt(i + 1);
                        switch (c1) {
                            case 'n':
                                finalString = "'\\n'";
                                i += 2;
                                break;
                            case 't':
                                finalString = "'\\t'";
                                i += 2;
                                break;
                            case 'a':
                                finalString = "'\\a'";
                                i += 2;
                                break;
                            case 'f':
                                finalString = "'\\f'";
                                i += 2;
                                break;
                            case 'v':
                                finalString = "'\\v'";
                                i += 2;
                                break;
                            case 'b':
                                finalString = "'\\b'";
                                i += 2;
                                break;
                            case '\\':
                                finalString = "'\\\\'";
                                i += 2;
                                break;
                            case '\'':
                                finalString = "'\\''";
                                i += 2;
                                break;
                            case '\"':
                                finalString = "'\\\"'";
                                i += 2;
                                break;
                                // 语义分析评测补丁；其余转义字符均设置为'\0'
                            default:
                                finalString = "'\\0'";
                                i += 2;
                                break;
                        }
                        result.add(new Token(TokenType.CHARSY, finalString, lineNum));
                    } else {
                        i++;
                        if (i < sourceCode.length() && sourceCode.charAt(i) == '\'') {
                            result.add(new Token(TokenType.CHARSY, "'" + c + "'", lineNum));
                        }
                    }
                }
            } else if (ch == '"') {
                i++;
                while (i < sourceCode.length() && sourceCode.charAt(i) != '"') {
                    buffer.add(sourceCode.charAt(i));
                    i++;
                }
                if (i < sourceCode.length() && sourceCode.charAt(i) == '"') {
                    StringBuilder str = new StringBuilder();
                    for (char c : buffer) {
                        str.append(c);
                    }
                    result.add(new Token(TokenType.STRINGSY, "\"" + str + "\"", lineNum));
                }
            }
        }
    }

    public static void printResultToFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter writer = new FileWriter(file);
        for (Token token : result) {
            writer.write(token + "\n");
        }
        writer.close();
    }

    public static void clearResult() {
        result.clear();
    }
}
