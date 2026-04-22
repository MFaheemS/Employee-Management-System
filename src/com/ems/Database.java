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

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS attendance_records ("
                    + "record_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "employee_id TEXT NOT NULL, "
                    + "attendance_date TEXT NOT NULL, "
                    + "check_in_at TEXT, "
                    + "check_out_at TEXT, "
                    + "total_hours REAL, "
                    + "UNIQUE(employee_id, attendance_date), "
                    + "FOREIGN KEY(employee_id) REFERENCES employees(employee_id)"
                    + ")");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS payroll_records ("
                    + "record_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "employee_id TEXT NOT NULL, "
                    + "month INTEGER NOT NULL, "
                    + "year INTEGER NOT NULL, "
                    + "base_salary REAL NOT NULL DEFAULT 0, "
                    + "overtime_hours REAL NOT NULL DEFAULT 0, "
                    + "overtime_rate REAL NOT NULL DEFAULT 0, "
                    + "deductions REAL NOT NULL DEFAULT 0, "
                    + "gross_salary REAL NOT NULL DEFAULT 0, "
                    + "net_salary REAL NOT NULL DEFAULT 0, "
                    + "generated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "generated_by TEXT, "
                    + "notes TEXT, "
                    + "FOREIGN KEY(employee_id) REFERENCES employees(employee_id)"
                    + ")");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS employee_documents ("
                    + "doc_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "employee_id TEXT NOT NULL, "
                    + "file_name TEXT NOT NULL, "
                    + "file_path TEXT NOT NULL, "
                    + "file_type TEXT, "
                    + "uploaded_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "uploaded_by TEXT, "
                    + "FOREIGN KEY(employee_id) REFERENCES employees(employee_id)"
                    + ")");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS departments ("
                    + "department_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "department_name TEXT NOT NULL UNIQUE, "
                    + "manager_username TEXT NOT NULL, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                    + ")");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS deactivation_requests ("
                    + "request_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "employee_id TEXT NOT NULL, "
                    + "requested_by TEXT NOT NULL, "
                    + "reason TEXT NOT NULL, "
                    + "status TEXT NOT NULL DEFAULT 'Pending', "
                    + "requested_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "decided_by TEXT, "
                    + "decided_at TEXT, "
                    + "FOREIGN KEY(employee_id) REFERENCES employees(employee_id)"
                    + ")");

                ensureColumn(connection, "employee_documents", "status", "TEXT NOT NULL DEFAULT 'Pending'");
                ensureColumn(connection, "users", "role", "TEXT NOT NULL DEFAULT 'Employee'");
                ensureColumn(connection, "users", "employee_id", "TEXT");
                ensureColumn(connection, "employees", "role", "TEXT NOT NULL DEFAULT 'Employee'");
                ensureColumn(connection, "employees", "manager_username", "TEXT");
                ensureColumn(connection, "employees", "leave_balance", "INTEGER NOT NULL DEFAULT 20");
                ensureColumn(connection, "employees", "phone", "TEXT");
                ensureColumn(connection, "employees", "salary", "REAL NOT NULL DEFAULT 0");
                ensureColumn(connection, "employees", "last_net_salary", "REAL NOT NULL DEFAULT 0");
        }

        seedDefaultAdmin();
            seedDemoUsers();
            assignDefaultManagers();
            seedDefaultDepartment();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void seedDefaultAdmin() throws SQLException {
        try (Connection connection = getConnection()) {
            // Ensure admin account exists with correct password
            try (PreparedStatement st = connection.prepareStatement(
                    "INSERT OR IGNORE INTO users (username, password, role, employee_id) VALUES ('admin', '123', 'Admin', NULL)")) {
                st.executeUpdate();
            }
            try (PreparedStatement st = connection.prepareStatement(
                    "UPDATE users SET password = '123', role = 'Admin' WHERE username = 'admin'")) {
                st.executeUpdate();
            }
        }
    }

    private static void seedDemoUsers() throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            // Remove old demo records that conflict with the new seed data
            String[] oldUsernames = {"manager", "employee"};
            String[] oldEmpIds    = {"MGR001", "EMP001"};
            try (PreparedStatement du = connection.prepareStatement("DELETE FROM users WHERE username = ?");
                 PreparedStatement de = connection.prepareStatement("DELETE FROM employees WHERE employee_id = ?")) {
                for (String u : oldUsernames) { du.setString(1, u); du.executeUpdate(); }
                for (String e : oldEmpIds)    { de.setString(1, e); de.executeUpdate(); }
            }

            try (PreparedStatement emp = connection.prepareStatement(
                    "INSERT OR IGNORE INTO employees "
                    + "(employee_id, full_name, job_title, department, email, is_active, role, manager_username, leave_balance, salary) "
                    + "VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, ?)");
                 PreparedStatement usr = connection.prepareStatement(
                    "INSERT OR IGNORE INTO users (username, password, role, employee_id) VALUES (?, '123', ?, ?)")) {

                // ── Managers ─────────────────────────────────────────────────
                insertEmployee(emp, "M001", "Alice Morgan",   "Engineering Manager",   "Engineering", "alice.m@ems.local",  "Manager",  "admin",   20, 120000);
                insertEmployee(emp, "M002", "Bob Khan",       "Operations Manager",    "Operations",  "bob.k@ems.local",   "Manager",  "admin",   20, 110000);

                insertUser(usr, "alice.m", "Manager", "M001");
                insertUser(usr, "bob.k",   "Manager", "M002");

                // ── Employees ────────────────────────────────────────────────
                insertEmployee(emp, "E001", "James Smith",   "Software Engineer",      "Engineering", "j.smith@ems.local",  "Employee", "alice.m", 20, 80000);
                insertEmployee(emp, "E002", "Sara Jones",    "QA Engineer",            "Engineering", "s.jones@ems.local",  "Employee", "alice.m", 18, 75000);
                insertEmployee(emp, "E003", "Raj Patel",     "DevOps Engineer",        "Engineering", "r.patel@ems.local",  "Employee", "alice.m", 15, 85000);
                insertEmployee(emp, "E004", "Linda Chen",    "Operations Analyst",     "Operations",  "l.chen@ems.local",   "Employee", "bob.k",   20, 70000);
                insertEmployee(emp, "E005", "Ahmed Malik",   "Logistics Coordinator",  "Operations",  "a.malik@ems.local",  "Employee", "bob.k",   22, 68000);

                insertUser(usr, "j.smith",  "Employee", "E001");
                insertUser(usr, "s.jones",  "Employee", "E002");
                insertUser(usr, "r.patel",  "Employee", "E003");
                insertUser(usr, "l.chen",   "Employee", "E004");
                insertUser(usr, "a.malik",  "Employee", "E005");
            }

            connection.commit();
        }
    }

    private static void insertEmployee(PreparedStatement st, String id, String name, String title,
                                       String dept, String email, String role, String manager,
                                       int leaveBalance, double salary) throws SQLException {
        st.setString(1, id);
        st.setString(2, name);
        st.setString(3, title);
        st.setString(4, dept);
        st.setString(5, email);
        st.setString(6, role);
        st.setString(7, manager);
        st.setInt(8, leaveBalance);
        st.setDouble(9, salary);
        st.executeUpdate();
    }

    private static void insertUser(PreparedStatement st, String username, String role,
                                   String employeeId) throws SQLException {
        st.setString(1, username);
        st.setString(2, role);
        st.setString(3, employeeId);
        st.executeUpdate();
    }

    private static void seedDefaultDepartment() throws SQLException {
        // Remove old Operations department that used old "manager" username
        try (Connection connection = getConnection();
             PreparedStatement st = connection.prepareStatement(
                     "DELETE FROM departments WHERE manager_username = 'manager'")) {
            st.executeUpdate();
        }

        String sql = "INSERT OR IGNORE INTO departments (department_name, manager_username) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, "Engineering");
            st.setString(2, "alice.m");
            st.executeUpdate();

            st.setString(1, "Operations");
            st.setString(2, "bob.k");
            st.executeUpdate();
        }
    }

    private static void assignDefaultManagers() throws SQLException {
        // No-op: all employees are seeded with explicit manager assignments
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
