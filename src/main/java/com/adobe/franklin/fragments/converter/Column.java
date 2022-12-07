package com.adobe.franklin.fragments.converter;

class Column {
    String name;
    String dataType;
    boolean isArray;
    
    public String toCreateSQL() {
        return "\"" + name + "\" " + dataType;
    }

    public String getDefaulValue() {
        switch (dataType) {
        case "bigint":
        case "float8":
            return "\"0\"";
        case "boolean":
            return "\"false\"";
        }
        return "\"\"";
    }
}