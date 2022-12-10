package com.adobe.franklin.fragments.converter.sql;

import java.sql.SQLException;

public interface SQLStatement {

    void batch(SQLConnection conn) throws SQLException;
    
}

