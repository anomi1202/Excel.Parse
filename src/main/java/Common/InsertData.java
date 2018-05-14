package Common;

import org.apache.poi.ss.usermodel.Cell;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class InsertData {
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

    public InsertData withDataInsert(Map<Integer, Cell> dataMap){
        StringBuilder dataInsertBuilder = new StringBuilder("values (");

        for (Map.Entry<Integer, FieldType> fielType : userFieldType.entrySet()) {
            String dataInsert = valueToUserType(fielType.getValue(), dataMap.get(fielType.getKey()));
            dataInsertBuilder.append(dataInsert)
                    .append(", ");
        }
        dataInsert = dataInsertBuilder.delete(dataInsertBuilder.lastIndexOf(","), dataInsertBuilder.length())
                .append(");\r\n").toString();

        return this;
    }

    private String valueToUserType(FieldType type, Cell cell){
        String l_dataInsert = "";
        switch (type){
            case STRING:
                l_dataInsert = String.format("'%s'", cell.getStringCellValue());
                break;
            case DECIMAL:
                l_dataInsert = String.format("%.0f", cell.getNumericCellValue());
                break;
            case DATE:
                DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
                l_dataInsert = String.format("'%s'", dateFormat.format(cell.getDateCellValue()));
                break;
            case TIMESTAMP:
                DateFormat timeStampFormat = new SimpleDateFormat("YYYY-MM-dd HH:MM:SS");
                l_dataInsert = String.format("'%s'", timeStampFormat.format(cell.getDateCellValue()));
                break;
            case MONEY:
                l_dataInsert = String.format(Locale.ENGLISH, "'%.2f'", cell.getNumericCellValue());
                break;
            default:
                System.out.print("''");
                break;
        }

        return l_dataInsert;
    }

    public String create(){
        return String.format("%s %s", headInsert, dataInsert);
    }
}
