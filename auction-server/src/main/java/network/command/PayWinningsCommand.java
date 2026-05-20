package network.command;

import com.google.gson.JsonObject;
import services.AuctionManager;
import network.ClientHandler;
import java.io.PrintWriter;

/**
 * Lớp PayWinningsCommand xử lý yêu cầu thanh toán tất cả các phiên đấu giá đã thắng của người dùng.
 */
public class PayWinningsCommand implements Command {

    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        String username = handler.getCurrentUsername();
        if (username == null && requestData.has("username")) {
            username = requestData.get("username").getAsString();
        }

        if (username == null || username.isEmpty()) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Username is required\"}");
            return;
        }

        boolean success = AuctionManager.getInstance().payWinnings(username);

        if (success) {
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Thanh toan thanh cong\"}");
            // Phát sóng cập nhật trạng thái mới nhất cho tất cả các Client
            AuctionManager.getInstance().notifyObservers("{\"command\":\"UPDATE_ITEMS\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Khong co san pham nao can thanh toan\"}");
        }
    }
}
