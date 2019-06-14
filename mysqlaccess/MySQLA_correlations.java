package com.company.mysqlaccess;

import java.util.*;
import java.util.stream.Collectors;

public class MySQLA_correlations {
    private static Map<Map<Class, Set<String>>, Map<String, String>> columnFieldCorrelations = new HashMap<>();
    private static Map<String, List<Class>> mySqlTypeEquivalency = MySQLA_typeEquivalency.getTypeEquivalency();

    public static Map<String, String> getColumnFieldCorrelation (Class type, List<String> columns, String database,
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
        MySQLA_loggers.logInfo("Saving new column/field correlation between table '" + tableName + "' and model '"
                + type.getName() + "': " + propertyMap);
        columnFieldCorrelations.put(correlationKey, propertyMap);
        return propertyMap;
    }

    private static void buildCorrelationsMap (
            List<Map<String, Object>> correlations, Class type, List<String> fields, String columnName,
            String database, String table) {

        String columnType = MySQLA_tableProperties.getAllColumnTypes().get(database).get(table).get(columnName);
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

            int currentLevenstein = MySQLA_levenstein.getLevensteinDistance(columnName, fieldName);
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
}
