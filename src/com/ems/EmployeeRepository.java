package com.ems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EmployeeRepository {

    public boolean addEmployee(Employee employee) throws SQLException {
        String managerUsername = employee.getManagerUsername();
        if (managerUsername == null || managerUsername.isBlank()) {
            managerUsername = findDefaultManagerUsername();
        }

        String sql = "INSERT INTO employees "
                + "(employee_id, full_name, job_title, department, email, phone, is_active, role, "
                + "manager_username, leave_balance, salary) "
                + "VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?)";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, employee.getEmployeeId());
            statement.setString(2, employee.getFullName());
            statement.setString(3, employee.getJobTitle());
            statement.setString(4, employee.getDepartment());
            statement.setString(5, employee.getEmail());
            statement.setString(6, employee.getPhone());
            statement.setString(7, employee.getRole());
            statement.setString(8, managerUsername);
            statement.setInt(9, employee.getLeaveBalance());
            statement.setDouble(10, employee.getSalary());

            return statement.executeUpdate() == 1;
        }
    }

    public Employee findById(String employeeId) throws SQLException {
        String sql = "SELECT employee_id, full_name, job_title, department, email, phone, is_active, "
                + "role, manager_username, leave_balance, salary "
                + "FROM employees WHERE employee_id = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, employeeId);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                return mapEmployee(rs);
            }
        }
    }

    public boolean updateEmployee(Employee employee) throws SQLException {
        String sql = "UPDATE employees "
                + "SET full_name = ?, job_title = ?, department = ?, email = ?, phone = ?, "
                + "is_active = ?, role = ?, manager_username = ?, leave_balance = ?, salary = ? "
                + "WHERE employee_id = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, employee.getFullName());
            statement.setString(2, employee.getJobTitle());
            statement.setString(3, employee.getDepartment());
            statement.setString(4, employee.getEmail());
            statement.setString(5, employee.getPhone());
            statement.setInt(6, employee.isActive() ? 1 : 0);
            statement.setString(7, employee.getRole());
            statement.setString(8, employee.getManagerUsername());
            statement.setInt(9, employee.getLeaveBalance());
            statement.setDouble(10, employee.getSalary());
            statement.setString(11, employee.getEmployeeId());

            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteEmployee(String employeeId) throws SQLException {
        String sql = "DELETE FROM employees WHERE employee_id = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, employeeId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean deactivateEmployee(String employeeId, String reason) throws SQLException {
        String sql = "UPDATE employees "
                + "SET is_active = 0, deactivation_reason = ?, deactivated_at = datetime('now') "
                + "WHERE employee_id = ? AND is_active = 1";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, reason);
            statement.setString(2, employeeId);

            return statement.executeUpdate() == 1;
        }
    }

    public List<Employee> searchEmployees(String searchText) throws SQLException {
        String normalizedText = searchText == null ? "" : searchText.trim();
        String pattern = "%" + normalizedText + "%";

        String sql = "SELECT employee_id, full_name, job_title, department, email, phone, is_active, "
                + "role, manager_username, leave_balance, salary "
                + "FROM employees "
                + "WHERE employee_id LIKE ? OR full_name LIKE ? OR department LIKE ? OR role LIKE ? "
                + "ORDER BY full_name ASC";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);

            try (ResultSet rs = statement.executeQuery()) {
                List<Employee> employees = new ArrayList<>();
                while (rs.next()) {
                    employees.add(mapEmployee(rs));
                }
                return employees;
            }
        }
    }

    public List<Employee> getAllActiveEmployees() throws SQLException {
        String sql = "SELECT employee_id, full_name, job_title, department, email, phone, is_active, "
                + "role, manager_username, leave_balance, salary "
                + "FROM employees WHERE is_active = 1 ORDER BY full_name ASC";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Employee> employees = new ArrayList<>();
            while (rs.next()) {
                employees.add(mapEmployee(rs));
            }
            return employees;
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void createUserAccount(String username, String password, String role, String employeeId) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, employee_id) VALUES (?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, role);
            statement.setString(4, employeeId);
            statement.executeUpdate();
        }
    }

    private String findDefaultManagerUsername() throws SQLException {
        String sql = "SELECT username FROM users WHERE role IN ('Manager', 'Admin') "
                + "ORDER BY CASE role WHEN 'Manager' THEN 0 ELSE 1 END, username LIMIT 1";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) return rs.getString("username");
            return "admin";
        }
    }

    private Employee mapEmployee(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getString("employee_id"),
                rs.getString("full_name"),
                rs.getString("job_title"),
                rs.getString("department"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getInt("is_active") == 1,
                rs.getString("role"),
                rs.getString("manager_username"),
                rs.getInt("leave_balance"),
                rs.getDouble("salary")
        );
    }
}
