package network.command;

import com.google.gson.JsonObject;
import network.ClientHandler;
import services.AuctionManager;
import java.io.PrintWriter;

public class CancelAutoBidCommand implements Command {
    @Override
    public void execute(JsonObject requestJson, PrintWriter out, ClientHandler handler) {
        String auctionId = requestJson.has("auctionId") ? requestJson.get("auctionId").getAsString() : "";
        String username = requestJson.has("username") ? requestJson.get("username").getAsString() : "";

        if (auctionId.isEmpty() || username.isEmpty()) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Thiếu thông tin\"}");
            return;
        }

        boolean removed = AuctionManager.getInstance().removeAutoBid(auctionId, username);
        if (removed) {
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Đã hủy Auto Bid\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Không tìm thấy Auto Bid để hủy\"}");
        }
    }
}
