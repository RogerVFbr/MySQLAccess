package com.company.mysqlaccess;

import com.company.mysqlaccess.models.Config;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MySQLA_crud_update {
    public static <T> void updateMain (T element, Config config, Connection conn, String selectedTable,
                                       OnComplete<Integer> callback) {

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("Unable to execute 'UPDATE' command because there is no connection to '" +
                    config.database + "' database.");
            if (callback != null) callback.onFailure();
            return;
        }

        // ---> If no table is selected, abort
        String table = selectedTable;
        if (!MySQLA_validators.isTableSelected(table, config)) {
            MySQLA_loggers.logError("No table selected on database '" + config.database + "'. Use setTable(..tablename..) " +
                    "before executing mysqlaccess commands.");
            if (callback != null) callback.onFailure();
            return;
        }

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
            try {
                String fieldName = propertyMap.get(primaryKey);
                Field primaryField = element.getClass().getDeclaredField(fieldName);
                primaryField.setAccessible(true);

                if (primaryField.getType() == String.class) {
                    primaryFieldData += "'" + primaryField.get(element) + "'";
                }

                else if (primaryField.getType() == Date.class) {
                    Date d = (Date) primaryField.get(element);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    primaryFieldData += "'" + format.format(d) + "'";
                }

                else {
                    primaryFieldData += primaryField.get(element).toString();
                }

            } catch (Exception e) {
                MySQLA_loggers.logError("Failed to modify row on database. Primary key equivalent field on model is non-existent, " +
                        "non-detectable or non-accessible.");
                if (callback != null) callback.onFailure();
                e.printStackTrace();
                return;
            }

            String query = "update " + table + " ";
            String set = "set ";
            String where = "where " + primaryKey + " = " + primaryFieldData;

            for (Map.Entry<String, String> entry : propertyMap.entrySet()) {

                if (entry.getKey() == primaryKey) continue;
                set += entry.getKey() + " = ";

                try {
                    Field field = element.getClass().getDeclaredField(entry.getValue());
                    field.setAccessible(true);

                    if (field.getType() == String.class) {
                        if (entry.getKey() == primaryKey) {
                            primaryFieldData += "'" + field.get(element) + "'";
                            continue;
                        }
                        set += "'" + field.get(element) + "', ";
                    }

                    else if (field.getType() == Date.class) {
                        Date d = (Date) field.get(element);
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        if (entry.getKey() == primaryKey) {
                            primaryFieldData += "'" + format.format(d) + "'";
                            continue;
                        }
                        set += "'" + format.format(d) + "', ";
                    }

                    else {
                        if (entry.getKey() == primaryKey) {
                            primaryFieldData += field.get(element).toString();
                            continue;
                        }
                        set += field.get(element).toString() + ", ";
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            set = set.replaceAll(", $", "") + " ";
            query += set + where;

            // ---> Execute query on connection
            try {

                Statement st = conn.createStatement();
                MySQLA_loggers.logInfo("Executing update query at '" + table +"': " + query);
                int result = st.executeUpdate(query);

                if (result == 0) {
                    MySQLA_loggers.logError("Failed to update row(s) on table '" + table + "'!");
                    if (callback != null) callback.onFailure();
                    return;
                }

                MySQLA_loggers.logInfo("Successfully updated " + result + " row(s) on table '" + table + "': " + element);
                if (callback != null) callback.onSuccess(result);

            } catch (SQLException e) {
                MySQLA_loggers.logError("Failed to update row on table '" + table + "'.");
                if (callback != null) callback.onFailure();
                e.printStackTrace();
            }
        });
        t.start();
    }
}
