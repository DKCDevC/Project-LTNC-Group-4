// 1. Khai báo package: Nằm trong phân hệ kết nối mạng (network) của Client.
package network;

// 2. Import các thư viện cốt lõi điều phối giao diện JavaFX và luồng tài nguyên
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
 * 
 * Kiến trúc Vòng đời JavaFX (JavaFX Lifecycle):
 * 1. JVM gọi phương thức main() -> Chạy launch(args).
 * 2. launch(args) khởi động JavaFX Runtime, ngầm tạo ra luồng điều khiển giao diện chính gọi là JavaFX Application Thread.
 * 3. JavaFX khởi tạo một đối tượng sân khấu gốc (`Stage`) đại diện cho cửa sổ hệ điều hành và tự động gọi start(Stage).
 * 4. Khi đóng cửa sổ, start kết thúc, JavaFX chạy phương thức stop() ẩn để giải phóng bộ nhớ đồ họa.
 */
public class MainUI extends Application {

    /**
     * Phương thức khởi chạy vòng đời giao diện JavaFX.
     * Hoạt động như một Entry Point giao diện đồ họa.
     * 
     * @param primaryStage Sân khấu chính hiển thị màn hình ứng dụng (cửa sổ gốc của HDH Windows/MacOS)
     * @throws Exception Các lỗi liên quan đến nạp tệp FXML hoặc tài nguyên đồ họa (IOException)
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Trỏ đường dẫn thông qua ClassLoader để tìm kiếm file cấu trúc giao diện XML (.fxml) trong thư mục resources
        URL fxmlLocation = getClass().getResource("/views/Login.fxml");
        if (fxmlLocation == null) {
            System.out.println("Không tìm thấy file Login.fxml! Hãy kiểm tra lại cấu trúc thư mục resources.");
            return;
        }

        // Thực hiện nạp (inflate) cây nút giao diện (Node Tree Hierarchy) từ tệp FXML tĩnh sang đối tượng Java Parent dạng động
        Parent root = FXMLLoader.load(fxmlLocation);
        primaryStage.setTitle("Hệ thống Đấu giá ebid");

        // Gắn cây giao diện vào Scene (Scene là khung cảnh chứa đựng toàn bộ các Node đồ họa, nút bấm, bảng biểu)
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        // Thiết lập kích hoạt bung mở giao diện tối đa toàn màn hình (Maximized) giúp UI chiếm trọn không gian, chuyên nghiệp hơn
        primaryStage.setMaximized(true);

        // Hiển thị sân khấu chính lên màn hình cho người dùng tương tác
        primaryStage.show();
    }

    /**
     * Phương thức main khởi chạy chuẩn của máy ảo JVM JavaFX.
     * 
     * @param args Tham số dòng lệnh truyền vào từ bên ngoài hệ thống
     */
    public static void main(String[] args) {
        // Khởi động động cơ đồ họa JavaFX
        launch(args);
    }
}