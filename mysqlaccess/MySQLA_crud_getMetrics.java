package com.company.mysqlaccess;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLA_crud_getMetrics {

    public static Number getMetrics(Connection conn, String database, String table, String column,
                                    String operation, String sqlWhereFilter, OnComplete<Number> callback) {

        Object returnData = null;

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("Unable to execute 'GETMETRICS' command because there is no connection to '" +
                    database + "' database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> If no table is selected, abort.
        if (!MySQLA_validators.isTableSelected(table)) {
            MySQLA_loggers.logError("No table selected on database '" + database + "'. Use setTable(..tablename..) " +
                    "before executing mysqlaccess commands.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Get table properties if not already present.
        MySQLA_tableProperties.updateTableProperties(database, conn, table);

        // ---> If unable to fetch table details, abort
        if (!MySQLA_validators.hasFetchedTableDetails(database, table)) {
            MySQLA_loggers.logError("Could not fetch table details from database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> If no column has been passed, use primary key as default
        if (column == null) {
            column = MySQLA_tableProperties.getPrimaryKey(database, table);
        }

        // ---> If column is non-existent, abort.
        if (!MySQLA_tableProperties.getAllColumns(database, table).contains(column)) {
            MySQLA_loggers.logError("GETMETRICS - Column '" + column + "' on table '" + table + "' @ '"
                    + database + "' could not be found.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Build query
        String query = "select " + operation + "(" + column + ") from " + table + " " +
                (sqlWhereFilter == null ? "" : ("where " + sqlWhereFilter));

        // ---> Retrieve data from cache if caching is enabled and available
        returnData = MySQLA_cache.isNumberCacheAvailable(database, table, query);
        if (returnData != null) {
            logResult(table, column, operation, sqlWhereFilter, returnData);
            if (callback != null) callback.onSuccess((Number) returnData);
            return (Number) returnData;
        }

        // ---> Execute query on connection
        ResultSet resultSet = null;
        try {
            Statement st = conn.createStatement();
            resultSet = st.executeQuery(query);
            resultSet.next();
            returnData = resultSet.getObject(1);
        } catch (SQLException e) {
            MySQLA_loggers.logError("Unable to create connection statement.");
            if (callback != null) callback.onFailure();
            e.printStackTrace();
            return null;
        }

        logResult(table, column, operation, sqlWhereFilter, returnData);

        // ---> Store result to cache if available and return.
        MySQLA_cache.storeToCache(database, table, query, returnData);
        if (callback != null) callback.onSuccess((Number) returnData);
        return (Number) returnData;
    }

    public static void getMetricsParallel(Connection conn, String database, String table, String column,
                                    String operation, String sqlWhereFilter, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            getMetrics(conn, database, table, column, operation, sqlWhereFilter, callback);
        });
        t.start();
    }

    private static void logResult(String table, String column, String operation, String sqlWhereFilter,
                                  Object returnData) {

        if (returnData == null) returnData = "null";

        if (sqlWhereFilter == null) {
            MySQLA_loggers.logDetails("GETMETRICS - " + operation + " (Table: '" + table + "', Column: '" + column +
                    "') operation returned value: " + returnData + " | " + returnData.getClass().getName());
        } else {
            MySQLA_loggers.logDetails("GETMETRICS - " + operation + " (Table: '" + table + "', Column: '" + column
                    + "' / Filter: '" + sqlWhereFilter +
                    "') operation returned value: " + returnData + " | " + returnData.getClass().getName());
        }
    }
}
