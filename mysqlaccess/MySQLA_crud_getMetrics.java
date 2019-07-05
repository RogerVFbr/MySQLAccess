package com.company.mysqlaccess;

import com.company.mysqlaccess.models.MySQLAConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLA_crud_getMetrics {
    public static Number getMetrics(MySQLAConfig config, Connection conn, String selectedTable, String column,
                                    String operation, String sqlWhereFilter, OnComplete<Number> callback) {

        String table = selectedTable;
        Object returnData = null;

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("Unable to execute 'GETMETRICS' command because there is no connection to '" +
                    config.database + "' database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> If no table is selected, abort.
        if (!MySQLA_validators.isTableSelected(table, config)) {
            MySQLA_loggers.logError("No table selected on database '" + config.database + "'. Use setTable(..tablename..) " +
                    "before executing mysqlaccess commands.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Get table properties if not already present.
        MySQLA_tableProperties.updateTableProperties(config, conn, table);

        // ---> If unable to fetch table details, abort
        if (!MySQLA_validators.hasFetchedTableDetails(config.database, table,
                MySQLA_tableProperties.getAllColumnNames())) {
            MySQLA_loggers.logError("Could not fetch table details from database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> If no column has been passed, use primary key as default
        if (column == null) {
            column = MySQLA_tableProperties.getPrimaryKeys().get(config.database).get(table);
        }

        // ---> If column is non-existent, abort.
        if (!MySQLA_tableProperties.getAllColumnNames().get(config.database).get(table).contains(column)) {
            MySQLA_loggers.logError("GETMETRICS - Column '" + column + "' on table '" + table + "' @ '"
                    + config.database + "' could not be found.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Build query
        String query = "select " + operation + "(" + column + ") from " + table + " " +
                (sqlWhereFilter == null ? "" : ("where " + sqlWhereFilter));

        // ---> Retrieve data from cache if caching is enabled and available
        returnData = MySQLA_cache.isNumberCacheAvailable(config.database, table, query);
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
        MySQLA_cache.storeToCache(config.database, table, query, returnData);
        if (callback != null) callback.onSuccess((Number) returnData);
        return (Number) returnData;
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
