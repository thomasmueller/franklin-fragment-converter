package com.adobe.franklin.fragments.converter.sql;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class SQLStringArray implements SQLArgument {
    private final List<String> values;

    public SQLStringArray(List<String> values) {
        this.values = values;
    }

    @Override
    public void insertInto(SQLConnection conn, PreparedStatement prep, int index) throws SQLException {
        Array array = conn.createArrayOf("varchar", values.toArray());
        prep.setArray(index, array);
    }
}
