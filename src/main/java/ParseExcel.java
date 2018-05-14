import Common.FieldType;
import Common.InsertData;
import javafx.beans.binding.StringBinding;
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
    private int countSheets = 0;
    private Path excel;
    private Path outFile;

    public ParseExcel(Path file) {
        this.excel = file;
    }

    public void parseExcelTo(Path outFile) {
        this.outFile = outFile;
        createOutFile();
        runParse();
    }

    private void runParse() {
        try (XSSFWorkbook book = new XSSFWorkbook(new FileInputStream(excel.toFile()))) {
            countSheets = book.getNumberOfSheets();
            logger.info(String.format("Find %d sheets in a book '%s'!", countSheets, excel.toFile().getName()));

            for (int i = 0; i < countSheets; i++) {
                XSSFSheet sheet = book.getSheetAt(i);

                // Проверка, что данная страница не игнорируется
                if (sheet.getSheetName().substring(0, 2).contains("--")){
                    logger.info(String.format("Ignored sheet %d: '%s'", i, sheet.getSheetName()));
                    continue;
                }
                logger.info(String.format("Read sheet %d: '%s'", i, sheet.getSheetName()));

                // Читается информация о пользовательских типах строк
                logger.info("Read user field type.");
                readUsersFieldType(sheet);
                // Читается информация о названиях колонок таблицы
                logger.info("Read column table name.");
                readColumnTabledName(sheet);

                // Формированияе "шапки" инсерта
                InsertData insertData = new InsertData(sheet.getSheetName());
                insertData.withHeadInsert(fieldType, columnTableName);

                StringBuilder sheetDataBuilder = new StringBuilder();
                for (Row row: sheet) {
                    if (row.getRowNum() < 4 || isIgnoredRow(row)) {
                        continue;
                    } else if (isRowEnd(row)) {
                        logger.info(String.format("The sheet %s is end!", sheet.getSheetName()));
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

                writeData(sheetDataBuilder.append("commit;\n\n\n\n").toString());
            }



        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private void createOutFile(){
        try {
            if (!outFile.toFile().exists()) {
                Files.createFile(outFile);
            } else {
                outFile.toFile().delete();
                Files.createFile(outFile);
            }
        } catch (IOException e) {
            logger.error("FAILED", e);
        }
    }

    private boolean isRowEnd(Row row) {
        boolean isRowEnd = false;
        try {
            isRowEnd = row.getLastCellNum() < 0;
        } catch (NullPointerException e){}

        return isRowEnd;
    }

    private boolean isIgnoredRow(Row row) {
        boolean isIgnored = false;
        try {
            isIgnored = row.getCell(0).getStringCellValue().toLowerCase().equals("ignored");
        } catch (NullPointerException e){}

        return isIgnored;
    }

    private void readUsersFieldType(Sheet sheet){
        fieldType = new TreeMap<>();
        for (Cell cell : sheet.getRow(2)) {
            fieldType.put(cell.getColumnIndex(), FieldType.valueOf(cell.getStringCellValue().toUpperCase()));
        }
    }

    private void readColumnTabledName(Sheet sheet){
        columnTableName = new TreeMap<>();
        for (Cell cell : sheet.getRow(3)){
            if (fieldType.containsKey(cell.getColumnIndex())) {
                columnTableName.put(cell.getColumnIndex(), cell.getStringCellValue());
            }
        }
    }

    private Map readRow(Row row){
        HashMap<Integer, Cell> rowDataMap = new HashMap<>();

        for (Cell cell: row) {
            if (cell.getColumnIndex() > 1) {
                if (fieldType.containsKey(cell.getColumnIndex())) {
                    rowDataMap.put(cell.getColumnIndex(), cell);
                }
            }
        }

        return rowDataMap;
    }

    private void writeData(String data){
        try (RandomAccessFile writer = new RandomAccessFile(outFile.toFile(), "rw")){
            writer.seek(outFile.toFile().length());
            writer.write(data.getBytes("Windows-1251"));
        }
        catch (IOException | NullPointerException e){
            logger.error("FAILED! ", e);
        }
    }
}
