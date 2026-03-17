package com.ems;

public class AttendanceRecord {

    private final int recordId;
    private final String employeeId;
    private final String attendanceDate;
    private final String checkInAt;
    private final String checkOutAt;
    private final Double totalHours;

    public AttendanceRecord(int recordId, String employeeId, String attendanceDate,
                            String checkInAt, String checkOutAt, Double totalHours) {
        this.recordId = recordId;
        this.employeeId = employeeId;
        this.attendanceDate = attendanceDate;
        this.checkInAt = checkInAt;
        this.checkOutAt = checkOutAt;
        this.totalHours = totalHours;
    }

    public int getRecordId() {
        return recordId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getAttendanceDate() {
        return attendanceDate;
    }

    public String getCheckInAt() {
        return checkInAt;
    }

    public String getCheckOutAt() {
        return checkOutAt;
    }

    public Double getTotalHours() {
        return totalHours;
    }

    public String getDisplayTotalHours() {
        if (totalHours == null) {
            return "-";
        }
        return String.format("%.2f", totalHours);
    }
}