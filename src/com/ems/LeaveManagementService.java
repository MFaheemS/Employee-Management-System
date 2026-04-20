package com.ems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class LeaveManagementService {

    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    public Employee getEmployeeProfile(String employeeId) throws SQLException {
        return employeeRepository.findById(employeeId);
    }

    public String submitLeaveRequest(AppUser user, String leaveType,
                                     LocalDate startDate, LocalDate endDate) throws SQLException {
        if (user == null || user.getEmployeeId() == null || user.getEmployeeId().isBlank()) {
            throw new IllegalArgumentException("This account is not linked to an employee profile.");
        }

        if (leaveType == null || leaveType.isBlank()) {
            throw new IllegalArgumentException("Please select a leave type.");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Please select both start and end dates.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }

        Employee employee = employeeRepository.findById(user.getEmployeeId());
        if (employee == null || !employee.isActive()) {
            throw new IllegalArgumentException("Only active employees can submit leave requests.");
        }

        if (employee.getManagerUsername() == null || employee.getManagerUsername().isBlank()) {
            throw new IllegalArgumentException("No manager is assigned to this employee.");
        }

        int daysRequested = calculateRequestedDays(startDate, endDate);
        if (daysRequested > employee.getLeaveBalance()) {
            throw new IllegalArgumentException("Insufficient leave balance. Available balance: "
                    + employee.getLeaveBalance() + " day(s).");
        }

        String sql = "INSERT INTO leave_requests "
                + "(employee_id, manager_username, leave_type, start_date, end_date, days_requested, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'Pending')";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, employee.getEmployeeId());
            statement.setString(2, employee.getManagerUsername());
            statement.setString(3, leaveType);
            statement.setString(4, startDate.toString());
            statement.setString(5, endDate.toString());
            statement.setInt(6, daysRequested);
            statement.executeUpdate();
        }

        return employee.getManagerUsername();
    }

    public List<LeaveRequest> getRequestsForEmployee(String employeeId) throws SQLException {
        String sql = "SELECT lr.request_id, lr.employee_id, e.full_name, lr.manager_username, lr.leave_type, "
                + "lr.start_date, lr.end_date, lr.days_requested, lr.status, lr.comments, lr.requested_at, "
                + "lr.decision_at, lr.decision_by "
                + "FROM leave_requests lr "
                + "JOIN employees e ON e.employee_id = lr.employee_id "
                + "WHERE lr.employee_id = ? "
                + "ORDER BY lr.requested_at DESC, lr.request_id DESC";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, employeeId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return readLeaveRequests(resultSet);
            }
        }
    }

    public List<LeaveRequest> getPendingRequestsForApprover(AppUser user) throws SQLException {
        // Only Managers can approve; Admin dashboard uses a read-only overview query instead.
        String sql = buildApproverQuery(true);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            try (ResultSet resultSet = statement.executeQuery()) {
                return readLeaveRequests(resultSet);
            }
        }
    }

    public List<LeaveRequest> getDecisionHistoryForApprover(AppUser user) throws SQLException {
        String sql = buildApproverQuery(false);
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            try (ResultSet resultSet = statement.executeQuery()) {
                return readLeaveRequests(resultSet);
            }
        }
    }

    /** For Admin dashboard only – read-only overview of all pending leaves. */
    public List<LeaveRequest> getAllPendingLeaves() throws SQLException {
        String sql = "SELECT lr.request_id, lr.employee_id, e.full_name, lr.manager_username, lr.leave_type, "
                + "lr.start_date, lr.end_date, lr.days_requested, lr.status, lr.comments, lr.requested_at, "
                + "lr.decision_at, lr.decision_by "
                + "FROM leave_requests lr "
                + "JOIN employees e ON e.employee_id = lr.employee_id "
                + "WHERE lr.status = 'Pending' "
                + "ORDER BY lr.requested_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            return readLeaveRequests(rs);
        }
    }

    public String decideLeaveRequest(AppUser user, int requestId,
                                     String action, String comments) throws SQLException {
        if (user == null || !user.canManageLeaveApprovals()) {
            throw new IllegalArgumentException(
                    "Only HR Managers are authorized to approve or reject leave requests.");
        }

        String normalizedAction = action == null ? "" : action.trim();
        if (!"Approved".equalsIgnoreCase(normalizedAction)
                && !"Rejected".equalsIgnoreCase(normalizedAction)) {
            throw new IllegalArgumentException("Action must be Approve or Reject.");
        }

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);

            LeaveRequest request = findRequestById(connection, requestId);
            if (request == null) {
                throw new IllegalArgumentException("Leave request not found.");
            }

            if (!"Pending".equalsIgnoreCase(request.getStatus())) {
                throw new IllegalArgumentException("This leave request has already been processed.");
            }

            if (!user.isAdmin() && !user.getUsername().equalsIgnoreCase(request.getManagerUsername())) {
                throw new IllegalArgumentException("Unauthorized access attempt.");
            }

            if ("Approved".equalsIgnoreCase(normalizedAction)) {
                deductLeaveBalance(connection, request.getEmployeeId(), request.getDaysRequested());
            }

            try (PreparedStatement updateStatement = connection.prepareStatement(
                    "UPDATE leave_requests SET status = ?, comments = ?, decision_at = CURRENT_TIMESTAMP, "
                            + "decision_by = ? WHERE request_id = ?")) {
                updateStatement.setString(1, capitalize(normalizedAction));
                updateStatement.setString(2, comments == null || comments.isBlank() ? null : comments.trim());
                updateStatement.setString(3, user.getUsername());
                updateStatement.setInt(4, requestId);
                updateStatement.executeUpdate();
            }

            connection.commit();
            return capitalize(normalizedAction);
        }
    }

    private void deductLeaveBalance(Connection connection, String employeeId, int daysRequested) throws SQLException {
        try (PreparedStatement updateStatement = connection.prepareStatement(
                "UPDATE employees SET leave_balance = leave_balance - ? "
                        + "WHERE employee_id = ? AND leave_balance >= ?")) {
            updateStatement.setInt(1, daysRequested);
            updateStatement.setString(2, employeeId);
            updateStatement.setInt(3, daysRequested);

            if (updateStatement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Insufficient leave balance at approval time.");
            }
        }
    }

    private LeaveRequest findRequestById(Connection connection, int requestId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT lr.request_id, lr.employee_id, e.full_name, lr.manager_username, lr.leave_type, "
                        + "lr.start_date, lr.end_date, lr.days_requested, lr.status, lr.comments, lr.requested_at, "
                        + "lr.decision_at, lr.decision_by "
                        + "FROM leave_requests lr "
                        + "JOIN employees e ON e.employee_id = lr.employee_id "
                        + "WHERE lr.request_id = ?")) {
            statement.setInt(1, requestId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<LeaveRequest> requests = readLeaveRequests(resultSet);
                return requests.isEmpty() ? null : requests.get(0);
            }
        }
    }

    private List<LeaveRequest> readLeaveRequests(ResultSet resultSet) throws SQLException {
        List<LeaveRequest> requests = new ArrayList<>();
        while (resultSet.next()) {
            requests.add(new LeaveRequest(
                    resultSet.getInt("request_id"),
                    resultSet.getString("employee_id"),
                    resultSet.getString("full_name"),
                    resultSet.getString("manager_username"),
                    resultSet.getString("leave_type"),
                    resultSet.getString("start_date"),
                    resultSet.getString("end_date"),
                    resultSet.getInt("days_requested"),
                    resultSet.getString("status"),
                    resultSet.getString("comments"),
                    resultSet.getString("requested_at"),
                    resultSet.getString("decision_at"),
                    resultSet.getString("decision_by")
            ));
        }
        return requests;
    }

    private String buildApproverQuery(boolean pendingOnly) {
        String statusFilter = pendingOnly ? "lr.status = 'Pending'" : "lr.status <> 'Pending'";
        return "SELECT lr.request_id, lr.employee_id, e.full_name, lr.manager_username, lr.leave_type, "
                + "lr.start_date, lr.end_date, lr.days_requested, lr.status, lr.comments, lr.requested_at, "
                + "lr.decision_at, lr.decision_by "
                + "FROM leave_requests lr "
                + "JOIN employees e ON e.employee_id = lr.employee_id "
                + "WHERE " + statusFilter + " AND lr.manager_username = ? "
                + "ORDER BY lr.requested_at DESC, lr.request_id DESC";
    }

    private int calculateRequestedDays(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }
}