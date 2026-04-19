package com.ems;

public class PayrollRecord {

    private final int recordId;
    private final String employeeId;
    private String employeeName;
    private final int month;
    private final int year;
    private final double baseSalary;
    private final double overtimeHours;
    private final double overtimeRate;
    private final double deductions;
    private final double grossSalary;
    private final double netSalary;
    private final String generatedAt;
    private final String generatedBy;
    private final String notes;

    public PayrollRecord(int recordId, String employeeId, String employeeName,
                         int month, int year, double baseSalary,
                         double overtimeHours, double overtimeRate, double deductions,
                         double grossSalary, double netSalary,
                         String generatedAt, String generatedBy, String notes) {
        this.recordId = recordId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.month = month;
        this.year = year;
        this.baseSalary = baseSalary;
        this.overtimeHours = overtimeHours;
        this.overtimeRate = overtimeRate;
        this.deductions = deductions;
        this.grossSalary = grossSalary;
        this.netSalary = netSalary;
        this.generatedAt = generatedAt;
        this.generatedBy = generatedBy;
        this.notes = notes;
    }

    public int getRecordId() { return recordId; }
    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName != null ? employeeName : employeeId; }
    public void setEmployeeName(String name) { this.employeeName = name; }
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public double getBaseSalary() { return baseSalary; }
    public double getOvertimeHours() { return overtimeHours; }
    public double getOvertimeRate() { return overtimeRate; }
    public double getDeductions() { return deductions; }
    public double getGrossSalary() { return grossSalary; }
    public double getNetSalary() { return netSalary; }
    public String getGeneratedAt() { return generatedAt; }
    public String getGeneratedBy() { return generatedBy; }
    public String getNotes() { return notes; }

    public String getPeriod() {
        String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                           "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String monthName = (month >= 1 && month <= 12) ? months[month] : String.valueOf(month);
        return monthName + " " + year;
    }

    public String getFormattedNet() {
        return String.format("PKR %.2f", netSalary);
    }
}
