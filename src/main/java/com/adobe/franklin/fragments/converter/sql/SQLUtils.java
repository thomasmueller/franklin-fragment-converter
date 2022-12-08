package com.adobe.franklin.fragments.converter.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class SQLUtils {
    public static String getSQLDataType(String metaType, String valueType) {
        switch (metaType + " " + valueType) {
            case "boolean boolean":
                return "boolean";

            case "reference string":
            case "date calendar/date":
            case "date calendar/datetime":
            case "enumeration string":
            case "enumeration string[]":
            case "text-multi string/multiline":
            case "text-single string":
                return "text";

            case "multifield string[]":
            case "reference string[]":
            case "string string[]":
            case "text-multi string/multiline[]":
            case "text-single string[]":
                return "text[]";

            case "fragment-reference string/content-fragment":
            case "reference string/content-fragment":
            case "reference string/reference":
                return "varchar(8000)";

            case "fragment-reference string/content-fragment[]":
            case "reference string/reference[]":
            case "tags string/tags[]":
                return "varchar(8000)[]";

            case "number double":
                return "float8";

            case "number long":
                return "bigint";

            default:
                throw new IllegalArgumentException(metaType + "/" + valueType);
        }
    }

    public static void executeSQL(Connection connection, List<SQLStatement> statements, int batchSize) {
        int executed = 0;

        for (SQLStatement statement : statements) {
            try {
                if (statement.addBatch(connection) >= batchSize) {
                    executed += statement.executeBatch();
                    System.out.println("Executed " + executed + " of " + statements.size() + " statements...");
                }
            } catch (SQLException e) {
                throw new IllegalArgumentException(statement.toString(), e);
            }
        }

        System.out.println("Done");
    }

    public static Connection getJdbcConnection(String driver, String url, String user, String password) {
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
