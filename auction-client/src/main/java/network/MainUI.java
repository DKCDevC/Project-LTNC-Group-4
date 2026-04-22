package network;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Trỏ đường dẫn tới file fxml trong thư mục resources
        URL fxmlLocation = getClass().getResource("/views/Login.fxml");
        if (fxmlLocation == null) {
            System.out.println("Không tìm thấy file Login.fxml! Hãy kiểm tra lại thư mục resources.");
            return;
        }

        Parent root = FXMLLoader.load(fxmlLocation);
<<<<<<< HEAD
        primaryStage.setTitle("Auction Client");

        // Kích thước chuẩn đẹp
        Scene scene = new Scene(root, 400, 500);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // Khóa resize để giữ form chuẩn
=======
        primaryStage.setTitle("Hệ thống Đấu giá ebid");

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        primaryStage.setMaximized(true);

>>>>>>> 6dd5a76 (change login)
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}