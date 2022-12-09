package com.adobe.franklin.fragments.tables;

import java.util.ArrayList;
import java.util.List;

public class FragmentReference {

    public static String TABLE_NAME = "fragmentRefs";
    
    private final Fragment parent, child;
    
    public FragmentReference(Fragment parent, Fragment child) {
        this.parent = parent;
        this.child = child;
    }
    
    public static String toDropSQL() {
        return "drop table if exists " + TABLE_NAME + " cascade";
    }
    
    public static List<String> toCreateSQL() {
        ArrayList<String> result = new ArrayList<>();
        StringBuilder buff = new StringBuilder();
        buff.append("create table " + TABLE_NAME + "(\n");
        buff.append("    parent bigint,\n");
        buff.append("    child bigint\n");
        buff.append(")");
        result.add(buff.toString());
        result.add("create index " + TABLE_NAME + "_parent_child on " + 
                TABLE_NAME + "(parent, child)");
        result.add("create index " + TABLE_NAME + "_child_parent on " + 
                TABLE_NAME + "(child, parent)");
        return result;
    }
    
    public String toInsertSQL() {
        return "insert into " + TABLE_NAME + "(parent, child) values(" +
                parent.getId() + ", " + child.getId() + ")";
    }

}
