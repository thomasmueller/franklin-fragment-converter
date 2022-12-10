package com.adobe.franklin.fragments.converter;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.adobe.franklin.fragments.converter.sql.SQLStatement;
import com.adobe.franklin.fragments.converter.sql.SQLUtils;
import com.adobe.franklin.fragments.extractor.FragmentExtractor;
import com.adobe.franklin.fragments.tables.Fragment;
import com.adobe.franklin.fragments.tables.FragmentReference;
import com.adobe.franklin.fragments.tables.Value;
import com.adobe.franklin.fragments.utils.Profiler;
import com.adobe.franklin.fragments.utils.ProgressLogger;

public class ConvertFragmentsToSQL {
    
    public static void main(String... args) {
        String fileName = null;
        String jdbcDriver = null;
        String jdbcUrl = null;
        String jdbcUser = "";
        String jdbcPassword = "";

        String oakNodeStore = null;
        String oakBlobStore = null;
        String oakUser = "admin";
        String oakPassword = "admin";

        long maxRows = Long.MAX_VALUE;
        int batchSize = 10000;

        boolean profile = false;
        boolean normalizeArrays = true;

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
            } else if ("--oakRepo".equals(args[i])) {
                String oakRepo = args[++i];
                oakNodeStore = oakRepo + "/segmentstore";
                oakBlobStore = oakRepo + "/datastore";
            } else if ("--oakNodeStore".equals(args[i])) {
                oakNodeStore = args[++i];
            } else if ("--oakBlobStore".equals(args[i])) {
                oakBlobStore = args[++i];
            } else if ("--oakUser".equals(args[i])) {
                oakUser = args[++i];
            } else if ("--oakPassword".equals(args[i])) {
                oakPassword = args[++i];
            } else if ("--batchSize".equals(args[i])) {
                batchSize = Integer.parseInt(args[++i]);
            } else if ("--profile".equals(args[i])) {
                profile = true;
            } else if ("--useArrayType".equals(args[i])) {
                normalizeArrays = false;
            } else {
                printUsage();
                throw new IllegalArgumentException(args[i]);
            }
        }
        Profiler prof = profile ? new Profiler().startCollecting() : null;
        if (fileName == null && oakNodeStore == null) {
            printUsage();
        } else {
            Json file;
            if (fileName != null) {
                file = Json.parseFile(fileName);
            } else {
                file = new FragmentExtractor().extract(oakNodeStore, oakBlobStore, oakUser, oakPassword);
            }
            List<SQLStatement> statements = getSQLStatements(file, maxRows, normalizeArrays);
            if (jdbcUrl != null) {
                Connection connection = SQLUtils.getJdbcConnection(jdbcDriver, jdbcUrl, jdbcUser, jdbcPassword);
                ProgressLogger.logMessage("Executing " + statements.size() + " SQL statements");
                SQLUtils.executeSQL(connection, statements, batchSize);
                ProgressLogger.logDone();
            } else {
                statements.forEach(System.out::println);
            }
        }
        if (prof != null) {
            ProgressLogger.logMessage(prof.getTop(10));
        }
    }
    
    public static void printUsage() {
        System.out.println("Usage: java " + ConvertFragmentsToSQL.class.getCanonicalName());
        System.out.println("  --fileName <fragment.json>  The file name of the json files that contains fragments and models");
        System.out.println("  --maxRows <count>           The maximum number of SQL statements to print (optional, default: all)");
    }
    
    public static List<SQLStatement> getSQLStatements(Json file, long maxLines, boolean normalizeArrays) {
        List<SQLStatement> statements = new ArrayList<>();

        long row = 0;

        if (normalizeArrays) {
            statements.add(Value.toDropSQL());
            statements.add(Value.toCreateSQL());
        }

        Json models = file.getChild("models");
        HashMap<String, Model> modelMap = new HashMap<>();
        for (Entry<String, Json> entry : models.getChildren().entrySet()) {
            if (++row > maxLines) {
                break;
            }
            String key = entry.getKey();
            Json columnsJson = entry.getValue();
            ArrayList<Column> columns = new ArrayList<>();
            String tableName = key.substring(key.lastIndexOf('/') + 1);
            for (Entry<String, Json> col : columnsJson.getChildren().entrySet()) {
                Column column = new Column();
                column.name = col.getKey();
                Json columnType = col.getValue();
                String metaType = columnType.getStringProperty("metaType");
                String valueType = columnType.getStringProperty("valueType");
                column.dataType = SQLUtils.getSQLDataType(metaType, valueType);
                column.isArray = column.dataType.endsWith("[]");
                if (column.isArray && normalizeArrays) {
                    column.dataType = "bigint";
                }
                columns.add(column);
            }
            Model model = new Model(tableName, columns);
            statements.add(model.toDropSQL());
            statements.add(model.toCreateSQL());
            modelMap.put(key, model);
        }

        Json fragments = file.getChildren().get("fragments");
        statements.add(Fragment.toDropSQL());
        statements.add(Fragment.toCreateSQL());
        statements.add(FragmentReference.toDropSQL());
        statements.addAll(FragmentReference.toCreateSQL());
        statements.add(SQLStatement.flushingStatement);

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
            statements.addAll(model.toInsertSQL(path, entry.getValue()));
            Fragment fragment = new Fragment(fragmentId, path, modelName);
            fragmentId++;
            fragmentMap.put(path, fragment);
            statements.add(fragment.toInsertSQL());
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
                statements.add(ref.toInsertSQL());
            }
        }

        statements.add(SQLStatement.flushingStatement);
        return statements;
    }

}
