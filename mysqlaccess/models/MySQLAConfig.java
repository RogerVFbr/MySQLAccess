package com.company.mysqlaccess.models;

public class MySQLAConfig {

    public String ip;
    public int port;
    public String database;
    public String user;
    public String password;

    public MySQLAConfig() {};

    public MySQLAConfig(String ip, int port, String database, String user, String password) {
        this.ip = ip;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }
}
