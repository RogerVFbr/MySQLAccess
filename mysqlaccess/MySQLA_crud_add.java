package com.company.mysqlaccess;

import com.company.mysqlaccess.models.Config;

import java.lang.reflect.Field;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class MySQLA_crud_add {
    public static <T> void addMain (T element, Config config, Connection conn, String selectedTable,
                                    OnComplete<String> callback) {

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("Unable to execute 'ADD' command because there is no connection to '" +
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

        // ---> Initiate code execution os separate thread
        Thread t = new Thread( () -> {

            // ---> Get table properties of not already present
            MySQLA_tableProperties.updateTableProperties(config, conn, table);

            // ---> If unable to fetch table details, abort
            if (!MySQLA_validators.hasFetchedTableDetails(config.database, table,
                    MySQLA_tableProperties.getAllColumnNames())) {
                MySQLA_loggers.logError("Could not fetch table details from database.");
                if (callback != null) callback.onFailure();
                return;
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
                MySQLA_loggers.logInfo("Executing add query at '" + table +"': " + query);
                PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                int result = ps.executeUpdate();
                if (result == 0) {
                    MySQLA_loggers.logError("Failed to obtain insertion confirmation on table '" + table + "'!");
                    if (callback != null) callback.onFailure();
                    return;
                }
                String key = null;
                try (ResultSet generatedObject = ps.getGeneratedKeys();) {
                    if (generatedObject.next()) {
                        key = generatedObject.getString(1);
                    }
                }
                if (key == null){
                    MySQLA_loggers.logInfo("Successfully written new row to table '" + table + ".");
                }
                else {
                    MySQLA_loggers.logInfo("Successfully written new row to table '" + table
                            + "' with auto-generated key: " + key);
                }
                if (callback != null) callback.onSuccess(key);

            } catch (SQLException e) {
                MySQLA_loggers.logError("Failed to create new row on table '" + table + "'.");
                if (callback != null) callback.onFailure();
                e.printStackTrace();
            }
        });

        t.start();
    }
}
