package com.adobe.franklin.fragments.converter.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface SQLArgument {
    void insertInto(Connection connection, PreparedStatement statement, int index) throws SQLException;
}
