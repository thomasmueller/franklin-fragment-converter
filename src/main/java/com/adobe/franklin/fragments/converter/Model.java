package com.adobe.franklin.fragments.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Model {
    String path;
    String tableName;
    ArrayList<Column> columns = new ArrayList<>();
    
    public String toDropCreateSQL() {
        StringBuilder buff = new StringBuilder();
        buff.append("drop table if exists \"" + tableName + "\";\n");
        buff.append("create table \"" + tableName + "\"(\n");
        // TODO probably the primary key needs to be a combination of the path and the variation
        buff.append("    \"_path\" varchar(255) primary key,\n");
        buff.append("    \"_variation\" varchar(255)");
        for (Column col : columns) {
            buff.append(",\n");
            buff.append("    " + col.toCreateSQL());
        }
        buff.append("\n);");
        return buff.toString();
    }
    
    public String toInsertSQL(String path, Json data) {
        StringBuilder buff = new StringBuilder();
        buff.append("insert into \"" + tableName + "\"(\"_path\", \"_variation\"");
        for (Column col : columns) {
            buff.append(", ");
            buff.append("\"" + col.name + "\"");
        }
        buff.append(") values (");
        buff.append(SQLUtils.convertToSQLString(path));
        buff.append(", ");
        String variation = data.getStringProperty("_variation");
        buff.append(SQLUtils.convertToSQLString(variation));
        for (Column col : columns) {
            buff.append(", ");
            String sqlValue;
            String key = col.name;
            if (!data.containsKey(key)) {
                key = col.name + "S";
            }
            if (!data.containsKey(key)) {
                sqlValue = "null";
            } else if (data.isStringProperty(key)) {
                String value = data.getStringProperty(key);
                if (col.isArray) {
                    List<String> list = Collections.singletonList(value);
                    sqlValue = SQLUtils.convertToSQLValue(list);
                } else {
                    sqlValue = SQLUtils.convertToSQLValue(value);
                }
            } else if (data.isArray(key)) {
                List<String> list = data.getStringArray(key);
                sqlValue = SQLUtils.convertToSQLValue(list);
            } else {
                throw new IllegalArgumentException(data.getChild(key).toString());
            }
            buff.append(sqlValue);
        }
        buff.append(");");
        return buff.toString();
    }
}