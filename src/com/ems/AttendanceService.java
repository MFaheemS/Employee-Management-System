package com.ems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AttendanceService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    public AttendanceRecord markAttendance(AppUser user) throws SQLException {
        if (user == null || user.getEmployeeId() == null || user.getEmployeeId().isBlank()) {
            throw new IllegalArgumentException("This account is not linked to an employee profile.");
        }

        Employee employee = employeeRepository.findById(user.getEmployeeId());
        if (employee == null || !employee.isActive()) {
            throw new IllegalArgumentException("Only active employees can mark attendance.");
        }

        LocalDate today = LocalDate.now();
        String dateText = today.format(DATE_FORMAT);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String nowText = now.format(DATE_TIME_FORMAT);

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);

            AttendanceRecord existing = getRecordForDate(connection, user.getEmployeeId(), dateText);
            if (existing == null) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO attendance_records (employee_id, attendance_date, check_in_at) VALUES (?, ?, ?)")) {
                    statement.setString(1, user.getEmployeeId());
                    statement.setString(2, dateText);
                    statement.setString(3, nowText);
                    statement.executeUpdate();
                }

                connection.commit();
                return getRecordForDate(connection, user.getEmployeeId(), dateText);
            }

            if (existing.getCheckInAt() == null || existing.getCheckInAt().isBlank()) {
                throw new IllegalArgumentException("Missing check-in for today's record.");
            }

            if (existing.getCheckOutAt() != null && !existing.getCheckOutAt().isBlank()) {
                throw new IllegalArgumentException("Attendance already completed for today.");
            }

            LocalDateTime checkInTime = LocalDateTime.parse(existing.getCheckInAt(), DATE_TIME_FORMAT);
            double totalHours = Duration.between(checkInTime, now).toMinutes() / 60.0;
            if (totalHours < 0) {
                throw new IllegalArgumentException("System time sync error detected.");
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE attendance_records SET check_out_at = ?, total_hours = ? WHERE record_id = ?")) {
                statement.setString(1, nowText);
                statement.setDouble(2, totalHours);
                statement.setInt(3, existing.getRecordId());
                statement.executeUpdate();
            }

            connection.commit();
            return getRecordForDate(connection, user.getEmployeeId(), dateText);
        }
    }

    public AttendanceRecord getTodayRecord(String employeeId) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            return getRecordForDate(connection, employeeId, LocalDate.now().format(DATE_FORMAT));
        }
    }

    public List<AttendanceRecord> getRecentRecords(String employeeId, int limit) throws SQLException {
        String sql = "SELECT record_id, employee_id, attendance_date, check_in_at, check_out_at, total_hours "
                + "FROM attendance_records WHERE employee_id = ? ORDER BY attendance_date DESC LIMIT ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, employeeId);
            statement.setInt(2, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<AttendanceRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(mapRecord(resultSet));
                }
                return records;
            }
        }
    }

    public List<AttendanceRecord> getRecordsByDate(String date) throws SQLException {
        String sql = "SELECT r.record_id, r.employee_id, e.full_name, r.attendance_date, "
                + "r.check_in_at, r.check_out_at, r.total_hours "
                + "FROM attendance_records r "
                + "JOIN employees e ON r.employee_id = e.employee_id "
                + "WHERE r.attendance_date = ? "
                + "ORDER BY e.full_name ASC";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AttendanceRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    double dbHours = resultSet.getDouble("total_hours");
                    Double totalHours = resultSet.wasNull() ? null : dbHours;
                    AttendanceRecord record = new AttendanceRecord(
                            resultSet.getInt("record_id"),
                            resultSet.getString("employee_id"),
                            resultSet.getString("attendance_date"),
                            resultSet.getString("check_in_at"),
                            resultSet.getString("check_out_at"),
                            totalHours
                    );
                    record.setEmployeeName(resultSet.getString("full_name"));
                    records.add(record);
                }
                return records;
            }
        }
    }

    private AttendanceRecord getRecordForDate(Connection connection, String employeeId,
                                              String dateText) throws SQLException {
        String sql = "SELECT record_id, employee_id, attendance_date, check_in_at, check_out_at, total_hours "
                + "FROM attendance_records WHERE employee_id = ? AND attendance_date = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, employeeId);
            statement.setString(2, dateText);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapRecord(resultSet);
            }
        }
    }

    private AttendanceRecord mapRecord(ResultSet resultSet) throws SQLException {
        Double totalHours = null;
        double dbHours = resultSet.getDouble("total_hours");
        if (!resultSet.wasNull()) {
            totalHours = dbHours;
        }

        return new AttendanceRecord(
                resultSet.getInt("record_id"),
                resultSet.getString("employee_id"),
                resultSet.getString("attendance_date"),
                resultSet.getString("check_in_at"),
                resultSet.getString("check_out_at"),
                totalHours
        );
    }
}