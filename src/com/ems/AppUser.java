package com.ems;

public class AppUser {

    private final String username;
    private final String role;
    private final String employeeId;

    public AppUser(String username, String role, String employeeId) {
        this.username = username;
        this.role = role;
        this.employeeId = employeeId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(role);
    }

    public boolean isManager() {
        return "Manager".equalsIgnoreCase(role);
    }

    public boolean isEmployee() {
        return "Employee".equalsIgnoreCase(role);
    }

    public boolean canManageLeaveApprovals() {
        return isAdmin() || isManager();
    }

    public boolean canManageEmployees() {
        return isAdmin() || isManager();
    }

    public boolean canSearchEmployees() {
        return isAdmin() || isManager();
    }
}