package Common;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static Common.FieldType.*;

public class InsertData {
    private Logger logger = LoggerFactory.getLogger(InsertData.class);
    private String sheetName;
    private Map<Integer, FieldType> userFieldType;
    private String headInsert;
    private String dataInsert;

    public InsertData(String sheetName) {
        this.sheetName = sheetName;
    }

    public InsertData withHeadInsert(TreeMap<Integer, FieldType> userFieldType, Map<Integer , String> columnTableName) {
        this.userFieldType = userFieldType;

        StringBuilder builderHeadInsert = new StringBuilder(String.format("insert into %s (", sheetName));
        userFieldType.forEach((indexCell, fieldType) -> builderHeadInsert.append(columnTableName.get(indexCell)).append(", "));
        headInsert = builderHeadInsert.delete(builderHeadInsert.lastIndexOf(","), builderHeadInsert.length())
                .append(")").toString();

        return this;
    }

    public InsertData withDataInsert(Map<Integer, Cell> dataMap) throws Exception {
        StringBuilder dataInsertBuilder = new StringBuilder("values (");

        try {
            for (Map.Entry<Integer, FieldType> fielType : userFieldType.entrySet()) {
                String dataInsert = valueToUserType(fielType.getValue(), dataMap.get(fielType.getKey()));
                dataInsertBuilder.append(dataInsert).append(", ");
            }
            dataInsert = dataInsertBuilder.delete(dataInsertBuilder.lastIndexOf(","), dataInsertBuilder.length())
                    .append(");\r\n").toString();
        } catch (IllegalStateException | NullPointerException e){
            logger.error("FAILED", e);
        }
        return this;
    }

    private String valueToUserType(FieldType type, Cell cell) throws Exception {
        String l_dataInsert = "";
        try {
            switch (type) {
                case STRING:
                    l_dataInsert = cell.toString();
                    CellType realCellType = cell.getCellTypeEnum();
                    if (!l_dataInsert.toUpperCase().equals("NULL")){
                        if (realCellType.equals(CellType.NUMERIC)){
                            l_dataInsert = String.format("'%s'", valueToUserType(DECIMAL, cell));
                        } else {
                            l_dataInsert = String.format("'%s'", l_dataInsert);
                        }
                    }
                    break;
                case DECIMAL:
                    l_dataInsert = cell.toString();
                    if (!l_dataInsert.contains("select") && !l_dataInsert.contains("from")){
                        l_dataInsert = String.format("%.0f", cell.getNumericCellValue());
                    }
                    break;
                case DATE:
                    DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
                    l_dataInsert = String.format("'%s'", dateFormat.format(cell.getDateCellValue()));
                    break;
                case TIMESTAMP:
                    DateFormat timeStampFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
                    l_dataInsert = String.format("'%s'", timeStampFormat.format(cell.getDateCellValue()));
                    break;
                case MONEY:
                    l_dataInsert = String.format(Locale.ENGLISH, "'%.2f'", cell.getNumericCellValue());
                    break;
                default:
                    System.out.print("''");
                    break;
            }
        } catch (IllegalStateException | NullPointerException e){
            if (cell != null
                    && (cell.toString().toUpperCase().equals("NULL") || cell.getStringCellValue().equals("NULL"))) {
                l_dataInsert = cell.getStringCellValue();
            } else if (cell == null) {
                logger.error(String.format("FAILED! User field type - %s", type.toString()), e);
                throw new Exception(e);
            } else {
                logger.error(String.format("FAILED! Sheet name: %s. Cell index: %s.", cell.getSheet().getSheetName(), cell.getAddress()), e);
                throw new Exception(e);
            }
        }

        return l_dataInsert;
    }

    public String create(){
        return String.format("%s %s", headInsert, dataInsert);
    }
}
