package com.adobe.franklin.fragments.converter.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.adobe.franklin.fragments.utils.ProgressLogger;

public class PreparedSQLStatement implements SQLStatement {
    
    private final static Map<String, Prepared> STATEMENTS = new LinkedHashMap<>();

    private final String template;
    private final List<SQLArgument> arguments;

    public PreparedSQLStatement(String template, List<SQLArgument> arguments) {
        this.template = template;
        this.arguments = arguments;
    }

    @Override
    public int addBatch(Connection connection) throws SQLException {
        Prepared statement = STATEMENTS.get(template);
        if (statement == null) {
            statement = new Prepared(template, connection.prepareStatement(template));
            STATEMENTS.put(template, statement);
        }

        for (int i = 0; i < arguments.size(); i++) {
            arguments.get(i).insertInto(connection, statement.prep, i+1);
        }
        return statement.addBatch();
    }

    @Override
    public int executeBatch() throws SQLException {
        int executeCount = 0;
        Prepared statement = STATEMENTS.get(template);
        if (statement != null) {
            executeCount += statement.flush();
        }
        return executeCount;
    }

    static int flushAll() throws SQLException {
        int flushCount = 0;
        for (Prepared statement : STATEMENTS.values()) {
            flushCount += statement.flush();
        }
        STATEMENTS.clear();
        return flushCount;
    }

    @Override
    public String toString() {
        return template;
    }
    
    private static class Prepared {
        private final String sql;
        private final PreparedStatement prep;
        private int count;
        
        Prepared(String sql, PreparedStatement prep) {
            this.sql = sql;
            this.prep = prep;
        }
        
        int addBatch() throws SQLException {
            prep.addBatch();
            count++;
            return count;
        }
        
        int flush() throws SQLException {
            int[] updateCounts = prep.executeBatch();
            for (int i = 0; i < updateCounts.length; i++) {
                if (updateCounts[i] == Statement.EXECUTE_FAILED) {
                    ProgressLogger.logMessage("Failed to execute " + i + ": " + sql);
                }
            }
            int result = count;
            count = 0;
            return result;
        }
    }
}
