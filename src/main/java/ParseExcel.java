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
                Sheet sheet = book.getSheetAt(i);
                String sheetName = sheet.getSheetName().trim();

                // isSheetIgnored - Проверка имени страницы на длину и на наличие символов игнорирования
                if (!isSheetIgnored(sheetName)){
                    // isSheetWithDeletes - Проверка, что страница - страница с делитами
                    if (isSheetWithDeletes(sheetName)){
                        logger.info(String.format("Read sheet %d: '%s'", i, sheetName));
                        readAndWriteDeletes(sheet);
                    } else {
                        logger.info(String.format("Read sheet %d: '%s'", i, sheetName));
                        // Читается информация о пользовательских типах строк
                        readUsersFieldType(sheet);
                        // Читается информация о названиях колонок таблицы
                        readColumnTabledName(sheet);

                        // Читается и сохраняется в файл страница с данными для инсерта
                        readAndWriteInsert(sheet);
                    }
                }
            }
        } catch (NullPointerException | IOException e) {
            logger.error("FAILED!", e);
        }
    }

    private void readAndWriteDeletes(Sheet sheet) throws Exception {
        String sheetName = sheet.getSheetName().trim();

        // Формированияе "данных" делитов
        StringBuilder sheetDataBuilder = new StringBuilder();
        for (Row row: sheet) {
            readRow(row).values().forEach((value) -> sheetDataBuilder.append(value).append("\r\n"));
            if (row.getRowNum() % 5 == 0 && row.getRowNum() != 0){
                writeData(sheetDataBuilder.toString());
                sheetDataBuilder.delete(0, sheetDataBuilder.length());
            }
        }
        logger.info(String.format("The sheet '%s' is end!", sheetName));

        writeData(sheetDataBuilder.append("\r\n\r\n\r\n\r\n").toString());
    }

    private void readAndWriteInsert(Sheet sheet) throws Exception {
        String sheetName = sheet.getSheetName().trim();
        // Формированияе "шапки" инсерта
        InsertData insertData = new InsertData(sheetName)
                .withHeadInsert(fieldType, columnTableName);

        // Формированияе "данных" инсерта
        StringBuilder sheetDataBuilder = new StringBuilder();
        for (Row row: sheet) {
            if (row.getRowNum() < 4 || isIgnoredRow(row)) {
                continue;
            } else if (isRowEnd(row)) {
                break;
            } else {
                insertData.withDataInsert(readRow(row));
            }

            sheetDataBuilder.append(insertData.create());
            if (row.getRowNum() % 500 == 0){
                writeData(sheetDataBuilder.append("commit;\r\n").toString());
                sheetDataBuilder = new StringBuilder();
            }
        }
        logger.info(String.format("The sheet '%s' is end!", sheetName));

        writeData(sheetDataBuilder.append("commit;\r\n\r\n\r\n\r\n").toString());
    }

    private boolean isSheetWithDeletes(String sheetName) {
        return sheetName.toUpperCase().equals("DELETES");
    }

    private boolean isSheetIgnored(String sheetName) {
        boolean isSheetIgnored = false;
        if (sheetName.length() >= 2 && sheetName.substring(0, 2).contains("--")){
            isSheetIgnored = true;
            logger.info(String.format("Ignored sheet: '%s'", sheetName));
        }

        return isSheetIgnored;
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

            isRowEnd = row.getLastCellNum() < 0
                    || row.getSheet().getRow(row.getRowNum() - 1) == null
                    || row.getLastCellNum() < fieldType.size();
            if (!isRowEnd) {
                isRowEnd = true;
                for (Cell cell : row){
                    if (!cell.toString().equals("")){
                        isRowEnd = false;
                        break;
                    }
                }
            }
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
        logger.info("Read user field type.");
        fieldType = new TreeMap<>();
        Cell userTypeCell = null;
        try {
            for (Cell cell : sheet.getRow(2)) {
                if (cell.getColumnIndex() < 3) {
                    continue;
                } else {
                    userTypeCell = cell;
                    fieldType.put(cell.getColumnIndex(), FieldType.valueOf(cell.getStringCellValue().toUpperCase()));
                }
            }
        } catch (NullPointerException  | IllegalStateException | IllegalArgumentException e){
            logger.error(String.format("FAILED read users field type - Sheet - '%s'. Cell - '%s'", sheet.getSheetName(), userTypeCell.getAddress()), e);
            throw new Exception(e);
        }
    }

    private void readColumnTabledName(Sheet sheet) throws Exception {
        logger.info("Read column table name.");
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

    private Map<Integer, Cell> readRow(Row row) throws Exception {
        HashMap<Integer, Cell> rowDataMap = new HashMap<>();

        try {
            if (row.getSheet().getSheetName().toUpperCase().equals("DELETES")){
                row.forEach((cell) -> rowDataMap.put(cell.getColumnIndex(), cell));
            } else {
                for (Cell cell : row) {
                    if (cell.getColumnIndex() > 2) {
                        if (fieldType.containsKey(cell.getColumnIndex())) {
                            rowDataMap.put(cell.getColumnIndex(), cell);
                        }
                    }
                }
            }
        } catch (NullPointerException  | IllegalStateException e){
            logger.error(String.format("FAILED read column table name - Sheet:%s. Row number - %d", row.getSheet().getSheetName(), row.getRowNum() + 1), e);
            throw new Exception(e);
        }

        return rowDataMap;
    }

    private void writeData(String textToWrite) throws Exception {
        try (RandomAccessFile writer = new RandomAccessFile(resultOutFilePath.toFile(), "rw")){
            writer.seek(resultOutFilePath.toFile().length());

            writer.write(textToWrite.getBytes("UTF-8"));
        }
        catch (IOException | NullPointerException e){
            logger.error("FAILED! ", e);
            throw new Exception(e);
        }
    }
}
