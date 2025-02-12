package frontend.lexer;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @Description ReservedWordsChecker
 */

public class ReservedWordsChecker {
    private static HashSet<String> reservedWords =
            new HashSet<>(Arrays.asList(
                    "const", "main", "if", "int", "char", "void", "else", "for",
                    "continue", "break", "return", "getint", "getchar", "printf"
            )
            );

    public static boolean isReservedWord(String word) {
        return reservedWords.contains(word);
    }

    public static TokenType getTokenType(String word) {
        if (isReservedWord(word)) {
            return switch (word) {
                case "const" -> TokenType.CONST;
                case "main" -> TokenType.MAIN;
                case "if" -> TokenType.IF;
                case "int" -> TokenType.INT;
                case "char" -> TokenType.CHAR;
                case "void" -> TokenType.VOID;
                case "else" -> TokenType.ELSE;
                case "for" -> TokenType.FOR;
                case "continue" -> TokenType.CONTINUE;
                case "break" -> TokenType.BREAK;
                case "return" -> TokenType.RETURN;
                case "getint" -> TokenType.GETINT;
                case "getchar" -> TokenType.GETCHAR;
                case "printf" -> TokenType.PRINTF;
                default -> TokenType.ERROR;
            };
        } else {
            return TokenType.ERROR;
        }
    }
}
