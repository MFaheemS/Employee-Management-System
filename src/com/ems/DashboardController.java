package com.ems;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DashboardController extends BaseController {

    private final LeaveManagementService leaveService = new LeaveManagementService();
    private final EmployeeRepository employeeRepository = new EmployeeRepository();
    private final AttendanceService attendanceService = new AttendanceService();

    @FXML private Label userLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label totalEmployeesLabel;
    @FXML private Label activeEmployeesLabel;
    @FXML private Label pendingLeavesLabel;
    @FXML private Label todayAttendanceLabel;

    @FXML private Button dashboardNavButton;
    @FXML private Button employeeAddNavButton;
    @FXML private Button employeeDeactivateNavButton;
    @FXML private Button employeeSearchNavButton;
    @FXML private Button attendanceNavButton;
    @FXML private Button leaveApplyNavButton;
    @FXML private Button leaveApprovalsNavButton;
    @FXML private Button payrollNavButton;
    @FXML private Button documentsNavButton;

    @FXML private TableView<DeptRow> deptTable;
    @FXML private TableColumn<DeptRow, String> colDept;
    @FXML private TableColumn<DeptRow, Integer> colDeptCount;
    @FXML private TableColumn<DeptRow, Integer> colDeptActive;

    @FXML private VBox leaveSection;
    @FXML private TableView<LeaveRequest> leaveTable;
    @FXML private TableColumn<LeaveRequest, String> colLeaveEmp;
    @FXML private TableColumn<LeaveRequest, String> colLeaveType;
    @FXML private TableColumn<LeaveRequest, String> colLeaveDates;
    @FXML private TableColumn<LeaveRequest, String> colLeaveStatus;
    @FXML private TableColumn<LeaveRequest, String> colLeaveManager;

    @FXML private VBox profileSection;
    @FXML private Label profileNameLabel;
    @FXML private Label profileDetailLabel;
    @FXML private Label profileLeaveLabel;

    @FXML
    private void initialize() {
        if (!ensureLoggedIn()) return;

        configureSidebarNavigation(userLabel, employeeAddNavButton, employeeDeactivateNavButton,
                employeeSearchNavButton, attendanceNavButton, leaveApplyNavButton, leaveApprovalsNavButton);
        configureAdditionalNavigation(dashboardNavButton, payrollNavButton, documentsNavButton);

        AppUser user = currentUser();
        welcomeLabel.setText("Welcome back, " + user.getUsername() + "! Here is your workforce overview.");

        configureTables();
        loadKPIs();

        boolean isManagerOrAdmin = user.canManageLeaveApprovals();
        leaveSection.setVisible(isManagerOrAdmin);
        leaveSection.setManaged(isManagerOrAdmin);

        boolean hasProfile = user.getEmployeeId() != null && !user.getEmployeeId().isBlank();
        profileSection.setVisible(hasProfile);
        profileSection.setManaged(hasProfile);

        if (isManagerOrAdmin) loadRecentLeaves();
        if (hasProfile) loadMyProfile();
    }

    @FXML private void goToDashboard() { /* already here */ }
    @FXML private void goToAdd() { navigate(() -> Main.showEmployeeAdd()); }
    @FXML private void goToDeactivate() { navigate(() -> Main.showEmployeeDeactivate()); }
    @FXML private void goToEmployeeSearch() { navigate(() -> Main.showEmployeeSearch()); }
    @FXML private void goToAttendance() { navigate(() -> Main.showAttendance()); }
    @FXML private void goToLeaveApply() { navigate(() -> Main.showLeaveApplication()); }
    @FXML private void goToLeaveApprovals() { navigate(() -> Main.showLeaveApprovals()); }
    @FXML private void goToPayroll() { navigate(() -> Main.showPayroll()); }
    @FXML private void goToDocuments() { navigate(() -> Main.showDocuments()); }
    @FXML protected void handleLogout() { super.handleLogout(); }

    private void navigate(NavigationAction action) {
        try { action.run(); } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    private void loadKPIs() {
        try (Connection conn = Database.getConnection()) {
            // Total employees
            try (PreparedStatement st = conn.prepareStatement("SELECT COUNT(*) FROM employees");
                 ResultSet rs = st.executeQuery()) {
                totalEmployeesLabel.setText(rs.next() ? String.valueOf(rs.getInt(1)) : "0");
            }

            // Active employees
            try (PreparedStatement st = conn.prepareStatement(
                    "SELECT COUNT(*) FROM employees WHERE is_active = 1");
                 ResultSet rs = st.executeQuery()) {
                activeEmployeesLabel.setText(rs.next() ? String.valueOf(rs.getInt(1)) : "0");
            }

            // Pending leaves
            try (PreparedStatement st = conn.prepareStatement(
                    "SELECT COUNT(*) FROM leave_requests WHERE status = 'Pending'");
                 ResultSet rs = st.executeQuery()) {
                pendingLeavesLabel.setText(rs.next() ? String.valueOf(rs.getInt(1)) : "0");
            }

            // Today's attendance
            String today = LocalDate.now().toString();
            try (PreparedStatement st = conn.prepareStatement(
                    "SELECT COUNT(*) FROM attendance_records WHERE attendance_date = ?")) {
                st.setString(1, today);
                try (ResultSet rs = st.executeQuery()) {
                    todayAttendanceLabel.setText(rs.next() ? String.valueOf(rs.getInt(1)) : "0");
                }
            }

            // Department table
            List<DeptRow> deptRows = new ArrayList<>();
            try (PreparedStatement st = conn.prepareStatement(
                    "SELECT department, COUNT(*) as total, SUM(is_active) as active "
                    + "FROM employees GROUP BY department ORDER BY department ASC");
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    deptRows.add(new DeptRow(rs.getString("department"),
                            rs.getInt("total"), rs.getInt("active")));
                }
            }
            deptTable.setItems(FXCollections.observableArrayList(deptRows));

        } catch (SQLException e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Dashboard Error",
                    "Could not load metrics: " + e.getMessage());
        }
    }

    private void loadRecentLeaves() {
        try {
            List<LeaveRequest> requests = leaveService.getPendingRequestsForApprover(currentUser());
            // Show up to 10 most recent
            if (requests.size() > 10) requests = requests.subList(0, 10);
            leaveTable.setItems(FXCollections.observableArrayList(requests));
        } catch (SQLException e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Dashboard Error",
                    "Could not load leave requests: " + e.getMessage());
        }
    }

    private void loadMyProfile() {
        try {
            Employee emp = employeeRepository.findById(currentUser().getEmployeeId());
            if (emp == null) return;
            profileNameLabel.setText(emp.getFullName() + "  |  " + emp.getJobTitle());
            profileDetailLabel.setText("Department: " + emp.getDepartment()
                    + "   |   Email: " + emp.getEmail()
                    + "   |   Phone: " + (emp.getPhone() != null ? emp.getPhone() : "—")
                    + "   |   Status: " + (emp.isActive() ? "Active" : "Inactive"));
            profileLeaveLabel.setText("Leave Balance: " + emp.getLeaveBalance() + " day(s)"
                    + "   |   Salary: PKR " + String.format("%.2f", emp.getSalary()));
        } catch (SQLException e) {
            profileDetailLabel.setText("Could not load profile: " + e.getMessage());
        }
    }

    private void configureTables() {
        deptTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colDeptCount.setCellValueFactory(new PropertyValueFactory<>("total"));
        colDeptActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        leaveTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colLeaveEmp.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colLeaveType.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        colLeaveDates.setCellValueFactory(cellData -> Bindings.createStringBinding(
                () -> cellData.getValue().getStartDate() + " → " + cellData.getValue().getEndDate()));
        colLeaveStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colLeaveManager.setCellValueFactory(new PropertyValueFactory<>("managerUsername"));
    }

    // Simple DTO for department summary rows
    public static class DeptRow {
        private final String department;
        private final int total;
        private final int active;

        public DeptRow(String department, int total, int active) {
            this.department = department;
            this.total = total;
            this.active = active;
        }

        public String getDepartment() { return department; }
        public int getTotal() { return total; }
        public int getActive() { return active; }
    }

    @FunctionalInterface
    private interface NavigationAction {
        void run() throws Exception;
    }
}
