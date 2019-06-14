package com.company.mysqlaccess;

import com.company.mysqlaccess.models.Config;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class MySQLA_tableProperties {
    private static Map<String, Map<String, List<ColumnProps>>> tableProperties = new HashMap<>();
    private static Map<String, Map<String, List<String>>> updateableColumns = new HashMap<>();
    private static Map<String, Map<String, List<String>>> allColumnNames = new HashMap<>();
    private static Map<String, Map<String, Map<String, String>>> allColumnTypes = new HashMap<>();
    private static Map<String, Map<String, String>> primaryKeys = new HashMap<>();

    public static Map<String, Map<String, List<String>>> getUpdateableColumns() {
        return updateableColumns;
    }

    public static Map<String, Map<String, List<String>>> getAllColumnNames() {
        return allColumnNames;
    }

    public static Map<String, Map<String, Map<String, String>>> getAllColumnTypes() {
        return allColumnTypes;
    }

    public static Map<String, Map<String, String>> getPrimaryKeys() {
        return primaryKeys;
    }

    public static void updateTableProperties(Config config, Connection conn, String table) {
        updateAllTablesProperties(config, conn, table);
        updateSingleTableProperties(config, conn, table);
    }

    private static void updateAllTablesProperties(Config config, Connection conn, String tableName) {

        // ---> If information has been fetched before, abort execution.
        if (tableProperties.containsKey(config.database)) {
            if (tableProperties.get(config.database).containsKey(tableName)) return;
        }

        // ---> Build query and prepare receiver list
        String query = "select * from information_schema.columns where table_schema = '" + config.database + "'";
        List<ColumnProps> currentTableProps = new ArrayList<>();
        Map<String, List<ColumnProps>> tablesDetails = new HashMap<>();
        String currentTableName = null;

        // ---> Execute query on database
        try {
            MySQLA_loggers.logFetch("Fetching details for all tables @ database '" + config.database + "'...");
            Statement st = conn.createStatement();
            ResultSet resultSet = st.executeQuery(query);

            // ---> Digest data
            while(resultSet.next()) {
                ColumnProps column = new ColumnProps(
                        resultSet.getString("COLUMN_NAME"),
                        resultSet.getString("DATA_TYPE"),
                        resultSet.getString("COLLATION_NAME"),
                        resultSet.getString("IS_NULLABLE"),
                        resultSet.getString("COLUMN_KEY"),
                        resultSet.getString("COLUMN_DEFAULT"),
                        resultSet.getString("EXTRA"),
                        resultSet.getString("PRIVILEGES")
                );
                currentTableName = resultSet.getString("TABLE_NAME");
                if (!tablesDetails.containsKey(currentTableName)) {
                    currentTableProps.clear();
                    currentTableProps.add(column);
                    tablesDetails.put(currentTableName, new ArrayList<>(currentTableProps));
                }
                else {
                    currentTableProps.add(column);
                    tablesDetails.replace(currentTableName, new ArrayList<>(currentTableProps));
                }
            }

            for (Map.Entry<String, List<ColumnProps>> entry : tablesDetails.entrySet()) {
                String table = entry.getKey();
                List<ColumnProps> newTableProps = entry.getValue();
                MySQLA_loggers.logFetch("Details for table '" + table + "' ...");
                saveFetchedTablesDetails(config, table, newTableProps);
            }

        } catch (SQLException e) {
            MySQLA_loggers.logError("Unable to retrieve details from database '" + config.database + "'.");
            e.printStackTrace();
        }
    }

    private static void updateSingleTableProperties(Config config, Connection conn, String table) {

        // ---> If information has been fetched before, abort execution.
        if (tableProperties.containsKey(config.database)) {
            if (tableProperties.get(config.database).containsKey(table)) return;
        }

        // ---> Build query and prepare receiver list
        String query = "show full columns from " + config.database + "." + table;
        List<MySQLA_tableProperties.ColumnProps> newTableProps = new ArrayList<>();

        // ---> Execute query on database
        try {
            MySQLA_loggers.logFetch("Fecthing details for table '" + table + "' @ '" + config.database + "'...");
            Statement st = conn.createStatement();
            ResultSet resultSet = st.executeQuery(query);

            // ---> Digest data
            while(resultSet.next()) {
                ColumnProps column = new ColumnProps(
                        resultSet.getString("Field"),
                        resultSet.getString("Type"),
                        resultSet.getString("Collation"),
                        resultSet.getString("Null"),
                        resultSet.getString("Key"),
                        resultSet.getString("Default"),
                        resultSet.getString("Extra"),
                        resultSet.getString("Privileges")
                );
                newTableProps.add(column);
            }
            saveFetchedTablesDetails(config, table, newTableProps);

        } catch (SQLSyntaxErrorException e) {
            MySQLA_loggers.logError("Table '" + table + "' @ '" + config.database + "' doesn't exist.");

        } catch (SQLException e) {
            MySQLA_loggers.logError("Unable to retrieve details from table '" + table + "' @ '" + config.database + "'.");
            e.printStackTrace();
        }
    }

    private static void saveFetchedTablesDetails (Config config, String table, List<ColumnProps> newTableProps) {
        // ---> Update main table properties map
        if (tableProperties.containsKey(config.database)) {
            Map propUpdate = new HashMap(tableProperties.get(config.database));
            propUpdate.put(table, newTableProps);
            tableProperties.replace(config.database, propUpdate);

        }
        else {
            tableProperties.put(config.database, Map.of(table, newTableProps));
        }

        // ---> Extract and store column names
        List<String> allColumnNames = newTableProps.stream().map(x -> x.field).collect(Collectors.toList());
        if (MySQLA_tableProperties.allColumnNames.containsKey(config.database)) {
            Map propUpdate = new HashMap(MySQLA_tableProperties.allColumnNames.get(config.database));
            propUpdate.put(table, allColumnNames);
            MySQLA_tableProperties.allColumnNames.replace(config.database, propUpdate);
        }
        else {
            MySQLA_tableProperties.allColumnNames.put(config.database, Map.of(table, allColumnNames));
        }

        // ---> Extract and store column types
        List<String> allColumnTypes = newTableProps.stream()
                .map(x -> x.type.replaceAll("\\(.*\\)", "")).collect(Collectors.toList());
        Map<String, String> columnTypes = new HashMap<>();
        for(int x = 0; x<allColumnNames.size(); x++) {
            columnTypes.put(allColumnNames.get(x), allColumnTypes.get(x));
        }
        if (MySQLA_tableProperties.allColumnTypes.containsKey(config.database)) {
            Map propUpdate = new HashMap(MySQLA_tableProperties.allColumnTypes.get(config.database));
            propUpdate.put(table, columnTypes);
            MySQLA_tableProperties.allColumnTypes.replace(config.database, propUpdate);
        }
        else {
            MySQLA_tableProperties.allColumnTypes.put(config.database, Map.of(table, columnTypes));
        }

        // ---> Extract and store non-default "updatable" columns names
        List<String> excludeIfExtraContains = new ArrayList<>(Arrays.asList(
                "auto_increment",
                "DEFAULT_GENERATED",
                "DEFAULT_GENERATED on update CURRENT_TIMESTAMP",
                "on update CURRENT_TIMESTAMP"));
        List<String> excludeIfDefaultContains = new ArrayList<>(Arrays.asList(
                "CURRENT_TIMESTAMP"));
        List<String> updatableColumnNames = newTableProps.stream()
                .filter(z -> !excludeIfExtraContains.contains(z.extra))
                .filter(z -> !excludeIfDefaultContains.contains(z.defaultable))
                .map(x -> x.field).collect(Collectors.toList());
        if (updateableColumns.containsKey(config.database)) {
            Map propUpdate = new HashMap(MySQLA_tableProperties.updateableColumns.get(config.database));
            propUpdate.put(table, updatableColumnNames);
            MySQLA_tableProperties.updateableColumns.replace(config.database, propUpdate);
        }
        else {
            updateableColumns.put(config.database, Map.of(table, updatableColumnNames));
        }

        // ---> Extract and store primary key
        String primaryKey = newTableProps.stream().filter(x -> x.key.contains("PRI")).findFirst().get().field;
        if (primaryKeys.containsKey(config.database)) {
            Map propUpdate = new HashMap(MySQLA_tableProperties.primaryKeys.get(config.database));
            propUpdate.put(table, primaryKey);
            MySQLA_tableProperties.primaryKeys.replace(config.database, propUpdate);
        }
        else {
            primaryKeys.put(config.database, Map.of(table, primaryKey));
        }

        // ---> Inform dev
        MySQLA_loggers.logFetch("Primary key: " + primaryKey);
        MySQLA_loggers.logFetch("Columns/types: " + columnTypes);
        MySQLA_loggers.logFetch("Updatable columns (no auto-increment, no defaults): " + updatableColumnNames);
    }

    private static class ColumnProps {

        public String field;
        public String type;
        public String collation;
        public String nullable;
        public String key;
        public String defaultable;
        public String extra;
        public String privileges;

        public ColumnProps(String field, String type, String collation, String nullable, String key,
                           String defaultable, String extra, String privileges) {

            this.field = field;
            this.type = type;
            this.collation = collation;
            this.nullable = nullable;
            this.key = key;
            this.defaultable = defaultable;
            this.extra = extra;
            this.privileges = privileges;
        }
    }
}
