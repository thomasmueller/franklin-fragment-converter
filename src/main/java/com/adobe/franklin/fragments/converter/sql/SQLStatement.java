package com.adobe.franklin.fragments.converter.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLStatement {

    SQLStatement flushingStatement = new SQLStatement() {
        @Override
        public int addBatch(Connection connection)  {
            // "force" call to executeBatch()
            return Integer.MAX_VALUE;
        }

        @Override
        public int executeBatch() throws SQLException {
            // +1 for flush statement itself
            return SimpleSQLStatement.flush() + PreparedSQLStatement.flushAll() + 1;
        }

        @Override
        public String toString() {
            return "";
        }
    };

    int addBatch(Connection connection) throws SQLException;

    int executeBatch() throws SQLException;

}

