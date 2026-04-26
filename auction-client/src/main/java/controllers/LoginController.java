package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private ToggleButton btnShowPassword;
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
            txtPasswordVisible.setText(savedPass);
            if (chkRemember != null) {
                chkRemember.setSelected(true);
            }
        }

        // Đồng bộ dữ liệu giữa PasswordField và TextField
        txtPassword.textProperty().bindBidirectional(txtPasswordVisible.textProperty());
    }

    @FXML
    public void togglePasswordVisibility() {
        if (btnShowPassword.isSelected()) {
            txtPasswordVisible.setVisible(true);
            txtPassword.setVisible(false);
            btnShowPassword.setText("🙈");
        } else {
            txtPasswordVisible.setVisible(false);
            txtPassword.setVisible(true);
            btnShowPassword.setText("👁");
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String identifier = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (identifier.isEmpty() || password.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        lblError.setText("Đang kết nối...");
        lblError.setTextFill(javafx.scene.paint.Color.web("#8e8e93"));

        new Thread(() -> {
            try {
                // Sử dụng port 1234 cho MainServer
                Socket socket = new Socket("127.0.0.1", 1234);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                JsonObject loginData = new JsonObject();
                loginData.addProperty("command", "LOGIN");
                loginData.addProperty("username", identifier); // identifier có thể là username hoặc email
                loginData.addProperty("password", password);

                out.println(new Gson().toJson(loginData));

                String response = in.readLine();
                JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
                String status = jsonResponse.get("status").getAsString();

                Platform.runLater(() -> {
                    if ("SUCCESS".equals(status)) {
                        if (chkRemember != null && chkRemember.isSelected()) {
                            prefs.put("username", identifier);
                            prefs.put("password", password);
                            prefs.putBoolean("remember", true);
                        } else {
                            prefs.remove("username");
                            prefs.remove("password");
                            prefs.putBoolean("remember", false);
                        }

                        String role = jsonResponse.get("role").getAsString();
                        lblError.setTextFill(javafx.scene.paint.Color.web("#34c759"));
                        lblError.setText("Đăng nhập thành công!");

                        try {
                            Stage stage = (Stage) btnLogin.getScene().getWindow();
                            FXMLLoader loader;
                            Parent root;

                            if ("SELLER".equalsIgnoreCase(role)) {
                                loader = new FXMLLoader(getClass().getResource("/views/SellerDashboard.fxml"));
                                root = loader.load();
                                SellerDashboardController sellerCtrl = loader.getController();
                                sellerCtrl.setUserInfo(identifier);
                                stage.setScene(new Scene(root));
                            } else {
                                loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
                                root = loader.load();
                                DashboardController dashboard = loader.getController();
                                dashboard.setUserInfo(identifier, role);
                                stage.setScene(new Scene(root));
                            }
                            stage.setMaximized(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                            lblError.setText("Lỗi khi tải màn hình chính.");
                        }
                    } else {
                        String msg = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Tài khoản hoặc mật khẩu không đúng.";
                        lblError.setTextFill(javafx.scene.paint.Color.web("#ff3b30"));
                        lblError.setText(msg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblError.setTextFill(javafx.scene.paint.Color.web("#ff3b30"));
                    lblError.setText("Không thể kết nối tới máy chủ.");
                });
            }
        }).start();
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