package com.ems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PayrollService {

    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    public PayrollRecord generatePayroll(AppUser generatedBy, String employeeId,
                                         int month, int year,
                                         double baseSalary, double overtimeHours,
                                         double overtimeRate, double deductions,
                                         String notes) throws SQLException {

        if (!generatedBy.canGeneratePayroll()) {
            throw new IllegalArgumentException("You are not authorized to generate payroll.");
        }

        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Please enter an Employee ID.");
        }

        Employee employee = employeeRepository.findById(employeeId);
        if (employee == null) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }

        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Please enter a valid year.");
        }

        if (baseSalary < 0 || overtimeHours < 0 || overtimeRate < 0 || deductions < 0) {
            throw new IllegalArgumentException("Salary values cannot be negative.");
        }

        // Check for duplicate payroll record for same employee/month/year
        if (payrollExists(employeeId, month, year)) {
            throw new IllegalArgumentException(
                    "Payroll for " + employee.getFullName() + " already exists for this period.");
        }

        double grossSalary = baseSalary + (overtimeHours * overtimeRate);
        double netSalary = Math.max(0, grossSalary - deductions);

        String sql = "INSERT INTO payroll_records "
                + "(employee_id, month, year, base_salary, overtime_hours, overtime_rate, "
                + "deductions, gross_salary, net_salary, generated_by, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, employeeId);
            statement.setInt(2, month);
            statement.setInt(3, year);
            statement.setDouble(4, baseSalary);
            statement.setDouble(5, overtimeHours);
            statement.setDouble(6, overtimeRate);
            statement.setDouble(7, deductions);
            statement.setDouble(8, grossSalary);
            statement.setDouble(9, netSalary);
            statement.setString(10, generatedBy.getUsername());
            statement.setString(11, notes == null || notes.isBlank() ? null : notes.trim());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                int newId = keys.next() ? keys.getInt(1) : -1;
                return new PayrollRecord(newId, employeeId, employee.getFullName(),
                        month, year, baseSalary, overtimeHours, overtimeRate,
                        deductions, grossSalary, netSalary, null,
                        generatedBy.getUsername(), notes);
            }
        }
    }

    public List<PayrollRecord> getPayrollForEmployee(String employeeId) throws SQLException {
        String sql = "SELECT pr.record_id, pr.employee_id, e.full_name, pr.month, pr.year, "
                + "pr.base_salary, pr.overtime_hours, pr.overtime_rate, pr.deductions, "
                + "pr.gross_salary, pr.net_salary, pr.generated_at, pr.generated_by, pr.notes "
                + "FROM payroll_records pr "
                + "JOIN employees e ON e.employee_id = pr.employee_id "
                + "WHERE pr.employee_id = ? "
                + "ORDER BY pr.year DESC, pr.month DESC";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, employeeId);
            try (ResultSet rs = statement.executeQuery()) {
                return readRecords(rs);
            }
        }
    }

    public List<PayrollRecord> getAllPayroll() throws SQLException {
        String sql = "SELECT pr.record_id, pr.employee_id, e.full_name, pr.month, pr.year, "
                + "pr.base_salary, pr.overtime_hours, pr.overtime_rate, pr.deductions, "
                + "pr.gross_salary, pr.net_salary, pr.generated_at, pr.generated_by, pr.notes "
                + "FROM payroll_records pr "
                + "JOIN employees e ON e.employee_id = pr.employee_id "
                + "ORDER BY pr.year DESC, pr.month DESC, e.full_name ASC";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return readRecords(rs);
        }
    }

    private boolean payrollExists(String employeeId, int month, int year) throws SQLException {
        String sql = "SELECT 1 FROM payroll_records WHERE employee_id = ? AND month = ? AND year = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, employeeId);
            statement.setInt(2, month);
            statement.setInt(3, year);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<PayrollRecord> readRecords(ResultSet rs) throws SQLException {
        List<PayrollRecord> records = new ArrayList<>();
        while (rs.next()) {
            records.add(new PayrollRecord(
                    rs.getInt("record_id"),
                    rs.getString("employee_id"),
                    rs.getString("full_name"),
                    rs.getInt("month"),
                    rs.getInt("year"),
                    rs.getDouble("base_salary"),
                    rs.getDouble("overtime_hours"),
                    rs.getDouble("overtime_rate"),
                    rs.getDouble("deductions"),
                    rs.getDouble("gross_salary"),
                    rs.getDouble("net_salary"),
                    rs.getString("generated_at"),
                    rs.getString("generated_by"),
                    rs.getString("notes")
            ));
        }
        return records;
    }
}
