package com.adobe.franklin.fragments.tables;

import com.adobe.franklin.fragments.converter.sql.DatabaseType;
import com.adobe.franklin.fragments.converter.sql.PreparedSQLStatement;
import com.adobe.franklin.fragments.converter.sql.SQLArgument;
import com.adobe.franklin.fragments.converter.sql.SQLValue;
import com.adobe.franklin.fragments.converter.sql.SimpleSQLStatement;

import java.sql.Types;
import java.util.List;

public class Values {
    public static String TABLE_NAME = "val";
    private final static String INSERT_STATEMENT = "insert into " + TABLE_NAME + "(id, val) values(?, ?)";
    private static long nextId = 0;

    private final long id;
    private final String value;

    public Values(long id, String value) {
        this.id = id;
        this.value = value;
    }

    public static long newId() {
        return nextId++;
    }

    public static SimpleSQLStatement toDropSQL(DatabaseType dbType) {
        return new SimpleSQLStatement("drop table if exists " + TABLE_NAME + " " + dbType.getCascade());
    }

    public static SimpleSQLStatement toCreateSQL() {
        String sql =  "create table " + TABLE_NAME + "(\n"
                + "    id bigint,\n"
                + "    val text\n"
                + ")";
        return new SimpleSQLStatement(sql);
    }

    public PreparedSQLStatement toInsertSQL() {
        List<SQLArgument> arguments = List.of(
                new SQLValue(Types.BIGINT, id),
                new SQLValue(Types.VARCHAR, value));
        return new PreparedSQLStatement(INSERT_STATEMENT, arguments);
    }
}
