package com.company.mysqlaccess;

import com.company.mysqlaccess.models.MySQLAConfig;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class MySQLA_validators {

    public static boolean isTableSelected (String table, MySQLAConfig mySQLAConfig) {
        if (table == "") return false;
        return true;
    }

    public static boolean hasConnection (Connection conn) {
        if (conn == null) return false;
        return true;
    }

    public static boolean hasFetchedTableDetails (String database, String table,
                                                   Map<String, Map<String, List<String>>> allColumnNames) {
        if (allColumnNames.get(database) == null || allColumnNames.get(database).get(table) == null) return false;
        return true;
    }
}
