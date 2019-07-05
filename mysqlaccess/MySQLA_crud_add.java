package com.company.mysqlaccess;

import com.company.mysqlaccess.models.MySQLAConfig;

import java.lang.reflect.Field;
import java.sql.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MySQLA_crud_add {
    public static <T> Object addMain (T element, MySQLAConfig config, Connection conn, String selectedTable,
                                    OnComplete<String> callback) {

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("ADD - Unable to execute command because there is no connection to '" +
                    config.database + "' database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> If no table is selected, abort.
        String table = selectedTable;
        if (!MySQLA_validators.isTableSelected(table, config)) {
            MySQLA_loggers.logError("ADD - No table selected on database '" + config.database
                    + "'. Use setTable(..tablename..) " + "before executing mysqlaccess commands.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Get table properties of not already present
        MySQLA_tableProperties.updateTableProperties(config, conn, table);

        // ---> If unable to fetch table details, abort
        if (!MySQLA_validators.hasFetchedTableDetails(config.database, table,
                MySQLA_tableProperties.getAllColumnNames())) {
            MySQLA_loggers.logError("ADD - Could not fetch table details from database.");
            if (callback != null) callback.onFailure();
            return null;
        }

        // ---> Figure out best correlation between database column names and model fields
        Map<String, String> propertyMap = MySQLA_correlations.getColumnFieldCorrelation(element.getClass(),
                MySQLA_tableProperties.getUpdateableColumns().get(config.database).get(table),
                config.database, table);

        // ---> Build SQL query
        String query = "";
        String columns = "";
        String values = "";

        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            String columnType = MySQLA_tableProperties
                    .getAllColumnTypes()
                    .get(config.database)
                    .get(table)
                    .get(entry.getKey()
                    );
            columns += entry.getKey() + ", ";
            try {
                Field field = element.getClass().getDeclaredField(entry.getValue());
                field.setAccessible(true);

                if (field.getType() == String.class) {
                    values += "'" + field.get(element) + "', ";
                }
                else if (field.getType() == java.util.Date.class || field.getType() == java.sql.Date.class) {
                    Date d = (Date) field.get(element);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    values += "'" + format.format(d) + "', ";
                }
                else if (field.getType() == Year.class) {
                    if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                        values += "'" + field.get(element) + "-01-01 00:00:00', ";
                    }
                    else {
                        values += "" + field.get(element).toString() + ", ";
                    }
                }
                else if (field.getType() == LocalDateTime.class) {
                    LocalDateTime d = (LocalDateTime) field.get(element);
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
                    values += "'" + d.format(format) + "', ";
                }
                else if (field.getType() == LocalDate.class) {
                    LocalDate d = (LocalDate) field.get(element);
                    if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                        values += "'" + d.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00', ";
                    }
                    else {
                        values += "'" + d.format(DateTimeFormatter.ISO_LOCAL_DATE) + "', ";
                    }
                }
                else if (field.getType() == LocalTime.class) {
                    if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                        values += "'1970-01-01 " + field.get(element).toString() + "', ";
                    }
                    else {
                        values += "'" + field.get(element).toString() + "', ";
                    }
                }
                else if (field.getType() == Timestamp.class) {
                    if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                        values += "'" + field.get(element).toString() + "', ";
                    }
                    else if (MySQLA_typeEquivalency.isDateColumn(columnType)) {
                        values += "'" + field.get(element).toString().split(" ")[0] + "', ";
                    }
                }
                else {
                    values += field.get(element).toString() + ", ";
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        columns = columns.replaceAll(", $", "");
        values = values.replaceAll(", $", "");
        query = "insert into " + table + " (" + columns + ") values (" + values + ")";

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
            String key = null;
            try (ResultSet generatedObject = ps.getGeneratedKeys();) {
                if (generatedObject.next()) {
                    key = generatedObject.getString(1);
                }
            }
            if (key == null){
                MySQLA_loggers.logInfo("ADD - Successfully written new row to table '" + table + ".");
            }
            else {
                MySQLA_loggers.logInfo("ADD - Successfully written new row to table '" + table
                        + "' with auto-generated key: " + key);
            }

            MySQLA_cache.deleteCache(config.database, table);
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
