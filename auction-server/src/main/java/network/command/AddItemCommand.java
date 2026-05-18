package network.command;

import com.google.gson.JsonObject;
import models.Auction;
import models.Item;
import models.Seller;
import services.AuctionManager;
import services.ItemFactory;
import services.ItemManager;
import network.ClientHandler;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp AddItemCommand xử lý yêu cầu thêm sản phẩm đấu giá mới đăng tải từ phía Client (Người bán).
 * Triển khai interface Command.
 */
public class AddItemCommand implements Command {
    
    /**
     * Thực thi nghiệp vụ thêm sản phẩm mới.
     * Trích xuất các tham số (tên, mô tả, giá khởi điểm, đơn vị thời gian đấu giá, thông tin đặc trưng).
     * Áp dụng Factory để tạo sản phẩm con tương ứng, lưu vào SQLite DB và đưa vào Memory AuctionManager để bắt đầu đấu giá lập tức.
     */
    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        try {
            // Trích xuất cấu trúc dữ liệu con "data" chứa thông số sản phẩm
            JsonObject data = requestData.getAsJsonObject("data");
            String type = data.get("type").getAsString();
            String name = data.get("name").getAsString();
            String desc = data.get("desc").getAsString();
            double price = data.get("price").getAsDouble();
            String extra = data.has("extra") ? data.get("extra").getAsString() : "";
            
            // Lọc ra danh tính người bán từ phiên ClientHandler đang đăng nhập hiện tại
            String sellerName = (handler.getCurrentUsername() != null) ? 
                               handler.getCurrentUsername() : 
                               (data.has("seller") ? data.get("seller").getAsString() : "Unknown");
                               
            int durationValue = data.has("durationValue") ? data.get("durationValue").getAsInt() : (data.has("duration") ? data.get("duration").getAsInt() : 7);
            String durationUnit = data.has("durationUnit") ? data.get("durationUnit").getAsString() : "Ngày";

            // Thiết lập mốc thời gian bắt đầu thầu là ngay bây giờ
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = startTime;

            // Xử lý linh hoạt đơn vị thời gian đấu giá (Phút, Giờ, Ngày)
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

            // 1. Tạo thực thể sản phẩm cụ thể thông qua ItemFactory (Factory Pattern)
            Item item = ItemFactory.createItem(type, name, desc, price, startTime, endTime, extra);
            
            // Gắn URL hình ảnh sản phẩm vào trường image_urls (lưu trữ trong extra)
            item.setImageUrls(extra); 
            
            // Sinh mã sản phẩm định dạng ngắn UUID độc nhất vô nhị
            String id = "ITEM-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            item.setId(id);

            // Khởi tạo đối tượng Seller
            Seller seller = new Seller(sellerName, "", "");
            
            // 2. Gọi ItemManager lưu thông tin sản phẩm và liên kết người bán xuống SQLite Database
            ItemManager.getInstance().addItem(id, item, seller);

            // 3. Tạo một phiên đấu giá trực tuyến (Auction Object) tương ứng lưu trữ trên bộ nhớ RAM Server
            Auction auction = new Auction(item, seller);
            auction.setAuctionId(id);
            AuctionManager.getInstance().addAuction(auction);

            // Gửi phản hồi báo thành công về cho giao diện Client đóng Dialog và cập nhật bảng hiển thị
            out.println("SUCCESS");
            System.out.println(">>> [SERVER] Đã thêm sản phẩm mới thành công: " + name + " (ID: " + id + ") bởi " + sellerName);

        } catch (Exception e) {
            System.err.println("!!! LỖI khi thêm sản phẩm: " + e.getMessage());
            e.printStackTrace();
            // Gửi phản hồi lỗi về Client
            out.println("FAILED");
        }
    }
}
