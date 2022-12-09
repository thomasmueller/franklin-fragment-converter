package com.adobe.franklin.fragments.converter.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            statement = new Prepared(connection.prepareStatement(template));
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
        private final PreparedStatement prep;
        private int count;
        
        Prepared(PreparedStatement prep) {
            this.prep = prep;
        }
        
        int addBatch() throws SQLException {
            prep.addBatch();
            count++;
            return count;
        }
        
        int flush() throws SQLException {
            prep.executeBatch();
            int result = count;
            count = 0;
            return result;
        }
    }
}
