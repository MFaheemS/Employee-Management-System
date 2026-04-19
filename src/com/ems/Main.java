package com.ems;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("Employee Management System");
        stage.setResizable(true);

        try {
            Database.initialize();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Could not initialize local database.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            throw e;
        }

        showLogin();
        stage.show();
    }

    public static void showLogin() throws Exception { showScene("login.fxml"); }
    public static void showDashboard() throws Exception { showScene("dashboard.fxml"); }
    public static void showEmployeeAdd() throws Exception { showScene("employee_add.fxml"); }
    public static void showEmployeeDeactivate() throws Exception { showScene("employee_deactivate.fxml"); }
    public static void showLeaveApplication() throws Exception { showScene("leave_apply.fxml"); }
    public static void showLeaveApprovals() throws Exception { showScene("leave_approvals.fxml"); }
    public static void showAttendance() throws Exception { showScene("attendance.fxml"); }
    public static void showEmployeeSearch() throws Exception { showScene("employee_search.fxml"); }
    public static void showPayroll() throws Exception { showScene("payroll.fxml"); }
    public static void showDocuments() throws Exception { showScene("document_upload.fxml"); }

    public static void showHomeForCurrentUser() throws Exception {
        AppUser user = AppSession.getCurrentUser();
        if (user == null) {
            showLogin();
            return;
        }
        showDashboard();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static void showScene(String resourceName) throws Exception {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(resourceName));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
    }
}
