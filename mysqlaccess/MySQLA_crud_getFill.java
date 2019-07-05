package com.company.mysqlaccess;

import com.company.mysqlaccess.models.MySQLAConfig;

import java.sql.*;
import java.util.*;

public class MySQLA_crud_getFill {
    public static <T> List<T> getFillMain(Class<T> type, MySQLAConfig config, Connection conn, String table,
                                          String[] IdColumns, String[] tablesToJoin, String sqlWhereFilter,
                                          OnGetComplete<T> callback) {

        List<T> returnData = new ArrayList<>();

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("Unable to execute 'GETFILL' command because there is no connection to '" +
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

        // ---> Tables to join contains main table, abort.
        List<String> tablesToJoinList = Arrays.asList(tablesToJoin);
        if (tablesToJoinList.contains(table)) {
            MySQLA_loggers.logError("GETFILL - Selected table must not be specified to be joined.");
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> Tables to join contains duplicated table, abort.
        if (tablesToJoinList.size() != new HashSet<>(tablesToJoinList).size()) {
            MySQLA_loggers.logError("GETFILL - Tables to join must not contain duplicate table names.");
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> Number of IdColumns and table names are not equal, abort.
        if (IdColumns.length != tablesToJoin.length) {
            MySQLA_loggers.logError("GETFILL - Method call must provide the same number of Id columns and table names.");
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> Get table properties if not already present.
        MySQLA_tableProperties.updateTableProperties(config, conn, table);
        for (String tableToJoinName : tablesToJoin) {
            MySQLA_tableProperties.updateTableProperties(config, conn, tableToJoinName);
        }

        // ---> If unable to fetch details for one or more tables, abort.
        if (!MySQLA_validators.hasFetchedTableDetails(config.database, table,
                MySQLA_tableProperties.getAllColumnNames())) {
            MySQLA_loggers.logError("Could not fetch table details from database.");
            if (callback != null) callback.onFailure();
            return returnData;
        }
        for (String tableToJoinName : tablesToJoin) {
            if (!MySQLA_validators.hasFetchedTableDetails(config.database, tableToJoinName,
                    MySQLA_tableProperties.getAllColumnNames())) {
                MySQLA_loggers.logError("Could not fetch table details from database.");
                if (callback != null) callback.onFailure();
                return returnData;
            }
        }

        // ---> Calculate column/field correlation from joined column names
        List<String> allColumns = new ArrayList<>(
                MySQLA_tableProperties.getAllColumnNames().get(config.database).get(table));
        for (String tableName : tablesToJoin) {
            allColumns.addAll(MySQLA_tableProperties.getAllColumnNames().get(config.database).get(tableName));
        }
        Map<String, String> propertyMap = MySQLA_correlations.getColumnFieldCorrelation(type,
                allColumns, config.database, table);

        // ---> Build SQL query
        String from = " from " + table + " ";
        String join = "";
        for (int x = 0; x<tablesToJoin.length; x++) {
            String tableToJoinPrimaryKey =
                    MySQLA_tableProperties.getPrimaryKeys().get(config.database).get(tablesToJoin[x]);
            join += "left join " + tablesToJoin[x] + " on " + table + "." + IdColumns[x] + "=" + tablesToJoin[x] + "." +
                    tableToJoinPrimaryKey + " ";
        }

        String where = "where " + sqlWhereFilter + " ";
        String query = "select ";

        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            if (MySQLA_tableProperties.getAllColumnNames()
                    .get(config.database).get(table).contains(entry.getKey())) {
                query += table + "." + entry.getKey() + ", ";
            }
            else {
                for (String tableName : tablesToJoin) {
                    if (MySQLA_tableProperties.getAllColumnNames()
                            .get(config.database).get(tableName).contains(entry.getKey())) {
                        query += tableName + "." + entry.getKey() + ", ";
                        break;
                    }
                }
            }
        }

        query = query.replaceAll(", $", "");
        query += from + join + (sqlWhereFilter == null ? "" : where);

        // ---> Retrieve data from cache if caching is enabled and available
        List<T> cacheData = null;
        cacheData = MySQLA_cache.isCacheAvailable(config.database, table, query);
        if (cacheData != null) {
            for (T t : cacheData) MySQLA_loggers.logDetails(t.toString());
            if (callback != null) callback.onSuccess(cacheData);
            return cacheData;
        };

        // ---> Execute query on connection
        ResultSet resultSet = null;
        try {
            Statement st = conn.createStatement();
            MySQLA_loggers.logInfo("Executing getFill query at '" + table +"': " + query);
            resultSet = st.executeQuery(query);

        } catch (SQLException e) {
            MySQLA_loggers.logError("Unable to create connection statement.");
            if (callback != null) callback.onFailure();
            e.printStackTrace();
            return returnData;
        }

        // ---> Build list from retrieved data
        if (!MySQLA_listBuilder.buildListFromRetrievedData(type, resultSet, returnData, propertyMap)) {
            if (callback != null) callback.onFailure();
            return returnData;
        };

        // ---> Store result to cache if available and return.
        MySQLA_cache.storeToCache(config.database, table, query, returnData);
        if (callback != null) callback.onSuccess(returnData);
        return returnData;
    }
}
