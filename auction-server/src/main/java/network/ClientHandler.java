package network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import models.Auction;
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
import network.command.AddItemCommand;
import network.command.DeleteItemCommand;
import network.command.AutoBidCommand;
import network.command.CancelAutoBidCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lớp ClientHandler chịu trách nhiệm đại diện và xử lý kết nối song song thời gian thực của một Client.
 * Thực thi Runnable để chạy trên một luồng Thread riêng biệt.
 * Thực thi interface AuctionObserver để đóng vai trò là một Quan sát viên nhận các bản tin đấu giá và chuyển tiếp về Client.
 * Áp dụng mẫu thiết kế Command (Command Pattern) để phân phối xử lý các loại yêu cầu mạng khác nhau.
 */
public class ClientHandler implements Runnable, AuctionObserver {

    // Danh sách toàn bộ các quan sát viên (Client) trực tuyến hiện tại.
    // Sử dụng CopyOnWriteArrayList để đảm bảo an toàn đa luồng khi có Client kết nối/ngắt kết nối đồng thời với thao tác phát thông báo.
    private static final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    // Đối tượng Socket kết nối TCP đến Client cụ thể
    private Socket clientSocket;
    
    // Bộ ghi dữ liệu ra luồng Socket gửi dữ liệu về Client
    private PrintWriter out;
    
    // Cấu hình GSON dùng chung đảm bảo đồng bộ định dạng tuần tự hóa ngày tháng LocalDateTime
    private Gson gson = GsonConfig.createGson();
    
    // Tên tài khoản người dùng tương ứng với kết nối này (Được lưu lại sau khi Đăng nhập thành công)
    private String currentUsername;

    public String getCurrentUsername() { return currentUsername; }
    public void setCurrentUsername(String username) { this.currentUsername = username; }

