package com.company.mysqlaccess;

import java.lang.reflect.Field;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MySQLA_orm {

    public static <T> String getSQLString(T element, String fieldName, String columnType) throws Exception {

        String values = "";

        Field field = element.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);

        if (field.getType() == String.class) {
            values = "'" + field.get(element) + "'";
        }

        else if (field.getType() == java.util.Date.class || field.getType() == java.sql.Date.class) {
            Date d = (Date) field.get(element);
            if (MySQLA_typeEquivalency.isTimeColumn(columnType)) {
                values = "'00:00:00'";
            }
            else {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                values = "'" + format.format(d) + "'";
            }
        }

        else if (field.getType() == Calendar.class) {
            Calendar c = (Calendar) field.get(element);
            if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                format.setCalendar(c);
                values = "'" + format.format(c.getTime()) + "'";
            }
            else if (MySQLA_typeEquivalency.isDateColumn(columnType)) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                format.setCalendar(c);
                values = "'" + format.format(c.getTime()) + "'";
            }
            else if (MySQLA_typeEquivalency.isTimeColumn(columnType)) {
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                format.setCalendar(c);
                values = "'" + format.format(c.getTime()) + "'";
            }
            else {
                values = "'" + field.get(element).toString() + "'";
            }
        }

        else if (field.getType() == Year.class) {
            if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                values = "'" + field.get(element) + "-01-01 00:00:00'";
            }
            else if (MySQLA_typeEquivalency.isDateColumn(columnType)) {
                values = "'" + field.get(element) + "-01-01'";
            }
            else if (MySQLA_typeEquivalency.isTimeColumn(columnType)) {
                values = "'00:00:00'";
            }
            else {
                values = "/" + field.get(element).toString() + "'";
            }
        }

        else if (field.getType() == Month.class) {
            int m = ((Month) field.get(element)).getValue();
            String mStr = (m<10) ? "0" + m : "" + m;
            if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                values = "'1970-" + mStr + "-01 00:00:00'";
            }
            else if (MySQLA_typeEquivalency.isDateColumn(columnType)) {
                values = "'1970-" + mStr + "-01'";
            }
            else if (MySQLA_typeEquivalency.isTimeColumn(columnType)) {
                values = "'00:00:00'";
            }
            else {
                values = "'" + field.get(element).toString() + "'";
            }
        }

        else if (field.getType() == MonthDay.class) {
            MonthDay m = ((MonthDay) field.get(element));
            if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                values = "'1970-" + m.getMonthValue() + "-" + m.getDayOfMonth() + " 00:00:00'";
            }
            else if (MySQLA_typeEquivalency.isDateColumn(columnType)) {
                values = "'1970-" + m.getMonthValue() + "-" + m.getDayOfMonth() + "'";
            }
            else if (MySQLA_typeEquivalency.isTimeColumn(columnType)) {
                values = "'00:00:00'";
            }
            else {
                values = "'" + field.get(element).toString() + "'";
            }
        }

        else if (field.getType() == Time.class) {
            Time t = ((Time) field.get(element));
            if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                values = "'1970-01-01 " + t.toString() + "'";
            }
            else if (MySQLA_typeEquivalency.isDateColumn(columnType)) {
                values = "'1970-01-01'";
            }
            else {
                values = "'" + field.get(element).toString() + "'";
            }
        }

        else if (field.getType() == LocalDateTime.class) {
            LocalDateTime d = (LocalDateTime) field.get(element);
            if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
                values = "'" + d.format(format) + "'";
            }
            else if (MySQLA_typeEquivalency.isDateColumn(columnType)) {
                DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu-MM-dd");
                values = "'" + d.format(format) + "'";
            }
            else if (MySQLA_typeEquivalency.isTimeColumn(columnType)) {
                DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm:ss");
                values = "'" + d.format(format) + "'";
            }
            else {
                values = "'" + field.get(element).toString() + "'";
            }
        }

        else if (field.getType() == LocalDate.class) {
            LocalDate d = (LocalDate) field.get(element);
            if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                values = "'" + d.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00'";
            }
            else if (MySQLA_typeEquivalency.isDateColumn(columnType)) {
                values = "'" + d.format(DateTimeFormatter.ISO_LOCAL_DATE) + "'";
            }
            else if (MySQLA_typeEquivalency.isTimeColumn(columnType)) {
                values = "'00:00:00'";
            }
            else {
                values = "'" + d.format(DateTimeFormatter.ISO_LOCAL_DATE) + "'";
            }
        }

        else if (field.getType() == LocalTime.class) {
            if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                values = "'1970-01-01 " + field.get(element).toString() + "'";
            }
            else if (MySQLA_typeEquivalency.isDateColumn(columnType)) {
                values = "'1970-01-01'";
            }
            else {
                values = "'" + field.get(element).toString() + "'";
            }
        }

        else if (field.getType() == Timestamp.class) {
            if (MySQLA_typeEquivalency.isDateTimeColumn(columnType)) {
                values = "'" + field.get(element).toString() + "'";
            }
            else if (MySQLA_typeEquivalency.isDateColumn(columnType)) {
                values = "'" + field.get(element).toString().split(" ")[0] + "'";
            }
            else if (MySQLA_typeEquivalency.isTimeColumn(columnType)) {
                values = "'" + field.get(element).toString().split(" ")[1] + "'";
            }
        }

        else {
            values = field.get(element).toString();
        }

        return values;
    }

    public static Object getObject (Class type, ResultSet resultSet, String columnName) throws SQLException {

        if (type == byte.class)            return resultSet.getByte(columnName);
        else if (type == short.class)      return resultSet.getShort(columnName);
        else if (type == int.class)        return resultSet.getInt(columnName);
        else if (type == long.class)       return resultSet.getLong(columnName);
        else if (type == float.class)      return resultSet.getFloat(columnName);
        else if (type == double.class)     return resultSet.getDouble(columnName);
        else if (type == boolean.class)    return resultSet.getBoolean(columnName);
        else if (type == String.class)     return resultSet.getString(columnName);

        else if (type == LocalDate.class) {
            return resultSet.getDate(columnName).toLocalDate();
        }

        else if (type == LocalDateTime.class) {
            String str = resultSet.getString(columnName);
            str = (str.contains(" ")) ? str.replace(" ", "T") :
                    (str.contains(":")) ? ("1970-01-01T" + str) : (str + "T00:00:00");
            return LocalDateTime.parse(str);
        }

        else if (type == LocalTime.class) {
            return resultSet.getTimestamp(columnName).toLocalDateTime().toLocalTime();
        }

        else if (type == Year.class) {
            return Year.of(resultSet.getDate(columnName).toLocalDate().getYear());
        }

        else if (type == Month.class) {
            return resultSet.getDate(columnName).toLocalDate().getMonth();
        }

        else if (type == MonthDay.class) {
            LocalDate ld = resultSet.getDate(columnName).toLocalDate();
            return MonthDay.of(ld.getMonthValue(), ld.getDayOfMonth());
        }

        else if (type == Time.class) {
            LocalTime lt = resultSet.getTimestamp(columnName).toLocalDateTime().toLocalTime();
            return Time.valueOf(lt);
        }

        else if (type == java.sql.Date.class || type == java.util.Date.class)  {
            LocalDate ld = resultSet.getTimestamp(columnName).toLocalDateTime().toLocalDate();
            return Date.valueOf(ld);
        }

        else if (type == Calendar.class)  {
            LocalDateTime ldt = resultSet.getTimestamp(columnName).toLocalDateTime();
            Calendar c = new GregorianCalendar(ldt.getYear(), ldt.getMonthValue()-2, ldt.getDayOfMonth(),
                    ldt.getHour(), ldt.getMinute(), ldt.getSecond());
            return c;
        }

        else if (type == Timestamp.class)  {
            String str = resultSet.getString(columnName);
            str = (str.contains(" ")) ? str.replace(" ", "T") :
                    (str.contains(":")) ? ("1970-01-01T" + str) : (str + "T00:00:00");
            return Timestamp.valueOf(LocalDateTime.parse(str));
        }

        else return resultSet.getObject(columnName);
    }
}
