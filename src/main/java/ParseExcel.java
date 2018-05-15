import Common.FieldType;
import Common.InsertData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class ParseExcel {
    private Logger logger = LoggerFactory.getLogger(ParseExcel.class);
    private TreeMap<Integer, FieldType> fieldType;
    private Map<Integer, String> columnTableName;
    private Path sourceExcelFilePath;
    private Path resultOutFilePath;

    public ParseExcel() {
    }

    public ParseExcel parseTo(Path resultOutFilePath) {
        this.resultOutFilePath = resultOutFilePath;

        return this;
    }

    public ParseExcel parseExcel(Path sourceFile) {
        this.sourceExcelFilePath = sourceFile;

        return this;
    }

    public void parse() throws Exception{
        createOutFile();
        runParse();
    }

    private void runParse() throws Exception {
        try (XSSFWorkbook book = new XSSFWorkbook(new FileInputStream(sourceExcelFilePath.toFile()))) {
            int countSheets = book.getNumberOfSheets();
            logger.info(String.format("Find %d sheets in a book '%s'!", countSheets, sourceExcelFilePath.toFile().getName()));

            for (int i = 0; i < countSheets; i++) {
                XSSFSheet sheet = book.getSheetAt(i);
                String sheetName = sheet.getSheetName().trim();

                // Проверка, что данная страница не игнорируется
                if (sheetName.length() > 2 && sheetName.substring(0, 2).contains("--")){
                    logger.info(String.format("Ignored sheet %d: '%s'", i, sheetName));
                    continue;
                } else if (sheetName.length() <= 2) {
                    logger.info(String.format("The sheet name '%s' is not correct!", sheetName));
                    continue;
                }
                logger.info(String.format("Read sheet %d: '%s'", i, sheetName));

                // Читается информация о пользовательских типах строк
                logger.info("Read user field type.");
                readUsersFieldType(sheet);
                // Читается информация о названиях колонок таблицы
                logger.info("Read column table name.");
                readColumnTabledName(sheet);

                // Формированияе "шапки" инсерта
                InsertData insertData = new InsertData(sheetName);
                insertData.withHeadInsert(fieldType, columnTableName);

                StringBuilder sheetDataBuilder = new StringBuilder();
                for (Row row: sheet) {
                    if (row.getRowNum() < 4 || isIgnoredRow(row)) {
                        continue;
                    } else if (isRowEnd(row)) {
                        break;
                    } else {
                        // Формированияе "данных" инсерта
                        insertData.withDataInsert(readRow(row));
                    }

                    sheetDataBuilder.append(insertData.create());
                    if (row.getRowNum() % 500 == 0){
                        writeData(sheetDataBuilder.append("commit;\r\n").toString());
                        sheetDataBuilder = new StringBuilder();
                    }
                }
                logger.info(String.format("The sheet %d '%s' is end!", i, sheetName));

                writeData(sheetDataBuilder.append("commit;\n\n\n\n").toString());
            }
        } catch (NullPointerException | IOException e) {
            logger.error("FAILED", e);
        }
    }

    private void createOutFile(){
        try {
            if (!resultOutFilePath.toFile().exists()) {
                Files.createFile(resultOutFilePath);
            } else {
                resultOutFilePath.toFile().delete();
                Files.createFile(resultOutFilePath);
            }
        } catch (IOException e) {
            logger.error("FAILED", e);
        }
    }

    private boolean isRowEnd(Row row) {
        boolean isRowEnd = false;
        try {
            isRowEnd = row.getLastCellNum() < 0 || row.getSheet().getRow(row.getRowNum() - 1) == null;
        } catch (NullPointerException e){
            logger.error(String.format("FAILED - Sheet:%s. Row: %d", row.getSheet().getSheetName(), row.getRowNum() + 1), e);
        }

        return isRowEnd;
    }

    private boolean isIgnoredRow(Row row) {
        boolean isIgnored = false;
        try {
            isIgnored = row.getCell(0) != null && row.getCell(0).getStringCellValue().toLowerCase().equals("ignored");
        } catch (NullPointerException e){
            logger.error(String.format("FAILED - Sheet:%s. Row: %d", row.getSheet().getSheetName(), row.getRowNum() + 1), e);
        }

        return isIgnored;
    }

    private void readUsersFieldType(Sheet sheet) throws Exception {
        fieldType = new TreeMap<>();
        try {
            for (Cell cell : sheet.getRow(2)) {
                fieldType.put(cell.getColumnIndex(), FieldType.valueOf(cell.getStringCellValue().toUpperCase()));
            }
        } catch (NullPointerException  | IllegalStateException e){
            logger.error(String.format("FAILED read users field type - Sheet:%s", sheet.getSheetName()), e);
            throw new Exception(e);
        }
    }

    private void readColumnTabledName(Sheet sheet) throws Exception {
        columnTableName = new TreeMap<>();
        try {
            for (Cell cell : sheet.getRow(3)) {
                if (fieldType.containsKey(cell.getColumnIndex())) {
                    columnTableName.put(cell.getColumnIndex(), cell.getStringCellValue());
                }
            }
        } catch (NullPointerException  | IllegalStateException e){
            logger.error(String.format("FAILED read column table name - Sheet:%s", sheet.getSheetName()), e);
            throw new Exception(e);
        }
    }

    private Map readRow(Row row) throws Exception {
        HashMap<Integer, Cell> rowDataMap = new HashMap<>();

        try {
            for (Cell cell : row) {
                if (cell.getColumnIndex() > 1) {
                    if (fieldType.containsKey(cell.getColumnIndex())) {
                        rowDataMap.put(cell.getColumnIndex(), cell);
                    }
                }
            }
        } catch (NullPointerException  | IllegalStateException e){
            logger.error(String.format("FAILED read column table name - Sheet:%s. Row number - %d", row.getSheet().getSheetName(), row.getRowNum() + 1), e);
            throw new Exception(e);
        }

        return rowDataMap;
    }

    private void writeData(String data) throws Exception {
        try (RandomAccessFile writer = new RandomAccessFile(resultOutFilePath.toFile(), "rw")){
            writer.seek(resultOutFilePath.toFile().length());
            writer.write(data.getBytes("Windows-1251"));
        }
        catch (IOException | NullPointerException e){
            logger.error("FAILED! ", e);
            throw new Exception(e);
        }
    }
}