    /**
     * Hàm khởi tạo ClientHandler và đăng ký các lệnh nghiệp vụ.
     * @param socket Đối tượng Socket của kết nối mới nhận được
     */
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        initializeCommands();
    }

    /**
     * Đăng ký các Lệnh xử lý (Commands) vào Router trung tâm.
     * Sử dụng mẫu thiết kế Command Pattern giúp tách biệt logic nghiệp vụ khỏi lớp xử lý mạng.
     */
    private void initializeCommands() {
        CommandRouter router = CommandRouter.getInstance();
        router.registerCommand("LOGIN", new LoginCommand());
        router.registerCommand("BID", new BidCommand());
        router.registerCommand("GET_SELLER_DASHBOARD", new GetSellerDashboardCommand());
        router.registerCommand("ADMIN", new AdminCommand());
        router.registerCommand("DELETE_ITEM", new DeleteItemCommand());
        router.registerCommand("ADD_ITEM", new AddItemCommand());
        router.registerCommand("AUTO_BID", new AutoBidCommand());
        router.registerCommand("CANCEL_AUTO_BID", new CancelAutoBidCommand());
    }

    /**
     * Điểm chạy chính của luồng Thread phụ xử lý Client.
     * Đọc các dòng dữ liệu dạng JSON gửi lên từ Client qua Socket, phân tích cú pháp 
     * và gọi Command tương ứng để xử lý.
     */
    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Đăng ký kết nối này vào danh sách Quan sát viên
            registerObserver(this);
            
            // Thiết lập callback notifyAll của AuctionManager liên kết trực tiếp với phương thức phát thông báo của ClientHandler
            AuctionManager.getInstance().setNotificationService(ClientHandler::notifyAllObservers);

            System.out.println(">>> Số Client đang kết nối: " + observers.size());

            String inputLine;
            // Vòng lặp liên tục đọc dữ liệu từ Socket của Client
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.trim().isEmpty()) {
                    continue;
                }

                try {
                    // Phân tích dòng nhận được thành JsonObject
                    JsonObject requestJson = gson.fromJson(inputLine, JsonObject.class);
                    String commandName = requestJson.has("command") ? requestJson.get("command").getAsString() : "";

                    // Tra cứu lệnh xử lý tương ứng
                    Command command = CommandRouter.getInstance().getCommand(commandName);
                    if (command != null) {
                        command.execute(requestJson, out, this);
                    } else {
                        // Nếu là các lệnh đời cũ (chưa refactor), chuyển sang hàm xử lý Legacy
                        handleLegacyCommands(commandName, inputLine, out);
                    }

                } catch (Exception e) {
                    System.out.println("--- LỖI DỮ LIỆU TỪ CLIENT ---");
                    e.printStackTrace();
                    out.println("ERROR: Sai dinh dang du lieu");
                }
            }
        } catch (IOException e) {
            // Khi Client mất kết nối đột ngột, ngoại lệ IOException sẽ được ném ra
        } finally {
            // Giải phóng quan sát viên và tài nguyên Socket khi luồng kết thúc
            removeObserver(this);
            System.out.println(">>> 1 Client vừa ngắt kết nối. Còn lại: " + observers.size());
        }
    }

    /**
     * Xử lý các gói tin nghiệp vụ đời cũ (Legacy Commands) để duy trì tính tương thích ngược.
     * @param command Tên lệnh cần xử lý
     * @param inputLine Chuỗi gói tin nhận được từ Socket
     * @param out Luồng gửi phản hồi về Client
     */
    private void handleLegacyCommands(String command, String inputLine, PrintWriter out) {
        try {
            // Lệnh Đăng Ký người dùng mới
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
            } 
            // Lệnh Lấy toàn bộ sản phẩm đang đấu giá
            else if ("GET_ITEMS".equals(command)) {
                // Lấy items đã lưu trong SQLite DB
                List<Item> dbItems = ItemManager.getInstance().getAllItems();
                
                // Lấy items của các phiên đang hoạt động trực tiếp trong bộ nhớ đệm Server (Memory)
                java.util.Map<String, models.Auction> activeAuctions = services.AuctionManager.getInstance().getAllActiveAuctionsMap();
                
                // Dùng cấu trúc Set để loại trừ trùng lặp mã sản phẩm giữa DB và bộ nhớ đệm
                Set<String> processedIds = new HashSet<>();
                com.google.gson.JsonArray itemsArray = new com.google.gson.JsonArray();
                
                // 1. Nạp các sản phẩm trong các phiên đấu giá đang diễn ra (in-memory)
                for (models.Auction auction : activeAuctions.values()) {
                    models.Item item = auction.getItem();
                    if (item != null && !processedIds.contains(item.getId())) {
                        processedIds.add(item.getId());
                        
                        JsonObject itemJson = gson.toJsonTree(item).getAsJsonObject();
                        itemJson.addProperty("auctionId", auction.getAuctionId());
                        itemJson.addProperty("currentHighestPrice", auction.getItem().getCurrentHighestPrice());
                        itemJson.addProperty("bidsCount", auction.getBidHistory().size());
                        itemJson.addProperty("auctionStatus", auction.getStatus().name());
                        
                        if (item.getSeller() != null) {
                            itemJson.addProperty("sellerName", item.getSeller().getUsername());
                        }
                        
                        itemsArray.add(itemJson);
                    }
                }
                
                // 2. Nạp thêm các sản phẩm khác từ SQLite (nếu chưa xuất hiện trong phiên đấu giá active)
                for (Item item : dbItems) {
                    if (!processedIds.contains(item.getId())) {
                        processedIds.add(item.getId());
                        
                        JsonObject itemJson = gson.toJsonTree(item).getAsJsonObject();
                        
                        // Tra cứu xem sản phẩm này có liên kết phiên thầu nào không để điền thông số
                        models.Auction auction = services.AuctionManager.getInstance().getAuctionByItemId(item.getId());
                        if (auction != null) {
                            itemJson.addProperty("auctionId", auction.getAuctionId());
                            itemJson.addProperty("currentHighestPrice", auction.getItem().getCurrentHighestPrice());
                            itemJson.addProperty("bidsCount", auction.getBidHistory().size());
                            itemJson.addProperty("auctionStatus", auction.getStatus().name());
                        } else {
                            itemJson.addProperty("currentHighestPrice", item.getCurrentHighestPrice());
                            itemJson.addProperty("bidsCount", 0);
                            itemJson.addProperty("auctionStatus", "FINISHED");
                        }
                        
                        itemsArray.add(itemJson);
                    }
                }
                
                // Tạo gói tin SET_ITEMS chứa danh sách sản phẩm gửi ngược lại cho Client hiển thị trên TableView
                JsonObject response = new JsonObject();
                response.addProperty("command", "SET_ITEMS");
                response.add("data", itemsArray);
                out.println(gson.toJson(response));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Thêm Quan sát viên kết nối trực tuyến vào danh sách quan sát chung.
     * @param observer Quan sát viên kết nối cần đăng ký
     */
    public static void registerObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    /**
     * Gỡ bỏ Quan sát viên khỏi danh sách (khi ngắt kết nối mạng).
     * @param observer Quan sát viên kết nối cần hủy đăng ký
     */
    public static void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    /**
     * Phát sóng thông báo mạng thời gian thực (Broadcast) tới toàn bộ các Client trực tuyến.
     * @param message Nội dung bản tin JSON dạng chuỗi
     */
    public static void notifyAllObservers(String message) {
        for (AuctionObserver obs : observers) {
            obs.updateClient(message);
        }
    }

    /**
     * Lắng nghe tín hiệu sự kiện từ Observer Pattern và thực thi gửi chuỗi tin nhắn JSON qua socket Client.
     * @param message Chuỗi tin nhắn JSON gửi về Client
     */
    @Override
    public void updateClient(String message) {
        if (this.out != null) {
            this.out.println(message);
        }
    }
}