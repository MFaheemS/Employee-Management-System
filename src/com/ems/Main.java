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
        stage.setResizable(false);

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

    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("login.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
    }

    public static void showEmployeeAdd() throws Exception {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("employee_add.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
    }

    public static void showEmployeeDeactivate() throws Exception {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("employee_deactivate.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
