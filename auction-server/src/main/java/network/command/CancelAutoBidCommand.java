// 1. Khai báo package: Nằm trong phân hệ command quản lý các lớp lệnh mạng của Server.
package network.command;

// 2. Import các thư viện JSON và các thành phần xử lý mạng, nghiệp vụ Server
import com.google.gson.JsonObject;
import network.ClientHandler;
import services.AuctionManager;
import java.io.PrintWriter;

/**
 * Lớp CancelAutoBidCommand xử lý yêu cầu hủy bỏ Bot Đấu Giá Tự Động từ phía Client (Người mua).
 * Triển khai interface Command (Command Design Pattern).
 * Lớp này chịu trách nhiệm trích xuất cấu hình bot, gỡ bỏ lắng nghe tự động khỏi phiên thầu trong RAM,
 * và trả về trạng thái SUCCESS/FAILED cho giao diện người dùng.
 */
public class CancelAutoBidCommand implements Command {
    
    /**
     * Thực thi nghiệp vụ hủy bot đặt giá tự động.
     * Trích xuất mã ID sản phẩm và tên tài khoản người thầu cần hủy bot.
     * Gọi xuống AuctionManager để tiến hành tìm kiếm và gỡ bỏ đối tượng AutoBid khỏi phiên thầu.
     * 
     * @param requestJson Dữ liệu JSON chứa thông tin hủy bot gửi từ Client
     * @param out Luồng ghi dữ liệu mạng gửi phản hồi về client
     * @param handler Session quản lý socket tương ứng
     */
    @Override
    public void execute(JsonObject requestJson, PrintWriter out, ClientHandler handler) {
        // 3. Trích xuất thông tin định danh phiên thầu và tài khoản đăng ký bot:
        // Sử dụng toán tử ba ngôi để phòng tránh NullPointerException nếu các thuộc tính này không tồn tại trong JSON.
        String auctionId = requestJson.has("auctionId") ? requestJson.get("auctionId").getAsString() : "";
        String username = requestJson.has("username") ? requestJson.get("username").getAsString() : "";

        // 4. Kiểm duyệt dữ liệu đầu vào (Validation Audit):
        // Nếu một trong hai tham số bị khuyết, Server từ chối xử lý và gửi thông báo lỗi ngược lại Client lập tức.
        if (auctionId.isEmpty() || username.isEmpty()) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Thiếu thông tin\"}");
            return;
        }

        // 5. Ủy thác nghiệp vụ cho AuctionManager:
        // removeAutoBid() chịu trách nhiệm khóa đồng bộ phiên thầu, tìm kiếm bot của `username` trong danh sách AutoBids
        // và hủy đăng ký (deregister) bot, giải phóng tài nguyên CPU tránh việc bot tự động nâng thầu vô định.
        boolean removed = AuctionManager.getInstance().removeAutoBid(auctionId, username);
        
        // 6. Phản hồi trạng thái (Protocol Response):
        // ClientFX nhận SUCCESS sẽ cập nhật giao diện Dashboard từ chế độ "ĐANG CHẠY" sang "ĐÃ HỦY".
        if (removed) {
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Đã hủy Auto Bid\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Không tìm thấy Auto Bid để hủy\"}");
        }
    }
}
