package com.company.mysqlaccess;

import java.sql.Time;
import java.time.*;
import java.util.*;

public class MySQLA_typeEquivalency {

    private static Map<String, List<Class>> typeEquivalency = new HashMap<>();
    private static List<String> numericColumnTypes = new ArrayList<>();
    private static List<String> dateTimeColumnTypes = new ArrayList<>();
    private static List<String> dateColumnTypes = new ArrayList<>();
    private static List<String> timeColumnTypes = new ArrayList<>();

    static {
        List<Class> ints = new ArrayList<>(Arrays.asList(Integer.class, int.class, Long.class, long.class));
        List<Class> bools = new ArrayList<>(Arrays.asList(Boolean.class, boolean.class));

        // ---> Types matching datetime columns
        List<Class> timestp = new ArrayList<>(Arrays.asList(
                java.sql.Date.class,
                java.util.Date.class,
                Calendar.class,
                Year.class,
                Month.class,
                MonthDay.class,
                Time.class,
                java.sql.Timestamp.class,
                LocalDate.class,
                LocalDateTime.class,
                LocalTime.class));

        typeEquivalency.put("int", ints);
        typeEquivalency.put("integer", ints);
        typeEquivalency.put("smallint", ints);
        typeEquivalency.put("mediumint", ints);
        typeEquivalency.put("bigint", Arrays.asList(Long.class, long.class));
        typeEquivalency.put("tinyint", bools);
        typeEquivalency.put("bool", bools);
        typeEquivalency.put("boolean", bools);
        typeEquivalency.put("float", Arrays.asList(Float.class, float.class));
        typeEquivalency.put("double", Arrays.asList(Double.class, double.class));
        typeEquivalency.put("decimal", Arrays.asList(java.math.BigDecimal.class));
        typeEquivalency.put("date", timestp);
        typeEquivalency.put("datetime", timestp);
        typeEquivalency.put("timestamp", timestp);
        typeEquivalency.put("time", timestp);
        typeEquivalency.put("year", Arrays.asList(Year.class));

        numericColumnTypes.addAll(
                Arrays.asList("int", "integer", "smallint", "mediumint", "bigint", "tinyint", "float",
                        "double", "decimal"));

        dateTimeColumnTypes.addAll(
                Arrays.asList("timestamp", "datetime"));

        dateColumnTypes.addAll(
                Arrays.asList("date"));

        timeColumnTypes.addAll(
                Arrays.asList("time"));
    }

    public static Map<String, List<Class>> getTypeEquivalency () {
        return typeEquivalency;
    }

    public static boolean isNumericColumn(String columnType) {
        return numericColumnTypes.contains(columnType);
    }

    public static boolean isDateTimeColumn(String columnType) {
        return dateTimeColumnTypes.contains(columnType);
    }

    public static boolean isDateColumn(String columnType) {
        return dateColumnTypes.contains(columnType);
    }

    public static boolean isTimeColumn(String columnType) {
        return timeColumnTypes.contains(columnType);
    }
}
