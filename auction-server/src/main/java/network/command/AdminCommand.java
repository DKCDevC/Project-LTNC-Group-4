package network.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import usecase.admin.UserManagementUseCase;
import utils.GsonConfig;
import network.ClientHandler;
import java.io.PrintWriter;

/**
 * Lớp AdminCommand đóng gói toàn bộ các tác vụ điều khiển hành chính của Quản trị viên (Admin).
 * Triển khai interface Command.
 * Phân phối xử lý thông qua hệ thống lệnh con (Sub-commands):
 * - GET_USERS: Xem danh sách người dùng.
 * - LOCK_USER: Khóa tài khoản người dùng vi phạm.
 * - UNLOCK_USER: Mở khóa tài khoản.
 * - VERIFY_SELLER: Phê duyệt tài khoản Người bán.
 * - GET_AUCTIONS: Xem toàn bộ các phiên thầu.
 * - CANCEL_AUCTION: Admin cưỡng chế hủy phiên đấu giá.
 */
public class AdminCommand implements Command {
    // Đối tượng Gson dùng chung được cấu hình đồng bộ ngày tháng
    private final Gson gson = GsonConfig.createGson();

    /**
     * Thực thi các lệnh con của Admin.
     * Chuyển tiếp yêu cầu nghiệp vụ hành chính xuống UserManagementUseCase.
     */
    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        // Trích xuất lệnh con (sub-command) cần thực thi
        String subCommand = requestData.has("subCommand") ? requestData.get("subCommand").getAsString() : "";
        
        switch (subCommand) {
            case "GET_USERS":
                // Lấy toàn bộ danh sách người dùng và chuyển đổi thành JSON để gửi về cho Admin Dashboard Table
                out.println(gson.toJson(UserManagementUseCase.getInstance().getUsers()));
                break;
                
            case "LOCK_USER":
                // Khóa tài khoản người dùng chỉ định
                String userToLock = requestData.get("targetUser").getAsString();
                boolean lockSuccess = UserManagementUseCase.getInstance().lockUser(userToLock);
                out.println(lockSuccess ? "SUCCESS" : "FAILED");
                break;
                
            case "UNLOCK_USER":
                // Mở khóa tài khoản người dùng chỉ định
                String userToUnlock = requestData.get("targetUser").getAsString();
                boolean unlockSuccess = UserManagementUseCase.getInstance().unlockUser(userToUnlock);
                out.println(unlockSuccess ? "SUCCESS" : "FAILED");
                break;

            case "VERIFY_SELLER":
                // Duyệt xác thực đối tác Người bán để họ có quyền đăng sản phẩm đấu giá
                String sellerToVerify = requestData.get("targetUser").getAsString();
                boolean verifySuccess = UserManagementUseCase.getInstance().verifySeller(sellerToVerify);
                out.println(verifySuccess ? "SUCCESS" : "FAILED");
                break;
                
            case "GET_AUCTIONS":
                // Lấy toàn bộ danh sách phiên đấu giá hiện hành gửi về bảng quản lý của Admin
                out.println(gson.toJson(UserManagementUseCase.getInstance().getAuctions()));
                break;
                
            case "CANCEL_AUCTION":
                // Cưỡng chế hủy bỏ một phiên đấu giá do vi phạm quy chế hoặc sản phẩm cấm
                String auctionToCancel = requestData.get("targetId").getAsString();
                boolean cancelSuccess = UserManagementUseCase.getInstance().removeAuction(auctionToCancel);
                out.println(cancelSuccess ? "SUCCESS" : "FAILED");
                break;

            default:
                // Trường hợp nhận được lệnh con không được hỗ trợ
                out.println("ERROR: Unknown Admin sub-command");
                break;
        }
    }
}
