package com.ems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    public AppUser authenticate(String username, String password) throws SQLException {
        String sql = "SELECT username, role, employee_id FROM users WHERE username = ? AND password = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, password);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                String employeeId = resultSet.getString("employee_id");
                if (employeeId != null && !employeeId.isBlank()) {
                    try (PreparedStatement employeeStatement = connection.prepareStatement(
                            "SELECT is_active FROM employees WHERE employee_id = ?")) {
                        employeeStatement.setString(1, employeeId);
                        try (ResultSet employeeResult = employeeStatement.executeQuery()) {
                            if (!employeeResult.next() || employeeResult.getInt("is_active") != 1) {
                                return null;
                            }
                        }
                    }
                }

                return new AppUser(
                        resultSet.getString("username"),
                        resultSet.getString("role"),
                        employeeId
                );
            }
        }
    }
}
