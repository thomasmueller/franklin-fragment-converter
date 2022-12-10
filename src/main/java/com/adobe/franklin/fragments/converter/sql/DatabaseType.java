package com.adobe.franklin.fragments.converter.sql;

public enum DatabaseType {
    POSTGRESQL,
    SQLITE {
        public String getCascade() {
            return "";
        }
    };

    
    public String getCascade() {
        return "cascade";
    }
    
    public static DatabaseType getFromURL(String url) {
        if (url != null) {
            if (url.startsWith("jdbc:postgresql:")) {
                return POSTGRESQL;
            } else if (url.startsWith("jdbc:sqlite:")) {
                return SQLITE;
            }
        }
        return POSTGRESQL;
    }
}
