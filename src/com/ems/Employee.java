package com.ems;

public class Employee {

    private final String employeeId;
    private String fullName;
    private String jobTitle;
    private String department;
    private String email;
    private boolean active;

    public Employee(String employeeId, String fullName, String jobTitle, String department, String email, boolean active) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.department = department;
        this.email = email;
        this.active = active;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
