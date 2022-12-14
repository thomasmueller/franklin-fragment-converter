package com.adobe.franklin.fragments.tables;

import com.adobe.franklin.fragments.converter.sql.DatabaseType;
import com.adobe.franklin.fragments.converter.sql.PreparedSQLStatement;
import com.adobe.franklin.fragments.converter.sql.SQLArgument;
import com.adobe.franklin.fragments.converter.sql.SQLValue;
import com.adobe.franklin.fragments.converter.sql.SimpleSQLStatement;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class FragmentReference {

    public static String TABLE_NAME = "fragmentRefs";
    private final static String INSERT_STATEMENT = 
            "insert into " + TABLE_NAME + "(parent, child) values(?, ?)";
    
    private final Fragment parent, child;
    
    public FragmentReference(Fragment parent, Fragment child) {
        this.parent = parent;
        this.child = child;
    }
    
    public static SimpleSQLStatement toDropSQL(DatabaseType dbType) {
        return new SimpleSQLStatement("drop table if exists " + TABLE_NAME + " " + dbType.getCascade());
    }
    
    public static List<SimpleSQLStatement> toCreateSQL() {
        ArrayList<SimpleSQLStatement> result = new ArrayList<>();
        result.add(new SimpleSQLStatement( "create table " + TABLE_NAME +
                "(\n" + "    parent bigint,\n" + "    child bigint\n" + ")"));
        result.add(new SimpleSQLStatement(
                "create index " + TABLE_NAME + "_parent_child on " +
                TABLE_NAME + "(parent, child)"));
        result.add(new SimpleSQLStatement("create index " + TABLE_NAME + "_child_parent on " +
                TABLE_NAME + "(child, parent)"));
        return result;
    }
    
    public PreparedSQLStatement toInsertSQL() {
        List<SQLArgument> arguments = List.of(
                new SQLValue(Types.BIGINT, parent.getId()),
                new SQLValue(Types.BIGINT, child.getId()));
        return new PreparedSQLStatement(INSERT_STATEMENT, arguments);
    }
}
