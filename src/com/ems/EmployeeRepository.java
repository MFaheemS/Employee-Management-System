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
                + "role, manager_username, leave_balance, salary, last_net_salary "
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

    /**
     * Permanently removes an employee and all their associated records from the database.
     * Called by admin on direct deactivation or on approving a manager's request.
     */
    public boolean deleteEmployeeCompletely(String employeeId) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            // Delete dependent records first (FK order)
            for (String sql : new String[]{
                    "DELETE FROM deactivation_requests WHERE employee_id = ?",
                    "DELETE FROM leave_requests        WHERE employee_id = ?",
                    "DELETE FROM attendance_records    WHERE employee_id = ?",
                    "DELETE FROM payroll_records       WHERE employee_id = ?",
                    "DELETE FROM employee_documents    WHERE employee_id = ?"
            }) {
                try (PreparedStatement st = conn.prepareStatement(sql)) {
                    st.setString(1, employeeId); st.executeUpdate();
                }
            }
            // Delete the user account
            try (PreparedStatement st = conn.prepareStatement(
                    "DELETE FROM users WHERE employee_id = ?")) {
                st.setString(1, employeeId); st.executeUpdate();
            }
            // Delete the employee record
            int rows;
            try (PreparedStatement st = conn.prepareStatement(
                    "DELETE FROM employees WHERE employee_id = ?")) {
                st.setString(1, employeeId); rows = st.executeUpdate();
            }
            conn.commit();
            return rows == 1;
        }
    }

    /**
     * Returns the next available ID in the sequence for the given prefix (M or E).
     * Scans M001..M999 (or E001..E999) and returns the first gap.
     */
    public String getNextAvailableId(String prefix) throws SQLException {
        String sql = "SELECT employee_id FROM employees WHERE employee_id LIKE ? ORDER BY employee_id ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, prefix + "%");
            try (ResultSet rs = st.executeQuery()) {
                java.util.Set<Integer> used = new java.util.HashSet<>();
                while (rs.next()) {
                    String id = rs.getString(1);
                    try {
                        used.add(Integer.parseInt(id.substring(prefix.length())));
                    } catch (NumberFormatException ignored) {}
                }
                for (int i = 1; i <= 999; i++) {
                    if (!used.contains(i)) {
                        return String.format("%s%03d", prefix, i);
                    }
                }
                return null; // no ID available (shouldn't happen in practice)
            }
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
                + "role, manager_username, leave_balance, salary, last_net_salary "
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

    public List<Employee> searchEmployeesForManager(String searchText, String managerUsername) throws SQLException {
        String pattern = "%" + (searchText == null ? "" : searchText.trim()) + "%";
        String sql = "SELECT employee_id, full_name, job_title, department, email, phone, is_active, "
                + "role, manager_username, leave_balance, salary, last_net_salary "
                + "FROM employees "
                + "WHERE manager_username = ? "
                + "AND (employee_id LIKE ? OR full_name LIKE ? OR department LIKE ? OR role LIKE ?) "
                + "ORDER BY full_name ASC";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, managerUsername);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            statement.setString(5, pattern);
            try (ResultSet rs = statement.executeQuery()) {
                List<Employee> employees = new ArrayList<>();
                while (rs.next()) employees.add(mapEmployee(rs));
                return employees;
            }
        }
    }

    public List<Employee> searchManagersOnly(String searchText) throws SQLException {
        String pattern = "%" + (searchText == null ? "" : searchText.trim()) + "%";
        String sql = "SELECT employee_id, full_name, job_title, department, email, phone, is_active, "
                + "role, manager_username, leave_balance, salary, last_net_salary "
                + "FROM employees WHERE role = 'Manager' "
                + "AND (employee_id LIKE ? OR full_name LIKE ? OR department LIKE ?) "
                + "ORDER BY full_name ASC";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            try (ResultSet rs = statement.executeQuery()) {
                List<Employee> employees = new ArrayList<>();
                while (rs.next()) employees.add(mapEmployee(rs));
                return employees;
            }
        }
    }

    public boolean isEmployeeInManagerDept(String employeeId, String managerUsername) throws SQLException {
        String sql = "SELECT 1 FROM employees WHERE employee_id = ? AND manager_username = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, employeeId);
            statement.setString(2, managerUsername);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    public List<Employee> getAllActiveEmployees() throws SQLException {
        String sql = "SELECT employee_id, full_name, job_title, department, email, phone, is_active, "
                + "role, manager_username, leave_balance, salary, last_net_salary "
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

    /** Updates a manager employee record when assigned to or unassigned from a department. */
    public void updateManagerAssignment(String managerUsername, String department, String jobTitle) throws SQLException {
        String sql = "UPDATE employees SET department = ?, job_title = ? "
                + "WHERE role = 'Manager' AND employee_id IN "
                + "(SELECT employee_id FROM users WHERE username = ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, department);
            st.setString(2, jobTitle);
            st.setString(3, managerUsername);
            st.executeUpdate();
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

    // ── Deactivation Requests ────────────────────────────────────────────────

    public void submitDeactivationRequest(String employeeId, String requestedBy, String reason) throws SQLException {
        String sql = "INSERT INTO deactivation_requests (employee_id, requested_by, reason) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, employeeId);
            st.setString(2, requestedBy);
            st.setString(3, reason);
            st.executeUpdate();
        }
    }

    public boolean hasPendingDeactivationRequest(String employeeId) throws SQLException {
        String sql = "SELECT 1 FROM deactivation_requests WHERE employee_id = ? AND status = 'Pending'";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, employeeId);
            try (ResultSet rs = st.executeQuery()) { return rs.next(); }
        }
    }

    public List<DeactivationRequest> getPendingDeactivationRequests() throws SQLException {
        String sql = "SELECT dr.request_id, dr.employee_id, e.full_name, dr.requested_by, dr.reason, dr.status, dr.requested_at "
                + "FROM deactivation_requests dr "
                + "JOIN employees e ON e.employee_id = dr.employee_id "
                + "WHERE dr.status = 'Pending' ORDER BY dr.requested_at ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            List<DeactivationRequest> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new DeactivationRequest(
                        rs.getInt("request_id"), rs.getString("employee_id"),
                        rs.getString("full_name"), rs.getString("requested_by"),
                        rs.getString("reason"), rs.getString("status"), rs.getString("requested_at")));
            }
            return list;
        }
    }

    public String approveDeactivationRequest(int requestId, String decidedBy) throws SQLException {
        // Fetch the employee ID from the request
        String empId = null;
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(
                     "SELECT employee_id FROM deactivation_requests WHERE request_id = ?")) {
            st.setInt(1, requestId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) empId = rs.getString("employee_id");
            }
        }
        if (empId == null) return null;
        // Hard-delete the employee (this also removes the deactivation_requests rows)
        deleteEmployeeCompletely(empId);
        return empId;
    }

    public void rejectDeactivationRequest(int requestId, String decidedBy) throws SQLException {
        String sql = "UPDATE deactivation_requests SET status='Rejected', decided_by=?, decided_at=datetime('now') WHERE request_id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, decidedBy); st.setInt(2, requestId); st.executeUpdate();
        }
    }

    public static class DeactivationRequest {
        private final int requestId;
        private final String employeeId;
        private final String employeeName;
        private final String requestedBy;
        private final String reason;
        private final String status;
        private final String requestedAt;

        public DeactivationRequest(int requestId, String employeeId, String employeeName,
                                   String requestedBy, String reason, String status, String requestedAt) {
            this.requestId = requestId; this.employeeId = employeeId; this.employeeName = employeeName;
            this.requestedBy = requestedBy; this.reason = reason; this.status = status; this.requestedAt = requestedAt;
        }
        public int getRequestId()       { return requestId; }
        public String getEmployeeId()   { return employeeId; }
        public String getEmployeeName() { return employeeName; }
        public String getRequestedBy()  { return requestedBy; }
        public String getReason()       { return reason; }
        public String getStatus()       { return status; }
        public String getRequestedAt()  { return requestedAt; }
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
        Employee emp = new Employee(
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
        try { emp.setLastNetSalary(rs.getDouble("last_net_salary")); } catch (Exception ignored) {}
        return emp;
    }
}
