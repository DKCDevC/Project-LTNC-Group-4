// 1. Khai báo package: Nằm trong phân hệ command quản lý các lớp lệnh mạng của Server.
package network.command;

// 2. Import thư viện Gson xử lý JSON và trình quản lý nghiệp vụ ItemManager
import com.google.gson.JsonObject;
import services.ItemManager;
import network.ClientHandler;
import java.io.PrintWriter;

/**
 * Lớp DeleteItemCommand xử lý yêu cầu xóa sản phẩm đấu giá được gửi từ Client (Người bán).
 * Triển khai interface Command (Command Design Pattern).
 * Lớp này chịu trách nhiệm kiểm duyệt session mạng của người dùng yêu cầu,
 * ủy quyền kiểm duyệt bảo mật xuống ItemManager và trả kết quả về Socket.
 */
public class DeleteItemCommand implements Command {
    
    /**
     * Thực thi nghiệp vụ xóa sản phẩm.
     * Kiểm tra phiên đăng nhập hiện tại, sau đó gọi xuống ItemManager kiểm duyệt quyền sở hữu 
     * và trạng thái thầu trước khi chính thức xóa khỏi SQLite Database.
     * 
     * @param requestData Gói dữ liệu yêu cầu xóa gửi từ client dạng JSON
     * @param out Luồng ghi dữ liệu mạng gửi phản hồi về client
     * @param handler Session quản lý socket tương ứng
     */
    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        // 3. Trích xuất mã ID sản phẩm cần xóa từ gói tin yêu cầu:
        // Sử dụng toán tử ba ngôi để phòng thủ tránh lỗi null-reference nếu trường "productId" bị thiếu.
        String productId = requestData.has("productId") ? requestData.get("productId").getAsString() : "";
        
        // Trích xuất username trực tiếp từ session ClientHandler được lưu trữ trên RAM
        String username = handler.getCurrentUsername();

        // 4. Kiểm tra xác thực phiên đăng nhập (Authentication Audit):
        // Nếu người dùng chưa đăng nhập hệ thống nhưng cố gửi gói tin thô để xóa sản phẩm,
        // Server chặn đứng tức khắc và báo lỗi "Unauthorized" để bảo vệ an ninh hệ thống.
        if (username == null) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Unauthorized: Please login first\"}");
            return;
        }

        // 5. Ủy nhiệm nghiệp vụ xuống ItemManager (De-coupled Business Rules):
        // Phương thức deleteItem() sẽ chịu trách nhiệm kiểm duyệt bảo mật kép (Double-Security Check):
        // - Người dùng yêu cầu xóa có thực sự là chủ sở hữu (Seller) của sản phẩm này không.
        // - Phiên đấu giá của sản phẩm này đã bắt đầu có người đặt thầu chưa. (Không cho phép xóa nếu đang đấu giá).
        boolean success = ItemManager.getInstance().deleteItem(productId, username);

        // 6. Phản hồi kết quả xóa về phía Client để làm mới giao diện TableView trên Dashboard:
        if (success) {
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Da xoa san pham\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Khong the xoa: Sai quyen so huu hoac dau gia dang dien ra\"}");
        }
    }
}
