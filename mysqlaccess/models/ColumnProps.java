package com.company.mysqlaccess.models;

public class ColumnProps {

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
