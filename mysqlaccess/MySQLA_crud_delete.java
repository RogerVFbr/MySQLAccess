package com.company.mysqlaccess;

import com.company.mysqlaccess.models.Config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLA_crud_delete {
    public static void deleteMain (String sqlWhere, Config config, Connection conn, String selectedTable,
                                   OnComplete<Integer> callback) {

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("Unable to execute 'DELETE' command because there is no connection to '" +
                    config.database + "' database.");
            if (callback != null) callback.onFailure();
            return;
        }

        // ---> If no table is selected, abort.
        String table = selectedTable;
        if (!MySQLA_validators.isTableSelected(table, config)) {
            MySQLA_loggers.logError("No table selected on database '" + config.database + "'. Use setTable(..tablename..) " +
                    "before executing mysqlaccess commands.");
            if (callback != null) callback.onFailure();
            return;
        }

        // ---> Build SQL query
        String query = "delete from " + table + " where " + sqlWhere;

        // ---> Execute query on connection
        try {
            Statement st = conn.createStatement();
            MySQLA_loggers.logInfo("Executing delete query at '" + table +"': " + query);
            int result = st.executeUpdate(query);

            if (result == 0) {
                MySQLA_loggers.logInfo("Delete query didn't affect any rows.");
                if (callback != null) callback.onFailure();
                return;
            }

            MySQLA_loggers.logInfo("Successfully deleted " + result + " row(s).");
            if (callback != null) callback.onSuccess(result);

        } catch (SQLException e) {
            MySQLA_loggers.logError("Failed to delete rows on database.");
            if (callback != null) callback.onFailure();
            e.printStackTrace();
        }
    }
}
