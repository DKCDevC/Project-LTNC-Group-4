// 1. Khai báo package: Nằm trong phân hệ command quản lý các lớp lệnh mạng của Server.
package network.command;

// 2. Import các mô hình (Models) dữ liệu liên quan đến phiên đấu giá, người bán, sản phẩm.
import com.google.gson.JsonObject;
import models.Auction;
import models.Item;
import models.Seller;
// 3. Import các lớp xử lý nghiệp vụ của Server
import services.AuctionManager;
import services.ItemFactory;
import services.ItemManager;
import network.ClientHandler;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp AddItemCommand xử lý yêu cầu thêm sản phẩm đấu giá mới đăng tải từ phía Client (Người bán).
 * Triển khai interface Command (Command Design Pattern).
 * Lớp này chịu trách nhiệm trích xuất cấu hình sản phẩm từ JSON, ánh xạ mốc kết thúc, 
 * tạo thực thể con đa hình thông qua ItemFactory và kích hoạt đấu giá trực tuyến.
 */
public class AddItemCommand implements Command {
    
    /**
     * Thực thi nghiệp vụ thêm sản phẩm mới.
     * Trích xuất các tham số (tên, mô tả, giá khởi điểm, đơn vị thời gian đấu giá, thông tin đặc trưng).
     * Áp dụng Factory để tạo sản phẩm con tương ứng, lưu vào SQLite DB và đưa vào Memory AuctionManager để bắt đầu đấu giá lập tức.
     * 
     * @param requestData Dữ liệu cấu hình sản phẩm gửi lên từ client dạng JSON
     * @param out Luồng ghi dữ liệu mạng gửi phản hồi về client
     * @param handler Session quản lý socket tương ứng
     */
    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        try {
            // 4. Trích xuất cấu trúc dữ liệu con "data" chứa thông số sản phẩm:
            JsonObject data = requestData.getAsJsonObject("data");
            String type = data.get("type").getAsString();
            String name = data.get("name").getAsString();
            String desc = data.get("desc").getAsString();
            double price = data.get("price").getAsDouble();
            String extra = data.has("extra") ? data.get("extra").getAsString() : "";
            
            // 5. Xác thực danh tính người bán từ phiên ClientHandler đang đăng nhập hiện tại:
            // State Session Tracking: Lấy username trực tiếp của người bán hiện tại để tránh giả mạo (Seller impersonation).
            String sellerName = (handler.getCurrentUsername() != null) ? 
                               handler.getCurrentUsername() : 
                               (data.has("seller") ? data.get("seller").getAsString() : "Unknown");
                               
            int durationValue = data.has("durationValue") ? data.get("durationValue").getAsInt() : (data.has("duration") ? data.get("duration").getAsInt() : 7);
            String durationUnit = data.has("durationUnit") ? data.get("durationUnit").getAsString() : "Ngày";

            // Thiết lập mốc thời gian bắt đầu thầu là ngay bây giờ
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = startTime;

            // 6. Xử lý linh hoạt đơn vị thời gian đấu giá (Flexible Time Units mapping):
            // Switch-case ánh xạ giúp nâng cao trải nghiệm người dùng, hỗ trợ cấu hình phiên đấu giá dạng Phút, Giờ, Ngày.
            switch (durationUnit) {
                case "Phút":
                    endTime = startTime.plusMinutes(durationValue);
                    break;
                case "Giờ":
                    endTime = startTime.plusHours(durationValue);
                    break;
                case "Ngày":
                default:
                    endTime = startTime.plusDays(durationValue);
                    break;
            }

            // 7. Sử dụng Mẫu thiết kế Factory Method (Factory Method Design Pattern):
            // Tránh việc gọi từ khóa `new Electronics(...)` hay `new GeneralItem(...)` thủ công tại đây.
            // ItemFactory sẽ đóng vai trò che giấu logic đúc đối tượng, tự động trả về lớp con tương thích
            // dựa trên tham số `type`. Điều này giúp tuân thủ nguyên lý Đóng/Mở (Open/Closed Principle - OCP).
            Item item = ItemFactory.createItem(type, name, desc, price, startTime, endTime, extra);
            
            // Gán URL hình ảnh sản phẩm vào trường image_urls (được tái sử dụng trong extra)
            item.setImageUrls(extra); 
            
            // 8. Sinh mã sản phẩm ngẫu nhiên dùng lớp UUID (Universally Unique Identifier):
            // Sinh mã ngắn 5 ký tự độc nhất vô nhị để định danh sản phẩm, tránh xung đột khóa chính SQLite.
            String id = "ITEM-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            item.setId(id);

            // Khởi tạo đối tượng Seller đại diện cho người bán
            Seller seller = new Seller(sellerName, "", "");
            
            // 9. Ghi dữ liệu xuống SQLite Database:
            // Sử dụng ItemManager lưu thông tin sản phẩm và liên kết người bán xuống đĩa cứng bền vững.
            ItemManager.getInstance().addItem(id, item, seller);

            // 10. Đăng ký phiên đấu giá trực tuyến trong bộ nhớ RAM Server:
            // Tạo đối tượng Auction, gắn ID trùng với ID sản phẩm để tạo liên kết 1-1 chặt chẽ,
            // sau đó nạp vào AuctionManager để bắt đầu lắng nghe đấu giá trực tiếp lập tức.
            Auction auction = new Auction(item, seller);
            auction.setAuctionId(id);
            AuctionManager.getInstance().addAuction(auction);

            // 11. Gửi phản hồi thành công mạng (Response Protocol):
            // ClientFX sau khi nhận chuỗi thô "SUCCESS" sẽ tự động đóng Dialog Pop-up và reload giao diện Dashboard Table.
            out.println("SUCCESS");
            System.out.println(">>> [SERVER] Đã thêm sản phẩm mới thành công: " + name + " (ID: " + id + ") bởi " + sellerName);

            // 12. Phát sóng cập nhật danh sách sản phẩm tới tất cả các Client trực tuyến (Real-time Broadcast)
            ClientHandler.notifyAllObservers("{\"command\":\"UPDATE_ITEMS\"}");

        } catch (Exception e) {
            System.err.println("!!! LỖI khi thêm sản phẩm: " + e.getMessage());
            e.printStackTrace();
            // Gửi phản hồi lỗi về Client
            out.println("FAILED");
        }
    }
}
