package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionSocketServer {
    private int port;

    public AuctionSocketServer(int port) {
        this.port = port;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("=== MỞ CỔNG MẠNG THÀNH CÔNG ===");
            System.out.println("Server đang lắng nghe kết nối tại cổng " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(">>> Có Client mới vừa kết nối từ IP: " + clientSocket.getInetAddress());

                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Lỗi: Không thể mở cổng " + port + ". Có thể cổng này đang bị ứng dụng khác chiếm dụng.");
            e.printStackTrace();
        }
    }
}