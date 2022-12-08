package com.adobe.franklin.fragments.converter.sql;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class SQLStringArray implements SQLArgument {
    private final List<String> values;

    public SQLStringArray(List<String> values) {
        this.values = values;
    }

    @Override
    public void insertInto(Connection connection, PreparedStatement statement, int index) throws SQLException {
        Array array = connection.createArrayOf("varchar", values.toArray());
        statement.setArray(index, array);
    }
}
