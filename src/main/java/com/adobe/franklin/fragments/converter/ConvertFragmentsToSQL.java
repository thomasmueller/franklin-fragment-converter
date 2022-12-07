package com.adobe.franklin.fragments.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class ConvertFragmentsToSQL {
    
    public static void main(String... args) {
        String fileName = null;
        long maxRows = Long.MAX_VALUE;
        for(int i=0; i<args.length; i++) {
            if ("--fileName".equals(args[i])) {
                fileName = args[++i];
            } else if ("--maxRows".equals(args[i])) {
                maxRows = Long.parseLong(args[++i]);
            } else {
                printUsage();
                throw new IllegalArgumentException(args[i]);
            }
        }
        if (fileName == null) {
            printUsage();
        } else {
            List<String> list = getSQLStatements(fileName, maxRows);
            for (String sql : list) {
                System.out.println(sql);
            }
        }
    }
    
    public static void printUsage() {
        System.out.println("Usage: java " + ConvertFragmentsToSQL.class.getCanonicalName());
        System.out.println("  --fileName <fragment.json>  The file name of the json files that contains fragments and models");
        System.out.println("  --maxRows <count>           The maximum number of SQL statements to print (optional, default: all)");
    }
    
    public static List<String> getSQLStatements(String fileName, long maxLines) {
        Json file = Json.parseFile(fileName);
        Json models = file.getChild("models");
        HashMap<String, Model> modelMap = new HashMap<>();
        ArrayList<String> result = new ArrayList<>();
        
        for (Entry<String, Json> entry : models.getChildren().entrySet()) {
            String key = entry.getKey();
            Json columns = entry.getValue();
            Model model = new Model();
            model.tableName = key.substring(key.lastIndexOf('/') + 1);
            model.path = key;
            for (Entry<String, Json> col : columns.getChildren().entrySet()) {
                Column column = new Column();
                column.name = col.getKey();
                Json columnType = col.getValue();
                String metaType = columnType.getStringProperty("metaType");
                String valueType = columnType.getStringProperty("valueType");
                column.dataType = SQLUtils.getSQLDataType(metaType, valueType);
                column.isArray = column.dataType.endsWith("]");
                model.columns.add(column);
            }
            result.add(model.toDropCreateSQL());
            modelMap.put(key, model);
        }
        Json fragments = file.getChildren().get("fragments");
        long row = 0;
        for (Entry<String, Json> entry : fragments.getChildren().entrySet()) {
            if (row++ > maxLines) {
                break;
            }
            String path = entry.getKey();
            Json data = entry.getValue();
            String modelName = data.getStringProperty("_model");
            Model model = modelMap.get(modelName);
            result.add(model.toInsertSQL(path, entry.getValue()));
        }
        return result;
    }

}
