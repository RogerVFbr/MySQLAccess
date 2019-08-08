package com.company.mysqlaccess;

import java.sql.Connection;

public class MySQLA_validators {

    public static boolean isTableSelected (String table) {
        if (table == "") return false;
        return true;
    }

    public static boolean hasConnection (Connection conn) {
        if (conn == null) return false;
        return true;
    }

    public static boolean hasFetchedTableDetails (String database, String table) {
        if (MySQLA_tableProperties.getAllColumns(database, table) == null) return false;
        return true;
    }
}
