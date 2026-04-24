package controllers;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import network.SocketManager;

public class SignUpController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbRole;
    @FXML private Label lblMessage;

    @FXML
    public void initialize() {
        SocketManager.getInstance().addListener("REGISTER_RESPONSE", resp -> {
            String status = resp.get("status").getAsString();
            Platform.runLater(() -> {
                if ("SUCCESS".equals(status)) {
                    lblMessage.setTextFill(javafx.scene.paint.Color.web("#34c759"));
                    lblMessage.setText("Đăng ký thành công! Hãy quay lại đăng nhập.");
                } else {
                    lblMessage.setTextFill(javafx.scene.paint.Color.RED);
                    lblMessage.setText(resp.has("message") ? resp.get("message").getAsString() : "Đăng ký thất bại.");
                }
            });
        });
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String role = cbRole.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            lblMessage.setTextFill(javafx.scene.paint.Color.RED);
            lblMessage.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        lblMessage.setTextFill(javafx.scene.paint.Color.GRAY);
        lblMessage.setText("Đang xử lý...");

        try {
            if (!SocketManager.getInstance().isConnected()) {
                SocketManager.getInstance().connect("127.0.0.1", 9999);
            }

            JsonObject regData = new JsonObject();
            regData.addProperty("command", "REGISTER");
            regData.addProperty("username", username);
            regData.addProperty("password", password);
            regData.addProperty("role", role);

            SocketManager.getInstance().send(regData);
        } catch (Exception e) {
            lblMessage.setTextFill(javafx.scene.paint.Color.RED);
            lblMessage.setText("Lỗi kết nối Server.");
        }
    }

    @FXML
    public void handleBackToLogin(ActionEvent event) {
        try {
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
