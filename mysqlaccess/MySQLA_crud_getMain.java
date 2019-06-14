package com.company.mysqlaccess;

import com.company.mysqlaccess.models.Config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MySQLA_crud_getMain {

    public static <T> List<T> getMain(Class<T> type, Config config, Connection conn, String table,
                                      String sqlWhereFilter, OnGetComplete<T> callback) {

        List<T> returnData = new ArrayList<>();

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("Unable to execute 'GET' command because there is no connection to '" +
                    config.database + "' database.");
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> If no table is selected, abort.
        if (!MySQLA_validators.isTableSelected(table, config)) {
            MySQLA_loggers.logError("No table selected on database '" + config.database + "'. Use setTable(..tablename..) " +
                    "before executing mysqlaccess commands.");
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> Get table properties if not already present.
        MySQLA_tableProperties.updateTableProperties(config, conn, table);

        // ---> If table properties could not be fetched, abort
        if (!MySQLA_validators.hasFetchedTableDetails(config.database, table,
                MySQLA_tableProperties.getAllColumnNames())) {
            MySQLA_loggers.logError("Could not fetch table details from database.");
            if (callback != null) callback.onFailure();
            return returnData;
        }

        Map<String, String> propertyMap = MySQLA_correlations.getColumnFieldCorrelation(
                type,
                MySQLA_tableProperties.getAllColumnNames().get(config.database).get(table),
                config.database,
                table);

        // ---> Build SQL query
        List<String> columns = new ArrayList<>(propertyMap.keySet());
        String columnNames = columns == null || columns.size() == 0 ? "*" : String.join(", ", columns);
        String query = "select " + columnNames + " from " + table;
        if (sqlWhereFilter != null) query += " where " + sqlWhereFilter;


        // ---> Execute query on connection
        ResultSet resultSet = null;
        try {
            Statement st = conn.createStatement();
            MySQLA_loggers.logInfo("Executing get query at '" + table +"': " + query);
            resultSet = st.executeQuery(query);

        } catch (SQLException e) {
            MySQLA_loggers.logError("Unable to create connection statement.");
            if (callback != null) callback.onFailure();
            e.printStackTrace();
            return returnData;
        }

        if (!MySQLA_listBuilder.buildListFromRetrievedData(type, resultSet, returnData, propertyMap)) {
            if (callback != null) callback.onFailure();
            return returnData;
        };

        if (callback != null) callback.onSuccess(returnData);
        return returnData;
    }
}