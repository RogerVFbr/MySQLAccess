package com.company.mysqlaccess;

import java.util.*;

public class MySQLA_typeEquivalency {
    public static Map<String, List<Class>>  getTypeEquivalency () {
        Map<String, List<Class>> mySqlTypeEquivalency = new HashMap<>();
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
        return mySqlTypeEquivalency;
    }
}
