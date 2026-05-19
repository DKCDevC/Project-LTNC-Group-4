// 1. Khai báo package: Nằm trong phân hệ network của Server
package network;

// 2. Import các thư viện tuần tự hóa dữ liệu JSON của Google GSON:
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
// 3. Import các mô hình (Models) dữ liệu dùng chung
import models.Auction;
import models.AuctionRequest;
import models.BidTransaction;
import models.Item;
import models.User;
import models.Seller;
// 4. Import các tầng nghiệp vụ xử lý logic (Services)
import services.AuctionManager;
import services.ItemManager;
import services.UserManager;
import services.ItemFactory;
import utils.GsonConfig;
// 5. Import hệ thống Lệnh điều hướng (Command Pattern)
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

// 6. Import các thư viện vào/ra I/O, mạng Socket TCP và Cấu trúc dữ liệu đồng thời:
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
 * 
 * - Thực thi interface Runnable (Runnable Interface) cho phép đối tượng chạy bất đồng bộ trên một luồng Thread
 *   riêng biệt, giúp server phục vụ đồng thời hàng ngàn Client mà không bị tắc nghẽn luồng chính.
 * 
 * - Thực thi interface AuctionObserver (Observer Pattern - Người quan sát): Đóng vai trò là một quan sát viên kết nối,
 *   lắng nghe và nhận các thông báo giá thầu mới từ phòng đấu giá chung, sau đó đóng gói đẩy qua Socket truyền về Client.
 * 
 * - Áp dụng mẫu thiết kế Command (Command Pattern) để phân phối điều phối xử lý các loại yêu cầu mạng qua CommandRouter.
 */
public class ClientHandler implements Runnable, AuctionObserver {

    // 7. Danh sách toàn bộ các quan sát viên (Client) trực tuyến hiện tại.
    // Sử dụng CopyOnWriteArrayList thay vì ArrayList thông thường để đảm bảo an toàn đa luồng (Thread-safety).
    // Nó hoạt động bằng cách tạo ra một bản sao mới của mảng mỗi khi có thay đổi (Add/Remove), giúp triệt tiêu
    // lỗi đồng thời ConcurrentModificationException khi một luồng đang duyệt danh sách để phát tin (Broadcast)
    // trong khi một luồng khác lại ngắt kết nối và tự động xóa mình khỏi danh sách.
    private static final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    // Đối tượng Socket kết nối TCP vật lý kết nối với Client cụ thể
    private Socket clientSocket;
    
    // Bộ ghi dữ liệu ra luồng Socket (Writer) gửi dữ liệu văn bản về Client (tự động đẩy bộ đệm autoflush = true)
    private PrintWriter out;
    
    // Cấu hình GSON dùng chung đảm bảo đồng bộ định dạng tuần tự hóa ngày tháng LocalDateTime
    private Gson gson = GsonConfig.createGson();
    
    // Tên tài khoản người dùng tương ứng với kết nối này (Được lưu lại sau khi Đăng nhập thành công)
    private String currentUsername;

    public String getCurrentUsername() { return currentUsername; }
    public void setCurrentUsername(String username) { this.currentUsername = username; }

