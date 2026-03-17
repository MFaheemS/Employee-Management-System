package com.ems;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
                    + "password TEXT NOT NULL, "
                    + "role TEXT NOT NULL DEFAULT 'Employee', "
                    + "employee_id TEXT"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS employees ("
                    + "employee_id TEXT PRIMARY KEY, "
                    + "full_name TEXT NOT NULL, "
                    + "job_title TEXT NOT NULL, "
                    + "department TEXT NOT NULL, "
                    + "email TEXT NOT NULL, "
                    + "is_active INTEGER NOT NULL DEFAULT 1, "
                    + "role TEXT NOT NULL DEFAULT 'Employee', "
                    + "manager_username TEXT, "
                    + "leave_balance INTEGER NOT NULL DEFAULT 20, "
                    + "deactivation_reason TEXT, "
                    + "deactivated_at TEXT"
                    + ")");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS leave_requests ("
                    + "request_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "employee_id TEXT NOT NULL, "
                    + "manager_username TEXT NOT NULL, "
                    + "leave_type TEXT NOT NULL, "
                    + "start_date TEXT NOT NULL, "
                    + "end_date TEXT NOT NULL, "
                    + "days_requested INTEGER NOT NULL, "
                    + "status TEXT NOT NULL DEFAULT 'Pending', "
                    + "comments TEXT, "
                    + "requested_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "decision_at TEXT, "
                    + "decision_by TEXT, "
                    + "FOREIGN KEY(employee_id) REFERENCES employees(employee_id)"
                    + ")");

                ensureColumn(connection, "users", "role", "TEXT NOT NULL DEFAULT 'Employee'");
                ensureColumn(connection, "users", "employee_id", "TEXT");
                ensureColumn(connection, "employees", "role", "TEXT NOT NULL DEFAULT 'Employee'");
                ensureColumn(connection, "employees", "manager_username", "TEXT");
                ensureColumn(connection, "employees", "leave_balance", "INTEGER NOT NULL DEFAULT 20");
        }

        seedDefaultAdmin();
            seedDemoUsers();
            assignDefaultManagers();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void seedDefaultAdmin() throws SQLException {
        String sql = "INSERT OR IGNORE INTO users (username, password, role, employee_id) VALUES (?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "admin");
            statement.setString(2, "admin");
            statement.setString(3, "Admin");
            statement.setString(4, null);
            statement.executeUpdate();
        }

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET role = 'Admin' WHERE username = 'admin'")) {
            statement.executeUpdate();
        }
    }

    private static void seedDemoUsers() throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement employeeStatement = connection.prepareStatement(
                    "INSERT OR IGNORE INTO employees "
                            + "(employee_id, full_name, job_title, department, email, is_active, role, manager_username, leave_balance) "
                            + "VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?)");
                 PreparedStatement userStatement = connection.prepareStatement(
                         "INSERT OR IGNORE INTO users (username, password, role, employee_id) VALUES (?, ?, ?, ?)")) {

                employeeStatement.setString(1, "MGR001");
                employeeStatement.setString(2, "Manager Demo");
                employeeStatement.setString(3, "Team Manager");
                employeeStatement.setString(4, "Operations");
                employeeStatement.setString(5, "manager@ems.local");
                employeeStatement.setString(6, "Manager");
                employeeStatement.setString(7, "admin");
                employeeStatement.setInt(8, 25);
                employeeStatement.executeUpdate();

                employeeStatement.setString(1, "EMP001");
                employeeStatement.setString(2, "Employee Demo");
                employeeStatement.setString(3, "Support Executive");
                employeeStatement.setString(4, "Operations");
                employeeStatement.setString(5, "employee@ems.local");
                employeeStatement.setString(6, "Employee");
                employeeStatement.setString(7, "manager");
                employeeStatement.setInt(8, 18);
                employeeStatement.executeUpdate();

                userStatement.setString(1, "manager");
                userStatement.setString(2, "manager");
                userStatement.setString(3, "Manager");
                userStatement.setString(4, "MGR001");
                userStatement.executeUpdate();

                userStatement.setString(1, "employee");
                userStatement.setString(2, "employee");
                userStatement.setString(3, "Employee");
                userStatement.setString(4, "EMP001");
                userStatement.executeUpdate();
            }

            connection.commit();
        }
    }

    private static void assignDefaultManagers() throws SQLException {
        String sql = "UPDATE employees SET manager_username = COALESCE("
                + "(SELECT username FROM users WHERE role = 'Manager' LIMIT 1), "
                + "(SELECT username FROM users WHERE role = 'Admin' LIMIT 1)) "
                + "WHERE manager_username IS NULL AND role = 'Employee'";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private static void ensureColumn(Connection connection, String tableName,
                                     String columnName, String columnDefinition) throws SQLException {
        if (hasColumn(connection, tableName, columnName)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName
                    + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private static boolean hasColumn(Connection connection, String tableName,
                                     String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {

            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }

            return false;
        }
    }
}
