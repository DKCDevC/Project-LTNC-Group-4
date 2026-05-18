package network;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Lớp MainUI là điểm khởi động giao diện người dùng JavaFX (Client GUI Application).
 * Kế thừa lớp Application từ thư viện JavaFX.
 * Chịu trách nhiệm nạp tệp FXML giao diện ban đầu (màn hình Đăng nhập - Login.fxml), 
 * cấu hình sân khấu hiển thị (Stage) và hỗ trợ bung toàn màn hình tối đa (Maximized).
 */
public class MainUI extends Application {

    /**
     * Phương thức khởi chạy vòng đời giao diện JavaFX.
     * @param primaryStage Sân khấu chính hiển thị màn hình ứng dụng
     * @throws Exception Các lỗi liên quan đến nạp tệp FXML hoặc tài nguyên đồ họa
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Trỏ đường dẫn tuyệt đối tới file fxml giao diện Đăng nhập trong thư mục resources
        URL fxmlLocation = getClass().getResource("/views/Login.fxml");
        if (fxmlLocation == null) {
            System.out.println("Không tìm thấy file Login.fxml! Hãy kiểm tra lại thư mục resources.");
            return;
        }

        // Thực hiện nạp cây nút giao diện từ FXML
        Parent root = FXMLLoader.load(fxmlLocation);
        primaryStage.setTitle("Hệ thống Đấu giá ebid");

        // Gắn cây giao diện vào Scene (khung cảnh hiển thị)
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        // Thiết lập kích hoạt bung mở giao diện tối đa toàn màn hình (Maximized) để tăng tính chuyên nghiệp UI
        primaryStage.setMaximized(true);

        // Hiển thị sân khấu chính lên màn hình
        primaryStage.show();
    }

    /**
     * Phương thức main khởi chạy chuẩn của JVM JavaFX.
     * @param args Tham số dòng lệnh truyền vào từ bên ngoài
     */
    public static void main(String[] args) {
        launch(args);
    }
}