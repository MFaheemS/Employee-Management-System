package com.ems;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class LoginController {

    private final AuthService authService = new AuthService();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordVisibleField;

    @FXML
    private Button togglePasswordButton;

    @FXML
    private Label hintLabel;

    private boolean showingPassword;

    @FXML
    private void initialize() {
        AppSession.clear();
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        hintLabel.setText("Demo logins: admin/admin, manager/manager, employee/employee");
        playEntranceAnimation();
    }

    private void playEntranceAnimation() {
        Platform.runLater(() -> {
            if (usernameField.getScene() == null || usernameField.getScene().getRoot() == null) {
                return;
            }

            javafx.scene.Parent root = usernameField.getScene().getRoot();
            if (Boolean.TRUE.equals(root.getProperties().get("emsAnimated"))) {
                return;
            }
            root.getProperties().put("emsAnimated", Boolean.TRUE);

            root.setOpacity(0);
            root.setScaleX(0.985);
            root.setScaleY(0.985);

            FadeTransition fade = new FadeTransition(Duration.millis(300), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), root);
            scale.setFromX(0.985);
            scale.setFromY(0.985);
            scale.setToX(1);
            scale.setToY(1);
            scale.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fade, scale).play();
        });
    }

    @FXML
    private void togglePasswordVisibility() {
        showingPassword = !showingPassword;

        passwordVisibleField.setVisible(showingPassword);
        passwordVisibleField.setManaged(showingPassword);

        passwordField.setVisible(!showingPassword);
        passwordField.setManaged(!showingPassword);

        togglePasswordButton.setText(showingPassword ? "Hide" : "Show");

        if (showingPassword) {
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
        } else {
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    @FXML
    private void handleSubmit() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please enter both username and password.");
            return;
        }

        try {
            AppUser user = authService.authenticate(username, password);
            if (user != null) {
                AppSession.setCurrentUser(user);
                Main.showHomeForCurrentUser();
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed",
                        "Invalid username, password, or inactive account.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Login Error",
                    "Could not complete login: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
