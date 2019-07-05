package com.company.mysqlaccess.models;

import java.time.Instant;
import java.util.Objects;

public class MySQLACache {
    public String database;
    public String table;
    public String query;
    public Object data;

    public MySQLACache(String database, String table, String query, Object data) {
        this.database = database;
        this.table = table;
        this.query = query;
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MySQLACache that = (MySQLACache) o;
        return Objects.equals(database, that.database) &&
               Objects.equals(table, that.table) &&
               Objects.equals(query, that.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(database, table, query);
    }

    @Override
    public String toString() {
        return "\n  MySQLACache{" +
                "\n     database='" + database + '\'' +
                "\n     table='" + table + '\'' +
                "\n     query='" + query + '\'' +
                "\n     data=" + data +
                '}';
    }
}
