package network.command;

import com.google.gson.JsonObject;
import models.Auction;
import models.AutoBid;
import models.Bidder;
import network.ClientHandler;
import services.AuctionManager;
import java.io.PrintWriter;

/**
 * Lớp AutoBidCommand xử lý yêu cầu đăng ký/cấu hình Bot Đấu Giá Tự Động (Auto Bidding) của người mua.
 * Triển khai interface Command.
 */
public class AutoBidCommand implements Command {
    // Ràng buộc quy định nghiệp vụ: Bước giá thầu tự động tối thiểu phải đạt 10,000 đ
    private static final double MIN_INCREMENT = 10000.0;

    /**
     * Thực thi đăng ký Bot đấu giá tự động.
     * Trích xuất thông tin người dùng, giới hạn thầu tối đa (maxBid), bước nâng thầu (increment).
     * Kiểm duyệt ràng buộc nghiệp vụ về bước giá thầu tối thiểu.
     * Xóa bỏ cấu hình bot cũ nếu đã tồn tại để tránh xung đột thầu.
     * Ghi nhận bot mới vào Auction và gọi khởi động cơ đấu giá tự động tức thời.
     */
    @Override
    public void execute(JsonObject requestJson, PrintWriter out, ClientHandler handler) {
        // Kiểm duyệt tính toàn vẹn của cấu trúc gói tin
        if (!requestJson.has("data")) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Thiếu dữ liệu\"}");
            return;
        }

        JsonObject data = requestJson.getAsJsonObject("data");
        String auctionId = data.get("auctionId").getAsString();
        String username = data.get("username").getAsString();
        double maxBid = data.get("maxBid").getAsDouble();
        double increment = data.get("increment").getAsDouble();

        // Kiểm tra ràng buộc bước thầu tối thiểu 10,000 ₫
        if (increment < MIN_INCREMENT) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Bước giá tối thiểu phải là " + String.format("%,.0f", MIN_INCREMENT) + " ₫\"}");
            return;
        }

        // Lấy phiên đấu giá tương ứng từ bộ quản lý trong memory
        Auction auction = AuctionManager.getInstance().getAuction(auctionId);
        if (auction != null) {
            Bidder bidder = new Bidder(username, "", "");
            AutoBid autoBid = new AutoBid(bidder, maxBid, increment);
            
            // Bước 1: Dọn dẹp bot cũ của người dùng này trên sản phẩm này (tránh việc trùng lặp bot)
            AuctionManager.getInstance().removeAutoBid(auctionId, username);
            
            // Bước 2: Đăng ký bot mới vào danh sách lắng nghe của phiên đấu giá
            auction.registerAutoBid(autoBid);
            
            // Bước 3: Kích hoạt động cơ tự động thầu ngay lập tức (nếu giá thầu hiện tại của người khác đang dẫn trước)
            AuctionManager.getInstance().checkAndTriggerAutoBids(auctionId);
            
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Đã kích hoạt Auto Bid\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Không tìm thấy phiên đấu giá\"}");
        }
    }
}
