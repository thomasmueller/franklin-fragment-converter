package com.adobe.franklin.fragments.tables;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;

import com.adobe.franklin.fragments.converter.sql.PreparedSQLStatement;
import com.adobe.franklin.fragments.converter.sql.SQLArgument;
import com.adobe.franklin.fragments.converter.sql.SQLValue;
import com.adobe.franklin.fragments.converter.sql.SimpleSQLStatement;

public class Fragment {

    public static String TABLE_NAME = "fragments";
    private final static String INSERT_STATEMENT = 
            "insert into " + TABLE_NAME + "(id, path, model) values(?, ?, ?)";
    
    private final long id;
    private final String path;
    private final String model;
    
    
    public Fragment(long id, String path, String model) {
        this.id = id;
        this.path = path;
        this.model = model;
    }
    
    public static SimpleSQLStatement toDropSQL() {
        return new SimpleSQLStatement("drop table if exists " + TABLE_NAME + " cascade");
    }
    
    public static SimpleSQLStatement toCreateSQL() {
        String sql = "create table " + TABLE_NAME + "(\n" +
                "    id bigint primary key,\n" +
                "    path varchar(8000),\n" +
                "    model varchar(8000)\n" +
                ")";
        return new SimpleSQLStatement(sql);
    }

    public PreparedSQLStatement toInsertSQL() {
        List<SQLArgument> arguments = List.of(
                new SQLValue(Types.BIGINT, id),
                new SQLValue(Types.VARCHAR, path),
                new SQLValue(Types.VARCHAR, model)
        );
        return new PreparedSQLStatement(INSERT_STATEMENT, arguments);
    }
    
    public FragmentReference createReferenceIfPossible(HashMap<String, Fragment> fragmentMap, String target) {
        Fragment targetFragment = fragmentMap.get(target);
        return targetFragment == null ? null : new FragmentReference(this, targetFragment);
    }

    public long getId() {
        return id;
    }

}
