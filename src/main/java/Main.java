import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    @Parameter(names = {"-source_file", "-sf"}, description = "Source excel file with format 'xlsx'")
    private static Path sourceExcelPath;

    @Parameter(names = {"-result_file", "-rf"}, description = "Result sql file.")
    private static Path outFilePath;

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        JCommander jCommander = new JCommander(main);
        try {
            jCommander.parse(args);
            if (outFilePath == null){
                String str_sourceExcelPath = sourceExcelPath.toString();
                outFilePath = Paths.get(str_sourceExcelPath.substring(0, str_sourceExcelPath.lastIndexOf(".xlsx")) + ".sql");
            }
            logger.info(String.format("Read excel file: %s", sourceExcelPath.toAbsolutePath()));
            logger.info(String.format("Write inserts to file: %s", outFilePath.toAbsolutePath()));
            new ParseExcel().parseExcel(sourceExcelPath).parseTo(outFilePath).parse();
        } catch (ParameterException e) {
            logger.error("FAILED", e);
            jCommander.usage();
        }
    }
}
