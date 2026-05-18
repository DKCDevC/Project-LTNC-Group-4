package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Lớp AuctionSocketServer chịu trách nhiệm khởi chạy Socket Server ở phía Server.
 * Lắng nghe kết nối TCP từ các Client tại một cổng mạng (port) cố định.
 * Khi nhận được kết nối mới, tự động khởi tạo luồng xử lý bất đồng bộ riêng biệt (ClientHandler) cho Client đó.
 */
public class AuctionSocketServer {
    // Cổng mạng lắng nghe kết nối
    private int port;

    /**
     * Hàm khởi tạo cấu hình Socket Server.
     * @param port Cổng lắng nghe
     */
    public AuctionSocketServer(int port) {
        this.port = port;
    }

    /**
     * Bắt đầu mở cổng mạng và chạy vòng lặp lắng nghe vô hạn (Connection Acceptance Loop).
     * Khi có Client kết nối, tạo luồng Thread mới bọc lấy ClientHandler để không block việc nhận kết nối tiếp theo.
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