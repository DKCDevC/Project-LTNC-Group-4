package network;

import com.google.gson.Gson;
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

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            registerObserver(this);
            System.out.println(">>> Số Client đang kết nối: " + observers.size());

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // CHỐT CHẶN 1: Bỏ qua các tín hiệu rỗng để tránh sập Server
                if (inputLine.trim().isEmpty()) {
                    continue;
                }

                try {
                    AuctionRequest request = gson.fromJson(inputLine, AuctionRequest.class);

                    if ("LOGIN".equals(request.getCommand())) {
                        JsonObject loginData = gson.fromJson(inputLine, JsonObject.class);

                        String username = loginData.has("username") ? loginData.get("username").getAsString() : "";
                        String password = loginData.has("password") ? loginData.get("password").getAsString() : "";

                        User loggedInUser = UserManager.getInstance().login(username, password);

                        if (loggedInUser != null) {
                            out.println("{\"status\":\"SUCCESS\", \"role\":\"" + loggedInUser.getRole() + "\"}");
                            System.out.println(">>> User [" + loggedInUser.getUsername() + "] đã đăng nhập thành công!");
                        } else {
                            out.println("{\"status\":\"FAILED\", \"message\":\"Sai tai khoan hoac mat khau\"}");
                        }
                    }

                    // --- ĐÃ THÊM: Xử lý Đăng ký tài khoản mới ---
                    else if ("REGISTER".equals(request.getCommand())) {
                        JsonObject regData = gson.fromJson(inputLine, JsonObject.class);
                        String username = regData.get("username").getAsString();
                        String password = regData.get("password").getAsString();
                        String role = regData.get("role").getAsString(); // BIDDER hoặc SELLER

                        boolean success = UserManager.getInstance().registerUser(username, password, role);

                        if (success) {
                            out.println("{\"status\":\"SUCCESS\"}");
                            System.out.println(">>> Đã tạo tài khoản mới: " + username + " (" + role + ")");
                        } else {
                            out.println("{\"status\":\"FAILED\", \"message\":\"Tài khoản đã tồn tại!\"}");
                        }
                    }
                    // ---------------------------------------------------------

                    // --- Xử lý lấy danh sách sản phẩm từ Database ---
                    else if ("GET_ITEMS".equals(request.getCommand())) {
                        List<Item> items = ItemManager.getInstance().getAllItems();

                        JsonObject response = new JsonObject();
                        response.addProperty("command", "SET_ITEMS");
                        response.add("data", gson.toJsonTree(items));

                        out.println(gson.toJson(response));
                    }

                    // --- Xử lý Seller thêm sản phẩm mới ---
                    else if ("ADD_ITEM".equals(request.getCommand())) {
                        JsonObject fullJson = gson.fromJson(inputLine, JsonObject.class);
                        JsonObject data = fullJson.getAsJsonObject("data");

                        String type = data.get("type").getAsString();
                        String name = data.get("name").getAsString();
                        String desc = data.get("desc").getAsString();
                        double price = data.get("price").getAsDouble();
                        String extra = data.get("extra").getAsString();
                        String sellerName = data.get("seller").getAsString();

                        java.time.LocalDateTime start = java.time.LocalDateTime.now();
                        java.time.LocalDateTime end = start.plusDays(7);

                        Item newItem = ItemFactory.createItem(type, name, desc, price, start, end, extra);

                        String newId = java.util.UUID.randomUUID().toString();
                        ItemManager.getInstance().addItem(newId, newItem, new Seller(sellerName, "", ""));

                        notifyAllObservers("{\"command\":\"UPDATE_PRICE\"}");
                    }

                    else if ("BID".equals(request.getCommand())) {
                        JsonElement jsonElement = gson.toJsonTree(request.getData());
                        BidTransaction bid = gson.fromJson(jsonElement, BidTransaction.class);

                        String realAuctionId = AuctionManager.getInstance().getFirstRunningAuctionId();

                        boolean success = AuctionManager.getInstance().placeBid(
                                realAuctionId,
                                bid.getBidder(),
                                bid.getAmount()
                        );

                        if (success) {
                            out.println("SUCCESS");

                            String broadcastMsg = "{\"command\":\"UPDATE_PRICE\", \"message\":\"Sản phẩm vừa được " +
                                    bid.getBidder().getUsername() + " đặt giá " + bid.getAmount() + "\"}";

                            notifyAllObservers(broadcastMsg);
                        } else {
                            out.println("FAILED: Gia thap hon hoac phien da dong");
                        }
                    }

                } catch (Exception e) {
                    System.out.println("--- LỖI DỮ LIỆU TỪ CLIENT ---");
                    System.out.println("Chuỗi lỗi: " + inputLine);
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