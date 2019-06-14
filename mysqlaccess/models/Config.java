package com.company.mysqlaccess.models;

public class Config {

    public String ip;
    public int port;
    public String database;
    public String user;
    public String password;

    public Config() {};

    public Config(String ip, int port, String database, String user, String password) {
        this.ip = ip;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }
}
