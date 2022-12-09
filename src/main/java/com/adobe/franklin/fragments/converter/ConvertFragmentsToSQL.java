package com.adobe.franklin.fragments.converter;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.adobe.franklin.fragments.tables.Fragment;
import com.adobe.franklin.fragments.tables.FragmentReference;
import com.adobe.franklin.fragments.utils.Profiler;

public class ConvertFragmentsToSQL {
    
    public static void main(String... args) {
        String fileName = null;
        String jdbcDriver = null;
        String jdbcUrl = null;
        String jdbcUser = "";
        String jdbcPassword = "";
        boolean profile = false;
        long maxRows = Long.MAX_VALUE;
        for(int i=0; i<args.length; i++) {
            if ("--fileName".equals(args[i])) {
                fileName = args[++i];
            } else if ("--maxRows".equals(args[i])) {
                maxRows = Long.parseLong(args[++i]);
            } else if ("--jdbcDriver".equals(args[i])) {
                jdbcDriver = args[++i];
            } else if ("--jdbcUrl".equals(args[i])) {
                jdbcUrl = args[++i];
            } else if ("--jdbcUser".equals(args[i])) {
                jdbcUser = args[++i];
            } else if ("--jdbcPassword".equals(args[i])) {
                jdbcPassword = args[++i];
            } else if ("--profile".equals(args[i])) {
                profile = true;
            } else {
                printUsage();
                throw new IllegalArgumentException(args[i]);
            }
        }
        Profiler prof = profile ? new Profiler().startCollecting() : null;
        if (fileName == null) {
            printUsage();
        } else {
            List<String> list = getSQLStatements(fileName, maxRows);
            if (jdbcUrl != null) {
                Connection conn = SQLUtils.getJdbcConnection(jdbcDriver, jdbcUrl, jdbcUser, jdbcPassword);
                long time = System.currentTimeMillis();
                SQLUtils.executeSQL(conn, list);
                time = System.currentTimeMillis() - time;
                System.out.println(list.size() + " SQL statements executed in " + time + " ms");
            } else {
                for (String sql : list) {
                    System.out.println(sql + ";");
                }
            }
        }
        if (prof != null) {
            System.out.println(prof.getTop(10));
        }
    }
    
    public static void printUsage() {
        System.out.println("Usage: java " + ConvertFragmentsToSQL.class.getCanonicalName());
        System.out.println("  --fileName <fragment.json>  The file name of the json files that contains fragments and models");
        System.out.println("  --maxRows <count>           The maximum number of SQL statements to print (optional, default: all)");
    }
    
    public static List<String> getSQLStatements(String fileName, long maxLines) {
        Json file = Json.parseFile(fileName);
        ArrayList<String> result = new ArrayList<>();
        long row = 0;

        Json models = file.getChild("models");
        HashMap<String, Model> modelMap = new HashMap<>();
        for (Entry<String, Json> entry : models.getChildren().entrySet()) {
            if (++row > maxLines) {
                break;
            }
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
            result.add(model.toDropSQL());
            result.add(model.toCreateSQL());
            modelMap.put(key, model);
        }
        
        Json fragments = file.getChildren().get("fragments");
        result.add(Fragment.toDropSQL());
        result.add(Fragment.toCreateSQL());
        result.add(FragmentReference.toDropSQL());
        result.addAll(FragmentReference.toCreateSQL());
        HashMap<String, Fragment> fragmentMap = new HashMap<>();
        long fragmentId = 0;
        for (Entry<String, Json> entry : fragments.getChildren().entrySet()) {
            if (++row > maxLines) {
                break;
            }
            String path = entry.getKey();
            Json data = entry.getValue();
            String modelName = data.getStringProperty("_model");
            Model model = modelMap.get(modelName);
            result.add(model.toInsertSQL(path, entry.getValue()));
            Fragment fragment = new Fragment(fragmentId, path, modelName);
            fragmentId++;
            fragmentMap.put(path, fragment);
            result.add(fragment.toInsertSQL());
        }
        for (Entry<String, Json> entry : fragments.getChildren().entrySet()) {
            if (++row > maxLines) {
                break;
            }
            String path = entry.getKey();
            Json data = entry.getValue();
            String modelName = data.getStringProperty("_model");
            Model model = modelMap.get(modelName);
            Fragment fragment = fragmentMap.get(path);
            List<FragmentReference> references = model.getReferenceList(fragmentMap, fragment, data);
            for (FragmentReference ref : references) {
                result.add(ref.toInsertSQL());
            }
        }
        
        return result;
    }

}
