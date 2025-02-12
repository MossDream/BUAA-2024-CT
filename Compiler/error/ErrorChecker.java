package error;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

/**
 * @Description ErrorChecker
 */

public class ErrorChecker {

    private static TreeMap<Integer, String> errorList = new TreeMap<>();

    public static void addError(ErrorType errorType, int lineNum) {
        String error = lineNum + " " + errorType.toString();
        errorList.put(lineNum, error);
    }

    public static void clearError() {
        errorList.clear();
    }

    public static boolean hasError() {
        return !errorList.isEmpty();
    }

    public static void printErrorToFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter writer = new FileWriter(file);
        for (String error : errorList.values()) {
            writer.write(error + "\n");
        }
        writer.close();
    }
}
