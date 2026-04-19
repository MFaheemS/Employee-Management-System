package com.ems;

public class Employee {

    private final String employeeId;
    private String fullName;
    private String jobTitle;
    private String department;
    private String email;
    private String phone;
    private boolean active;
    private String role;
    private String managerUsername;
    private int leaveBalance;
    private double salary;

    public Employee(String employeeId, String fullName, String jobTitle, String department, String email, boolean active) {
        this(employeeId, fullName, jobTitle, department, email, null, active, "Employee", null, 20, 0.0);
    }

    public Employee(String employeeId, String fullName, String jobTitle, String department,
                    String email, boolean active, String role, String managerUsername, int leaveBalance) {
        this(employeeId, fullName, jobTitle, department, email, null, active, role, managerUsername, leaveBalance, 0.0);
    }

    public Employee(String employeeId, String fullName, String jobTitle, String department,
                    String email, String phone, boolean active, String role,
                    String managerUsername, int leaveBalance, double salary) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.department = department;
        this.email = email;
        this.phone = phone;
        this.active = active;
        this.role = role;
        this.managerUsername = managerUsername;
        this.leaveBalance = leaveBalance;
        this.salary = salary;
    }

    public String getEmployeeId() { return employeeId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getManagerUsername() { return managerUsername; }
    public void setManagerUsername(String managerUsername) { this.managerUsername = managerUsername; }

    public int getLeaveBalance() { return leaveBalance; }
    public void setLeaveBalance(int leaveBalance) { this.leaveBalance = leaveBalance; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }
}
