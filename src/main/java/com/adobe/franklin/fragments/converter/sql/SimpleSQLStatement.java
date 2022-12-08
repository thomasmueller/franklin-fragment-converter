package com.adobe.franklin.fragments.converter.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SimpleSQLStatement implements SQLStatement {
    private static Statement statement = null;
    private static int batchCount = 0;

    private final String sql;

    public SimpleSQLStatement(String sql) {
        this.sql = sql;
    }

    @Override
    public int addBatch(Connection connection) throws SQLException {
        if (statement == null) {
            statement = connection.createStatement();
        }
        statement.addBatch(sql);
        return ++batchCount;
    }

    @Override
    public int executeBatch() throws SQLException {
        int executeCount = batchCount;
        if (batchCount > 0) {
            statement.executeBatch();
            batchCount = 0;
        }
        return executeCount;
    }

    static int flush() throws SQLException {
        int flushCount = batchCount;
        if (statement != null) {
            statement.executeBatch();
            statement = null;
            batchCount = 0;
        }
        return flushCount;
    }

    @Override
    public String toString() {
        return sql;
    }
}
