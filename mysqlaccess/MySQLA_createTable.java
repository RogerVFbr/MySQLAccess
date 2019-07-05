package com.company.mysqlaccess;

import java.sql.*;

public class MySQLA_createTable {

    public static boolean createTable (Connection conn, String database, String tableName, String[] tableConfig,
                                    OnComplete<String> callback) {

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("CREATE_TABLE - Unable to execute command because there is no connection to '" +
                    database + "' database.");
            if (callback != null) callback.onFailure();
            return false;
        }

        // ---> If no table is selected, abort.
        if (tableName.equals(null) || tableName.equals("")) {
            MySQLA_loggers.logError("CREATE_TABLE - Please provide a valid table name.");
            if (callback != null) callback.onFailure();
            return false;
        }

        // ---> If tableConfig length is not even, abort.
        if (tableConfig.length%2 != 0) {
            MySQLA_loggers.logError("CREATE_TABLE - Table configuration provided is invalid. " +
                    "Needs to contains even amount of entries.");
            if (callback != null) callback.onFailure();
            return false;
        }


        try {
            DatabaseMetaData mtd = conn.getMetaData();
            ResultSet resultset = mtd.getTables(null, null, tableName, null);
            if (resultset.next()) {
                MySQLA_loggers.logError("CREATE_TABLE - Table '" + tableName + "' already existed at database '"
                        + database + "'.");
                if (callback != null) callback.onFailure();
                return false;
            }

        } catch (SQLException e) {
            MySQLA_loggers.logError("CREATE_TABLE - Unable to verify table '" + tableName + "' existence '" +
                    database + "' database.");
            e.printStackTrace();
            if (callback != null) callback.onFailure();
            return false;
        }

        String query = "create table if not exists " + tableName + " (";

        for (int x = 0; x<tableConfig.length; x+=2) {
            query += tableConfig[x] + " " + tableConfig[x+1] + ", ";
        }

        query = query.replaceAll(", $", "") + ")";
//        query += "SET time_zone='+00:00'";

        try {
            MySQLA_loggers.logInfo("CREATE_TABLE - Executing query at '" + database +"' database: " + query);
            PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();
        } catch (SQLException e) {
            MySQLA_loggers.logError("CREATE_TABLE - Failed to create new table '" + tableName + "' at database '"
                    + database + "'.");
            e.printStackTrace();
            if (callback != null) callback.onFailure();
            return false;
        }

        try {
            DatabaseMetaData mtd = conn.getMetaData();
            ResultSet resultset = mtd.getTables(null, null, tableName, null);
            if (resultset.next()) {
                MySQLA_loggers.logInfo("CREATE_TABLE - Table '" + tableName + "' @ '"
                        + database + "' successfully created.");
            }
            else {
                MySQLA_loggers.logError("CREATE_TABLE - Could not create table '" + tableName + "' @ '" +
                        database + "'.");
                if (callback != null) callback.onFailure();
                return false;
            }

        } catch (SQLException e) {
            MySQLA_loggers.logError("CREATE_TABLE - Unable to verify table '" + tableName + "' @ '" +
                    database + "' creation.");
            e.printStackTrace();
            if (callback != null) callback.onFailure();
            return false;
        }

        if (callback != null) callback.onSuccess("");
        return true;


    }

    public static boolean dropTable (Connection conn, String database, String tableName, OnComplete<String> callback) {

        // ---> If no connection has been established, abort.
        if (!MySQLA_validators.hasConnection(conn)) {
            MySQLA_loggers.logError("DROP_TABLE - Unable to execute command because there is no connection to '" +
                    database + "' database.");
            if (callback != null) callback.onFailure();
            return false;
        }

        // ---> If no table is selected, abort.
        if (tableName.equals(null) || tableName.equals("")) {
            MySQLA_loggers.logError("DROP_TABLE - Please provide a valid table name.");
            if (callback != null) callback.onFailure();
            return false;
        }

        try {
            DatabaseMetaData mtd = conn.getMetaData();
            ResultSet resultset = mtd.getTables(null, null, tableName, null);
            if (!resultset.next()) {
                MySQLA_loggers.logError("DROP_TABLE - Table '" + tableName + "' @ '"
                        + database + "' doesn't exist.");
                if (callback != null) callback.onFailure();
                return false;
            }

        } catch (SQLException e) {
            MySQLA_loggers.logError("DROP_TABLE - Unable to verify table '" + tableName + "' existence '" +
                    database + "' database.");
            e.printStackTrace();
            if (callback != null) callback.onFailure();
            return false;
        }

        String query = "drop table  " + tableName;

        try {
            MySQLA_loggers.logInfo("DROP_TABLE - Executing query at '" + database +"' database: " + query);
            PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();
        } catch (SQLException e) {
            MySQLA_loggers.logError("DROP_TABLE - Failed to drop table '" + tableName + "' at database '"
                    + database + "'.");
            e.printStackTrace();
            if (callback != null) callback.onFailure();
            e.printStackTrace();
        }

        try {
            DatabaseMetaData mtd = conn.getMetaData();
            ResultSet resultset = mtd.getTables(null, null, tableName, null);
            if (!resultset.next()) {
                MySQLA_loggers.logInfo("DROP_TABLE - Table '" + tableName + "' @ '"
                        + database + "' successfully deleted.");
            }
            else {
                MySQLA_loggers.logError("DROP_TABLE - Could not delete table '" + tableName + "' @ '" +
                        database + "'.");
                if (callback != null) callback.onFailure();
                return false;
            }

        } catch (SQLException e) {
            MySQLA_loggers.logError("DROP_TABLE - Unable to verify table '" + tableName + "' @ '" +
                    database + "' deletion.");
            e.printStackTrace();
            if (callback != null) callback.onFailure();
            return false;
        }

        if (callback != null) callback.onSuccess("");
        return true;
    }
}