    /**
     * Hàm khởi tạo ClientHandler và đăng ký các lệnh nghiệp vụ.
     * 
     * @param socket Đối tượng Socket của kết nối mới nhận được từ ServerSocket.accept()
     */
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        initializeCommands(); // Đăng ký toàn bộ ánh xạ lệnh nghiệp vụ
    }

    /**
     * Đăng ký các Lệnh xử lý (Commands) vào Router trung tâm.
     * Sử dụng mẫu thiết kế Command Pattern giúp tách biệt hoàn toàn logic nghiệp vụ khỏi lớp xử lý mạng ClientHandler.
     * Mỗi lệnh là một lớp riêng biệt kế thừa từ interface Command.
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
     * Điểm chạy chính của luồng Thread phụ xử lý Client (Phương thức bắt buộc từ Runnable).
     * Đọc các dòng dữ liệu dạng JSON gửi lên từ Client qua Socket, phân tích cú pháp 
     * và gọi Command tương ứng để xử lý.
     */
    @Override
    public void run() {
        try {
            // 8. BufferedReader (Reader): Đọc dữ liệu văn bản từ luồng vào Socket của Client một cách tối ưu hiệu năng qua bộ đệm.
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // 9. Đăng ký kết nối hiện tại này vào danh sách Quan sát viên
            registerObserver(this);
            
            // 10. Thiết lập callback notifyAll của AuctionManager liên kết trực tiếp với phương thức phát thông báo của ClientHandler
            // Sử dụng kỹ thuật Method Reference (Tham chiếu phương thức) của Java 8: `ClientHandler::notifyAllObservers`
            AuctionManager.getInstance().setNotificationService(ClientHandler::notifyAllObservers);

            System.out.println(">>> Số Client đang kết nối: " + observers.size());

            String inputLine;
            // 11. Vòng lặp vô tận (Event Loop) đọc dữ liệu từ Socket của Client cho đến khi dòng đọc được là null (ngắt kết nối).
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.trim().isEmpty()) {
                    continue;
                }

                try {
                    // 12. Phân tích dòng nhận được thành JsonObject để tra cứu lệnh
                    JsonObject requestJson = gson.fromJson(inputLine, JsonObject.class);
                    String commandName = requestJson.has("command") ? requestJson.get("command").getAsString() : "";

                    // 13. Tra cứu lệnh xử lý tương ứng trong CommandRouter
                    Command command = CommandRouter.getInstance().getCommand(commandName);
                    if (command != null) {
                        // Gọi thực thi Lệnh và truyền các tham số ngữ cảnh
                        command.execute(requestJson, out, this);
                    } else {
                        // Nếu là các lệnh đời cũ (chưa refactor), chuyển sang hàm xử lý tương thích ngược
                        handleLegacyCommands(commandName, inputLine, out);
                    }

                } catch (Exception e) {
                    System.out.println("--- LỖI DỮ LIỆU TỪ CLIENT ---");
                    e.printStackTrace();
                    out.println("ERROR: Sai dinh dang du lieu");
                }
            }
        } catch (IOException e) {
            // Khi Client mất kết nối mạng đột ngột hoặc tắt ứng dụng, ngoại lệ IOException sẽ được ném ra ở đây.
        } finally {
            // 14. Dọn dẹp tài nguyên (Clean up resources):
            // Giải phóng quan sát viên và giải phóng bộ nhớ để tránh rò rỉ bộ nhớ (Memory Leak) trên JVM.
            removeObserver(this);
            System.out.println(">>> 1 Client vừa ngắt kết nối. Còn lại: " + observers.size());
        }
    }

    /**
     * Xử lý các gói tin nghiệp vụ đời cũ (Legacy Commands) để duy trì tính tương thích ngược với client cũ.
     * 
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
            // Lệnh Lấy toàn bộ danh sách sản phẩm đấu giá
            else if ("GET_ITEMS".equals(command)) {
                // Lấy items đã lưu trong SQLite DB
                List<Item> dbItems = ItemManager.getInstance().getAllItems();
                
                // Lấy items của các phiên đang hoạt động trực tiếp trong bộ nhớ đệm Server (Memory)
                java.util.Map<String, models.Auction> activeAuctions = services.AuctionManager.getInstance().getAllActiveAuctionsMap();
                
                // Dùng cấu trúc dữ liệu Set để loại trừ trùng lặp mã sản phẩm giữa DB và bộ nhớ đệm
                Set<String> processedIds = new HashSet<>();
                com.google.gson.JsonArray itemsArray = new com.google.gson.JsonArray();
                
                // 1. Nạp các sản phẩm trong các phiên đấu giá đang diễn ra (in-memory active auctions)
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
     * 
     * @param observer Quan sát viên kết nối cần đăng ký
     */
    public static void registerObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    /**
     * Gỡ bỏ Quan sát viên khỏi danh sách (khi ngắt kết nối mạng).
     * 
     * @param observer Quan sát viên kết nối cần hủy đăng ký
     */
    public static void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    /**
     * Phát sóng thông báo mạng thời gian thực (Broadcast) tới toàn bộ các Client trực tuyến.
     * 
     * @param message Nội dung bản tin JSON dạng chuỗi
     */
    public static void notifyAllObservers(String message) {
        for (AuctionObserver obs : observers) {
            obs.updateClient(message);
        }
    }

    /**
     * Lắng nghe tín hiệu sự kiện từ Observer Pattern và thực thi gửi chuỗi tin nhắn JSON qua socket Client.
     * 
     * @param message Chuỗi tin nhắn JSON gửi về Client
     */
    @Override
    public void updateClient(String message) {
        if (this.out != null) {
            this.out.println(message);
        }
    }
}