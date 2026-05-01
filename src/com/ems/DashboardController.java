package com.ems;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardController extends BaseController {

    private final LeaveManagementService leaveService     = new LeaveManagementService();
    private final EmployeeRepository     employeeRepo     = new EmployeeRepository();

    // ── Shared header ────────────────────────────────────────────────────────
    @FXML private Label userLabel;
    @FXML private Label welcomeLabel;

    // ── KPI labels (Admin only) ───────────────────────────────────────────────
    @FXML private Label totalEmployeesLabel;
    @FXML private Label activeEmployeesLabel;

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML private Button dashboardNavButton;
    @FXML private Button employeeAddNavButton;
    @FXML private Button employeeDeactivateNavButton;
    @FXML private Button employeeSearchNavButton;
    @FXML private Button attendanceNavButton;
    @FXML private Button leaveApplyNavButton;
    @FXML private Button leaveApprovalsNavButton;
    @FXML private Button payrollNavButton;
    @FXML private Button documentsNavButton;
    @FXML private Button departmentNavButton;

    // ── KPI strip (Admin + Manager only) ────────────────────────────────────
    @FXML private HBox kpiSection;

    // ── Admin-only section ────────────────────────────────────────────────────
    @FXML private VBox adminSection;
    @FXML private TableView<DeptRow> deptTable;
    @FXML private TableColumn<DeptRow, String>  colDept;
    @FXML private TableColumn<DeptRow, String>  colDeptManager;
    @FXML private TableColumn<DeptRow, Integer> colDeptCount;
    @FXML private TableColumn<DeptRow, Integer> colDeptActive;

    // ── Manager section ───────────────────────────────────────────────────────
    @FXML private VBox unassignedManagerSection;
    @FXML private VBox managerSection;
    @FXML private Label managerTeamSizeLabel;
    @FXML private Label managerPendingLeavesLabel;
    @FXML private Label managerTodayAttendanceLabel;
    @FXML private Label managerPayrollDueLabel;
    @FXML private TableView<LeaveRequest> pendingLeaveTable;
    @FXML private TableColumn<LeaveRequest, String> colMgrLeaveEmp;
    @FXML private TableColumn<LeaveRequest, String> colMgrLeaveType;
    @FXML private TableColumn<LeaveRequest, String> colMgrLeaveDates;
    @FXML private TableColumn<LeaveRequest, Integer> colMgrLeaveDays;

    // ── Employee profile section ──────────────────────────────────────────────
    @FXML private VBox employeeSection;
    @FXML private Label empNameLabel;
    @FXML private Label empIdLabel;
    @FXML private Label empTitleLabel;
    @FXML private Label empDeptLabel;
    @FXML private Label empEmailLabel;
    @FXML private Label empPhoneLabel;
    @FXML private Label empStatusLabel;
    @FXML private Label empLeaveBalanceLabel;
    @FXML private Label empBaseSalaryLabel;
    @FXML private Label empLastPaidLabel;
    @FXML private Label empTodayAttLabel;
    @FXML private Label empManagerLabel;


    @FXML
    private void initialize() {
        if (!ensureLoggedIn()) return;

        configureSidebarNavigation(userLabel, employeeAddNavButton, employeeDeactivateNavButton,
                employeeSearchNavButton, attendanceNavButton, leaveApplyNavButton, leaveApprovalsNavButton);
        configureAdditionalNavigation(dashboardNavButton, payrollNavButton, documentsNavButton);
        configureDepartmentNavigation(departmentNavButton);

        AppUser user = currentUser();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"));
        welcomeLabel.setText(dateStr);

        // Show role-specific sections (manager dept check handled after role branch)
        showHBox(kpiSection, user.isAdmin());
        showSection(adminSection,            user.isAdmin());
        showSection(managerSection,          false); // set in manager branch below
        showSection(unassignedManagerSection, false); // set in manager branch below
        showSection(employeeSection, user.isEmployee()
                || (user.getEmployeeId() != null && !user.getEmployeeId().isBlank()));

        try {
            if (user.isAdmin()) {
                loadAdminKPIs();
            } else if (user.isManager()) {
                boolean hasDept = isManagerWithDepartment();
                showSection(unassignedManagerSection, !hasDept);
                showSection(managerSection, hasDept);
                if (hasDept) loadManagerDashboard();
                // If the manager also has a linked employee profile, load it as well
                if (user.getEmployeeId() != null && !user.getEmployeeId().isBlank()) {
                    loadEmployeeDashboard();
                }
            } else {
                loadEmployeeDashboard();
            }
        } catch (SQLException e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Dashboard Error", e.getMessage());
        }
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    private void loadAdminKPIs() throws SQLException {
        try (Connection conn = Database.getConnection()) {
            setLabel(totalEmployeesLabel, count(conn, "SELECT COUNT(*) FROM employees"));
            setLabel(activeEmployeesLabel, count(conn, "SELECT COUNT(*) FROM employees WHERE is_active = 1"));

            // Department table with manager name
            if (deptTable != null) {
                configureDeptTable();
                List<DeptRow> rows = new ArrayList<>();
                try (PreparedStatement st = conn.prepareStatement(
                        "SELECT e.department, COALESCE(d.manager_username, '—') as manager_username, "
                        + "COUNT(*) as total, SUM(e.is_active) as active "
                        + "FROM employees e "
                        + "LEFT JOIN departments d ON d.department_name = e.department "
                        + "GROUP BY e.department ORDER BY e.department ASC");
                     ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new DeptRow(rs.getString("department"),
                                rs.getString("manager_username"),
                                rs.getInt("total"), rs.getInt("active")));
                    }
                }
                deptTable.setItems(FXCollections.observableArrayList(rows));
            }
        }
    }

    // ── Manager ──────────────────────────────────────────────────────────────

    private void loadManagerDashboard() throws SQLException {
        AppUser user = currentUser();
        try (Connection conn = Database.getConnection()) {
            // Team size (employees whose manager = this user)
            int teamSize = 0;
            try (PreparedStatement st = conn.prepareStatement(
                    "SELECT COUNT(*) FROM employees WHERE manager_username = ? AND is_active = 1")) {
                st.setString(1, user.getUsername());
                try (ResultSet rs = st.executeQuery()) { if (rs.next()) teamSize = rs.getInt(1); }
            }
            setLabel(managerTeamSizeLabel, teamSize);

            // Pending leaves assigned to this manager
            List<LeaveRequest> pending = leaveService.getPendingRequestsForApprover(user);
            setLabel(managerPendingLeavesLabel, pending.size());

            // Employees not yet paid this month
            LocalDate now = LocalDate.now();
            int unpaid = 0;
            try (PreparedStatement st = conn.prepareStatement(
                    "SELECT COUNT(*) FROM employees e "
                    + "WHERE e.is_active = 1 AND e.manager_username = ? "
                    + "AND NOT EXISTS (SELECT 1 FROM payroll_records p "
                    + "WHERE p.employee_id = e.employee_id AND p.month = ? AND p.year = ?)")) {
                st.setString(1, user.getUsername());
                st.setInt(2, now.getMonthValue());
                st.setInt(3, now.getYear());
                try (ResultSet rs = st.executeQuery()) { if (rs.next()) unpaid = rs.getInt(1); }
            }
            setLabel(managerPayrollDueLabel, unpaid);

            // Today attendance for team
            String today = LocalDate.now().toString();
            int todayAtt = 0;
            try (PreparedStatement st = conn.prepareStatement(
                    "SELECT COUNT(*) FROM attendance_records ar "
                    + "JOIN employees e ON e.employee_id = ar.employee_id "
                    + "WHERE ar.attendance_date = ? AND e.manager_username = ?")) {
                st.setString(1, today);
                st.setString(2, user.getUsername());
                try (ResultSet rs = st.executeQuery()) { if (rs.next()) todayAtt = rs.getInt(1); }
            }
            setLabel(managerTodayAttendanceLabel, todayAtt);
        }

        // Pending leave table
        if (pendingLeaveTable != null) {
            configureManagerLeaveTable();
            List<LeaveRequest> pending = leaveService.getPendingRequestsForApprover(user);
            pendingLeaveTable.setItems(FXCollections.observableArrayList(pending));
        }
    }

    // ── Employee ─────────────────────────────────────────────────────────────

    private void loadEmployeeDashboard() throws SQLException {
        AppUser user = currentUser();
        String eid = user.getEmployeeId();
        if (eid == null || eid.isBlank()) return;

        Employee emp = employeeRepo.findById(eid);
        if (emp == null) return;

        setLabelText(empNameLabel,    emp.getFullName());
        setLabelText(empIdLabel,      emp.getEmployeeId());
        setLabelText(empTitleLabel,   emp.getJobTitle());
        setLabelText(empDeptLabel,    emp.getDepartment());
        setLabelText(empEmailLabel,   emp.getEmail());
        setLabelText(empPhoneLabel,   emp.getPhone() != null && !emp.getPhone().isBlank()
                                            ? emp.getPhone() : "—");
        setLabelText(empStatusLabel,  emp.isActive() ? "✅  Active" : "⛔  Inactive");
        setLabelText(empLeaveBalanceLabel, emp.getLeaveBalance() + " days remaining");
        setLabelText(empBaseSalaryLabel,   "PKR " + String.format("%,.0f", emp.getSalary()));
        setLabelText(empLastPaidLabel,
                emp.getLastNetSalary() > 0
                        ? "PKR " + String.format("%,.0f", emp.getLastNetSalary())
                        : "No payroll processed yet");
        setLabelText(empManagerLabel, emp.getManagerUsername() != null ? emp.getManagerUsername() : "—");

        // Today's attendance
        try {
            String today = LocalDate.now().toString();
            try (Connection conn = Database.getConnection();
                 PreparedStatement st = conn.prepareStatement(
                         "SELECT check_in_at, check_out_at FROM attendance_records "
                         + "WHERE employee_id = ? AND attendance_date = ?")) {
                st.setString(1, eid);
                st.setString(2, today);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        String in  = rs.getString("check_in_at");
                        String out = rs.getString("check_out_at");
                        String status = "Checked in" + (in != null ? " at " + in.substring(11, 16) : "");
                        if (out != null) status = "Checked out at " + out.substring(11, 16);
                        setLabelText(empTodayAttLabel, status);
                    } else {
                        setLabelText(empTodayAttLabel, "Not checked in yet today");
                    }
                }
            }
        } catch (Exception ignored) {
            setLabelText(empTodayAttLabel, "—");
        }
    }

    // ── Table config ─────────────────────────────────────────────────────────

    private void configureDeptTable() {
        deptTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colDeptManager.setCellValueFactory(new PropertyValueFactory<>("managerUsername"));
        colDeptCount.setCellValueFactory(new PropertyValueFactory<>("total"));
        colDeptActive.setCellValueFactory(new PropertyValueFactory<>("active"));
    }

    private void configureManagerLeaveTable() {
        pendingLeaveTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colMgrLeaveEmp.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colMgrLeaveType.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        colMgrLeaveDates.setCellValueFactory(cellData -> Bindings.createStringBinding(
                () -> cellData.getValue().getStartDate() + " → " + cellData.getValue().getEndDate()));
        colMgrLeaveDays.setCellValueFactory(new PropertyValueFactory<>("daysRequested"));
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void goToDashboard()       { /* already here */ }
    @FXML private void goToAdd()             { navigate(Main::showEmployeeAdd); }
    @FXML private void goToDeactivate()      { navigate(Main::showEmployeeDeactivate); }
    @FXML private void goToDepartments()     { navigate(Main::showDepartmentManagement); }
    @FXML private void goToEmployeeSearch()  { navigate(Main::showEmployeeSearch); }
    @FXML private void goToAttendance()      { navigate(Main::showAttendance); }
    @FXML private void goToLeaveApply()      { navigate(Main::showLeaveApplication); }
    @FXML private void goToLeaveApprovals()  { navigate(Main::showLeaveApprovals); }
    @FXML private void goToPayroll()         { navigate(Main::showPayroll); }
    @FXML private void goToDocuments()       { navigate(Main::showDocuments); }
    @FXML protected void handleLogout()      { super.handleLogout(); }

    private void navigate(NavigationAction action) {
        try { action.run(); } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showSection(VBox section, boolean visible) {
        if (section == null) return;
        section.setVisible(visible);
        section.setManaged(visible);
    }

    private void showHBox(HBox box, boolean visible) {
        if (box == null) return;
        box.setVisible(visible);
        box.setManaged(visible);
    }

    private void setLabel(Label label, int value) {
        if (label != null) label.setText(String.valueOf(value));
    }

    private void setLabelText(Label label, String text) {
        if (label != null) label.setText(text != null ? text : "—");
    }

    private int count(Connection conn, String sql) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    public static class DeptRow {
        private final String department;
        private final String managerUsername;
        private final int    total;
        private final int    active;

        public DeptRow(String department, String managerUsername, int total, int active) {
            this.department      = department;
            this.managerUsername = managerUsername;
            this.total           = total;
            this.active          = active;
        }

        public String getDepartment()      { return department; }
        public String getManagerUsername() { return managerUsername; }
        public int    getTotal()           { return total; }
        public int    getActive()          { return active; }
    }

    @FunctionalInterface
    private interface NavigationAction { void run() throws Exception; }
}
