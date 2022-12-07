package com.adobe.franklin.fragments.tables;

import java.util.HashMap;

import com.adobe.franklin.fragments.converter.SQLUtils;

public class Fragment {

    public static String TABLE_NAME = "fragments";
    
    private final long id;
    private final String path;
    private final String model;
    
    public Fragment(long id, String path, String model) {
        this.id = id;
        this.path = path;
        this.model = model;
    }
    
    public static String toDropSQL() {
        return "drop table if exists " + TABLE_NAME;
    }
    
    public static String toCreateSQL() {
        StringBuilder buff = new StringBuilder();
        buff.append("create table " + TABLE_NAME + "(\n");
        buff.append("    id bigint primary key,\n");
        buff.append("    path varchar(8000),\n");
        buff.append("    model varchar(8000)\n");
        buff.append(")");
        return buff.toString();
    }

    public String toInsertSQL() {
        return "insert into " + TABLE_NAME + "(id, path, model) values(" +
                id + ", " + SQLUtils.convertToSQLString(path) +
                ", " + SQLUtils.convertToSQLString(model) + ");\n";
    }
    
    public FragmentReference createReferenceIfPossible(HashMap<String, Fragment> fragmentMap, String target) {
        Fragment targetFragment = fragmentMap.get(target);
        return targetFragment == null ? null : new FragmentReference(this, targetFragment);
    }

    public long getId() {
        return id;
    }

}
