package com.adobe.franklin.fragments.converter;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.adobe.franklin.fragments.converter.sql.PreparedSQLStatement;
import com.adobe.franklin.fragments.converter.sql.SQLArgument;
import com.adobe.franklin.fragments.converter.sql.SQLStringArray;
import com.adobe.franklin.fragments.converter.sql.SQLValue;
import com.adobe.franklin.fragments.converter.sql.SimpleSQLStatement;
import com.adobe.franklin.fragments.tables.Fragment;
import com.adobe.franklin.fragments.tables.FragmentReference;
import com.adobe.franklin.fragments.tables.Values;

class Model {
    
    private final String tableName;
    private final ArrayList<Column> columns;
    private final String insertStatement;

    public Model(String tableName, ArrayList<Column> columns) {
        this.tableName = tableName;
        this.columns = columns;
        this.insertStatement = toInsertStatement();
    }
    
    public SimpleSQLStatement toDropSQL() {
        return new SimpleSQLStatement("drop table if exists \"" + tableName + "\"");
    }
    
    public SimpleSQLStatement toCreateSQL() {
        // TODO probably the primary key needs to be a combination of the path and the variation
        StringBuilder buff = new StringBuilder()
                .append("create table \"").append(tableName).append("\"(\n")
                .append("    \"_path\" varchar(8000) primary key,\n")
                .append("    \"_variation\" varchar(8000)");
        for (Column col : columns) {
            buff.append(",\n    ").append(col.toCreateSQL());
        }
        buff.append("\n)");
        return new SimpleSQLStatement(buff.toString());
    }
    
    private String toInsertStatement() {
        StringBuilder buff = new StringBuilder();
        buff.append("insert into \"").append(tableName).append("\"(\"_path\", \"_variation\"");
        for (Column col : columns) {
            buff.append(", ").append("\"").append(col.name).append("\"");
        }
        buff.append(") values (?, ?");
        buff.append(", ?".repeat(columns.size()));
        buff.append(")");
        return buff.toString();
    }
    
    public List<PreparedSQLStatement> toInsertSQL(String path, Json data) {
        List<PreparedSQLStatement> result = new ArrayList<>();
        List<SQLArgument> arguments = new ArrayList<>();

        arguments.add(new SQLValue(Types.VARCHAR, path));
        String variation = data.getStringProperty("_variation");
        arguments.add(new SQLValue(Types.VARCHAR, variation));
        for (Column col : columns) {
            SQLArgument sqlValue;
            String key = col.name;
            if (!data.containsKey(key)) {
                key = col.name + "S";
            }
            if (!data.containsKey(key)) {
                sqlValue = new SQLValue(Types.NULL, null);
            } else if (data.isStringProperty(key)) {
                String value = data.getStringProperty(key);
                if (col.isArray) {
                    List<String> list = Collections.singletonList(value);
                    sqlValue = handleArray(col, list, result);
                } else {
                    sqlValue = new SQLValue(col.getTypeCode(), value);
                }
            } else if (data.isArray(key)) {
                List<String> list = data.getStringArray(key);
                sqlValue = handleArray(col, list, result);
            } else {
                throw new IllegalArgumentException(data.getChild(key).toString());
            }
            arguments.add(sqlValue);
        }

        result.add(new PreparedSQLStatement(insertStatement, arguments));
        return result;
    }

    private SQLArgument handleArray(Column column, List<String> values, List<PreparedSQLStatement> sqlStatements) {
        if (column.normalize) {
            long valueId = Values.newId();
            for (String value : values) {
                Values stringValue = new Values(valueId, value);
                sqlStatements.add(stringValue.toInsertSQL());
            }
            return new SQLValue(Types.BIGINT, valueId);
        } else {
            return new SQLStringArray(values);
        }
    }

    public List<FragmentReference> getReferenceList(HashMap<String, Fragment> fragmentMap, Fragment source, Json data) {
        List<FragmentReference> result = new ArrayList<>();
        for (Column col : columns) {
            String key = col.name;
            if (!data.containsKey(key)) {
                key = col.name + "S";
            }
            if (!data.containsKey(key)) {
                continue;
            }
            if (data.isStringProperty(key)) {
                String value = data.getStringProperty(key);
                FragmentReference ref = source.createReferenceIfPossible(fragmentMap, value);
                if (ref != null) {
                    result.add(ref);
                }
            } else if (data.isArray(key)) {
                List<String> list = data.getStringArray(key);
                for (String value : list) {
                    FragmentReference ref = source.createReferenceIfPossible(fragmentMap, value);
                    if (ref != null) {
                        result.add(ref);
                    }
                }
            } else {
                throw new IllegalArgumentException(data.getChild(key).toString());
            }
        }
        return result;
    }
    
}