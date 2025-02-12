package frontend.parser;

import frontend.parser.ast.CompUnit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @Description Parser
 */

public class Parser {

    private static CompUnit compUnit = new CompUnit();

    public static CompUnit getCompUnit() {
        return compUnit;
    }

    public static void parse() {
        compUnit.parse();
    }

    public static void printResultToFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter writer = new FileWriter(file);
        writer.write(compUnit.toString());
        writer.close();
    }
}
