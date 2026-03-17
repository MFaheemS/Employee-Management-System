package com.ems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmployeeRepository {

    public boolean addEmployee(Employee employee) throws SQLException {
        String managerUsername = employee.getManagerUsername();
        if (managerUsername == null || managerUsername.isBlank()) {
            managerUsername = findDefaultManagerUsername();
        }

        String sql = "INSERT INTO employees "
                + "(employee_id, full_name, job_title, department, email, is_active, role, manager_username, leave_balance) "
                + "VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?)";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, employee.getEmployeeId());
            statement.setString(2, employee.getFullName());
            statement.setString(3, employee.getJobTitle());
            statement.setString(4, employee.getDepartment());
            statement.setString(5, employee.getEmail());
            statement.setString(6, employee.getRole());
            statement.setString(7, managerUsername);
            statement.setInt(8, employee.getLeaveBalance());

            return statement.executeUpdate() == 1;
        }
    }

    public Employee findById(String employeeId) throws SQLException {
        String sql = "SELECT employee_id, full_name, job_title, department, email, is_active, "
                + "role, manager_username, leave_balance "
                + "FROM employees WHERE employee_id = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, employeeId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new Employee(
                        resultSet.getString("employee_id"),
                        resultSet.getString("full_name"),
                        resultSet.getString("job_title"),
                        resultSet.getString("department"),
                        resultSet.getString("email"),
                    resultSet.getInt("is_active") == 1,
                    resultSet.getString("role"),
                    resultSet.getString("manager_username"),
                    resultSet.getInt("leave_balance")
                );
            }
        }
    }

    public boolean updateEmployee(Employee employee) throws SQLException {
        String sql = "UPDATE employees "
            + "SET full_name = ?, job_title = ?, department = ?, email = ?, is_active = ?, role = ?, "
            + "manager_username = ?, leave_balance = ? "
                + "WHERE employee_id = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, employee.getFullName());
            statement.setString(2, employee.getJobTitle());
            statement.setString(3, employee.getDepartment());
            statement.setString(4, employee.getEmail());
            statement.setInt(5, employee.isActive() ? 1 : 0);
            statement.setString(6, employee.getRole());
            statement.setString(7, employee.getManagerUsername());
            statement.setInt(8, employee.getLeaveBalance());
            statement.setString(9, employee.getEmployeeId());

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

    private String findDefaultManagerUsername() throws SQLException {
        String sql = "SELECT username FROM users WHERE role IN ('Manager', 'Admin') "
                + "ORDER BY CASE role WHEN 'Manager' THEN 0 ELSE 1 END, username LIMIT 1";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getString("username");
            }

            return "admin";
        }
    }
}
