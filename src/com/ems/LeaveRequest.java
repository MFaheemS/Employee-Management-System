package com.ems;

public class LeaveRequest {

    private final int requestId;
    private final String employeeId;
    private final String employeeName;
    private final String managerUsername;
    private final String leaveType;
    private final String startDate;
    private final String endDate;
    private final int daysRequested;
    private final String status;
    private final String comments;
    private final String requestedAt;
    private final String decisionAt;
    private final String decisionBy;

    public LeaveRequest(int requestId, String employeeId, String employeeName, String managerUsername,
                        String leaveType, String startDate, String endDate, int daysRequested,
                        String status, String comments, String requestedAt,
                        String decisionAt, String decisionBy) {
        this.requestId = requestId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.managerUsername = managerUsername;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.daysRequested = daysRequested;
        this.status = status;
        this.comments = comments;
        this.requestedAt = requestedAt;
        this.decisionAt = decisionAt;
        this.decisionBy = decisionBy;
    }

    public int getRequestId() {
        return requestId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getManagerUsername() {
        return managerUsername;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public int getDaysRequested() {
        return daysRequested;
    }

    public String getStatus() {
        return status;
    }

    public String getComments() {
        return comments;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

    public String getDecisionAt() {
        return decisionAt;
    }

    public String getDecisionBy() {
        return decisionBy;
    }
}