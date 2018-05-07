import Common.FieldType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import static Common.FieldType.*;

public class ParseExcel {
    private ArrayList<FieldType> fieldType;
    private ArrayList<String> fieldName;
    private File excel = new File("src/main/resources/table_tmp.xlsx");

    public ParseExcel() {
    }

    public void parseExcel() throws IOException {
        try (XSSFWorkbook book = new XSSFWorkbook(new FileInputStream(excel))) {
            for (int i = 0; i < book.getNumberOfSheets(); i++) {
                XSSFSheet sheet = book.getSheetAt(i);
                System.out.println(sheet.getSheetName());

                readUsersFieldType(sheet);
                readFieldName(sheet);

                for (FieldType type : fieldType) {
                    System.out.print(type + "\t\t");
                }
                System.out.println();

                for (String fieldName : fieldName) {
                    System.out.print(fieldName + "\t\t");
                }
                System.out.println();

                for (Row row: sheet) {
                    if (row.getRowNum() < 2){
                        continue;
                    }
                    for (Cell cell: row) {
                        switch (fieldType.get(cell.getColumnIndex())){
                            case STRING:
                                print(STRING, cell);
                                break;
                            case DECIMAL:
                                print(DECIMAL, cell);
                                break;
                            case DATE:
                                print(DATE, cell);
                                break;
                            case TIMESTAMP:
                                print(TIMESTAMP, cell);
                                break;
                            case MONEY:
                                print(MONEY, cell);
                                break;
                            default :
                                break;
                        }
                        if (cell.getColumnIndex() == row.getLastCellNum() - 1){
                            System.out.println();
                        }
                    }
                }
            }

        }
    }

    private void readUsersFieldType(Sheet sheet){
        fieldType = new ArrayList<>();
        for (Row row : sheet) {
            if (row.getRowNum() > 0) {
                break;
            } else if (row.getRowNum() == 0) {
                for (Cell cell : row) {
                    fieldType.add(valueOf(cell.getStringCellValue().toUpperCase()));
                }
            } else {
                continue;
            }
        }
    }

    private void readFieldName(Sheet sheet){
        fieldName = new ArrayList<>();
        for (Row row : sheet){
            if (row.getRowNum() > 1){
                break;
            } else if (row.getRowNum() == 1){
                for (Cell cell : row){
                    fieldName.add(cell.getStringCellValue());
                }
            }else {
                continue;
            }
        }
    }

    private void print(FieldType type, Cell cell){
        switch (type){
            case STRING:
                System.out.print(cell.getStringCellValue() + "\t\t");
                break;
            case DECIMAL:
                System.out.print(cell.getNumericCellValue() + "\t\t");
                break;
            case DATE:
                DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
                System.out.print(dateFormat.format(cell.getDateCellValue()) + "\t\t");
                break;
            case TIMESTAMP:
                DateFormat timeStampFormat = new SimpleDateFormat("YYYY-MM-dd HH:MM:SS");
                System.out.print(timeStampFormat.format(cell.getDateCellValue()) + "\t\t");
                break;
            case MONEY:
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                System.out.print(String.format("%s", decimalFormat.format(cell.getNumericCellValue() + 0.01)));
                System.out.print(String.format(" - %.1f", cell.getNumericCellValue()));
                System.out.print(String.format(Locale.ENGLISH," - %.1f", cell.getNumericCellValue()));
                break;
            default:
                System.out.print(cell.getCellFormula() + "\t\t");
                break;
        }
    }

}
