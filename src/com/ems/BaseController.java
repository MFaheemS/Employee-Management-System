package com.ems;

import javafx.scene.control.Alert;

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

        if (currentUser().canManageEmployees()) {
            return true;
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

        if (currentUser().canManageLeaveApprovals()) {
            return true;
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

    protected void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}