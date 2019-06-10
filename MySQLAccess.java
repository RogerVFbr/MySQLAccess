package com.company;

import java.lang.reflect.Field;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

public class MySQLAccess {

    /*======================================================================================================\
                                                 PRE INSTALLATION
    \======================================================================================================*/

    //region Pre installation instructions

    /*

    PRE INSTALLATION

    0 - Needs at least Java SDK 12.
    1 - To use local database install MySQL Community Server (https://dev.mysql.com/downloads/mysql/)
    2 - To manage your databases visually use MySQL Workbench (https://dev.mysql.com/downloads/workbench/)

    SQL CONNECTOR INSTALLATION INSTRUCTIONS

    1 - Go to https://dev.mysql.com/downloads/connector/
    2 - Download corresponding CONNECTOR
    3 - Create LIB folder in project
    4 - Copy JAR file from downloaded zip to LIB folder (Ex. mysql-connector-java-8.0.16.jar)

    5 - If in INTELLIJ:
        a) FILE > PROJECT STRUCTURE > LIBRARIES
        b) Click on plus sign
        c) Select Java
        d) Navigate to JAR file and apply

     */

    //endregion


    /*======================================================================================================\
                                             STATIC CLASS VARIABLES
    \======================================================================================================*/

    //region Static class variables

    private static final String TAG = "MySQLA";
    private static boolean logInfo = false;
    private static boolean logDetails = false;
    private static boolean logFetch = false;
    private static final String serverTimeZone =
            "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private static Map<String, Map<String, List<ColumnProps>>> tableProperties = new HashMap<>();
    private static Map<Map<Class, Set<String>>, Map<String, String>> columnFieldCorrelations = new HashMap<>();
    private static Map<String, Map<String, List<String>>> updateableColumns = new HashMap<>();
    private static Map<String, Map<String, List<String>>> allColumnNames = new HashMap<>();
    private static Map<String, Map<String, Map<String, String>>> allColumnTypes = new HashMap<>();
    private static Map<String, Map<String, String>> primaryKeys = new HashMap<>();
    private static Map<String, List<Class>> mySqlTypeEquivalency = new HashMap<>();

    //endregion

    //region Colors for logs

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    //endregion

    //region Static initialization

    static {
        List<Class> ints = new ArrayList<>(Arrays.asList(Integer.class, int.class, Long.class, long.class));
        List<Class> bools = new ArrayList<>(Arrays.asList(Boolean.class, boolean.class));
        List<Class> timestp = new ArrayList<>(Arrays.asList(java.sql.Timestamp.class));
        mySqlTypeEquivalency.put("int", ints);
        mySqlTypeEquivalency.put("integer", ints);
        mySqlTypeEquivalency.put("smallint", ints);
        mySqlTypeEquivalency.put("mediumint", ints);
        mySqlTypeEquivalency.put("bigint", Arrays.asList(Long.class, long.class));
        mySqlTypeEquivalency.put("tinyint", bools);
        mySqlTypeEquivalency.put("bool", bools);
        mySqlTypeEquivalency.put("boolean", bools);
        mySqlTypeEquivalency.put("float", Arrays.asList(Float.class, float.class));
        mySqlTypeEquivalency.put("double", Arrays.asList(Double.class, double.class));
        mySqlTypeEquivalency.put("decimal", Arrays.asList(java.math.BigDecimal.class));
        mySqlTypeEquivalency.put("date", Arrays.asList(java.sql.Date.class, java.util.Date.class));
        mySqlTypeEquivalency.put("datetime", timestp);
        mySqlTypeEquivalency.put("timestamp", timestp);
        mySqlTypeEquivalency.put("time", Arrays.asList(java.sql.Time.class));
    }

    //endregion


    /*======================================================================================================\
                                               INSTANCE VARIABLES
    \======================================================================================================*/

    //region Non-static instance variables

    private Connection conn;
    private String url = "";
    private String table = "";
    private Config config = new Config();

    //endregion


    /*======================================================================================================\
                                                  INTERFACES
    \======================================================================================================*/

    //region Interfaces

    public interface OnGetComplete<T> {
        void onSuccess(List<T> data);
        void onFailure();
    }

    public interface OnComplete<T> {
        void onSuccess(T feedback);
        void onFailure();
    }

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

            //Register JDBC Driver
            //Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver").newInstance();

