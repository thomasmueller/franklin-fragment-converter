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
        StringBuilder buff = new StringBuilder();
        buff.append("create table \"" + tableName + "\"(\n");
        // TODO probably the primary key needs to be a combination of the path and the variation
        buff.append("    \"_path\" varchar(8000) primary key,\n");
        buff.append("    \"_variation\" varchar(8000)");
        for (Column col : columns) {
            buff.append(",\n");
            buff.append("    " + col.toCreateSQL());
        }
        buff.append("\n)");
        return new SimpleSQLStatement(buff.toString());
    }
    
    private String toInsertStatement() {
        StringBuilder buff = new StringBuilder();
        buff.append("insert into \"" + tableName + "\"(\"_path\", \"_variation\"");
        for (Column col : columns) {
            buff.append(", ");
            buff.append("\"" + col.name + "\"");
        }
        buff.append(") values (");
        buff.append("?, ");
        buff.append("?");
        for (int i = 0; i < columns.size(); i++) {
            buff.append(", ");
            buff.append("?");
        }
        buff.append(")");
        return buff.toString();
    }
    
    public PreparedSQLStatement toInsertSQL(String path, Json data) {
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
                    sqlValue = new SQLStringArray(list);
                } else {
                    sqlValue = new SQLValue(col.getTypeNumber(), value);
                }
            } else if (data.isArray(key)) {
                List<String> list = data.getStringArray(key);
                sqlValue = new SQLStringArray(list);
            } else {
                throw new IllegalArgumentException(data.getChild(key).toString());
            }
            arguments.add(sqlValue);
        }
        return new PreparedSQLStatement(insertStatement, arguments);
    }

    public List<FragmentReference> getReferenceList(HashMap<String, Fragment> fragmentMap, Fragment source, Json data) {
        List<FragmentReference> result = new ArrayList<>();
        for (Column col : columns) {
            String key = col.name;
            if (!data.containsKey(key)) {
                key = col.name + "S";
            }
            if (!data.containsKey(key)) {
                // ignore
            } else if (data.isStringProperty(key)) {
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