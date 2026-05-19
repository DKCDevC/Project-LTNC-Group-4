package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Lớp SignUpController điều khiển logic giao diện Đăng ký tài khoản (SignUp.fxml).
 * Hiện thực hóa quy trình thiết lập tài khoản mới an toàn và bảo mật cho eBid.
 * 
 * Các nguyên lý kỹ thuật:
 * 1. Client-Side Integrity Validation (Kiểm duyệt dữ liệu đầu vào): Thực hiện kiểm tra tính toàn vẹn 
 *    của thông tin (Username, Email, Password, Role) trước khi đóng gói gói tin mạng nhằm tránh lãng phí 
 *    tài nguyên kết nối và giảm tải áp lực cho Server.
 * 2. Multi-Role Account Provisioning (Cung cấp tài khoản đa vai trò): Đọc giá trị tuyển chọn của ComboBox 
 *    (BIDDER - Người mua hoặc SELLER - Người bán) để ánh xạ vào trường dữ liệu tài khoản gửi đi.
 * 3. Asynchronous TCP Socket Pipeline (Luồng TCP Socket bất đồng bộ): Đóng gói dữ liệu dạng JSON, 
 *    thực hiện bắt tay gửi gói tin REGISTER qua luồng socket bất đồng bộ độc lập (Worker Thread) để bảo toàn 
 *    tính mượt mà và chống giật lag cho giao diện đồ họa.
 * 4. UI Synchronization (Đồng bộ đồ họa): Bơm ngược phản hồi trạng thái (Thành công / Thất bại do trùng Username/Email) 
 *    về JavaFX Application Thread an toàn thông qua Platform.runLater.
 */
public class SignUpController {
    // Các trường UI liên kết từ FXML bằng chú thích FXML Injection
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbRole; // Menu chọn vai trò
    @FXML private Label lblMessage;

    /**
     * Xử lý sự kiện khi người dùng click nút Đăng Ký.
     * Thu thập thông tin, thực hiện kiểm duyệt thô tại chỗ và đẩy luồng Thread phụ xử lý đăng ký mạng.
     * @param event Sự kiện ActionEvent kích hoạt từ nút bấm
     */
    @FXML
    public void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();
        String role = cbRole.getValue();

        // 1. Kiểm duyệt hợp lệ dữ liệu thô (Client-side validation)
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {
            lblMessage.setTextFill(javafx.scene.paint.Color.RED);
            lblMessage.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        lblMessage.setTextFill(javafx.scene.paint.Color.GRAY);
        lblMessage.setText("Đang xử lý...");

        // 2. Chạy Thread bất đồng bộ giao tiếp Socket mạng
        new Thread(() -> {
            try {
                // Kết nối tới Socket Server cổng 9999
                Socket socket = new Socket("127.0.0.1", 9999);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Tạo đối tượng JSON chứa thông tin đăng ký gửi lên
                JsonObject regData = new JsonObject();
                regData.addProperty("command", "REGISTER");
                regData.addProperty("username", username);
                regData.addProperty("email", email);
                regData.addProperty("password", password);
                regData.addProperty("role", role);

                out.println(new Gson().toJson(regData));

                // Đọc phản hồi đăng ký từ Server
                String response = in.readLine();
                JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
                String status = jsonResponse.get("status").getAsString();

                // 3. Trả kết quả cập nhật về JavaFX UI Thread
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(status)) {
                        lblMessage.setTextFill(javafx.scene.paint.Color.web("#34c759"));
                        lblMessage.setText("Đăng ký thành công! Hãy quay lại đăng nhập.");
                    } else {
                        // Hiển thị lý do thất bại (Ví dụ: Trùng tên đăng nhập hoặc email)
                        String msg = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Tài khoản hoặc Email đã tồn tại.";
                        lblMessage.setTextFill(javafx.scene.paint.Color.RED);
                        lblMessage.setText(msg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblMessage.setTextFill(javafx.scene.paint.Color.RED);
                    lblMessage.setText("Lỗi kết nối Server.");
                });
            }
        }).start();
    }

    /**
     * Chuyển hướng người dùng quay trở lại giao diện màn hình Đăng nhập (Login.fxml).
     * @param event Sự kiện ActionEvent kích hoạt từ nút bấm
     */
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