package com.ems;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;

public class LeaveApplicationController extends BaseController {

    private final LeaveManagementService leaveManagementService = new LeaveManagementService();

    @FXML
    private Label userLabel;

    @FXML
    private Label employeeSummaryLabel;

    @FXML
    private Label leaveBalanceLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<String> leaveTypeBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Button employeeAddNavButton;

    @FXML
    private Button employeeDeactivateNavButton;

    @FXML
    private Button leaveApprovalsNavButton;

    @FXML
    private Button attendanceNavButton;

    @FXML
    private TableView<LeaveRequest> requestHistoryTable;

    @FXML
    private TableColumn<LeaveRequest, Integer> historyIdColumn;

    @FXML
    private TableColumn<LeaveRequest, String> historyTypeColumn;

    @FXML
    private TableColumn<LeaveRequest, String> historyDatesColumn;

    @FXML
    private TableColumn<LeaveRequest, String> historyStatusColumn;

    @FXML
    private void initialize() {
        if (!ensureLeaveApplicationAccess()) {
            return;
        }

        configureNavigation();
        configureHistoryTable();
        leaveTypeBox.setItems(FXCollections.observableArrayList("Annual", "Sick", "Casual", "Emergency"));
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        loadEmployeeSummary();
        loadHistory();
    }

    @FXML
    private void handleSubmitLeaveRequest() {
        statusLabel.setText("");

        try {
            String managerUsername = leaveManagementService.submitLeaveRequest(
                    currentUser(),
                    leaveTypeBox.getValue(),
                    startDatePicker.getValue(),
                    endDatePicker.getValue()
            );

            statusLabel.getStyleClass().removeAll("status-error", "status-success");
            statusLabel.getStyleClass().add("status-success");
            statusLabel.setText("Leave request submitted and routed to manager: " + managerUsername);
            showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Leave Submitted",
                    "Your leave request has been submitted successfully.");
            leaveTypeBox.getSelectionModel().clearSelection();
            startDatePicker.setValue(LocalDate.now());
            endDatePicker.setValue(LocalDate.now());
            loadHistory();
        } catch (IllegalArgumentException e) {
            setErrorStatus(e.getMessage());
        } catch (SQLException e) {
            setErrorStatus("Could not submit leave request: " + e.getMessage());
        }
    }

    @FXML
    private void goToAdd() {
        try {
            Main.showEmployeeAdd();
        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToDeactivate() {
        try {
            Main.showEmployeeDeactivate();
        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToLeaveApply() {
        // already on this page
    }

    @FXML
    private void goToLeaveApprovals() {
        try {
            Main.showLeaveApprovals();
        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToAttendance() {
        try {
            Main.showAttendance();
        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    protected void handleLogout() {
        super.handleLogout();
    }

    private void configureNavigation() {
        AppUser user = currentUser();
        userLabel.setText(user.getUsername() + " (" + user.getRole() + ")");
        employeeAddNavButton.setDisable(!user.canManageEmployees());
        employeeDeactivateNavButton.setDisable(!user.canManageEmployees());
        leaveApprovalsNavButton.setDisable(!user.canManageLeaveApprovals());
        attendanceNavButton.setDisable(user.getEmployeeId() == null || user.getEmployeeId().isBlank());
    }

    private void configureHistoryTable() {
        requestHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyIdColumn.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        historyTypeColumn.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        historyDatesColumn.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getStartDate() + " to " + cellData.getValue().getEndDate()));
        historyStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadEmployeeSummary() {
        try {
            Employee employee = leaveManagementService.getEmployeeProfile(currentUser().getEmployeeId());
            if (employee == null) {
                setErrorStatus("Employee profile not found.");
                return;
            }

            employeeSummaryLabel.setText(employee.getFullName() + " | ID: " + employee.getEmployeeId()
                    + " | Manager: " + employee.getManagerUsername());
            leaveBalanceLabel.setText("Available Leave Balance: " + employee.getLeaveBalance() + " day(s)");
        } catch (SQLException e) {
            setErrorStatus("Could not load employee details: " + e.getMessage());
        }
    }

    private void loadHistory() {
        try {
            requestHistoryTable.setItems(FXCollections.observableArrayList(
                    leaveManagementService.getRequestsForEmployee(currentUser().getEmployeeId())
            ));
            loadEmployeeSummary();
        } catch (SQLException e) {
            setErrorStatus("Could not load leave history: " + e.getMessage());
        }
    }

    private void setErrorStatus(String message) {
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add("status-error");
        statusLabel.setText(message);
    }
}