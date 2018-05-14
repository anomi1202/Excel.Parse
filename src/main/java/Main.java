import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Main {
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    @Parameter(names = {"-source_file", "-sf"}, description = "Source excel file with format 'xlsx'")
    private static Path sourceExcelPath;

    @Parameter(names = {"-result_file", "-rf"}, description = "Result sql file.")
    private static Path outFilePath;

    public static void main(String[] args) {
        Main main = new Main();
        JCommander jCommander = new JCommander(main);
        try {
            jCommander.parse(args);
            logger.info(String.format("Read excel file: %s", sourceExcelPath));
            logger.info(String.format("Write inserts to file: %s", outFilePath));
            new ParseExcel(sourceExcelPath).parseExcelTo(outFilePath);
        } catch (ParameterException e) {
            logger.error("FAILED", e);
            jCommander.usage();
        }
    }
}
