package com.adobe.franklin.fragments.converter.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface SQLArgument {
    void insertInto(SQLConnection connection, PreparedStatement statement, int index) throws SQLException;
}
