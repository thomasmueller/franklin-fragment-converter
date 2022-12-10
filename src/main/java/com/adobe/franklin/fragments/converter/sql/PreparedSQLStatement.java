package com.adobe.franklin.fragments.converter.sql;

import java.sql.SQLException;
import java.util.List;

import com.adobe.franklin.fragments.converter.sql.SQLConnection.Prepared;

public class PreparedSQLStatement implements SQLStatement {
    
    private final String sql;
    private final List<SQLArgument> arguments;

    public PreparedSQLStatement(String template, List<SQLArgument> arguments) {
        this.sql = template;
        this.arguments = arguments;
    }
    
    @Override
    public String toString() {
        return sql;
    }
    
    @Override
    public void batch(SQLConnection conn) throws SQLException {
        try {
            Prepared prep = conn.prepareStatement(sql);
            for (int i = 0; i < arguments.size(); i++) {
                arguments.get(i).insertInto(conn, prep.getPreparedStatement(), i + 1);
            }
            prep.addBatch();
        } catch (SQLException e) {
            throw new SQLException(sql, e);
        }
    }

}
