package network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import models.AuctionRequest;
import models.BidTransaction;
import models.Bidder;
import utils.GsonConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class AuctionClient {
    public static void main(String[] args) {
        Gson gson = GsonConfig.createGson();
        String hostname = "127.0.0.1";
        int port = 9999;

        try {
            Socket socket = new Socket(hostname, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in);

            System.out.println("--- KẾT NỐI SERVER THÀNH CÔNG ---");

            String loggedInUsername = null;
            while (loggedInUsername == null) {
                System.out.println("\n=== ĐĂNG NHẬP HỆ THỐNG ===");
                System.out.print("Username: ");
                String username = scanner.nextLine();

                System.out.print("Password: ");
                String password = scanner.nextLine();

                Map<String, String> loginData = new HashMap<>();
                loginData.put("username", username);
                loginData.put("password", password);

                AuctionRequest loginReq = new AuctionRequest("LOGIN", loginData);
                out.println(gson.toJson(loginReq)); // Gửi lên Server

                String serverResponse = in.readLine();
                JsonObject responseJson = gson.fromJson(serverResponse, JsonObject.class);

                if (responseJson.has("status") && responseJson.get("status").getAsString().equals("SUCCESS")) {
                    System.out.println(">>> ĐĂNG NHẬP THÀNH CÔNG! Chào mừng " + username + " <<<");
                    loggedInUsername = username;
                } else {
                    System.out.println(">>> Đăng nhập thất bại. Sai tài khoản hoặc mật khẩu!");
                }
            }

            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println("\n[THÔNG BÁO TỪ SERVER]: " + serverMessage);
                        System.out.print("[NHẬP SỐ TIỀN BẠN MUỐN ĐẤU GIÁ]: ");
                    }
                } catch (Exception e) {
                    System.out.println("\nMất kết nối tới Server!");
                }
            });
            listenerThread.start();

            Bidder bidder = new Bidder(loggedInUsername, "", "");

            while (true) {
                System.out.print("\n[NHẬP SỐ TIỀN BẠN MUỐN ĐẤU GIÁ]: ");
                if(scanner.hasNextDouble()) {
                    double amount = scanner.nextDouble();
                    scanner.nextLine();

                    BidTransaction bidData = new BidTransaction(bidder, amount);
                    AuctionRequest request = new AuctionRequest("BID", bidData);

                    out.println(gson.toJson(request));
                    System.out.println(">>> Đã gửi yêu cầu đặt giá " + amount + " lên Server...");
                } else {
                    System.out.println("Lỗi: Vui lòng nhập một số hợp lệ!");
                    scanner.nextLine();
                }
            }

        } catch (Exception e) {
            System.out.println("Lỗi kết nối hoặc Server chưa bật!");
        }
    }
}
