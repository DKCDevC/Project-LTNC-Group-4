// 1. Khai báo package: Nằm trong phân hệ command quản lý các lớp lệnh mạng của Server.
package network.command;

// 2. Import các mô hình (Models) dữ liệu liên quan đến phiên đấu giá, bot tự động, người mua.
import com.google.gson.JsonObject;
import models.Auction;
import models.AutoBid;
import models.Bidder;
// 3. Import các lớp xử lý mạng và logic thầu
import network.ClientHandler;
import services.AuctionManager;
import java.io.PrintWriter;

/**
 * Lớp AutoBidCommand xử lý yêu cầu đăng ký/cấu hình Bot Đấu Giá Tự Động (Auto Bidding) của người mua.
 * Triển khai interface Command (Command Design Pattern).
 * Lớp này kiểm duyệt gói tin thầu tự động gửi từ ClientFX, thiết lập thực thể cấu hình AutoBid
 * và kích hoạt phản ứng nâng giá tức thì cho Robot.
 */
public class AutoBidCommand implements Command {
    // 4. Khai báo hằng số nghiệp vụ (Business Rule):
    // Bước nâng giá tự động tối thiểu bắt buộc phải đạt 10,000 đ để chống spam mạng hoặc làm loãng phiên thầu.
    private static final double MIN_INCREMENT = 10000.0;

    /**
     * Thực thi đăng ký Bot đấu giá tự động.
     * Trích xuất thông tin người dùng, giới hạn thầu tối đa (maxBid), bước nâng thầu (increment).
     * Kiểm duyệt ràng buộc nghiệp vụ về bước giá thầu tối thiểu.
     * Xóa bỏ cấu hình bot cũ nếu đã tồn tại để tránh xung đột thầu.
     * Ghi nhận bot mới vào Auction và gọi khởi động cơ đấu giá tự động tức thời.
     * 
     * @param requestJson Dữ liệu yêu cầu JSON từ Client gửi lên
     * @param out Luồng gửi dữ liệu phản hồi về socket client
     * @param handler Session quản lý socket tương ứng
     */
    @Override
    public void execute(JsonObject requestJson, PrintWriter out, ClientHandler handler) {
        // 5. Kiểm duyệt tính toàn vẹn của cấu trúc gói tin (Data Validation):
        // Nếu gói tin hoàn toàn trống hoặc thiếu thuộc tính "data", từ chối xử lý lập tức để bảo vệ server khỏi crash.
        if (!requestJson.has("data")) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Thiếu dữ liệu\"}");
            return;
        }

        // 6. Phân tích bóc tách các trường nghiệp vụ từ JsonObject:
        JsonObject data = requestJson.getAsJsonObject("data");
        String auctionId = data.get("auctionId").getAsString();
        String username = data.get("username").getAsString();
        double maxBid = data.get("maxBid").getAsDouble();
        double increment = data.get("increment").getAsDouble();

        // 7. Kiểm tra ràng buộc bước thầu tối thiểu 10,000 ₫:
        // Nếu client cố tình tìm cách bypass qua mặt UI bằng cách gửi gói tin thô increment < 10,000 đ,
        // Server sẽ chặn đứng và gửi thông báo lỗi ngược lại (Phòng thủ đa tầng - Defense in Depth).
        if (increment < MIN_INCREMENT) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Bước giá tối thiểu phải là " + String.format("%,.0f", MIN_INCREMENT) + " ₫\"}");
            return;
        }

        // 8. Lấy phiên đấu giá tương ứng từ bộ quản lý trong bộ nhớ đệm RAM (Memory) của Server:
        Auction auction = AuctionManager.getInstance().getAuction(auctionId);
        if (auction != null) {
            // 9. Đúc (Instantiate) đối tượng Người mua và Cấu hình Bot tự động trên RAM:
            Bidder bidder = new Bidder(username, "", "");
            AutoBid autoBid = new AutoBid(bidder, maxBid, increment);
            
            // 10. Bước 1: Dọn dẹp bot cũ của người dùng này trên sản phẩm này (nếu có).
            // Tránh việc người dùng nhấn kích hoạt nhiều lần tạo ra nhiều bot trùng lặp của cùng một tài khoản
            // chạy song song, gây xung đột nâng giá lẫn nhau dẫn đến lỗi cạn tiền vô lý (Bot Clash Hazard).
            AuctionManager.getInstance().removeAutoBid(auctionId, username);
            
            // 11. Bước 2: Đăng ký bot mới vào danh sách quan sát tự động của riêng phiên đấu giá đó.
            auction.registerAutoBid(autoBid);
            
            // 12. Bước 3: Kích hoạt động cơ tự động thầu ngay lập tức (Reactive Trigger):
            // Nếu giá thầu hiện tại của người khác đang cao hơn thầu của ta, robot sẽ lập tức phản ứng
            // tự động nâng thầu cho ta lên một bước giá mới mà không cần chờ tác động thủ công.
            AuctionManager.getInstance().checkAndTriggerAutoBids(auctionId);
            
            // Gửi phản hồi thành công về cho ClientFX chuyển trạng thái UI sang chế độ "ĐANG HOẠT ĐỘNG"
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Đã kích hoạt Auto Bid\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Không tìm thấy phiên đấu giá\"}");
        }
    }
}
