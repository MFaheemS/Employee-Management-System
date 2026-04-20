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

    public String getUsername()    { return username; }
    public String getRole()        { return role; }
    public String getEmployeeId()  { return employeeId; }

    // ── Role predicates ─────────────────────────────────────────────────────

    /** System administrator – manages users, employees, system config. */
    public boolean isAdmin()    { return "Admin".equalsIgnoreCase(role); }

    /** HR / People manager – manages leaves, payroll, views documents. */
    public boolean isManager()  { return "Manager".equalsIgnoreCase(role); }

    /** Regular employee. */
    public boolean isEmployee() { return "Employee".equalsIgnoreCase(role); }

    // ── Feature permissions ──────────────────────────────────────────────────

    /** Only HR Manager approves/rejects leave requests. */
    public boolean canManageLeaveApprovals() { return isManager(); }

    /** Admin manages Manager accounts; Manager manages Employee accounts in their dept. */
    public boolean canManageEmployees()      { return isAdmin() || isManager(); }

    /** Admin and Manager can search employees (each scoped to their own context). */
    public boolean canSearchEmployees()      { return isAdmin() || isManager(); }

    /** Admin generates payroll for employees; employees view their own. */
    public boolean canGeneratePayroll()      { return isAdmin(); }

    /** Manager can view documents for employees in their department. */
    public boolean canViewAllDocuments()     { return isManager(); }

    /** Employee uploads their own documents only. */
    public boolean canUploadDocuments()      { return isEmployee(); }

    /** Manager can approve, reject, and delete documents for their department employees. */
    public boolean canDeleteDocuments()      { return isManager(); }

    /** Admin can create and manage departments (each with a required manager). */
    public boolean canManageDepartments()    { return isAdmin(); }

    // ── Legacy alias ─────────────────────────────────────────────────────────
    @Deprecated
    public boolean canManageDocuments()      { return isManager(); }
}
