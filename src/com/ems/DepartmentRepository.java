package com.ems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DepartmentRepository {

    public List<Department> getAllDepartments() throws SQLException {
        String sql = "SELECT department_id, department_name, manager_username, created_at "
                + "FROM departments ORDER BY department_name ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            List<Department> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    }

    public List<String> getAllDepartmentNames() throws SQLException {
        String sql = "SELECT department_name FROM departments ORDER BY department_name ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            List<String> names = new ArrayList<>();
            while (rs.next()) names.add(rs.getString(1));
            return names;
        }
    }

    public String getManagerForDepartment(String departmentName) throws SQLException {
        String sql = "SELECT manager_username FROM departments WHERE department_name = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, departmentName);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public boolean departmentExists(String departmentName) throws SQLException {
        String sql = "SELECT 1 FROM departments WHERE LOWER(department_name) = LOWER(?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, departmentName);
            try (ResultSet rs = st.executeQuery()) { return rs.next(); }
        }
    }

    public void addDepartment(String departmentName, String managerUsername) throws SQLException {
        String sql = "INSERT INTO departments (department_name, manager_username) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, departmentName);
            st.setString(2, managerUsername);
            st.executeUpdate();
        }
    }

    public List<String> getAllManagerUsernames() throws SQLException {
        String sql = "SELECT username FROM users WHERE role = 'Manager' ORDER BY username ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            List<String> names = new ArrayList<>();
            while (rs.next()) names.add(rs.getString(1));
            return names;
        }
    }

    public List<String> getUnassignedManagers() throws SQLException {
        String sql = "SELECT u.username FROM users u "
                + "WHERE u.role = 'Manager' "
                + "AND NOT EXISTS (SELECT 1 FROM departments d WHERE d.manager_username = u.username) "
                + "ORDER BY u.username ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString(1));
            return list;
        }
    }

    public boolean hasDepartment(String managerUsername) throws SQLException {
        String sql = "SELECT 1 FROM departments WHERE manager_username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, managerUsername);
            try (ResultSet rs = st.executeQuery()) { return rs.next(); }
        }
    }

    public boolean hasEmployeesInDepartment(String departmentName) throws SQLException {
        String sql = "SELECT 1 FROM employees WHERE department = ? AND is_active = 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, departmentName);
            try (ResultSet rs = st.executeQuery()) { return rs.next(); }
        }
    }

    public void deleteDepartment(int departmentId) throws SQLException {
        String sql = "DELETE FROM departments WHERE department_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, departmentId);
            st.executeUpdate();
        }
    }

    private Department map(ResultSet rs) throws SQLException {
        return new Department(
                rs.getInt("department_id"),
                rs.getString("department_name"),
                rs.getString("manager_username"),
                rs.getString("created_at")
        );
    }
}
