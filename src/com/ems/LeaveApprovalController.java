package com.ems;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;

public class LeaveApprovalController extends BaseController {

    private final LeaveManagementService leaveManagementService = new LeaveManagementService();

    @FXML
    private Label userLabel;

    @FXML
    private Label selectedRequestLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Button employeeAddNavButton;

    @FXML
    private Button employeeDeactivateNavButton;

    @FXML
    private Button leaveApprovalsNavButton;

    @FXML
    private TableView<LeaveRequest> pendingTable;

    @FXML
    private TableColumn<LeaveRequest, Integer> pendingIdColumn;

    @FXML
    private TableColumn<LeaveRequest, String> pendingEmployeeColumn;

    @FXML
    private TableColumn<LeaveRequest, String> pendingTypeColumn;

    @FXML
    private TableColumn<LeaveRequest, Integer> pendingDaysColumn;

    @FXML
    private TableColumn<LeaveRequest, String> pendingDatesColumn;

    @FXML
    private TableView<LeaveRequest> historyTable;

    @FXML
    private TableColumn<LeaveRequest, Integer> historyIdColumn;

    @FXML
    private TableColumn<LeaveRequest, String> historyEmployeeColumn;

    @FXML
    private TableColumn<LeaveRequest, String> historyStatusColumn;

    @FXML
    private TableColumn<LeaveRequest, String> historyDecisionByColumn;

    @FXML
    private TextArea commentsArea;

    @FXML
    private void initialize() {
        if (!ensureLeaveApprovalAccess()) {
            return;
        }

        configureNavigation();
        configureTables();
        pendingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateSelectionSummary(newValue));
        loadTables();
    }

    @FXML
    private void handleApprove() {
        processSelectedRequest("Approved");
    }

    @FXML
    private void handleReject() {
        processSelectedRequest("Rejected");
    }

    @FXML
    private void handleRefresh() {
        loadTables();
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
        try {
            Main.showLeaveApplication();
        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    @FXML
    private void goToLeaveApprovals() {
        // already on this page
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
        leaveApprovalsNavButton.setDisable(false);
    }

    private void configureTables() {
        pendingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        pendingIdColumn.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        pendingEmployeeColumn.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getEmployeeName() + " (" + cellData.getValue().getEmployeeId() + ")"));
        pendingTypeColumn.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        pendingDaysColumn.setCellValueFactory(new PropertyValueFactory<>("daysRequested"));
        pendingDatesColumn.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getStartDate() + " to " + cellData.getValue().getEndDate()));

        historyIdColumn.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        historyEmployeeColumn.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getEmployeeName() + " (" + cellData.getValue().getEmployeeId() + ")"));
        historyStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        historyDecisionByColumn.setCellValueFactory(new PropertyValueFactory<>("decisionBy"));
    }

    private void processSelectedRequest(String action) {
        LeaveRequest selectedRequest = pendingTable.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            setErrorStatus("Please select a pending leave request first.");
            return;
        }

        try {
            String result = leaveManagementService.decideLeaveRequest(
                    currentUser(),
                    selectedRequest.getRequestId(),
                    action,
                    commentsArea.getText()
            );

            statusLabel.getStyleClass().removeAll("status-error", "status-success");
            statusLabel.getStyleClass().add("status-success");
            statusLabel.setText("Leave request #" + selectedRequest.getRequestId() + " marked as " + result + ".");
            commentsArea.clear();
            loadTables();
        } catch (IllegalArgumentException e) {
            setErrorStatus(e.getMessage());
        } catch (SQLException e) {
            setErrorStatus("Could not update leave request: " + e.getMessage());
        }
    }

    private void loadTables() {
        try {
            pendingTable.setItems(FXCollections.observableArrayList(
                    leaveManagementService.getPendingRequestsForApprover(currentUser())
            ));
            historyTable.setItems(FXCollections.observableArrayList(
                    leaveManagementService.getDecisionHistoryForApprover(currentUser())
            ));
            selectedRequestLabel.setText("Select a pending request to review its details.");
        } catch (SQLException e) {
            setErrorStatus("Could not load leave requests: " + e.getMessage());
        }
    }

    private void updateSelectionSummary(LeaveRequest request) {
        if (request == null) {
            selectedRequestLabel.setText("Select a pending request to review its details.");
            return;
        }

        selectedRequestLabel.setText("Request #" + request.getRequestId()
                + " | " + request.getEmployeeName()
                + " | " + request.getLeaveType()
                + " | " + request.getStartDate() + " to " + request.getEndDate()
                + " | " + request.getDaysRequested() + " day(s)");
    }

    private void setErrorStatus(String message) {
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add("status-error");
        statusLabel.setText(message);
    }
}