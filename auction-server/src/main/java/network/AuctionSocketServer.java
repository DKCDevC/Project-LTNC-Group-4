// 1. Khai báo package: Nằm trong phân hệ network quản lý hạ tầng socket máy chủ.
package network;

// 2. Import các lớp Socket của Java SE IO
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Lớp AuctionSocketServer chịu trách nhiệm khởi chạy Socket Server ở phía Server.
 * Lắng nghe kết nối TCP từ các Client tại một cổng mạng (port) cố định.
 * Khi nhận được kết nối mới, tự động khởi tạo luồng xử lý bất đồng bộ riêng biệt (ClientHandler) cho Client đó.
 * 
 * Vai trò kiến trúc:
 * - Hạ tầng mạng ở tầng thấp nhất (Low-level TCP Socket Listener).
 * - Quản lý việc thiết lập cổng mạng và khởi chạy các phân luồng xử lý con (Worker Threads).
 */
public class AuctionSocketServer {
    // Cổng mạng lắng nghe kết nối (Ví dụ: 12345)
    private int port;

    /**
     * Hàm khởi tạo cấu hình Socket Server trên RAM.
     * @param port Cổng lắng nghe
     */
    public AuctionSocketServer(int port) {
        this.port = port;
    }

    /**
     * Bắt đầu mở cổng mạng và chạy vòng lặp lắng nghe vô hạn (Connection Acceptance Loop).
     * Khi có Client kết nối, tạo luồng Thread mới bọc lấy ClientHandler để không block việc nhận kết nối tiếp theo.
     * 
     * Kỹ thuật & Lý thuyết Java:
     * - Try-with-resources: `ServerSocket` được tạo ra trong khối ngoặc đơn `try` để JVM tự động đóng (AutoCloseable)
     *   khi có lỗi nghiêm trọng xảy ra, tránh rò rỉ cổng mạng hệ điều hành (Port Leaks).
     * - Vòng lặp `while (true)`: Vòng lặp vô hạn chạy ở nền (background) liên tục đón chờ Client mới.
     * - `serverSocket.accept()`: Lệnh đồng bộ (Blocking IO). Luồng khởi động server sẽ bị chặn (Block) tại dòng này 
     *   cho tới khi một client bắt tay TCP kết nối thành công, trả về đối tượng `Socket` đại diện cho kênh truyền thông đó.
     * - Concurrency (Đa luồng): Khởi tạo một đối tượng Thread mới bao lấy ClientHandler (Triển khai Runnable) và gọi `.start()`.
     *   Điều này đẩy tác vụ giao tiếp sang một Luồng công nhân (Worker Thread) riêng biệt, giải phóng ngay luồng chính
     *   để quay trở lại đầu vòng lặp `accept()` tiếp tục đón nhận các client khác, đảm bảo xử lý đồng thời (Concurrent handling).
     */
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("=== MỞ CỔNG MẠNG THÀNH CÔNG ===");
            System.out.println("Server đang lắng nghe kết nối tại cổng " + port + "...");

            // Vòng lặp vô hạn chấp nhận kết nối từ Client
            while (true) {
                // Đợi kết nối từ phía Client (Luồng chính sẽ block tại đây cho đến khi có Client kết nối)
                Socket clientSocket = serverSocket.accept();
                System.out.println(">>> Có Client mới vừa kết nối từ IP: " + clientSocket.getInetAddress());

                // Khởi chạy một Thread mới để xử lý Client này một cách bất đồng bộ
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Lỗi: Không thể mở cổng " + port + ". Có thể cổng này đang bị ứng dụng khác chiếm dụng.");
            e.printStackTrace();
        }
    }
}