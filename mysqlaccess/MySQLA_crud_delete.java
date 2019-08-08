package com.company.mysqlaccess;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLA_crud_delete {

    public static Integer deleteMain (Connection conn, String database, String table, String sqlWhere,
                                      OnComplete<Integer> callback) {

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("DELETE - Unable to execute command because there is no connection to '" +
                    database + "' database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> If no table is selected, abort.
        if (!MySQLA_validators.isTableSelected(table)) {
            MySQLA_loggers.logError("DELETE - No table selected on database '" + database
                    + "'. Use setTable(..tablename..) " + "before executing mysqlaccess commands.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Get table properties of not already present
        MySQLA_tableProperties.updateTableProperties(database, conn, table);

        // ---> If unable to fetch table details, abort
        if (!MySQLA_validators.hasFetchedTableDetails(database, table)) {
            MySQLA_loggers.logError("DELETE - Could not fetch table details from database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Build SQL query
        String query = "delete from " + table + " where " + sqlWhere;

        // ---> Execute query on connection
        return executeQueryOnConnection(conn, database, table, query, callback);
    }

    public static void deleteMainParallel (Connection conn, String database, String table, String sqlWhere,
                                      OnComplete<Integer> callback) {
        Thread t = new Thread( () -> {
            deleteMain(conn, database, table, sqlWhere, callback);
        });
        t.start();
    }

    private static Integer executeQueryOnConnection (Connection conn, String database, String table,
                                                    String query, OnComplete<Integer> callback) {
        // ---> Execute query on connection
        try {
            Statement st = conn.createStatement();
            MySQLA_loggers.logInfo("DELETE - Executing query at '" + table +"': " + query);
            int result = st.executeUpdate(query);

            if (result == 0) {
                MySQLA_loggers.logInfo("DELETE - Query didn't affect any rows.");
                if (callback != null) callback.onFailure();
                return null;
            }

            MySQLA_loggers.logInfo("DELETE - Successfully deleted " + result + " row(s).");
            MySQLA_cache.deleteCache(database, table);
            if (callback != null) callback.onSuccess(result);
            return result;

        } catch (SQLException e) {
            MySQLA_loggers.logError("DELETE - Failed to delete rows on database.");
            if (callback != null) callback.onFailure();
            e.printStackTrace();
            return null;
        }
    }
}
