package com.company.mysqlaccess;

import com.company.mysqlaccess.models.MySQLACache;
import java.util.*;

public class MySQLA_cache {

    private static Map<String, Map<String, Integer>> cacheable = new HashMap<>();
    private static List<MySQLACache> cache = new ArrayList<>();

    public static void setCache(String database, String table, int expirationTimeInSeconds) {
        if (table.equals(null) || table.equals("")) {
            MySQLA_loggers.logError("CACHE - Could not set caching on database '" + database + "', no table selected.");
            return;
        }

        if (expirationTimeInSeconds <=0) {
            MySQLA_loggers.logError("CACHE - Could not set cache on table '" + table + "' @ '" + database +
                    "', expiration time in seconds must be greater than zero.");
            return;
        }

        if (cacheable.containsKey(database)) {
            if (!cacheable.get(database).containsKey(table)) {
                Map<String, Integer> updatedTables = new HashMap<>(cacheable.get(database));
                updatedTables.put(table, expirationTimeInSeconds);
                cacheable.replace(database, updatedTables);
                MySQLA_loggers.logInfo("CACHE - " + expirationTimeInSeconds + "s cache activated for table '"
                        + table + "' @ '" + database + "'");
            }
            else if (cacheable.get(database).get(table) != expirationTimeInSeconds) {
                Map<String, Integer> updatedTables = new HashMap<>(cacheable.get(database));
                updatedTables.remove(table);
                updatedTables.put(table, expirationTimeInSeconds);
                cacheable.replace(database, updatedTables);
                MySQLA_loggers.logInfo("CACHE - Updated cache time to " + expirationTimeInSeconds + "s for table '"
                        + table + "' @ '" + database + "'");
            }
        }
        else {
            Map<String, Integer> newTable = Map.of(table, expirationTimeInSeconds);
            cacheable.put(database, newTable);
            MySQLA_loggers.logInfo("CACHE - " + expirationTimeInSeconds + "s cache activated for table '"
                    + table + "' @ '" + database + "'");
        }
    }

    public static void stopCache(String database, String table) {
        if (table.equals(null) || table.equals("")) {
            MySQLA_loggers.logError("CACHE - Could not stop caching on database '" + database
                    + "', no table selected.");
            return;
        }
        synchronized (cacheable) {
            if (cacheable.containsKey(database) && cacheable.get(database).containsKey(table)) {
                deleteCache(database, table);
                Map<String, Integer> updatedTables = new HashMap<>(cacheable.get(database));
                updatedTables.remove(table);
                cacheable.replace(database, updatedTables);
                MySQLA_loggers.logInfo("CACHE - Stopped cache for table '" + table + "' @ '" + database + "'");
                return;
            }
            MySQLA_loggers.logError("CACHE - Could not stop cache for table '" + table + "' @ '" + database
                    + "' for it hadn't been enabled.");
        }
    }

    public static void storeToCache (String database, String table, String query, Object data) {
        boolean isCacheable = cacheable.containsKey(database) && cacheable.get(database).containsKey(table);
        if (!isCacheable) return;
        MySQLACache newCache = new MySQLACache(database, table, query, data);
        int cacheIndex = cache.indexOf(newCache);
        if (cacheIndex != -1) return;
        cache.add(newCache);
        int expiration = cacheable.get(database).get(table);
        Thread t = new Thread( () -> {
            try {
                Thread.sleep(expiration*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (cache){
                if (cache.contains(newCache)) {
                    cache.remove(newCache);
                    MySQLA_loggers.logInfo("CACHE - " + expiration + "s cache expired for table '"
                            + table + "' @ '" + database
                            + "' from query: '" + query + "'");
                }
            }
        });
        t.setDaemon(true);
        t.start();
        MySQLA_loggers.logInfo("CACHE - Added new " + expiration + "s cache for table '"
                + table + "' @ '" + database
                + "' from query: '" + query + "'");
    }

    public static void deleteCache(String database, String table) {
        boolean isCacheable = cacheable.containsKey(database) && cacheable.get(database).containsKey(table);
        if (!isCacheable) return;
        synchronized (cache) {
            int currentEntries = cache.size();
            cache.removeIf(c -> c.database.equals(database) && c.table.equals(table));
            int finalEntries = cache.size();
            if (currentEntries != finalEntries)
                MySQLA_loggers.logInfo("CACHE - Cleared " + (currentEntries-finalEntries) + " cache(s) entry for table '"
                        + table + "' @ '" + database + "'");
        }
    }

    public static <T> List<T> isCacheAvailable (String database, String table, String query) {
        synchronized (cache) {
            int cacheIndex = checkCache(database, table, query);
            if (cacheIndex == -1) return null;
            List<T> cacheData = (List<T>) cache.get(cacheIndex).data;
            String type = (cacheData.size()>0) ? (" of type '" + cacheData.get(0).getClass().toString() + "'") : "";
            MySQLA_loggers.logInfo("CACHE - Retrieving cache (" + cacheData.size() + " items" + type + ") for table '"
                    + table + "' @ '" + database + "' for query: '" + query + "'");
            return cacheData;
        }
    }

    public static Number isNumberCacheAvailable(String database, String table, String query) {
        synchronized (cache) {
            int cacheIndex = checkCache(database, table, query);
            if (cacheIndex == -1) return null;
            MySQLA_loggers.logInfo("CACHE - Retrieving cache for table '" + table + "' @ '" + database
                    + "' for query: '" + query + "'");
            return (Number) cache.get(cacheIndex).data;
        }
    }

    private static int checkCache (String database, String table, String query) {
        boolean isCacheable = cacheable.containsKey(database) && cacheable.get(database).containsKey(table);
        if (!isCacheable) return -1;
        MySQLACache newCache = new MySQLACache(database, table, query, "");
        int cacheIndex = cache.indexOf(newCache);
        if (cacheIndex == -1) return -1;
        return cacheIndex;
    }
}
