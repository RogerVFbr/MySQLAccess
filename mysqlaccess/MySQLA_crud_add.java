package com.company.mysqlaccess;

import java.sql.*;
import java.util.*;

public class MySQLA_crud_add {

    public static <T> Object addMain (T element, Connection conn, String database, String table,
                                    OnComplete<Object> callback) {

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("ADD - Unable to execute command because there is no connection to '" +
                    database + "' database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> If no table is selected, abort.
        if (!MySQLA_validators.isTableSelected(table)) {
            MySQLA_loggers.logError("ADD - No table selected on database '" + database
                    + "'. Use setTable(..tablename..) " + "before executing mysqlaccess commands.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Get table properties of not already present
        MySQLA_tableProperties.updateTableProperties(database, conn, table);

        // ---> If unable to fetch table details, abort
        if (!MySQLA_validators.hasFetchedTableDetails(database, table)) {
            MySQLA_loggers.logError("ADD - Could not fetch table details from database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Build SQL query
        String query = buildSQLQuery(element, database, table);

        // ---> Execute query on connection
        return executeQueryOnConnection(conn, database, table, query, callback);
    }

    public static <T> void addMainParallel (T element, Connection conn, String database, String table,
                                      OnComplete<Object> callback) {

        Thread t = new Thread( () -> {
            addMain(element, conn, database, table, callback);
        });
        t.start();
    }

    private static <T> String buildSQLQuery (T element, String database, String table) {

        // ---> Figure out best correlation between database column names and model fields
        Map<String, String> propertyMap = MySQLA_correlations.getColumnFieldCorrelation(
                element.getClass(),
                MySQLA_tableProperties.getUpdateableColumns(database, table),
                database,
                table
        );

        // ---> Build SQL query
        String query = "";
        String columns = "";
        String values = "";

        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            String columnType = MySQLA_tableProperties.getColumnType(database, table, entry.getKey());
            columns += entry.getKey() + ", ";
            try { values += MySQLA_orm.getSQLString(element, entry.getValue(), columnType) + ", "; }
            catch (Exception e) { e.printStackTrace(); }
        }

        columns = columns.replaceAll(", $", "");
        values = values.replaceAll(", $", "");
        query = "insert into " + table + " (" + columns + ") values (" + values + ")";
        return query;
    }

    private static Object executeQueryOnConnection (Connection conn, String database, String table,
                                                     String query, OnComplete<Object> callback) {
        try {
            MySQLA_loggers.logInfo("ADD - Executing query at '" + table +"': " + query);
            PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            int result = ps.executeUpdate();
            if (result == 0) {
                MySQLA_loggers.logError("ADD - Failed to obtain insertion confirmation on table '" + table
                        + "'!");
                if (callback != null) callback.onFailure();
                return null;
            }
            Object key = null;
            try (ResultSet generatedObject = ps.getGeneratedKeys();) {
                if (generatedObject.next()) {
                    key = generatedObject.getObject(1);
                }
            }
            if (key == null){
                MySQLA_loggers.logInfo("ADD - Successfully written new row to table '" + table + ".");
            }
            else {
                MySQLA_loggers.logInfo("ADD - Successfully written new row to table '" + table
                        + "' with auto-generated key: " + key);
            }

            MySQLA_cache.deleteCache(database, table);
            if (callback != null) callback.onSuccess(key);
            return key;

        } catch (SQLException e) {
            MySQLA_loggers.logError("ADD - Failed to create new row on table '" + table + "'. Query: '"
                    + query + "'");
            System.err.println(e.getMessage());
            if (callback != null) callback.onFailure();
            return null;
        }
    }
}
