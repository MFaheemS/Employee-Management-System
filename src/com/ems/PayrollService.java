package com.ems;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PayrollService {

    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    /**
     * Generate payroll for an employee.
     * Only HR Managers can generate payroll.
     * After generation, updates the employee's last_net_salary so their profile
     * always reflects the most recent pay.
     */
    public PayrollRecord generatePayroll(AppUser generatedBy, String employeeId,
                                         int month, int year,
                                         double baseSalary, double overtimeHours,
                                         double overtimeRate, double deductions,
                                         String notes) throws SQLException {

        if (!generatedBy.canGeneratePayroll()) {
            throw new IllegalArgumentException(
                    "Only Administrators are authorized to generate payroll.");
        }

        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Please enter an Employee ID.");
        }

        Employee employee = employeeRepository.findById(employeeId);
        if (employee == null) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        if (!employee.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot generate payroll for an inactive employee.");
        }

        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }

        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Please enter a valid year (2000–2100).");
        }

        if (baseSalary < 0 || overtimeHours < 0 || overtimeRate < 0 || deductions < 0) {
            throw new IllegalArgumentException("Salary values cannot be negative.");
        }

        if (payrollExists(employeeId, month, year)) {
            throw new IllegalArgumentException(
                    "Payroll for " + employee.getFullName() + " already exists for this period.");
        }

        double grossSalary = baseSalary + (overtimeHours * overtimeRate);
        double netSalary   = Math.max(0, grossSalary - deductions);

        String insertSql = "INSERT INTO payroll_records "
                + "(employee_id, month, year, base_salary, overtime_hours, overtime_rate, "
                + "deductions, gross_salary, net_salary, generated_by, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int newId;
                try (PreparedStatement st = conn.prepareStatement(insertSql,
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    st.setString(1, employeeId);
                    st.setInt(2, month);
                    st.setInt(3, year);
                    st.setDouble(4, baseSalary);
                    st.setDouble(5, overtimeHours);
                    st.setDouble(6, overtimeRate);
                    st.setDouble(7, deductions);
                    st.setDouble(8, grossSalary);
                    st.setDouble(9, netSalary);
                    st.setString(10, generatedBy.getUsername());
                    st.setString(11, notes == null || notes.isBlank() ? null : notes.trim());
                    st.executeUpdate();

                    try (ResultSet keys = st.getGeneratedKeys()) {
                        newId = keys.next() ? keys.getInt(1) : -1;
                    }
                }

                // ── Update employee's last net salary ────────────────────
                // Keeps employee profile salary current after each payroll run.
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE employees SET last_net_salary = ? WHERE employee_id = ?")) {
                    upd.setDouble(1, netSalary);
                    upd.setString(2, employeeId);
                    upd.executeUpdate();
                }

                conn.commit();
                return new PayrollRecord(newId, employeeId, employee.getFullName(),
                        month, year, baseSalary, overtimeHours, overtimeRate,
                        deductions, grossSalary, netSalary, null,
                        generatedBy.getUsername(), notes);

            } catch (Exception e) {
                conn.rollback();
                throw e;
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

        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, employeeId);
            try (ResultSet rs = st.executeQuery()) { return readRecords(rs); }
        }
    }

    public List<PayrollRecord> getAllPayroll() throws SQLException {
        String sql = "SELECT pr.record_id, pr.employee_id, e.full_name, pr.month, pr.year, "
                + "pr.base_salary, pr.overtime_hours, pr.overtime_rate, pr.deductions, "
                + "pr.gross_salary, pr.net_salary, pr.generated_at, pr.generated_by, pr.notes "
                + "FROM payroll_records pr "
                + "JOIN employees e ON e.employee_id = pr.employee_id "
                + "ORDER BY pr.year DESC, pr.month DESC, e.full_name ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            return readRecords(rs);
        }
    }

    private boolean payrollExists(String employeeId, int month, int year) throws SQLException {
        String sql = "SELECT 1 FROM payroll_records WHERE employee_id = ? AND month = ? AND year = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, employeeId);
            st.setInt(2, month);
            st.setInt(3, year);
            try (ResultSet rs = st.executeQuery()) { return rs.next(); }
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
