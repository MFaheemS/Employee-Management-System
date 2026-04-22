package com.ems;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.sql.SQLException;

public abstract class BaseController {

    protected AppUser currentUser() {
        return AppSession.getCurrentUser();
    }

    protected boolean ensureLoggedIn() {
        if (AppSession.isLoggedIn()) {
            return true;
        }

        showAlert(Alert.AlertType.WARNING, "Session Expired",
                "Please log in again.");
        navigateToLogin();
        return false;
    }

    protected boolean ensureEmployeeManagementAccess() {
        if (!ensureLoggedIn()) {
            return false;
        }

        if (currentUser().isAdmin() && currentUser().canManageEmployees()) {
            return true;
        }

        if (currentUser().isManager()) {
            if (isManagerWithDepartment()) {
                return true;
            }
            showAlert(Alert.AlertType.ERROR, "No Department Assigned",
                    "You have not been assigned to a department yet. Contact your administrator.");
            goHome();
            return false;
        }

        showAlert(Alert.AlertType.ERROR, "Access Denied",
                "You are not authorized to manage employee records.");
        goHome();
        return false;
    }

    protected boolean ensureLeaveApprovalAccess() {
        if (!ensureLoggedIn()) {
            return false;
        }

        if (currentUser().canManageLeaveApprovals() && isManagerWithDepartment()) {
            return true;
        }

        if (currentUser().isManager() && !isManagerWithDepartment()) {
            showAlert(Alert.AlertType.ERROR, "No Department Assigned",
                    "You have not been assigned to a department yet. Contact your administrator.");
            goHome();
            return false;
        }

        showAlert(Alert.AlertType.ERROR, "Access Denied",
                "You are not authorized to review leave requests.");
        goHome();
        return false;
    }

    protected boolean ensureLeaveApplicationAccess() {
        if (!ensureLoggedIn()) {
            return false;
        }

        if (currentUser().getEmployeeId() != null && !currentUser().getEmployeeId().isBlank()) {
            return true;
        }

        showAlert(Alert.AlertType.ERROR, "Unavailable",
                "This account is not linked to an employee profile.");
        goHome();
        return false;
    }

    protected boolean ensureAttendanceAccess() {
        if (!ensureLoggedIn()) {
            return false;
        }

        if (currentUser().getEmployeeId() != null && !currentUser().getEmployeeId().isBlank()) {
            return true;
        }

        showAlert(Alert.AlertType.ERROR, "Unavailable",
                "This account is not linked to an employee profile.");
        goHome();
        return false;
    }

    protected boolean ensureEmployeeSearchAccess() {
        if (!ensureLoggedIn()) {
            return false;
        }

        if (currentUser().isAdmin() && currentUser().canSearchEmployees()) {
            return true;
        }

        if (currentUser().isManager()) {
            if (isManagerWithDepartment()) {
                return true;
            }
            showAlert(Alert.AlertType.ERROR, "No Department Assigned",
                    "You have not been assigned to a department yet. Contact your administrator.");
            goHome();
            return false;
        }

        showAlert(Alert.AlertType.ERROR, "Access Denied",
                "You are not authorized to search employee records.");
        goHome();
        return false;
    }

    protected boolean isManagerWithDepartment() {
        AppUser user = currentUser();
        if (user == null || !user.isManager()) return true;
        try {
            return new DepartmentRepository().hasDepartment(user.getUsername());
        } catch (SQLException e) {
            return false;
        }
    }

    protected void handleLogout() {
        AppSession.clear();
        navigateToLogin();
    }

    protected void navigateToLogin() {
        try {
            Main.showLogin();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the login page: " + e.getMessage());
        }
    }

    protected void goHome() {
        try {
            Main.showHomeForCurrentUser();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not load the page: " + e.getMessage());
        }
    }

