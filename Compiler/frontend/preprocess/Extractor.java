package frontend.preprocess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @Description Extractor
 */

public class Extractor {
    public static String extractSourceCode()
            throws IOException {
        String filePath = "testfile.txt";
        String sourceCode = Files.readString(Paths.get(filePath));
        return sourceCode;
    }
}
