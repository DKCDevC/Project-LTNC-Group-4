package network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import models.AuctionRequest;
import models.BidTransaction;
import models.Item;
import models.User;
import models.Seller;
import services.AuctionManager;
import services.ItemManager;
import services.UserManager;
import services.ItemFactory;
import utils.GsonConfig;
import network.command.Command;
import network.command.CommandRouter;
import network.command.LoginCommand;
import network.command.BidCommand;
import network.command.GetSellerDashboardCommand;
import network.command.AdminCommand;
import network.command.DeleteItemCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable, AuctionObserver {

    private static final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private Socket clientSocket;
    private PrintWriter out;
    private Gson gson = GsonConfig.createGson();
    private String currentUsername;

    public String getCurrentUsername() { return currentUsername; }
    public void setCurrentUsername(String username) { this.currentUsername = username; }

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        initializeCommands();
    }

    private void initializeCommands() {
        CommandRouter router = CommandRouter.getInstance();
        router.registerCommand("LOGIN", new LoginCommand());
        router.registerCommand("BID", new BidCommand());
        router.registerCommand("GET_SELLER_DASHBOARD", new GetSellerDashboardCommand());
        router.registerCommand("ADMIN", new AdminCommand());
        router.registerCommand("DELETE_ITEM", new DeleteItemCommand());
        // Các command khác sẽ được đăng ký tương tự
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            registerObserver(this);
            // Thiết lập notification service cho AuctionManager
            AuctionManager.getInstance().setNotificationService(ClientHandler::notifyAllObservers);

            System.out.println(">>> Số Client đang kết nối: " + observers.size());

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.trim().isEmpty()) {
                    continue;
                }

                try {
                    JsonObject requestJson = gson.fromJson(inputLine, JsonObject.class);
                    String commandName = requestJson.has("command") ? requestJson.get("command").getAsString() : "";

                    Command command = CommandRouter.getInstance().getCommand(commandName);
                    if (command != null) {
                        command.execute(requestJson, out, this);
                    } else {
                        // Xử lý các lệnh cũ chưa refactor hoặc báo lỗi
                        handleLegacyCommands(commandName, inputLine, out);
                    }

                } catch (Exception e) {
                    System.out.println("--- LỖI DỮ LIỆU TỪ CLIENT ---");
                    e.printStackTrace();
                    out.println("ERROR: Sai dinh dang du lieu");
                }
            }
        } catch (IOException e) {
        } finally {
            removeObserver(this);
            System.out.println(">>> 1 Client vừa ngắt kết nối. Còn lại: " + observers.size());
        }
    }

    private void handleLegacyCommands(String command, String inputLine, PrintWriter out) {
        // Tạm thời giữ lại các lệnh chưa chuyển sang Command Pattern để đảm bảo tính năng
        try {
            if ("REGISTER".equals(command)) {
                JsonObject regData = gson.fromJson(inputLine, JsonObject.class);
                String username = regData.get("username").getAsString();
                String password = regData.get("password").getAsString();
                String email = regData.has("email") ? regData.get("email").getAsString() : (username + "@gmail.com");
                String role = regData.get("role").getAsString();
                boolean success = UserManager.getInstance().registerUser(username, password, email, role);
                if (success) {
                    out.println("{\"status\":\"SUCCESS\"}");
                } else {
                    out.println("{\"status\":\"FAILED\", \"message\":\"Tài khoản đã tồn tại!\"}");
                }
            } else if ("GET_ITEMS".equals(command)) {
                List<Item> items = ItemManager.getInstance().getAllItems();
                JsonObject response = new JsonObject();
                response.addProperty("command", "SET_ITEMS");
                response.add("data", gson.toJsonTree(items));
                out.println(gson.toJson(response));
            }
            // ... các legacy command khác
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void registerObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public static void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    public static void notifyAllObservers(String message) {
        for (AuctionObserver obs : observers) {
            obs.updateClient(message);
        }
    }

    @Override
    public void updateClient(String message) {
        if (this.out != null) {
            this.out.println(message);
        }
    }
}