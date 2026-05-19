// 1. Khai báo package: Thuộc phân hệ điều phối lệnh mạng của Server.
package network.command;

// 2. Import GSON và JsonObject của Google để phục vụ tuần tự hóa, phân tích cú pháp gói tin JSON.
import com.google.gson.Gson;
import com.google.gson.JsonObject;
// 3. Import UserManagementUseCase: Tầng trường hợp sử dụng (Use Case Layer) đóng gói logic nghiệp vụ của Admin.
// Đây là kỹ thuật thiết kế kiến trúc sạch (Clean Architecture): Lớp mạng (Command) 
// không chứa trực tiếp logic nghiệp vụ mà chỉ đóng vai trò phân phối tham số xuống tầng Use Case xử lý.
import usecase.admin.UserManagementUseCase;
import utils.GsonConfig;
import network.ClientHandler;
import java.io.PrintWriter;

/**
 * Lớp AdminCommand đóng gói toàn bộ các tác vụ điều khiển hành chính của Quản trị viên (Admin).
 * Triển khai interface Command (Command Design Pattern).
 * Phân phối xử lý thông qua hệ thống lệnh con (Sub-commands Dispatcher Pattern) để tránh việc
 * phải tạo quá nhiều file Command nhỏ lẻ không cần thiết cho cùng một vai trò Admin.
 * 
 * Các lệnh con bao gồm:
 * - GET_USERS: Xem danh sách người dùng.
 * - LOCK_USER: Khóa tài khoản người dùng vi phạm.
 * - UNLOCK_USER: Mở khóa tài khoản.
 * - VERIFY_SELLER: Phê duyệt tài khoản Người bán.
 * - GET_AUCTIONS: Xem toàn bộ các phiên thầu.
 * - CANCEL_AUCTION: Admin cưỡng chế hủy phiên đấu giá.
 */
public class AdminCommand implements Command {
    // 4. Đối tượng Gson dùng chung được cấu hình đồng bộ định dạng ngày tháng (LocalDateTime).
    // Từ khóa final giúp chống sửa đổi địa chỉ tham chiếu ô nhớ sau khi đối tượng được đúc trên Heap.
    private final Gson gson = GsonConfig.createGson();

    /**
     * Thực thi các lệnh con của Admin.
     * Chuyển tiếp yêu cầu nghiệp vụ hành chính xuống UserManagementUseCase.
     * 
     * @param requestData Gói dữ liệu JSON gửi lên từ Client của Admin
     * @param out Luồng ghi dữ liệu mạng (Socket Output Stream) gửi phản hồi về client
     * @param handler Session quản lý socket tương ứng
     */
    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        // 5. Trích xuất lệnh con (sub-command) từ JSON. 
        // Toán tử ba ngôi (Ternary Operator) được dùng ở đây nhằm kiểm duyệt phòng thủ dữ liệu:
        // Nếu không tồn tại khóa "subCommand", gán giá trị mặc định là chuỗi rỗng để tránh lỗi NullPointerException (NPE).
        String subCommand = requestData.has("subCommand") ? requestData.get("subCommand").getAsString() : "";
        
        // 6. Cấu trúc Switch-Case Control Flow: Điều phối và phân mảnh luồng chạy tối ưu hơn chuỗi `if-else` lồng nhau.
        switch (subCommand) {
            case "GET_USERS":
                // 7. Lấy danh sách người dùng từ Use Case, tuần tự hóa thành chuỗi JSON thô và ghi thẳng ra luồng Socket mạng.
                out.println(gson.toJson(UserManagementUseCase.getInstance().getUsers()));
                break;
                
            case "LOCK_USER":
                // 8. Khóa tài khoản người dùng chỉ định. Trích xuất tham số username mục tiêu ("targetUser").
                String userToLock = requestData.get("targetUser").getAsString();
                boolean lockSuccess = UserManagementUseCase.getInstance().lockUser(userToLock);
                // Trả về kết quả thô để client cập nhật giao diện (xác nhận trạng thái)
                out.println(lockSuccess ? "SUCCESS" : "FAILED");
                break;
                
            case "UNLOCK_USER":
                // 9. Mở khóa tài khoản người dùng chỉ định.
                String userToUnlock = requestData.get("targetUser").getAsString();
                boolean unlockSuccess = UserManagementUseCase.getInstance().unlockUser(userToUnlock);
                out.println(unlockSuccess ? "SUCCESS" : "FAILED");
                break;
 
            case "VERIFY_SELLER":
                // 10. Duyệt xác thực đối tác Người bán để nâng cấp vai trò hợp lệ.
                String sellerToVerify = requestData.get("targetUser").getAsString();
                boolean verifySuccess = UserManagementUseCase.getInstance().verifySeller(sellerToVerify);
                out.println(verifySuccess ? "SUCCESS" : "FAILED");
                break;
                
            case "GET_AUCTIONS":
                // 11. Lấy toàn bộ danh sách phiên đấu giá hiện hành gửi về bảng quản lý của Admin.
                out.println(gson.toJson(UserManagementUseCase.getInstance().getAuctions()));
                break;
                
            case "CANCEL_AUCTION":
                // 12. Cưỡng chế hủy bỏ một phiên đấu giá do vi phạm quy chế hoặc sản phẩm cấm.
                String auctionToCancel = requestData.get("targetId").getAsString();
                boolean cancelSuccess = UserManagementUseCase.getInstance().removeAuction(auctionToCancel);
                out.println(cancelSuccess ? "SUCCESS" : "FAILED");
                break;
 
            default:
                // 13. Trường hợp nhận được lệnh con không nằm trong danh mục hỗ trợ.
                // In ra thông báo lỗi giao thức chuẩn.
                out.println("ERROR: Unknown Admin sub-command");
                break;
        }
    }
}
