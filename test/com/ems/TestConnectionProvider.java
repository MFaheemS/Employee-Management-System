package com.ems;

import java.sql.Connection;

/**
 * Holds the shared in-memory SQLite connection used by all tests.
 * WhiteBoxTestRunner calls setConnection() once; Database.getConnection()
 * checks this before opening a file-based connection.
 */
public class TestConnectionProvider {
    private static Connection testConnection;

    public static void setConnection(Connection conn) {
        testConnection = conn;
    }

    public static Connection get() {
        return testConnection;
    }

    public static boolean isActive() {
        return testConnection != null;
    }
}
