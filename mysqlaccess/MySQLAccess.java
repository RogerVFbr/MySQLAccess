package com.company.mysqlaccess;

import com.company.mysqlaccess.models.MySQLAConfig;

import java.sql.*;
import java.util.*;

public class MySQLAccess {

    /*======================================================================================================\
                                                   VARIABLES
    \======================================================================================================*/

    //region Static class variables

    //    private static final String serverTimeZone =
    //            "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";

    private static final String serverTimeZone =
            "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false";
    private Connection conn;
    private String url = "";
    private String table = "";
    private MySQLAConfig config = new MySQLAConfig();

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

    public MySQLAccess(MySQLAConfig config) {
        constructorProcedures(config.ip, config.port, config.database, config.user, config.password);
    }

    public MySQLAccess(MySQLAConfig config, String tableName) {
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

    public void setCache(String tableName, int time) { MySQLA_cache.setCache(config.database, tableName, time); }

    public void setCache(int time) { MySQLA_cache.setCache(config.database, this.table, time); }

    public void stopCache() { MySQLA_cache.stopCache(config.database, this.table); }

    public void stopCache(String tableName) { MySQLA_cache.stopCache(config.database, tableName); }

    public void clearCache() { MySQLA_cache.deleteCache(config.database, this.table); };

    public String getCurrentPrimaryFieldName() {
        return MySQLA_tableProperties.getPrimaryKey(config.database, this.table);
    }

    //endregion


    /*======================================================================================================\
                                                  DATABASE GET
    \======================================================================================================*/

    //region Adaptive get overloads

    public <T> List<T> get(Class<T> type) {
        return MySQLA_crud_getMain.getMain(type, conn, config.database, table,null, null);
    }

    public <T> void get(Class<T> type, OnGetComplete callback) {
        MySQLA_crud_getMain.getMainParallel(type, conn, config.database, this.table, null, callback);
    }

    public <T> List<T> get(Class<T> type, String sqlWhereFilter) {
        return MySQLA_crud_getMain.getMain(type, conn, config.database, table, sqlWhereFilter, null);
    }

    public <T> void get(Class<T> type, String sqlWhereFilter, OnGetComplete<T> callback) {
        MySQLA_crud_getMain.getMainParallel(type, conn, config.database, this.table, sqlWhereFilter, callback);
    }

    //endregion


    /*======================================================================================================\
                                            DATABASE GET FILL (JOIN)
    \======================================================================================================*/

    //region Get fill overloads

    public <T> List<T> getFill(Class type, String IdColumn, String tableToJoin) {
        return MySQLA_crud_getFill.getFillMain(type, conn, config.database, table, new String[]{IdColumn},
                new String[]{tableToJoin}, null, null);
    }

    public void getFill(Class type, String IdColumn, String tableToJoin, OnGetComplete callback) {
        MySQLA_crud_getFill.getFillMainParallel(type, conn, config.database, this.table, new String[]{IdColumn},
                new String[]{tableToJoin},null, callback);
    }

    public <T> List<T> getFill(Class type, String IdColumn, String tableToJoin, String sqlWhereFilter) {
        return MySQLA_crud_getFill.getFillMain(type, conn, config.database, table, new String[]{IdColumn},
                new String[]{tableToJoin}, sqlWhereFilter, null);
    }

    public void getFill(Class type, String IdColumn, String tableToJoin, String sqlWhereFilter,
                        OnGetComplete callback) {
        MySQLA_crud_getFill.getFillMainParallel(type, conn, config.database, this.table, new String[]{IdColumn},
                new String[]{tableToJoin}, sqlWhereFilter, callback);
    }

    public <T> List<T> getFill(Class type, String[] IdColumns, String[] tablesToJoin) {
        return MySQLA_crud_getFill.getFillMain(type, conn, config.database, table, IdColumns, tablesToJoin, null,
                null);
    }

    public void getFill(Class type, String[] IdColumns, String[] tablesToJoin, OnGetComplete callback) {
        MySQLA_crud_getFill.getFillMainParallel(type, conn, config.database, this.table, IdColumns, tablesToJoin,
                null, callback);
    }

    public <T> List<T> getFill(Class type, String[] IdColumns, String[] tablesToJoin, String sqlWhereFilter) {
        return MySQLA_crud_getFill.getFillMain(type, conn, config.database, table, IdColumns, tablesToJoin, sqlWhereFilter,
                null);
    }

    public void getFill(Class type, String[] IdColumns, String[] tablesToJoin, String sqlWhereFilter,
                               OnGetComplete callback) {
        MySQLA_crud_getFill.getFillMainParallel(type, conn, config.database, this.table, IdColumns, tablesToJoin,
                sqlWhereFilter, callback);
    }

    //endregion


    /*======================================================================================================\
                                          MIN, MAX, COUNT, AVERAGE, SUM
    \======================================================================================================*/

    //region Overloads

    public Number getCount() {
        return MySQLA_crud_getMetrics.getMetrics(conn, config.database, table, null, "COUNT",
                null, null);
    }

    public void getCount(OnComplete<Number> callback) {
        MySQLA_crud_getMetrics.getMetricsParallel(conn, config.database, table, null, "COUNT",
                null, callback);
    }

    public Number getCount(String sqlWhereFilter) {
        return MySQLA_crud_getMetrics.getMetrics(conn, config.database, table, null, "COUNT",
                sqlWhereFilter, null);
    }

    public void getCount(String sqlWhereFilter, OnComplete<Number> callback) {
        MySQLA_crud_getMetrics.getMetricsParallel(conn, config.database, table, null, "COUNT",
                sqlWhereFilter, callback);
    }

    public Number getSum(String column) {
        return MySQLA_crud_getMetrics.getMetrics(conn, config.database, table, column, "SUM",
                null, null);
    }

    public void getSum(String column, OnComplete<Number> callback) {
        MySQLA_crud_getMetrics.getMetricsParallel(conn, config.database, table, column, "SUM",
                null, callback);
    }

    public Number getSum(String column, String sqlWhereFilter) {
        return MySQLA_crud_getMetrics.getMetrics(conn, config.database, table, column, "SUM",
                sqlWhereFilter, null);
    }

    public void getSum(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        MySQLA_crud_getMetrics.getMetricsParallel(conn, config.database, table, column, "SUM",
                sqlWhereFilter, callback);
    }

    public Number getAvg(String column) {
        return MySQLA_crud_getMetrics.getMetrics(conn, config.database, table, column, "AVG",
                null, null);
    }

    public void getAvg(String column, OnComplete<Number> callback) {
        MySQLA_crud_getMetrics.getMetricsParallel(conn, config.database, table, column, "AVG",
                null, callback);
    }

    public Number getAvg(String column, String sqlWhereFilter) {
        return MySQLA_crud_getMetrics.getMetrics(conn, config.database, table, column, "AVG",
                sqlWhereFilter, null);
    }

    public void getAvg(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        MySQLA_crud_getMetrics.getMetricsParallel(conn, config.database, table, column, "AVG",
                sqlWhereFilter, callback);
    }

    public Number getMax(String column) {
        return MySQLA_crud_getMetrics.getMetrics(conn, config.database, table, column, "MAX",
                null, null);
    }

    public void getMax(String column, OnComplete<Number> callback) {
        MySQLA_crud_getMetrics.getMetricsParallel(conn, config.database, table, column, "MAX",
                null, callback);
    }

    public Number getMax(String column, String sqlWhereFilter) {
        return MySQLA_crud_getMetrics.getMetrics(conn, config.database, table, column, "MAX",
                sqlWhereFilter, null);
    }

    public void getMax(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        MySQLA_crud_getMetrics.getMetricsParallel(conn, config.database, table, column, "MAX",
                sqlWhereFilter, callback);
    }

    public Number getMin(String column) {
        return MySQLA_crud_getMetrics.getMetrics(conn, config.database, table, column, "MIN",
                null, null);
    }

    public void getMin(String column, OnComplete<Number> callback) {
        MySQLA_crud_getMetrics.getMetricsParallel(conn, config.database, table, column, "MIN",
                null, callback);
    }

    public Number getMin(String column, String sqlWhereFilter) {
        return MySQLA_crud_getMetrics.getMetrics(conn, config.database, table, column, "MIN",
                sqlWhereFilter, null);
    }

    public void getMin(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        MySQLA_crud_getMetrics.getMetricsParallel(conn, config.database, table, column, "MIN",
                sqlWhereFilter, callback);
    }

    //endregion


    /*======================================================================================================\
                                                  DATABASE ADD
    \======================================================================================================*/

    //region Database add overloads

    public <T> Object add (T element) {
        return MySQLA_crud_add.addMain(element, conn, config.database, table, null);
    }

    public <T> void add (T element, OnComplete<Object> callback) {
        MySQLA_crud_add.addMainParallel(element, conn, config.database, table, callback);
    }

    //endregion


    /*======================================================================================================\
                                                 DATABASE UPDATE
    \======================================================================================================*/

    //region Database update overloads

    public <T> Integer update (T element) {
        return MySQLA_crud_update.updateMain(element, conn, config.database, table, null);
    }

    public <T> void update (T element, OnComplete<Integer> callback) {
        MySQLA_crud_update.updateMainParallel(element, conn, config.database, table, callback);
    }

    //endregion


    /*======================================================================================================\
                                                 DATABASE DELETE
    \======================================================================================================*/

    //region Database delete overloads

    public Integer delete (String sqlWhere) {
        return MySQLA_crud_delete.deleteMain(conn, config.database, table, sqlWhere, null);
    }

    public void delete (String sqlWhere, OnComplete<Integer> callback) {
        MySQLA_crud_delete.deleteMainParallel(conn, config.database, table, sqlWhere, callback);
    }

    //endregion

    /*======================================================================================================\
                                             DATABASE TABLE OPERATIONS
    \======================================================================================================*/

    public boolean createTable (String tableName, String[] tableConfig) {
        return MySQLA_createTable.createTable(conn, config.database, tableName, tableConfig, null);
    }

    public void createTable (String tableName, String[] tableConfig, OnComplete callback) {
        MySQLA_createTable.createTableParallel(conn, config.database, tableName, tableConfig, callback);
    }

    public boolean dropTable (String tableName) {
        return MySQLA_createTable.dropTable(conn, config.database, tableName, null);
    }

    public void dropTable (String tableName, OnComplete callback) {
        MySQLA_createTable.dropTableParallel(conn, config.database, tableName, callback);
    }

}
