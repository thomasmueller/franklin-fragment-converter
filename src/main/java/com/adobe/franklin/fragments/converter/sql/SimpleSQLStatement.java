package com.adobe.franklin.fragments.converter.sql;

import java.sql.SQLException;

public class SimpleSQLStatement implements SQLStatement {

    private final String sql;

    public SimpleSQLStatement(String sql) {
        this.sql = sql;
    }

    @Override
    public String toString() {
        return sql;
    }
    
    @Override
    public void batch(SQLConnection conn) throws SQLException {
        conn.addBatch(sql);
    }
    
}
