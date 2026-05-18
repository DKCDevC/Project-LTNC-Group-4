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

/**
 * Lớp LoginController điều khiển logic giao diện Đăng nhập (Login.fxml).
 * Xử lý nhập liệu tài khoản/mật khẩu, ghi nhớ thông tin đăng nhập (Remember Me)
 * bằng Preferences API, ẩn/hiển thị mật khẩu bằng binding song phương (bidirectional binding), 
 * và thực hiện gửi yêu cầu xác thực bất đồng bộ qua Socket TCP tới Server.
 */
public class LoginController {

    // Các thành phần UI được liên kết từ file FXML
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private ToggleButton btnShowPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblError;
    @FXML private CheckBox chkRemember;

    // Đối tượng preferences của Java API để lưu trữ cục bộ cấu hình đăng nhập trên ổ đĩa máy khách (Client Registry)
    private Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    /**
     * Phương thức khởi tạo tự động gọi sau khi tệp FXML được nạp thành công.
     * Nạp lại dữ liệu đăng nhập cũ đã được lưu (nếu người dùng tích chọn Remember Me trước đó).
     */
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

        // Kỹ thuật JavaFX Bindings: Đồng bộ hóa dữ liệu 2 chiều (Bidirectional Binding) giữa ô ẩn mật khẩu 
        // và ô hiển thị văn bản mật khẩu trần để dữ liệu luôn ăn khớp khi gõ vào bất kỳ ô nào
        txtPassword.textProperty().bindBidirectional(txtPasswordVisible.textProperty());
    }

    /**
     * Thay đổi trạng thái hiển thị của mật khẩu (Ẩn dưới dạng dấu sao hoặc Hiển thị văn bản trần).
     */
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

    /**
     * Xử lý sự kiện nhấn nút Đăng nhập.
     * Mở một luồng Thread phụ chạy Socket TCP bất đồng bộ kết nối máy chủ để tránh block (đơ) JavaFX UI Thread.
     * @param event Sự kiện ActionEvent kích hoạt từ nút bấm
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String identifier = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        // 1. Kiểm tra xác thực tính hợp lệ tại chỗ (Client-side validation)
        if (identifier.isEmpty() || password.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        lblError.setText("Đang kết nối...");
        lblError.setTextFill(javafx.scene.paint.Color.web("#8e8e93"));

        // 2. Chạy Thread phụ xử lý mạng (Asynchronous Network Operation)
        new Thread(() -> {
            try {
                // Kết nối tới Server eBid chạy tại localhost cổng 9999
                Socket socket = new Socket("127.0.0.1", 9999);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Đóng gói gói tin yêu cầu đăng nhập LOGIN dạng JSON
                JsonObject loginData = new JsonObject();
                loginData.addProperty("command", "LOGIN");
                loginData.addProperty("username", identifier); 
                loginData.addProperty("password", password);

                out.println(new Gson().toJson(loginData));

                // Chờ và đọc phản hồi từ Server
                String response = in.readLine();
                JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
                String status = jsonResponse.get("status").getAsString();

                // 3. Sử dụng Platform.runLater để đẩy các thao tác cập nhật giao diện đồ họa (UI Thread safe)
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(status)) {
                        // Lưu thông tin đăng nhập nếu người dùng chọn Ghi nhớ tài khoản
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

                            // Phân luồng chuyển hướng màn hình dựa trên vai trò (Role Routing)
                            if ("SELLER".equalsIgnoreCase(role)) {
                                // Giao diện Dashboard của Người Bán
                                loader = new FXMLLoader(getClass().getResource("/views/SellerDashboard.fxml"));
                                root = loader.load();
                                SellerDashboardController sellerCtrl = loader.getController();
                                sellerCtrl.setUserInfo(identifier);
                                stage.setScene(new Scene(root));
                            } else if ("ADMIN".equalsIgnoreCase(role)) {
                                // Giao diện Dashboard của Admin Quản trị viên
                                loader = new FXMLLoader(getClass().getResource("/views/admin_dashboard.fxml"));
                                root = loader.load();
                                stage.setScene(new Scene(root));
                            } else {
                                // Giao diện Dashboard chung của Người mua (Bidder)
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
                        // Phản hồi lỗi nghiệp vụ từ server (Sai mật khẩu, tài khoản bị khóa)
                        String msg = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Tài khoản hoặc mật khẩu không đúng.";
                        lblError.setTextFill(javafx.scene.paint.Color.web("#ff3b30"));
                        lblError.setText(msg);
                    }
                });
            } catch (Exception e) {
                // Xử lý lỗi không thể kết nối mạng tới Server
                Platform.runLater(() -> {
                    lblError.setTextFill(javafx.scene.paint.Color.web("#ff3b30"));
                    lblError.setText("Không thể kết nối tới máy chủ.");
                });
            }
        }).start();
    }

    /**
     * Chuyển hướng người dùng sang màn hình Đăng ký tài khoản mới.
     * @param event Sự kiện ActionEvent kích hoạt từ nút bấm
     */
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