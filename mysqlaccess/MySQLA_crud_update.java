package com.company.mysqlaccess;

import java.sql.*;
import java.util.List;
import java.util.Map;

public class MySQLA_crud_update {

    public static <T> Integer updateMain (T element, Connection conn, String database, String table,
                                       OnComplete<Integer> callback) {

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("UPDATE - Unable to execute command because there is no connection to '" +
                    database + "' database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> If no table is selected, abort
        if (!MySQLA_validators.isTableSelected(table)) {
            MySQLA_loggers.logError("UPDATE - No table selected on database '" + database
                    + "'. Use setTable(..tablename..) " + "before executing mysqlaccess commands.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Get table properties of not already present
        MySQLA_tableProperties.updateTableProperties(database, conn, table);

        // ---> If unable to fetch table details, abort
        if (!MySQLA_validators.hasFetchedTableDetails(database, table)) {
            MySQLA_loggers.logError("UPDATE - Could not fetch table details from database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Build update query
        String query = buildSQLQuery(database, table, element);

        // ---> Execute query on connection
        return executeQueryOnConnection(conn, database, table, query, callback);
    }

    public static <T> void updateMainParallel (T element, Connection conn, String database, String table,
                                          OnComplete<Integer> callback) {
        Thread t = new Thread( () -> {
            updateMain(element, conn, database, table, callback);
        });
        t.start();
    }

    private static <T> String buildSQLQuery(String database, String table, T element) {

        // ---> Use updatable columns + primary key
        String primaryKey = MySQLA_tableProperties.getPrimaryKey(database, table);
        List<String> relevantColumns = MySQLA_tableProperties.getUpdateableColumns(database, table);
        if (!relevantColumns.contains(primaryKey)) relevantColumns.add(primaryKey);

        // ---> Figure out best correlation between database column names and model fields
        Map<String, String> propertyMap = MySQLA_correlations.getColumnFieldCorrelation(
                element.getClass(),
                relevantColumns,
                database,
                table);

        // ---> Build update query
        String primaryFieldData = "";
        String query = "update " + table + " ";
        String set = "set ";

        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            String columnType = MySQLA_tableProperties.getColumnType(database, table, entry.getKey());
            boolean isPrimaryKey = entry.getKey().equals(primaryKey);
            if (!isPrimaryKey) set += entry.getKey() + " = ";
            String encode = null;
            try { encode = MySQLA_orm.getSQLString(element, entry.getValue(), columnType); }
            catch (Exception e) { e.printStackTrace(); }
            if (isPrimaryKey) primaryFieldData += encode;
            else set += encode + ", ";
        }

        String where = "where " + primaryKey + " = " + primaryFieldData;
        set = set.replaceAll(", $", "") + " ";
        query += set + where;
        return query;
    }

    private static Integer executeQueryOnConnection (Connection conn, String database, String table,
                                                     String query, OnComplete<Integer> callback) {
        // ---> Execute query on connection
        try {

            Statement st = conn.createStatement();
            MySQLA_loggers.logInfo("UPDATE - Executing query at '" + table +"': " + query);
            int result = st.executeUpdate(query);

            if (result == 0) {
                MySQLA_loggers.logError("UPDATE - Failed to update row(s) on table '" + table + "'!");
                if (callback != null) callback.onFailure();
                return null;
            }

            MySQLA_loggers.logInfo("UPDATE - Successfully updated " + result + " row(s) on table '" + table + "'.");
            MySQLA_cache.deleteCache(database, table);
            if (callback != null) callback.onSuccess(result);
            return result;

        } catch (SQLException e) {
            MySQLA_loggers.logError("UPDATE - Failed to update row on table '" + table + "'.");
            if (callback != null) callback.onFailure();
            e.printStackTrace();
            return null;
        }
    }
}
