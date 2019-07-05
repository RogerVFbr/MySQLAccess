package com.company.mysqlaccess;

import com.company.mysqlaccess.models.MySQLAConfig;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MySQLA_crud_update {
    public static <T> Integer updateMain (T element, MySQLAConfig config, Connection conn, String selectedTable,
                                       OnComplete<Integer> callback) {

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("UPDATE - Unable to execute command because there is no connection to '" +
                    config.database + "' database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> If no table is selected, abort
        String table = selectedTable;
        if (!MySQLA_validators.isTableSelected(table, config)) {
            MySQLA_loggers.logError("UPDATE - No table selected on database '" + config.database
                    + "'. Use setTable(..tablename..) " + "before executing mysqlaccess commands.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Get table properties of not already present
        MySQLA_tableProperties.updateTableProperties(config, conn, table);

        // ---> If unable to fetch table details, abort
        if (!MySQLA_validators.hasFetchedTableDetails(config.database, table,
                MySQLA_tableProperties.getAllColumnNames())) {
            MySQLA_loggers.logError("UPDATE - Could not fetch table details from database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Use updatable columns + primary key
        List<String> relevantColumns = MySQLA_tableProperties.getUpdateableColumns().get(config.database).get(table);
        if (!relevantColumns.contains(MySQLA_tableProperties.getPrimaryKeys().get(config.database).get(table))) {
            relevantColumns.add(MySQLA_tableProperties.getPrimaryKeys().get(config.database).get(table));
        }

        // ---> Figure out best correlation between database column names and model fields
        Map<String, String> propertyMap = MySQLA_correlations.getColumnFieldCorrelation(
                element.getClass(),
                relevantColumns,
                config.database, table);

        String primaryKey = MySQLA_tableProperties.getPrimaryKeys().get(config.database).get(table);

        // ---> Build update query
        String primaryFieldData = "";
        String query = "update " + table + " ";
        String set = "set ";

        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {

            String columnType = MySQLA_tableProperties
                    .getAllColumnTypes()
                    .get(config.database)
                    .get(table)
                    .get(entry.getKey()
                    );

            if (entry.getKey() != primaryKey) set += entry.getKey() + " = ";

            try {
                boolean isPrimaryKey = entry.getKey() == primaryKey;
                String encode = "";
                Field field = element.getClass().getDeclaredField(entry.getValue());
                field.setAccessible(true);
                Class fieldType = field.getType();

                if (fieldType == String.class) {
                    encode =  "'" + field.get(element) + "'";
                }

                else if (fieldType == Date.class) {
                    encode = "'" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format((Date) field.get(element)) + "'";
                }

                else if (fieldType == Year.class) {
                    encode = "'" + field.get(element).toString() + "-01-01 00:00:00'";
                }

                else if (fieldType == LocalDateTime.class) {
                    encode = "'" + DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
                            .format((LocalDateTime) field.get(element)) + "'";
                }

                else if (fieldType == LocalDate.class) {
                    encode = "'" + ((LocalDate) field.get(element))
                            .format(DateTimeFormatter.ISO_LOCAL_DATE) + "'";
                }

                else if (fieldType == LocalTime.class) {
                    encode = "'1970-01-01 " + field.get(element).toString() + "'";
                }

                else if (fieldType == java.sql.Date.class || field.getType() == java.sql.Date.class) {
                    encode = "'" + field.get(element).toString() + "'";
                }

                else if (fieldType == Timestamp.class) {
                    if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                        encode = "'" + field.get(element).toString() + "'";
                    }
                    else if (MySQLA_typeEquivalency.isDateColumn(columnType)) {
                        encode = "'" + field.get(element).toString().split(" ")[0] + "'";
                    }
                }

                else {
                    encode = field.get(element).toString();
                }

                if (isPrimaryKey) primaryFieldData += encode;
                else set += encode + ", ";

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String where = "where " + primaryKey + " = " + primaryFieldData;
        set = set.replaceAll(", $", "") + " ";
        query += set + where;

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

            MySQLA_loggers.logInfo("UPDATE - Successfully updated " + result + " row(s) on table '" + table + "': "
                    + element);
            MySQLA_cache.deleteCache(config.database, table);
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
