package com.adobe.franklin.fragments.converter;

import java.util.List;

public class SQLUtils {

    static String escapeSQL(String x) {
        return "'" + x.substring(1, x.length() - 1) + "'";
    }

    static String escapeSQLArray(String x) {
        return "'{" + x.substring(1, x.length() - 1) + "}'";
    }

    public static String convertToSQLValue(List<String> array) {
        StringBuilder buff = new StringBuilder();
        buff.append("array[");
        for (int i=0; i<array.size(); i++) {
            if (i>0) {
                buff.append(", ");
            }
            buff.append(convertToSQLString(array.get(i)));
        }
        buff.append("]");
        return buff.toString();
    }

    public static String convertToSQLValue(String jsonValue) {
        return convertToSQLString(jsonValue);
    }

    public static String convertToSQLString(String x) {
        return "'" + x.replaceAll("'", "''") + "'";
    }

    static String getSQLDataType(String metaType, String valueType) {
        switch (metaType + " " + valueType) {
        case "boolean boolean":
            return "boolean";
        case "date calendar/date":
        case "date calendar/datetime":
            return "text";
        case "enumeration string":
        case "enumeration string[]":
            return "text";
        case "fragment-reference string/content-fragment":
            return "varchar(255)";
        case "fragment-reference string/content-fragment[]":
            return "varchar(255)[]";
        case "multifield string[]":
            return "text[]";
        case "number double":
            return "float8";
        case "number long":
            return "bigint";
        case "reference string":
            return "text";
        case "reference string[]":
            return "text[]";
        case "reference string/content-fragment":
            return "varchar(255)";
        case "reference string/reference":
            return "varchar(255)";
        case "reference string/reference[]":
            return "varchar(255)[]";
        case "string string[]":
            return "text[]";
        case "tags string/tags[]":
            return "varchar(255)[]";
        case "text-multi string/multiline":
            return "text";
        case "text-multi string/multiline[]":
            return "text[]";
        case "text-single string":
            return "text";
        case "text-single string[]":            
            return "text[]";
        default:
            throw new IllegalArgumentException(metaType + "/" + valueType);    
        }
    }

}
