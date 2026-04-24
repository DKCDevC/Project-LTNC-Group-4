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

import java.util.prefs.Preferences;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblError;
    @FXML private CheckBox chkRemember;

    private Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    @FXML
    public void initialize() {
        String savedUser = prefs.get("username", "");
        String savedPass = prefs.get("password", "");
        boolean isRemembered = prefs.getBoolean("remember", false);

        if (isRemembered) {
            txtUsername.setText(savedUser);
            txtPassword.setText(savedPass);
            if (chkRemember != null) chkRemember.setSelected(true);
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        lblError.setText("Đang kết nối...");
        lblError.setTextFill(javafx.scene.paint.Color.web("#8e8e93"));

        new Thread(() -> {
            try {
                SocketManager sm = SocketManager.getInstance();
                sm.connect("127.0.0.1", 9999);

                // Set up listener for login response
                sm.addListener("LOGIN_RESPONSE", resp -> {
                    String status = resp.get("status").getAsString();
                    if ("SUCCESS".equals(status)) {
                        handleLoginSuccess(username, password, resp.get("role").getAsString());
                    } else {
                        Platform.runLater(() -> {
                            lblError.setTextFill(javafx.scene.paint.Color.web("#ff3b30"));
                            lblError.setText("Tài khoản hoặc mật khẩu không đúng.");
                        });
                    }
                });

                JsonObject loginData = new JsonObject();
                loginData.addProperty("command", "LOGIN");
                loginData.addProperty("username", username);
                loginData.addProperty("password", password);

                sm.send(loginData);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblError.setTextFill(javafx.scene.paint.Color.web("#ff3b30"));
                    lblError.setText("Không thể kết nối tới máy chủ.");
                });
            }
        }).start();
    }

    private void handleLoginSuccess(String username, String password, String role) {
        Platform.runLater(() -> {
            if (chkRemember != null && chkRemember.isSelected()) {
                prefs.put("username", username);
                prefs.put("password", password);
                prefs.putBoolean("remember", true);
            } else {
                prefs.remove("username");
                prefs.remove("password");
                prefs.putBoolean("remember", false);
            }

            try {
                Stage stage = (Stage) btnLogin.getScene().getWindow();
                FXMLLoader loader;
                Parent root;

                if ("SELLER".equalsIgnoreCase(role)) {
                    loader = new FXMLLoader(getClass().getResource("/views/SellerDashboard.fxml"));
                    root = loader.load();
                    SellerDashboardController sellerCtrl = loader.getController();
                    sellerCtrl.setUserInfo(username);
                } else {
                    loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
                    root = loader.load();
                    DashboardController dashboard = loader.getController();
                    dashboard.setUserInfo(username, role);
                }

                stage.setTitle("Hệ thống đấu giá eBid");
                stage.setScene(new Scene(root));
                stage.setMaximized(true);
            } catch (Exception e) {
                e.printStackTrace();
                lblError.setText("Lỗi khi tải màn hình chính.");
            }
        });
    }

    @FXML
    public void handleGoToSignUp(ActionEvent event) {
        try {
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/views/SignUp.fxml"));
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
