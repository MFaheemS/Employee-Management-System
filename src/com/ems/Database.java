package com.ems;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String DB_DIRECTORY = "data";
    private static final String DB_FILE = "ems.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIRECTORY + File.separator + DB_FILE;

    private Database() {
    }

    public static void initialize() throws SQLException {
        File directory = new File(DB_DIRECTORY);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new SQLException("Could not create database directory: " + directory.getAbsolutePath());
        }

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("PRAGMA foreign_keys = ON");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                    + "username TEXT PRIMARY KEY, "
                    + "password TEXT NOT NULL"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS employees ("
                    + "employee_id TEXT PRIMARY KEY, "
                    + "full_name TEXT NOT NULL, "
                    + "job_title TEXT NOT NULL, "
                    + "department TEXT NOT NULL, "
                    + "email TEXT NOT NULL, "
                    + "is_active INTEGER NOT NULL DEFAULT 1, "
                    + "deactivation_reason TEXT, "
                    + "deactivated_at TEXT"
                    + ")");
        }

        seedDefaultAdmin();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void seedDefaultAdmin() throws SQLException {
        String sql = "INSERT OR IGNORE INTO users (username, password) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "admin");
            statement.setString(2, "admin");
            statement.executeUpdate();
        }
    }
}
