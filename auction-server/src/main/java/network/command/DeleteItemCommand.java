package network.command;

import com.google.gson.JsonObject;
import services.ItemManager;
import network.ClientHandler;
import java.io.PrintWriter;

/**
 * Lớp DeleteItemCommand xử lý yêu cầu xóa sản phẩm đấu giá được gửi từ Client (Người bán).
 * Triển khai interface Command.
 */
public class DeleteItemCommand implements Command {
    
    /**
     * Thực thi nghiệp vụ xóa sản phẩm.
     * Kiểm tra phiên đăng nhập hiện tại, sau đó gọi xuống ItemManager kiểm duyệt quyền sở hữu 
     * và trạng thái thầu trước khi chính thức xóa khỏi SQLite Database.
     */
    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        // Trích xuất mã ID sản phẩm cần xóa từ gói tin yêu cầu
        String productId = requestData.has("productId") ? requestData.get("productId").getAsString() : "";
        String username = handler.getCurrentUsername();

        // 1. Kiểm tra xác thực phiên đăng nhập của Client kết nối
        if (username == null) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Unauthorized: Please login first\"}");
            return;
        }

        // 2. Gọi ItemManager thực hiện xóa sản phẩm (đã bọc logic nghiệp vụ kiểm tra quyền + an toàn trạng thái)
        boolean success = ItemManager.getInstance().deleteItem(productId, username);

        // Phản hồi kết quả xóa về phía Client để làm mới giao diện TableView
        if (success) {
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Da xoa san pham\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Khong the xoa: Sai quyen so huu hoac dau gia dang dien ra\"}");
        }
    }
}