    protected void configureSidebarNavigation(Label userLabel,
                                              Button employeeAddButton,
                                              Button employeeDeactivateButton,
                                              Button employeeSearchButton,
                                              Button attendanceButton,
                                              Button leaveApplyButton,
                                              Button leaveApprovalsButton) {
        AppUser user = currentUser();
        if (user == null) {
            return;
        }

        if (userLabel != null) {
            userLabel.setText(user.getUsername() + " (" + user.getRole() + ")");
        }

        boolean hasEmployeeProfile = hasLinkedEmployeeProfile(user);
        boolean managerHasDept = !user.isManager() || isManagerWithDepartment();

        // Admin creates managers; Manager creates/deactivates their own dept employees
        setNavigationVisibility(employeeAddButton, user.canManageEmployees() && (user.isAdmin() || managerHasDept));
        setNavigationVisibility(employeeDeactivateButton, user.canManageEmployees() && (user.isAdmin() || managerHasDept));
        if (employeeAddButton != null) {
            employeeAddButton.setText(user.isAdmin() ? "＋  Manager Add" : "＋  Employee Add");
        }
        if (employeeDeactivateButton != null) {
            employeeDeactivateButton.setText(user.isAdmin() ? "⊗  Manager Deactivate" : "⊗  Deactivate");
        }
        // Admin + Manager can search (each scoped in the controller)
        setNavigationVisibility(employeeSearchButton, user.canSearchEmployees() && (user.isAdmin() || managerHasDept));
        // Attendance: employees and managers with a linked profile
        setNavigationVisibility(attendanceButton, hasEmployeeProfile);
        // Leave apply: employees only (managers do not apply for leave)
        setNavigationVisibility(leaveApplyButton, hasEmployeeProfile && user.isEmployee());
        // Only HR Manager handles leave approvals (only when assigned to a department)
        setNavigationVisibility(leaveApprovalsButton, user.canManageLeaveApprovals() && managerHasDept);

        playEntranceAnimation(firstAvailable(userLabel,
                employeeAddButton,
                employeeDeactivateButton,
                employeeSearchButton,
                attendanceButton,
                leaveApplyButton,
                leaveApprovalsButton));
    }

    protected void configureAdditionalNavigation(Button dashboardButton,
                                                  Button payrollButton,
                                                  Button documentsButton) {
        AppUser user = currentUser();
        if (user == null) return;
        setNavigationVisibility(dashboardButton, true);
        // Payroll: Admin generates + views all; Employee views own slips; Manager has no payroll
        setNavigationVisibility(payrollButton, user.isAdmin() || user.isEmployee());
        // Documents: Employee uploads own docs; Manager views their department's docs (only if assigned)
        boolean hasProfile = hasLinkedEmployeeProfile(user);
        boolean managerHasDeptForDocs = !user.isManager() || isManagerWithDepartment();
        setNavigationVisibility(documentsButton, hasProfile || (user.isManager() && managerHasDeptForDocs));
    }

    protected void configureDepartmentNavigation(Button departmentButton) {
        AppUser user = currentUser();
        if (user == null) return;
        setNavigationVisibility(departmentButton, user.canManageDepartments());
    }

    protected void setNavigationVisibility(Button button, boolean allowed) {
        if (button == null) {
            return;
        }

        button.setVisible(allowed);
        button.setManaged(allowed);
        button.setDisable(!allowed);
    }

    protected void playEntranceAnimation(Node anchorNode) {
        if (anchorNode == null) {
            return;
        }

        Platform.runLater(() -> {
            if (anchorNode.getScene() == null || anchorNode.getScene().getRoot() == null) {
                return;
            }

            Node root = anchorNode.getScene().getRoot();
            if (Boolean.TRUE.equals(root.getProperties().get("emsAnimated"))) {
                return;
            }
            root.getProperties().put("emsAnimated", Boolean.TRUE);

            root.setOpacity(0);
            root.setTranslateY(14);

            FadeTransition fade = new FadeTransition(Duration.millis(280), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition lift = new TranslateTransition(Duration.millis(280), root);
            lift.setFromY(14);
            lift.setToY(0);
            lift.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fade, lift).play();
        });
    }

    private boolean hasLinkedEmployeeProfile(AppUser user) {
        return user.getEmployeeId() != null && !user.getEmployeeId().isBlank();
    }

    private Node firstAvailable(Node... nodes) {
        for (Node node : nodes) {
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    protected void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}