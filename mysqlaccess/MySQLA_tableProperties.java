package com.company.mysqlaccess;

import com.company.mysqlaccess.models.ColumnProps;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class MySQLA_tableProperties {
    private static Map<String, Map<String, List<ColumnProps>>> tableProperties = new HashMap<>();
    private static Map<String, Map<String, List<String>>> updateableColumns = new HashMap<>();
    private static Map<String, Map<String, List<String>>> numericColumns = new HashMap<>();
    private static Map<String, Map<String, List<String>>> allColumnNames = new HashMap<>();
    private static Map<String, Map<String, Map<String, String>>> allColumnTypes = new HashMap<>();
    private static Map<String, Map<String, String>> primaryKeys = new HashMap<>();

    public static List<String> getUpdateableColumns(String database, String table) {
        if (updateableColumns.containsKey(database)) {
            if (updateableColumns.get(database).containsKey(table)) {
                return updateableColumns.get(database).get(table);
            }
        }
        return null;
    }

    public static List<String> getAllColumns(String database, String table) {
        if (allColumnNames.containsKey(database)) {
            if (allColumnNames.get(database).containsKey(table)) {
                return allColumnNames.get(database).get(table);
            }
        }
        return null;
    }

    public static List<String> getNumericColumns(String database, String table) {
        if (numericColumns.containsKey(database)) {
            if (numericColumns.get(database).containsKey(table)) {
                return numericColumns.get(database).get(table);
            }
        }
        return null;
    }

    public static String getColumnType(String database, String table, String column) {
        if (allColumnTypes.containsKey(database)) {
            if (allColumnTypes.get(database).containsKey(table)) {
                if (allColumnTypes.get(database).get(table).containsKey(column)) {
                    return allColumnTypes.get(database).get(table).get(column);
                }
            }
        }
        return null;
    }

    public static String getPrimaryKey(String database, String table) {
        if (primaryKeys.containsKey(database)) {
            if (primaryKeys.get(database).containsKey(table)) {
                return primaryKeys.get(database).get(table);
            }
        }
        return null;
    }

    public static void updateTableProperties(String database, Connection conn, String table) {
        updateAllTablesProperties(database, conn, table);
        updateSingleTableProperties(database, conn, table);
    }

    private static void updateAllTablesProperties(String database, Connection conn, String tableName) {

        // ---> If information has been fetched before, abort execution.
        if (tableProperties.containsKey(database)) {
            if (tableProperties.get(database).containsKey(tableName)) return;
        }

        // ---> Build query and prepare receiver list
        String query = "select * from information_schema.columns where table_schema = '" + database + "'";
        List<ColumnProps> currentTableProps = new ArrayList<>();
        Map<String, List<ColumnProps>> tablesDetails = new HashMap<>();
        String currentTableName = null;

        // ---> Execute query on database
        try {
            MySQLA_loggers.logFetch("Fetching details for all tables @ database '" + database + "'...");
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
                MySQLA_loggers.logFetch("");
                MySQLA_loggers.logFetch("Details for table '" + table + "' ...");
                saveFetchedTablesDetails(database, table, newTableProps);
            }

        } catch (SQLException e) {
            MySQLA_loggers.logError("TABLEPROPS - Unable to retrieve details from database '" + database + "'.");
            e.printStackTrace();
        }
    }

    private static void updateSingleTableProperties(String database, Connection conn, String table) {

        // ---> If information has been fetched before, abort execution.
        if (tableProperties.containsKey(database)) {
            if (tableProperties.get(database).containsKey(table)) return;
        }

        // ---> Build query and prepare receiver list
        String query = "show full columns from " + database + "." + table;
        List<ColumnProps> newTableProps = new ArrayList<>();

        // ---> Execute query on database
        try {
            MySQLA_loggers.logFetch("Fecthing details for table '" + table + "' @ '" + database
                    + "'...");
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
            saveFetchedTablesDetails(database, table, newTableProps);

        } catch (SQLSyntaxErrorException e) {
            MySQLA_loggers.logError("TABLEPROPS - Table '" + table + "' @ '" + database
                    + "' doesn't exist.");

        } catch (SQLException e) {
            MySQLA_loggers.logError("TABLEPROPS - Unable to retrieve details from table '" + table + "' @ '"
                    + database + "'.");
            e.printStackTrace();
        }
    }

    private static void saveFetchedTablesDetails (String database, String table, List<ColumnProps> newTableProps) {
        // ---> Update main table properties map
        if (tableProperties.containsKey(database)) {
            Map propUpdate = new HashMap(tableProperties.get(database));
            propUpdate.put(table, newTableProps);
            tableProperties.replace(database, propUpdate);

        }
        else {
            tableProperties.put(database, Map.of(table, newTableProps));
        }

        // ---> Extract and store column names
        List<String> allColumnNames = newTableProps.stream().map(x -> x.field).collect(Collectors.toList());
        if (MySQLA_tableProperties.allColumnNames.containsKey(database)) {
            Map propUpdate = new HashMap(MySQLA_tableProperties.allColumnNames.get(database));
            propUpdate.put(table, allColumnNames);
            MySQLA_tableProperties.allColumnNames.replace(database, propUpdate);
        }
        else {
            MySQLA_tableProperties.allColumnNames.put(database, Map.of(table, allColumnNames));
        }

        // ---> Extract and store numeric column names
        List<String> numericColumnNames = newTableProps
                .stream()
                .filter(f -> MySQLA_typeEquivalency.isNumericColumn(f.type))
                .map(x -> x.field)
                .collect(Collectors.toList());
        if (MySQLA_tableProperties.numericColumns.containsKey(database)) {
            Map propUpdate = new HashMap(MySQLA_tableProperties.numericColumns.get(database));
            propUpdate.put(table, numericColumnNames);
            MySQLA_tableProperties.numericColumns.replace(database, propUpdate);
        }
        else {
            MySQLA_tableProperties.numericColumns.put(database, Map.of(table, numericColumnNames));
        }

        // ---> Extract and store column types
        List<String> allColumnTypes = newTableProps.stream()
                .map(x -> x.type.replaceAll("\\(.*\\)", "")).collect(Collectors.toList());
        Map<String, String> columnTypes = new HashMap<>();
        for(int x = 0; x<allColumnNames.size(); x++) {
            columnTypes.put(allColumnNames.get(x), allColumnTypes.get(x));
        }
        if (MySQLA_tableProperties.allColumnTypes.containsKey(database)) {
            Map propUpdate = new HashMap(MySQLA_tableProperties.allColumnTypes.get(database));
            propUpdate.put(table, columnTypes);
            MySQLA_tableProperties.allColumnTypes.replace(database, propUpdate);
        }
        else {
            MySQLA_tableProperties.allColumnTypes.put(database, Map.of(table, columnTypes));
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
        if (updateableColumns.containsKey(database)) {
            Map propUpdate = new HashMap(MySQLA_tableProperties.updateableColumns.get(database));
            propUpdate.put(table, updatableColumnNames);
            MySQLA_tableProperties.updateableColumns.replace(database, propUpdate);
        }
        else {
            updateableColumns.put(database, Map.of(table, updatableColumnNames));
        }

        // ---> Extract and store primary key
        String primaryKey = newTableProps.stream().filter(x -> x.key.contains("PRI")).findFirst().get().field;
        if (primaryKeys.containsKey(database)) {
            Map propUpdate = new HashMap(MySQLA_tableProperties.primaryKeys.get(database));
            propUpdate.put(table, primaryKey);
            MySQLA_tableProperties.primaryKeys.replace(database, propUpdate);
        }
        else {
            primaryKeys.put(database, Map.of(table, primaryKey));
        }

        // ---> Inform dev
        MySQLA_loggers.logFetch("Primary key: " + primaryKey);
        MySQLA_loggers.logFetch("Columns/types: " + columnTypes);
        MySQLA_loggers.logFetch("Numeric columns: " + numericColumnNames);
        MySQLA_loggers.logFetch("Updatable columns (no auto-increment, no defaults): " + updatableColumnNames);
    }
}
