package com.adobe.franklin.fragments.converter.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLValue implements SQLArgument {
    private final int type;
    private final Object value;

    public SQLValue(int type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public void insertInto(Connection connection, PreparedStatement statement, int index) throws SQLException {
        statement.setObject(index, value, type);
    }
}
