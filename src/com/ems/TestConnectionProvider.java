package com.ems;

import java.sql.Connection;

/**
 * Holds the shared in-memory SQLite connection used by all tests.
 * WhiteBoxTestRunner calls setConnection() once; Database.getConnection()
 * checks this before opening a file-based connection.
 * Returns a NonClosingConnection wrapper so service code's close() / setAutoCommit()
 * calls don't destroy the shared connection between tests.
 */
public class TestConnectionProvider {
    private static Connection testConnection;

    public static void setConnection(Connection conn) {
        testConnection = conn;
    }

    public static Connection get() {
        // Wrap in NonClosingConnection when available (test classpath only).
        try {
            Class<?> wrapperClass = Class.forName("com.ems.NonClosingConnection");
            return (Connection) wrapperClass
                    .getConstructor(Connection.class)
                    .newInstance(testConnection);
        } catch (Exception e) {
            return testConnection;
        }
    }

    public static boolean isActive() {
        return testConnection != null;
    }
}
