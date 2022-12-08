package com.adobe.franklin.fragments.converter.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PreparedSQLStatement implements SQLStatement {
    private final static Map<String, PreparedStatement> statements = new LinkedHashMap<>();
    private final static Map<PreparedStatement, Integer> batchCounts = new HashMap<>();

    private final String template;
    private final List<SQLArgument> arguments;

    public PreparedSQLStatement(String template, List<SQLArgument> arguments) {
        this.template = template;
        this.arguments = arguments;
    }

    @Override
    public int addBatch(Connection connection) throws SQLException {
        PreparedStatement statement = statements.get(template);
        if (statement == null) {
            statement = connection.prepareStatement(template);
            statements.put(template, statement);
            batchCounts.put(statement, 0);
        }

        for (int i = 0; i < arguments.size(); i++) {
            arguments.get(i).insertInto(connection, statement, i+1);
        }
        statement.addBatch();

        int newCount = batchCounts.get(statement) + 1;
        batchCounts.put(statement, newCount);
        return newCount;
    }

    @Override
    public int executeBatch() throws SQLException {
        int executeCount = 0;
        PreparedStatement statement = statements.get(template);
        if (statement != null) {
            statement.executeBatch();
            executeCount = batchCounts.get(statement);
            batchCounts.put(statement, 0);
        }
        return executeCount;
    }

    static int flush() throws SQLException {
        int flushCount = 0;
        for (PreparedStatement statement : statements.values()) {
            statement.executeBatch();
            flushCount += batchCounts.get(statement);
        }
        statements.clear();
        batchCounts.clear();
        return flushCount;
    }

    @Override
    public String toString() {
        return template;
    }
}
