// 1. Khai báo package: Nằm trong phân hệ kết nối mạng (network) của Client.
package network;

/**
 * Lớp UILauncher đóng vai trò là "Cổng khởi động giao diện gián tiếp" (Indirect Bootstrap Entry Point).
 * 
 * Ý nghĩa thiết kế và giải pháp kỹ thuật đặc thù:
 * - JavaFX 11+ Modularity System (Hệ thống mô-đun Java 9+): Từ phiên bản JavaFX 11 trở đi, JavaFX đã bị tách khỏi 
 *   bộ JDK cốt lõi và trở thành các thư viện ngoài.
 * - FAT JAR Packaging Issue (Lỗi đóng gói JAR gom cả phụ thuộc): Khi chạy ứng dụng đóng gói thành tệp JAR duy nhất, 
 *   nếu lớp chứa hàm main() kế thừa trực tiếp từ `javafx.application.Application` (như MainUI.java), 
 *   JVM khi khởi động sẽ quét kiểm tra lớp đó và lập tức ném ra lỗi Runtime:
 *   "Error: JavaFX runtime components are missing, and are required to run this application".
 *   Lý do là vì JVM tìm kiếm cấu trúc module-info chứa mô-đun JavaFX nhưng không tìm thấy trên đường dẫn Classpath thông thường.
 * - Giải pháp UILauncher: Bằng cách tạo ra một lớp mồi thuần túy (`UILauncher`) KHÔNG hề kế thừa từ `Application` 
 *   và chứa hàm `main()`, JVM sẽ bỏ qua việc quét kiểm tra mô-đun JavaFX lúc khởi động. Lớp này sau đó sẽ mồi 
 *   cuộc gọi tĩnh gián tiếp sang `MainUI.main(args)`, lách qua lớp phòng vệ kiểm tra mô-đun của JVM và khởi chạy 
 *   ứng dụng đồ họa bình thường và trơn tru.
 */
public class UILauncher {
    
    /**
     * Điểm mồi khởi chạy JVM gián tiếp, gọi sang hàm main của MainUI để kích hoạt JavaFX Runtime.
     * 
     * @param args Tham số dòng lệnh truyền từ hệ điều hành
     */
    public static void main(String[] args){
        // Gọi tĩnh gián tiếp lách luật kiểm tra Module Path của máy ảo Java
        MainUI.main(args);
    }
}
