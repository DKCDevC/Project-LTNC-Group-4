package network.command;

import com.google.gson.JsonObject;
import network.ClientHandler;
import services.AuctionManager;
import java.io.PrintWriter;

/**
 * Lớp CancelAutoBidCommand xử lý yêu cầu hủy bỏ Bot Đấu Giá Tự Động từ phía Client (Người mua).
 * Triển khai interface Command.
 */
public class CancelAutoBidCommand implements Command {
    
    /**
     * Thực thi nghiệp vụ hủy bot đặt giá tự động.
     * Trích xuất mã ID sản phẩm và tên tài khoản người thầu cần hủy bot.
     * Gọi xuống AuctionManager để tiến hành tìm kiếm và gỡ bỏ đối tượng AutoBid khỏi phiên thầu.
     */
    @Override
    public void execute(JsonObject requestJson, PrintWriter out, ClientHandler handler) {
        String auctionId = requestJson.has("auctionId") ? requestJson.get("auctionId").getAsString() : "";
        String username = requestJson.has("username") ? requestJson.get("username").getAsString() : "";

        // Kiểm duyệt tính toàn vẹn của dữ liệu gửi lên
        if (auctionId.isEmpty() || username.isEmpty()) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Thiếu thông tin\"}");
            return;
        }

        // Gọi xuống dịch vụ quản lý đấu giá để xóa cấu hình AutoBid
        boolean removed = AuctionManager.getInstance().removeAutoBid(auctionId, username);
        
        // Phản hồi kết quả về phía Client
        if (removed) {
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Đã hủy Auto Bid\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Không tìm thấy Auto Bid để hủy\"}");
        }
    }
}