            if (conn == null) conn = DriverManager.getConnection(url, user, password);

        } catch (SQLException e) {
            logError("Unable to connect to database " + database + " @ " + ip + ":" + port +
                    " with credentials " + user + " | " + password);
            logError(e.getMessage());
        }
    }

    //endregion


    /*======================================================================================================\
                                               GETTERS & SETTERS
    \======================================================================================================*/

    //region Getters & setters

    public void setTable(String tableName) {
        this.table = tableName;
    }

    public static void logDetails () {
        logDetails = true;
    }

    public static void logInfo () {
        logInfo = true;
    }

    public static void logFetch () {
        logFetch = true;
    }

    private static boolean isTableSelected (String table, Config config) {
        if (table == "") {
            logError("No table selected on database '" + config.database + "'. Use setTable(..tablename..) " +
                    "before executing MySQLAccess commands.");
            return false;
        }

        return true;
    }

    private static boolean hasConnection (Connection conn, String command, Config config) {
        if (conn == null) {
            logError("Unable to execute " + command + " command because there is no connection to '" +
                    config.database + "' database.");
            return false;
        }

        return true;
    }

    private static boolean hasFetchedTableDetails (String database, String table) {
        if (allColumnNames.get(database) == null || allColumnNames.get(database).get(table) == null) {
            logError("Could not fetch table details from database.");
            return false;
        }
        return true;
    }

    //endregion


    /*======================================================================================================\
                                                  DATABASE GET
    \======================================================================================================*/

    //region Adaptive get overloads

    public <T> List<T> get(Class<T> type) {
        return getMain(type, config, conn, table,null, null);
    }

    public <T> void get(Class<T> type, OnGetComplete callback) {
        get(type, null, callback);
    }

    public <T> List<T> get(Class<T> type, String sqlWhereFilter) {
        return getMain(type, config, conn, table, sqlWhereFilter, null);
    }

    public <T> void get(Class<T> type, String sqlWhereFilter, OnGetComplete<T> callback) {
        String table = this.table;
        Thread t = new Thread( () -> {
            getMain(type, config, conn, table, sqlWhereFilter, callback);
        });
        t.start();
    }

    //endregion

    //region Adaptive get main method

    private static <T> List<T> getMain(Class<T> type, Config config, Connection conn, String table,
                                       String sqlWhereFilter, OnGetComplete<T> callback) {

        List<T> returnData = new ArrayList<>();

        // ---> If no connection has been established, abort.
        if (!hasConnection(conn, "GET", config)) {
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> If no table is selected, abort.
        if (!isTableSelected(table, config)) {
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> Get table properties if not already present.
        getTableProperties(config, conn, table);

        // ---> If table properties could not be fetched, abort
        if (!hasFetchedTableDetails(config.database, table)) {
            if (callback != null) callback.onFailure();
            return returnData;
        }

        Map<String, String> propertyMap = getColumnFieldCorrelation(type,
                allColumnNames.get(config.database).get(table), config.database, table);

        // ---> Build SQL query
        List<String> columns = new ArrayList<>(propertyMap.keySet());
        String columnNames = columns == null || columns.size() == 0 ? "*" : String.join(", ", columns);
        String query = "select " + columnNames + " from " + table;
        if (sqlWhereFilter != null) query += " where " + sqlWhereFilter;


        // ---> Execute query on connection
        ResultSet resultSet = null;
        try {
            Statement st = conn.createStatement();
            logInfo("Executing get query at '" + table +"': " + query);
            resultSet = st.executeQuery(query);

        } catch (SQLException e) {
            logError("Unable to create connection statement.");
            if (callback != null) callback.onFailure();
            e.printStackTrace();
            return returnData;
        }

        if (!buildListFromRetrievedData(type, resultSet, returnData, propertyMap)) {
            if (callback != null) callback.onFailure();
            return returnData;
        };

        if (callback != null) callback.onSuccess(returnData);
        return returnData;
    }

    //endregion


    /*======================================================================================================\
                                            DATABASE GET FILL (JOIN)
    \======================================================================================================*/

    //region Get fill overloads

    public <T> List<T> getFill(Class type, String IdColumn, String tableToJoin) {
        return getFillMain(type, config, conn, table, new String[]{IdColumn}, new String[]{tableToJoin},
                null, null);
    }

    public void getFill(Class type, String IdColumn, String tableToJoin, OnGetComplete callback) {
        String currentTable = this.table;
        Thread t = new Thread( () -> {
            getFillMain(type, config, conn, currentTable, new String[]{IdColumn}, new String[]{tableToJoin},
                    null, callback);
        });
        t.start();
    }

    public <T> List<T> getFill(Class type, String IdColumn, String tableToJoin, String sqlWhereFilter) {
        return getFillMain(type, config, conn, table, new String[]{IdColumn}, new String[]{tableToJoin},
                sqlWhereFilter, null);
    }

    public void getFill(Class type, String IdColumn, String tableToJoin, String sqlWhereFilter,
                        OnGetComplete callback) {
        String currentTable = this.table;
        Thread t = new Thread( () -> {
            getFillMain(type, config, conn, currentTable, new String[]{IdColumn}, new String[]{tableToJoin},
                    sqlWhereFilter, callback);
        });
        t.start();
    }

    public <T> List<T> getFill(Class type, String[] IdColumns, String[] tablesToJoin) {
        return getFillMain(type, config, conn, table, IdColumns, tablesToJoin, null, null);
    }

    public void getFill(Class type, String[] IdColumns, String[] tablesToJoin, OnGetComplete callback) {
        String currentTable = this.table;
        Thread t = new Thread( () -> {
            getFillMain(type, config, conn, currentTable, IdColumns, tablesToJoin, null, callback);
        });
        t.start();
    }

    public <T> List<T> getFill(Class type, String[] IdColumns, String[] tablesToJoin, String sqlWhereFilter) {
        return getFillMain(type, config, conn, table, IdColumns, tablesToJoin, sqlWhereFilter, null);
    }

    public void getFill(Class type, String[] IdColumns, String[] tablesToJoin, String sqlWhereFilter,
                               OnGetComplete callback) {
        String currentTable = this.table;
        Thread t = new Thread( () -> {
            getFillMain(type, config, conn, currentTable, IdColumns, tablesToJoin, sqlWhereFilter, callback);
        });
        t.start();
    }

    //endregion

    //region Get fill main method

    private static <T> List<T> getFillMain(Class<T> type, Config config, Connection conn, String table,
                                           String[] IdColumns, String[] tablesToJoin, String sqlWhereFilter,
                                           OnGetComplete<T> callback) {

        List<T> returnData = new ArrayList<>();

        // ---> If no connection has been established, abort.
        if (!hasConnection(conn, "GETFILL", config)) {
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> If no table is selected, abort.
        if (!isTableSelected(table, config)) {
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> Tables to join contains main table, abort.
        List<String> tablesToJoinList = Arrays.asList(tablesToJoin);
        if (tablesToJoinList.contains(table)) {
            logError("GETFILL - Selected table must not be specified to be joined.");
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> Tables to join contains duplicated table, abort.
        if (tablesToJoinList.size() != new HashSet<>(tablesToJoinList).size()) {
            logError("GETFILL - Tables to join must not contain duplicate table names.");
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> Number of IdColumns and table names are not equal, abort.
        if (IdColumns.length != tablesToJoin.length) {
            logError("GETFILL - Method call must provide the same number of Id columns and table names.");
            if (callback != null) callback.onFailure();
            return returnData;
        }

        // ---> Get table properties if not already present.
        getTableProperties(config, conn, table);
        for (String tableToJoinName : tablesToJoin) {
            getTableProperties(config, conn, tableToJoinName);
        }

        // ---> If unable to fetch details for one or more tables, abort.
        if (!hasFetchedTableDetails(config.database, table)) {
            if (callback != null) callback.onFailure();
            return returnData;
        }
        for (String tableToJoinName : tablesToJoin) {
            if (!hasFetchedTableDetails(config.database, tableToJoinName)) {
                if (callback != null) callback.onFailure();
                return returnData;
            }
        }

        // ---> Calculate column/field correlation from joined column names
        List<String> allColumns = new ArrayList<>(allColumnNames.get(config.database).get(table));
        for (String tableName : tablesToJoin) {
            allColumns.addAll(allColumnNames.get(config.database).get(tableName));
        }
        Map<String, String> propertyMap = getColumnFieldCorrelation(type,
                allColumns, config.database, table);

        // ---> Build SQL query
        String from = " from " + table + " ";
        String join = "";
        for (int x = 0; x<tablesToJoin.length; x++) {
            String tableToJoinPrimaryKey = primaryKeys.get(config.database).get(tablesToJoin[x]);
            join += "left join " + tablesToJoin[x] + " on " + table + "." + IdColumns[x] + "=" + tablesToJoin[x] + "." +
                    tableToJoinPrimaryKey + " ";
        }

        String where = "where " + sqlWhereFilter + " ";
        String query = "select ";

        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            if (allColumnNames.get(config.database).get(table).contains(entry.getKey())) {
                query += table + "." + entry.getKey() + ", ";
            }
            else {
                for (String tableName : tablesToJoin) {
                    if (allColumnNames.get(config.database).get(tableName).contains(entry.getKey())) {
                        query += tableName + "." + entry.getKey() + ", ";
                        break;
                    }
                }
            }
        }

        query = query.replaceAll(", $", "");
        query += from + join + (sqlWhereFilter == null ? "" : where);

        // ---> Execute query on connection
        ResultSet resultSet = null;
        try {
            Statement st = conn.createStatement();
            logInfo("Executing getFill query at '" + table +"': " + query);
            resultSet = st.executeQuery(query);

        } catch (SQLException e) {
            logError("Unable to create connection statement.");
            if (callback != null) callback.onFailure();
            e.printStackTrace();
            return returnData;
        }

        if (!buildListFromRetrievedData(type, resultSet, returnData, propertyMap)) {
            if (callback != null) callback.onFailure();
            return returnData;
        };

        if (callback != null) callback.onSuccess(returnData);
        return returnData;
    }

    //endregion


    /*======================================================================================================\
                                          MIN, MAX, COUNT, AVERAGE, SUM
    \======================================================================================================*/

    //region Overloads

    public Number getCount() {
        return getMetrics(config, conn, table, null, "COUNT", null, null);
    }

    public void getCount(OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            getMetrics(config, conn, table, null, "COUNT", null, callback);
        });
        t.start();
    }

    public Number getCount(String sqlWhereFilter) {
        return getMetrics(config, conn, table, null, "COUNT", sqlWhereFilter, null);
    }

    public void getCount(String sqlWhereFilter, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            getMetrics(config, conn, table, null, "COUNT", sqlWhereFilter, callback);
        });
        t.start();
    }

    public Number getSum(String column) {
        return getMetrics(config, conn, table, column, "SUM", null, null);
    }

    public void getSum(String column, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            getMetrics(config, conn, table, column, "SUM", null, callback);
        });
        t.start();
    }

    public Number getSum(String column, String sqlWhereFilter) {
        return getMetrics(config, conn, table, column, "SUM", sqlWhereFilter, null);
    }

    public void getSum(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            getMetrics(config, conn, table, column, "SUM", sqlWhereFilter, callback);
        });
        t.start();
    }

    public Number getAvg(String column) {
        return getMetrics(config, conn, table, column, "AVG", null, null);
    }

    public void getAvg(String column, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            getMetrics(config, conn, table, column, "AVG", null, callback);
        });
        t.start();
    }

    public Number getAvg(String column, String sqlWhereFilter) {
        return getMetrics(config, conn, table, column, "AVG", sqlWhereFilter, null);
    }

    public void getAvg(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            getMetrics(config, conn, table, column, "AVG", sqlWhereFilter, callback);
        });
        t.start();
    }

    public Number getMax(String column) {
        return getMetrics(config, conn, table, column, "MAX", null, null);
    }

    public void getMax(String column, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            getMetrics(config, conn, table, column, "MAX", null, callback);
        });
        t.start();
    }

    public Number getMax(String column, String sqlWhereFilter) {
        return getMetrics(config, conn, table, column, "MAX", sqlWhereFilter, null);
    }

    public void getMax(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            getMetrics(config, conn, table, column, "MAX", sqlWhereFilter, callback);
        });
        t.start();
    }

    public Number getMin(String column) {
        return getMetrics(config, conn, table, column, "MIN", null, null);
    }

    public void getMin(String column, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            getMetrics(config, conn, table, column, "MIN", null, null);
        });
        t.start();
    }

    public Number getMin(String column, String sqlWhereFilter) {
        return getMetrics(config, conn, table, column, "MIN", sqlWhereFilter, null);
    }

    public void getMin(String column, String sqlWhereFilter, OnComplete<Number> callback) {
        Thread t = new Thread( () -> {
            getMetrics(config, conn, table, column, "MIN", sqlWhereFilter, callback);
        });
        t.start();
    }

    //endregion

    //region Main method

    private static Number getMetrics(Config config, Connection conn, String selectedTable, String column,
                                   String operation, String sqlWhereFilter, OnComplete<Number> callback) {

        String table = selectedTable;
        Object returnData = null;

        // ---> If no connection has been established, abort.
        if (!hasConnection(conn, "GETMETRICS", config)) {
            if (callback != null) callback.onFailure();
            return (Number) returnData;
        }

        // ---> If no table is selected, abort.
        if (!isTableSelected(table, config)) {
            if (callback != null) callback.onFailure();
            return (Number) returnData;
        }

        // ---> Get table properties if not already present.
        getTableProperties(config, conn, table);

        // ---> If unable to fetch table details, abort
        if (!hasFetchedTableDetails(config.database, table)) {
            if (callback != null) callback.onFailure();
            return (Number) returnData;
        }

        // ---> If no column has been passed, use primary key as default
        if (column == null) {
            column = primaryKeys.get(config.database).get(table);
        }

        // ---> If column is non-existent, abort.
        if (!allColumnNames.get(config.database).get(table).contains(column)) {
            logError("GETMETRICS - Column '" + column + "' on table '" + table + "' @ '" + config.database +
                    "' could not be found.");
            if (callback != null) callback.onFailure();
            return (Number) returnData;
        }

        // ---> Build query
        String query = "select " + operation + "(" + column + ") from " + table + " " +
                (sqlWhereFilter == null ? "" : sqlWhereFilter);

        // ---> Execute query on connection
        ResultSet resultSet = null;
        try {
            Statement st = conn.createStatement();
            resultSet = st.executeQuery(query);
            resultSet.next();
            returnData = resultSet.getObject(1);
        } catch (SQLException e) {
            logError("Unable to create connection statement.");
            if (callback != null) callback.onFailure();
            e.printStackTrace();
            return (Number) returnData;
        }

        if (sqlWhereFilter == null) {
            logDetails("GETMETRICS - " + operation + " (Table: '" + table + "', Column: '" + column +
                    "') operation returned value: " + returnData + " | " + returnData.getClass().getName());
        } else {
            logDetails("GETMETRICS - " + operation + " (Table: '" + table + "', Column: '" + column
                    + "' / Filter: '" + sqlWhereFilter +
                    "') operation returned value: " + returnData + " | " + returnData.getClass().getName());
        }

        if (callback != null) callback.onSuccess((Number) returnData);
        return (Number) returnData;
    }

    //endregion


    /*======================================================================================================\
                                                  DATABASE ADD
    \======================================================================================================*/

    //region Database add overloads

    public <T> void add (T element) {
        addMain(element, config, conn, table, null);
    }

    public <T> void add (T element, OnComplete callback) {
        addMain(element, config, conn, table, callback);
    }

    //endregion

    //region Database add main code

    private static <T> void addMain (T element, Config config, Connection conn, String selectedTable,
                                     OnComplete callback) {

        // ---> If no connection has been established, abort.
        if (!hasConnection(conn, "ADD", config)) {
            if (callback != null) callback.onFailure();
            return;
        }

        // ---> If no table is selected, abort.
        String table = selectedTable;
        if (!isTableSelected(table, config)) {
            if (callback != null) callback.onFailure();
            return;
        }

        // ---> Initiate code execution os separate thread
        Thread t = new Thread( () -> {

            // ---> Get table properties of not already present
            getTableProperties(config, conn, table);

            // ---> If unable to fetch table details, abort
            if (!hasFetchedTableDetails(config.database, table)) {
                if (callback != null) callback.onFailure();
                return;
            }

            // ---> Figure out best correlation between database column names and model fields
            Map<String, String> propertyMap = getColumnFieldCorrelation(element.getClass(),
                    updateableColumns.get(config.database).get(table), config.database, table);

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
                logInfo("Executing add query at '" + table +"': " + query);
                PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                int result = ps.executeUpdate();
                if (result == 0) {
                    logError("Failed to obtain insertion confirmation on table '" + table + "'!");
                    if (callback != null) callback.onFailure();
                    return;
                }
                Object key = null;
                try (ResultSet generatedObject = ps.getGeneratedKeys();) {
                    if (generatedObject.next()) {
                        key = generatedObject.getObject(1);
                    }
                }
                if (key == null){
                    logInfo("Successfully written new row to table '" + table + ".");
                }
                else {
                    logInfo("Successfully written new row to table '" + table + "' with auto-generated key: " + key);
                }
                if (callback != null) callback.onSuccess(key);

            } catch (SQLException e) {
                logError("Failed to create new row on table '" + table + "'.");
                if (callback != null) callback.onFailure();
                e.printStackTrace();
            }
        });

        t.start();
    }

    //endregion


    /*======================================================================================================\
                                                 DATABASE UPDATE
    \======================================================================================================*/

    //region Database update overloads

    public <T> void update (T element) {
        updateMain(element, config, conn, table, null);
    }

    public <T> void update (T element, OnComplete callback) {
        updateMain(element, config, conn, table, callback);
    }

    //endregion

    //region Database update main code

    private static <T> void updateMain (T element, Config config, Connection conn, String selectedTable,
                                        OnComplete callback) {

        // ---> If no connection has been established, abort.
        if (!hasConnection(conn, "UPDATE", config)) {
            if (callback != null) callback.onFailure();
            return;
        }

        // ---> If no table is selected, abort
        String table = selectedTable;
        if (!isTableSelected(table, config)) {
            if (callback != null) callback.onFailure();
            return;
        }

        Thread t = new Thread( () -> {

            // ---> Get table properties of not already present
            getTableProperties(config, conn, table);

            // ---> If unable to fetch table details, abort
            if (!hasFetchedTableDetails(config.database, table)) {
                if (callback != null) callback.onFailure();
                return;
            }

            // ---> Use updatable columns + primary key
            List<String> relevantColumns = updateableColumns.get(config.database).get(table);
            if (!relevantColumns.contains(primaryKeys.get(config.database).get(table))) {
                relevantColumns.add(primaryKeys.get(config.database).get(table));
            }

            // ---> Figure out best correlation between database column names and model fields
            Map<String, String> propertyMap = getColumnFieldCorrelation(element.getClass(), relevantColumns,
                    config.database, table);

            String primaryKey = primaryKeys.get(config.database).get(table);

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
                logError("Failed to modify row on database. Primary key equivalent field on model is non-existent, " +
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
                logInfo("Executing update query at '" + table +"': " + query);
                int result = st.executeUpdate(query);

                if (result == 0) {
                    logError("Failed to update row(s) on table '" + table + "'!");
                    if (callback != null) callback.onFailure();
                    return;
                }

                logInfo("Successfully updated " + result + " row(s) on table '" + table + "': " + element);
                if (callback != null) callback.onSuccess(result);

            } catch (SQLException e) {
                logError("Failed to update row on table '" + table + "'.");
                if (callback != null) callback.onFailure();
                e.printStackTrace();
            }
        });
        t.start();


    }

    //endregion


    /*======================================================================================================\
                                                 DATABASE DELETE
    \======================================================================================================*/

    //region Database delete overloads

    public void delete (String sqlWhere) {
        deleteMain(sqlWhere, config, conn, table, null);
    }

    public void delete (String sqlWhere, OnComplete callback) {
        deleteMain(sqlWhere, config, conn, table, callback);
    }

    //endregion

    //region Database delete main code

    private static void deleteMain (String sqlWhere, Config config, Connection conn, String selectedTable,
                                    OnComplete callback) {

        // ---> If no connection has been established, abort.
        if (!hasConnection(conn, "DELETE", config)) {
            if (callback != null) callback.onFailure();
            return;
        }

        // ---> If no table is selected, abort.
        String table = selectedTable;
        if (!isTableSelected(table, config)) {
            if (callback != null) callback.onFailure();
            return;
        }

        // ---> Build SQL query
        String query = "delete from " + table + " where " + sqlWhere;

        // ---> Execute query on connection
        try {

            Statement st = conn.createStatement();
            logInfo("Executing delete query at '" + table +"': " + query);
            int result = st.executeUpdate(query);

            if (result == 0) {
                logInfo("Delete query didn't affect any rows.");
                if (callback != null) callback.onFailure();
                return;
            }

            logInfo("Successfully deleted " + result + " row(s).");
            if (callback != null) callback.onSuccess(result);

        } catch (SQLException e) {
            logError("Failed to delete rows on database.");
            if (callback != null) callback.onFailure();
            e.printStackTrace();
        }
    }

    //endregion


    /*======================================================================================================\
                                                AUXILIARY METHODS
    \======================================================================================================*/

    //region Auxiliary methods

    private static Map<String, String> getColumnFieldCorrelation (Class type, List<String> columns, String database,
                                                                  String tableName) {

        // ---> Check if this correlation has been previously calculated and returns from memory if so.
        Map<Class, Set<String>> correlationKey = Map.of(type, new HashSet<>(columns));
        if (columnFieldCorrelations.containsKey(correlationKey))
            return columnFieldCorrelations.get(correlationKey);

        // ---> New column/field correlation, initiate calculations.
        List<Map<String, Object>> correlations = new ArrayList<>();

        // ---> Build class' fields list
        List<String> fields = Arrays.stream(type.getDeclaredFields()).map(x -> x.getName())
                .collect(Collectors.toList());

        // ---> Calculate all possible columns/field correlations
        for (String column : columns) {
            buildCorrelationsMap(correlations, type, fields, column, database, tableName);
        }

        // ---> Build final property map with best correlations
        Map<String, String> propertyMap = buildFinalPropertyMap(correlations);
        logInfo("Saving new column/field correlation between table '" + tableName + "' and model '"
                + type.getName() + "': " + propertyMap);
        columnFieldCorrelations.put(correlationKey, propertyMap);
        return propertyMap;
    }

    private static void buildCorrelationsMap (
            List<Map<String, Object>> correlations, Class type, List<String> fields, String columnName,
            String database, String table) {

        String columnType = allColumnTypes.get(database).get(table).get(columnName);
        for (String fieldName : fields) {

            // ---> If column type is not convertible to non-string field type, do not consider.
            if (mySqlTypeEquivalency.containsKey(columnType)) {
                Class fieldType = null;
                try {
                    fieldType = type.getDeclaredField(fieldName).getType();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
                if (!mySqlTypeEquivalency.get(columnType).contains(fieldType) && fieldType != String.class) {
                    continue;
                }

            }

            int currentLevenstein = getLevensteinDistance(columnName, fieldName);
            Map<String, Object> newMap = new HashMap<>();
            newMap.put("levenstein", currentLevenstein);
            newMap.put("column", columnName);
            newMap.put("field", fieldName);
            correlations.add(newMap);
        }
    }

    private static Map<String, String> buildFinalPropertyMap (List<Map<String, Object>> correlations) {
        Map<String, String> propertyMap = new HashMap<>();

        // ---> Order correlations by best correlations (lowest Levenstein value)
        correlations = correlations.stream()
                .sorted(Comparator.comparingInt(o -> (Integer) o.get("levenstein")))
                .collect(Collectors.toList());

        // ---> Iterate on correlations list to build final property correlation Hashmap
        for (Map<String, Object> entry : correlations) {
            if (!propertyMap.containsKey(entry.get("column")) && !propertyMap.containsValue(entry.get("field"))) {
                propertyMap.put((String) entry.get("column"), (String) entry.get("field"));
            }
        }

        return propertyMap;
    }

    private static <T> T instantiateDataObject(Class<T> type) {
        T obj = null;

        try {
            obj = type.getConstructor().newInstance();

        } catch (Exception e) {
            String[] classComps = type.toString().split("\\.");
            String classPath = Arrays.stream(classComps).skip(1).collect(Collectors.joining("."));
            String className = Arrays.stream(classComps).skip(classComps.length-1).findFirst().get();
            logError("Model class constructor needs to have overload without parameters -> " + classPath);
            logError("Include on model class' constructors: public "+ className + " () {}");
        }

        return obj;
    }

    private static <T> void updateFieldValue (T data, Set<String> incompatibleFields, String columnName,
                                              String fieldName, ResultSet resultSet) {

        if (incompatibleFields.contains(fieldName)) return;

        Field field = null;

        try {
            field = data.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            if (field.getType() == byte.class)         field.set(data, resultSet.getByte(columnName));
            else if (field.getType() == short.class)   field.set(data, resultSet.getShort(columnName));
            else if (field.getType() == int.class)     field.set(data, resultSet.getInt(columnName));
            else if (field.getType() == long.class)    field.set(data, resultSet.getLong(columnName));
            else if (field.getType() == float.class)   field.set(data, resultSet.getFloat(columnName));
            else if (field.getType() == double.class)  field.set(data, resultSet.getDouble(columnName));
            else if (field.getType() == boolean.class) field.set(data, resultSet.getBoolean(columnName));
            else if (field.getType() == String.class)  field.set(data, resultSet.getString(columnName));
            else field.set(data, field.getType().cast(resultSet.getObject(columnName)));
            return;

        } catch (NoSuchFieldException e) {
            StackTraceElement frame = e.getStackTrace()[1];
            logError("Required field (" + e.getMessage() + ") doesn't exist on model. File: "
                    + frame.getFileName() + " | Method: " + frame.getMethodName() + " | Line number: "
                    + frame.getLineNumber());

        } catch (Exception e) {
            String columnType = e.getMessage().replaceAll("Cannot cast ", "").split(" to ")[0];
            String fieldType = field.getType().toString().replaceAll("class ", "");
            logError("Table column and model field type mismatch -> Column: " + columnName + " (" + columnType + ") | " +
                    "Field: " + fieldName + " (" + fieldType + ")");
        }
        incompatibleFields.add(fieldName);
    }

    private static <T> boolean buildListFromRetrievedData(Class<T> type, ResultSet resultSet, List<T> returnData,
                                                       Map<String, String> propertyMap) {

        Set<String> incompatibleFields = new HashSet<>();

        // ---> Build list from retrieved data
        while (true) {
            try {
                if (!resultSet.next()) break;

            } catch (SQLException e) {
                e.printStackTrace();
                continue;
            }

            // ---> Build single data object from retrieved row
            T data = instantiateDataObject(type);
            if (data == null) {
                return false;
            }

            for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
                updateFieldValue(data, incompatibleFields, entry.getKey(), entry.getValue(), resultSet);
            }

            logDetails(data.toString());
            returnData.add(data);
        }
        return true;
    }

    private static void getTableProperties (Config config, Connection conn, String table) {
        getAllTablesProperties(config, conn, table);
        getSingleTableProperties(config, conn, table);
    }

    private static void getAllTablesProperties (Config config, Connection conn, String tableName) {

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
            logFetch("Fetching details for all tables @ database '" + config.database + "'...");
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
                logFetch("Details for table '" + table + "' ...");
                saveFetchedTablesDetails(config, table, newTableProps);
            }

        } catch (SQLException e) {
            logError("Unable to retrieve details from database '" + config.database + "'.");
            e.printStackTrace();
        }
    }

    private static void getSingleTableProperties(Config config, Connection conn, String table) {

        // ---> If information has been fetched before, abort execution.
        if (tableProperties.containsKey(config.database)) {
            if (tableProperties.get(config.database).containsKey(table)) return;
        }

        // ---> Build query and prepare receiver list
        String query = "show full columns from " + config.database + "." + table;
        List<ColumnProps> newTableProps = new ArrayList<>();

        // ---> Execute query on database
        try {
            logFetch("Fecthing details for table '" + table + "' @ '" + config.database + "'...");
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
            logError("Table '" + table + "' @ '" + config.database + "' doesn't exist.");

        } catch (SQLException e) {
            logError("Unable to retrieve details from table '" + table + "' @ '" + config.database + "'.");
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
        if (MySQLAccess.allColumnNames.containsKey(config.database)) {
            Map propUpdate = new HashMap(MySQLAccess.allColumnNames.get(config.database));
            propUpdate.put(table, allColumnNames);
            MySQLAccess.allColumnNames.replace(config.database, propUpdate);
        }
        else {
            MySQLAccess.allColumnNames.put(config.database, Map.of(table, allColumnNames));
        }

        // ---> Extract and store column types
        List<String> allColumnTypes = newTableProps.stream()
                .map(x -> x.type.replaceAll("\\(.*\\)", "")).collect(Collectors.toList());
        Map<String, String> columnTypes = new HashMap<>();
        for(int x = 0; x<allColumnNames.size(); x++) {
            columnTypes.put(allColumnNames.get(x), allColumnTypes.get(x));
        }
        if (MySQLAccess.allColumnTypes.containsKey(config.database)) {
            Map propUpdate = new HashMap(MySQLAccess.allColumnTypes.get(config.database));
            propUpdate.put(table, columnTypes);
            MySQLAccess.allColumnTypes.replace(config.database, propUpdate);
        }
        else {
            MySQLAccess.allColumnTypes.put(config.database, Map.of(table, columnTypes));
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
            Map propUpdate = new HashMap(MySQLAccess.updateableColumns.get(config.database));
            propUpdate.put(table, updatableColumnNames);
            MySQLAccess.updateableColumns.replace(config.database, propUpdate);
        }
        else {
            updateableColumns.put(config.database, Map.of(table, updatableColumnNames));
        }

        // ---> Extract and store primary key
        String primaryKey = newTableProps.stream().filter(x -> x.key.contains("PRI")).findFirst().get().field;
        if (primaryKeys.containsKey(config.database)) {
            Map propUpdate = new HashMap(MySQLAccess.primaryKeys.get(config.database));
            propUpdate.put(table, primaryKey);
            MySQLAccess.primaryKeys.replace(config.database, propUpdate);
        }
        else {
            primaryKeys.put(config.database, Map.of(table, primaryKey));
        }

        // ---> Inform dev
        logFetch("Primary key: " + primaryKey);
        logFetch("Columns/types: " + columnTypes);
        logFetch("Updatable columns (no auto-increment, no defaults): " + updatableColumnNames);
    }

    //endregion


    /*======================================================================================================\
                                               LEVENSTEIN DISTANCE
    \======================================================================================================*/

    //region Levenstein Distance methods

    private static int getLevensteinDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                }
                else if (j == 0) {
                    dp[i][j] = i;
                }
                else {
                    dp[i][j] = min(dp[i - 1][j - 1]
                                    + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    private static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private static int min(int... numbers) {
        return Arrays.stream(numbers)
                .min().orElse(Integer.MAX_VALUE);
    }

    //endregion


    /*======================================================================================================\
                                                       LOG
    \======================================================================================================*/

    //region Loggers

    private static void log (String message) {
        System.out.println(TAG + " - " + message);
    }

    private static void logFetch (String message) {
        if (!logFetch) return;
        System.out.println(ANSI_YELLOW + TAG + " - " + message + ANSI_RESET);
    }

    private static void logInfo (String message) {
        if (!logInfo) return;
        System.out.println(ANSI_GREEN + TAG + " - " + message + ANSI_RESET);
    }

    private static void logDetails (String message) {
        if (!logDetails) return;
        System.out.println(ANSI_CYAN + TAG + " - " + message + ANSI_RESET);
    }

    private static void logError (String message) {
        System.out.println(ANSI_PURPLE + TAG + " - " + message + ANSI_RESET);
    }

    //endregion


    /*======================================================================================================\
                                          CONNECTION CONFIGURATION OBJECT
    \======================================================================================================*/

    public static class Config {

        public String ip;
        public int port;
        public String database;
        public String user;
        public String password;

        public Config() {};

        public Config(String ip, int port, String database, String user, String password) {
            this.ip = ip;
            this.port = port;
            this.database = database;
            this.user = user;
            this.password = password;
        }
    }

    /*======================================================================================================\
                                            TABLE PROPERTIES OBJECT
    \======================================================================================================*/

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
