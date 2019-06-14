package com.company.mysqlaccess;

import com.company.mysqlaccess.models.Config;

import java.sql.*;
import java.util.*;

public class MySQLAccess {

    /*======================================================================================================\
                                                   VARIABLES
    \======================================================================================================*/

    //region Static class variables

    private static final String serverTimeZone =
            "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private Connection conn;
    private String url = "";
    private String table = "";
    private Config config = new Config();

    //endregion


    /*======================================================================================================\
                                                  CONSTRUCTORS
    \======================================================================================================*/

    //region Constructor methods

    public MySQLAccess(String ip, int port, String database, String user, String password) {
        constructorProcedures(ip, port, database, user, password);
    }

    public MySQLAccess(String ip, int port, String database, String user, String password, String tableName) {
        constructorProcedures(ip, port, database, user, password);
        setTable(tableName);
    }

    public MySQLAccess(Config config) {
        constructorProcedures(config.ip, config.port, config.database, config.user, config.password);
    }

    public MySQLAccess(Config config, String tableName) {
        constructorProcedures(config.ip, config.port, config.database, config.user, config.password);
        setTable(tableName);
    }

    private void constructorProcedures(String ip, int port, String database, String user, String password) {
        this.config.ip = ip;
        this.config.port = port;
        this.config.database = database;
        this.config.user = user;
        this.config.password = password;
        this.url = "jdbc:mysql://" + ip + ":" + port + "/" + database + serverTimeZone;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            if (conn == null) conn = DriverManager.getConnection(url, user, password);

        } catch (SQLException e) {
            MySQLA_loggers.logError("Unable to connect to database " + database + " @ " + ip + ":" + port +
                    " with credentials " + user + " | " + password);
            MySQLA_loggers.logError(e.getMessage());
        } catch (ClassNotFoundException e) {
            MySQLA_loggers.logError("Unable to locate Java JDBC Driver.");
            MySQLA_loggers.logError(e.getMessage());
        }
    }

    //endregion


    /*======================================================================================================\
                                               GETTERS & SETTERS
    \======================================================================================================*/

    //region Getters & setters

    public void setTable(String tableName) { this.table = tableName; }

    public static void logDetails () { MySQLA_loggers.logDetails(); }

    public static void logInfo () { MySQLA_loggers.logInfo(); }

    public static void logFetch () { MySQLA_loggers.logFetch(); }

    //endregion


    /*======================================================================================================\
                                                  DATABASE GET
    \======================================================================================================*/

    //region Adaptive get overloads

    public <T> List<T> get(Class<T> type) {
        return MySQLA_crud_getMain.getMain(type, config, conn, table,null, null);
    }

    public <T> void get(Class<T> type, OnGetComplete callback) {
        get(type, null, callback);
    }

    public <T> List<T> get(Class<T> type, String sqlWhereFilter) {
        return MySQLA_crud_getMain.getMain(type, config, conn, table, sqlWhereFilter, null);
    }

    public <T> void get(Class<T> type, String sqlWhereFilter, OnGetComplete<T> callback) {
        String table = this.table;
        Thread t = new Thread( () -> {
            MySQLA_crud_getMain.getMain(type, config, conn, table, sqlWhereFilter, callback);
        });
        t.start();
    }

    //endregion


    /*======================================================================================================\
                                            DATABASE GET FILL (JOIN)
    \======================================================================================================*/

    //region Get fill overloads

    public <T> List<T> getFill(Class type, String IdColumn, String tableToJoin) {
        return MySQLA_crud_getFill.getFillMain(type, config, conn, table, new String[]{IdColumn}, new String[]{tableToJoin},
                null, null);
    }

    public void getFill(Class type, String IdColumn, String tableToJoin, OnGetComplete callback) {
        String currentTable = this.table;
        Thread t = new Thread( () -> {
            MySQLA_crud_getFill.getFillMain(type, config, conn, currentTable, new String[]{IdColumn},
                    new String[]{tableToJoin},null, callback);
        });
        t.start();
    }

    public <T> List<T> getFill(Class type, String IdColumn, String tableToJoin, String sqlWhereFilter) {
        return MySQLA_crud_getFill.getFillMain(type, config, conn, table, new String[]{IdColumn}, new String[]{tableToJoin},
                sqlWhereFilter, null);
    }

    public void getFill(Class type, String IdColumn, String tableToJoin, String sqlWhereFilter,
                        OnGetComplete callback) {
        String currentTable = this.table;
        Thread t = new Thread( () -> {
            MySQLA_crud_getFill.getFillMain(type, config, conn, currentTable, new String[]{IdColumn},
                    new String[]{tableToJoin}, sqlWhereFilter, callback);
        });
        t.start();
    }

    public <T> List<T> getFill(Class type, String[] IdColumns, String[] tablesToJoin) {
        return MySQLA_crud_getFill.getFillMain(type, config, conn, table, IdColumns, tablesToJoin, null,
                null);
    }

    public void getFill(Class type, String[] IdColumns, String[] tablesToJoin, OnGetComplete callback) {
        String currentTable = this.table;
        Thread t = new Thread( () -> {
            MySQLA_crud_getFill.getFillMain(type, config, conn, currentTable, IdColumns, tablesToJoin, null,
                    callback);
        });
        t.start();
    }

    public <T> List<T> getFill(Class type, String[] IdColumns, String[] tablesToJoin, String sqlWhereFilter) {
        return MySQLA_crud_getFill.getFillMain(type, config, conn, table, IdColumns, tablesToJoin, sqlWhereFilter,
                null);
    }

    public void getFill(Class type, String[] IdColumns, String[] tablesToJoin, String sqlWhereFilter,
                               OnGetComplete callback) {
        String currentTable = this.table;
        Thread t = new Thread( () -> {
            MySQLA_crud_getFill.getFillMain(type, config, conn, currentTable, IdColumns, tablesToJoin, sqlWhereFilter,
                    callback);
        });
        t.start();
    }

    //endregion


    /*======================================================================================================\
                                          MIN, MAX, COUNT, AVERAGE, SUM
    \======================================================================================================*/

    //region Overloads

    public Number getCount() {
        return MySQLA_crud_getMetrics.getMetrics(config, conn, table, null, "COUNT", null,
                null);
    }

    public void getCount(OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            MySQLA_crud_getMetrics.getMetrics(config, conn, table, null, "COUNT", null,
                    callback);
        });
        t.start();
    }

    public Number getCount(String sqlWhereFilter) {
        return MySQLA_crud_getMetrics.getMetrics(config, conn, table, null, "COUNT", sqlWhereFilter,
                null);
    }

    public void getCount(String sqlWhereFilter, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            MySQLA_crud_getMetrics.getMetrics(config, conn, table, null, "COUNT", sqlWhereFilter,
                    callback);
        });
        t.start();
    }

    public Number getSum(String column) {
        return MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "SUM", null,
                null);
    }

    public void getSum(String column, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "SUM", null, callback);
        });
        t.start();
    }

    public Number getSum(String column, String sqlWhereFilter) {
        return MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "SUM", sqlWhereFilter, null);
    }

    public void getSum(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "SUM", sqlWhereFilter, callback);
        });
        t.start();
    }

    public Number getAvg(String column) {
        return MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "AVG", null,
                null);
    }

    public void getAvg(String column, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "AVG", null, callback);
        });
        t.start();
    }

    public Number getAvg(String column, String sqlWhereFilter) {
        return MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "AVG", sqlWhereFilter,
                null);
    }

    public void getAvg(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "AVG", sqlWhereFilter, callback);
        });
        t.start();
    }

    public Number getMax(String column) {
        return MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "MAX", null,
                null);
    }

    public void getMax(String column, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "MAX", null, callback);
        });
        t.start();
    }

    public Number getMax(String column, String sqlWhereFilter) {
        return MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "MAX", sqlWhereFilter,
                null);
    }

    public void getMax(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "MAX", sqlWhereFilter, callback);
        });
        t.start();
    }

    public Number getMin(String column) {
        return MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "MIN", null,
                null);
    }

    public void getMin(String column, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "MIN", null,
                    null);
        });
        t.start();
    }

    public Number getMin(String column, String sqlWhereFilter) {
        return MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "MIN", sqlWhereFilter,
                null);
    }

    public void getMin(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            MySQLA_crud_getMetrics.getMetrics(config, conn, table, column, "MIN", sqlWhereFilter, callback);
        });
        t.start();
    }

    //endregion


    /*======================================================================================================\
                                                  DATABASE ADD
    \======================================================================================================*/

    //region Database add overloads

    public <T> void add (T element) {
        MySQLA_crud_add.addMain(element, config, conn, table, null);
    }

    public <T> void add (T element, OnComplete<String> callback) {
        MySQLA_crud_add.addMain(element, config, conn, table, callback);
    }

    //endregion


    /*======================================================================================================\
                                                 DATABASE UPDATE
    \======================================================================================================*/

    //region Database update overloads

    public <T> void update (T element) {
        MySQLA_crud_update.updateMain(element, config, conn, table, null);
    }

    public <T> void update (T element, OnComplete<Integer> callback) {
        MySQLA_crud_update.updateMain(element, config, conn, table, callback);
    }

    //endregion


    /*======================================================================================================\
                                                 DATABASE DELETE
    \======================================================================================================*/

    //region Database delete overloads

    public void delete (String sqlWhere) {
        MySQLA_crud_delete.deleteMain(sqlWhere, config, conn, table, null);
    }

    public void delete (String sqlWhere, OnComplete<Integer> callback) {
        MySQLA_crud_delete.deleteMain(sqlWhere, config, conn, table, callback);
    }

    //endregion

}
