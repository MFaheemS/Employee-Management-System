package com.ems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmployeeRepository {

    public boolean addEmployee(Employee employee) throws SQLException {
        String sql = "INSERT INTO employees (employee_id, full_name, job_title, department, email, is_active) "
                + "VALUES (?, ?, ?, ?, ?, 1)";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, employee.getEmployeeId());
            statement.setString(2, employee.getFullName());
            statement.setString(3, employee.getJobTitle());
            statement.setString(4, employee.getDepartment());
            statement.setString(5, employee.getEmail());

            return statement.executeUpdate() == 1;
        }
    }

    public Employee findById(String employeeId) throws SQLException {
        String sql = "SELECT employee_id, full_name, job_title, department, email, is_active "
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
                        resultSet.getInt("is_active") == 1
                );
            }
        }
    }

    public boolean updateEmployee(Employee employee) throws SQLException {
        String sql = "UPDATE employees "
                + "SET full_name = ?, job_title = ?, department = ?, email = ?, is_active = ? "
                + "WHERE employee_id = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, employee.getFullName());
            statement.setString(2, employee.getJobTitle());
            statement.setString(3, employee.getDepartment());
            statement.setString(4, employee.getEmail());
            statement.setInt(5, employee.isActive() ? 1 : 0);
            statement.setString(6, employee.getEmployeeId());

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
}
