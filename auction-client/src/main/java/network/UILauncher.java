package network;

/**
 * Lớp UILauncher đóng vai trò là "Cổng khởi động giao diện gián tiếp".
 * Được thiết kế đặc biệt để giải quyết các lỗi khởi chạy (Bootstrap errors) của JavaFX 11+ 
 * khi đóng gói ứng dụng thành tệp FAT JAR (nếu lớp khởi chạy trực tiếp kế thừa từ Application,
 * JVM sẽ báo lỗi thiếu các mô-đun Runtime của JavaFX).
 */
public class UILauncher {
    
    /**
     * Điểm mồi khởi chạy JVM gián tiếp, gọi sang hàm main của MainUI.
     * @param args Tham số dòng lệnh
     */
    public static void main(String[] args){
        MainUI.main(args);
    }
}
