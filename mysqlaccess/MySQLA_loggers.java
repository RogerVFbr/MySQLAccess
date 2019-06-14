package com.company.mysqlaccess;

public class MySQLA_loggers {

    private static final String TAG = "MySQLA";
    private static boolean logInfoB = false;
    private static boolean logDetailsB = false;
    private static boolean logFetchB = false;

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";

    public static void logDetails () {
        logDetailsB = true;
    }

    public static void logInfo () {
        logInfoB = true;
    }

    public static void logFetch () {
        logFetchB = true;
    }

    public static void log (String message) {
        System.out.println(TAG + " - " + message);
    }

    public static void logFetch (String message) {
        if (!logFetchB) return;
        System.out.println(ANSI_YELLOW + TAG + " - " + message + ANSI_RESET);
    }

    public static void logInfo (String message) {
        if (!logInfoB) return;
        System.out.println(ANSI_GREEN + TAG + " - " + message + ANSI_RESET);
    }

    public static void logDetails (String message) {
        if (!logDetailsB) return;
        System.out.println(ANSI_CYAN + TAG + " - " + message + ANSI_RESET);
    }

    public static void logError (String message) {
        System.out.println(ANSI_PURPLE + TAG + " - " + message + ANSI_RESET);
    }
}
