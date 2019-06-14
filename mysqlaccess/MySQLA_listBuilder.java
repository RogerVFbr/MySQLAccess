package com.company.mysqlaccess;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class MySQLA_listBuilder {

    private static <T> T instantiateDataObject(Class<T> type) {
        T obj = null;

        try {
            obj = type.getConstructor().newInstance();

        } catch (Exception e) {
            String[] classComps = type.toString().split("\\.");
            String classPath = Arrays.stream(classComps).skip(1).collect(Collectors.joining("."));
            String className = Arrays.stream(classComps).skip(classComps.length-1).findFirst().get();
            MySQLA_loggers.logError("Model class constructor needs to have overload without parameters -> " + classPath);
            MySQLA_loggers.logError("Include on model class' constructors: public "+ className + " () {}");
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
            MySQLA_loggers.logError("Required field (" + e.getMessage() + ") doesn't exist on model. File: "
                    + frame.getFileName() + " | Method: " + frame.getMethodName() + " | Line number: "
                    + frame.getLineNumber());

        } catch (Exception e) {
            String columnType = e.getMessage().replaceAll("Cannot cast ", "").split(" to ")[0];
            String fieldType = field.getType().toString().replaceAll("class ", "");
            MySQLA_loggers.logError("Table column and model field type mismatch -> Column: " + columnName + " (" + columnType + ") | " +
                    "Field: " + fieldName + " (" + fieldType + ")");
        }
        incompatibleFields.add(fieldName);
    }

    public static <T> boolean buildListFromRetrievedData(Class<T> type, ResultSet resultSet, List<T> returnData,
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

            MySQLA_loggers.logDetails(data.toString());
            returnData.add(data);
        }
        return true;
    }
}
