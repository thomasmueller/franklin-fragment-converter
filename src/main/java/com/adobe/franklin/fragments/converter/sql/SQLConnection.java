package com.adobe.franklin.fragments.converter.sql;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.adobe.franklin.fragments.utils.ProgressLogger;

public class SQLConnection {
    
    private final Connection conn;
    private final int maxBatchCount;
    
    private Statement stat;
    private ArrayList<String> batchedStatements = new ArrayList<>();
    private int batchCount;
    
    private Map<String, Prepared> prepared = new LinkedHashMap<>();

    SQLConnection(Connection conn, int maxBatchCount) {
        this.conn = conn;
        this.maxBatchCount = maxBatchCount;
    }
    
    void flush() throws SQLException {
        flushStatements();
        for (Prepared prep : prepared.values()) {
            prep.flush();
        }
    }
    
    void flushStatements() throws SQLException {
        if (stat != null && batchCount > 0) {
            int[] updateCounts = stat.executeBatch();
            for (int i = 0; i < updateCounts.length; i++) {
                if (updateCounts[i] == Statement.EXECUTE_FAILED) {
                    String sql = batchedStatements.get(i);
                    ProgressLogger.logMessage("Failed to execute " + i + ": " + sql);
                }
            }
            batchCount = 0;
            batchedStatements.clear();
        }
    }
    
    public void addBatch(String sql) throws SQLException {
        if (stat == null) {
            stat = conn.createStatement();
        }
        stat.addBatch(sql);
        batchedStatements.add(sql);
        if (batchCount++ > maxBatchCount) {
            flush();
        }
    }
    
    public Prepared prepareStatement(String sql) throws SQLException {
        Prepared prep = prepared.get(sql);
        if (prep == null) {
            prep = new Prepared(this, sql, maxBatchCount, conn.prepareStatement(sql));
            prepared.put(sql, prep);
        }
        return prep;
    }

    public Array createArrayOf(String string, Object[] array) throws SQLException {
        return conn.createArrayOf(string, array);
    }
    
    public static class Prepared {
        private final SQLConnection conn;
        private final String sql;
        private final PreparedStatement prep;
        private final int maxBatchCount;
        private int batchCount;
        
        Prepared(SQLConnection conn, String sql, int maxBatchCount, PreparedStatement prep) {
            this.conn = conn;
            this.sql = sql;
            this.maxBatchCount = maxBatchCount;
            this.prep = prep;
        }
        
        void addBatch() throws SQLException {
            prep.addBatch();
            if (batchCount++ > maxBatchCount) {
                // flush statements first, 
                // as there could be a "create table" statement
                conn.flushStatements();
                flush();
            }
        }
        
        void flush() throws SQLException {
            int[] updateCounts = prep.executeBatch();
            for (int i = 0; i < updateCounts.length; i++) {
                if (updateCounts[i] == Statement.EXECUTE_FAILED) {
                    ProgressLogger.logMessage("Failed to execute " + i + ": " + sql);
                }
            }
            batchCount = 0;
        }

        public PreparedStatement getPreparedStatement() {
            return prep;
        }
    }

}
