package com.adobe.franklin.fragments.converter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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
            return "varchar(8000)";
        case "fragment-reference string/content-fragment[]":
            return "varchar(8000)[]";
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
            return "varchar(8000)";
        case "reference string/reference":
            return "varchar(8000)";
        case "reference string/reference[]":
            return "varchar(8000)[]";
        case "string string[]":
            return "text[]";
        case "tags string/tags[]":
            return "varchar(8000)[]";
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

    static void executeSQL(Connection conn, List<String> list) {
        try {
            long lastTime = System.currentTimeMillis();
            Statement stat = conn.createStatement();
            long count = 0;
            for(String sql : list) {
                try {
                    stat.execute(sql);
                } catch (SQLException e) {
                    throw new IllegalArgumentException(sql, e);
                }
                count++;
                long time = System.currentTimeMillis();
                if (time - lastTime > 2000) {
                    System.out.println("Executed " + count + " of " + list.size() + " statements...");
                    lastTime = time;
                }
            }
            System.out.println("Done");
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static Connection getJdbcConnection(String driver, String url, String user, String password) {
        System.out.println("Connecting to " + url + "...");
        if (driver == null) {
            if (url.startsWith("jdbc:postgresql:")) {
                driver = "org.postgresql.Driver";
            } else if (url.startsWith("jdbc:sqlite:")) {
                driver = "org.sqlite.JDBC";
            } else if (url.startsWith("jdbc:mariadb:")) {
                driver = "org.mariadb.jdbc.Driver";
            } else if (url.startsWith("jdbc:mysql:")) {
                driver = "com.mysql.cj.jdbc.Driver";
            } else if (url.startsWith("jdbc:h2:")) {
                driver = "org.h2.Driver";
            }
        }
        if (driver != null && driver.length() > 0) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("Done");
            return conn;
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
