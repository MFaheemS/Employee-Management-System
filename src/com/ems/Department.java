package com.ems;

public class Department {

    private final int    departmentId;
    private final String departmentName;
    private final String managerUsername;
    private final String createdAt;

    public Department(int departmentId, String departmentName,
                      String managerUsername, String createdAt) {
        this.departmentId    = departmentId;
        this.departmentName  = departmentName;
        this.managerUsername = managerUsername;
        this.createdAt       = createdAt;
    }

    public int    getDepartmentId()    { return departmentId; }
    public String getDepartmentName()  { return departmentName; }
    public String getManagerUsername() { return managerUsername; }
    public String getCreatedAt()       { return createdAt; }
}
