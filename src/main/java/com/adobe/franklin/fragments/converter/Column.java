package com.adobe.franklin.fragments.converter;

import java.sql.Types;

class Column {
    String name;
    String dataType;
    boolean isArray;
    
    public String toCreateSQL() {
        return "\"" + name + "\" " + dataType;
    }

    public int getTypeCode() {
        switch (dataType) {
            case "bigint":
                return Types.BIGINT;
            case "float8":
                return Types.FLOAT;
            case "boolean":
                return Types.BOOLEAN;
            default:
                return Types.VARCHAR;
        }
    }

}