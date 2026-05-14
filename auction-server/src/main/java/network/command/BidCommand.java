package network.command;

import com.google.gson.JsonObject;
import services.AuctionManager;
import models.Bidder;
import network.ClientHandler;
import java.io.PrintWriter;

public class BidCommand implements Command {
    @Override
    public void execute(JsonObject bidData, PrintWriter out, ClientHandler handler) {
        // Lấy username từ session (nếu cùng connection login)
        String username = handler.getCurrentUsername();

        // Client gửi dạng: {"command":"BID","data":{"auctionId":"...","amount":...,"bidder":{"username":"..."}}}
        String auctionId = "";
        double amount = 0.0;

        if (bidData.has("data")) {
            JsonObject dataObj = bidData.getAsJsonObject("data");
            auctionId = dataObj.has("auctionId") ? dataObj.get("auctionId").getAsString() : "";
            amount = dataObj.has("amount") ? dataObj.get("amount").getAsDouble() : 0.0;
            
            // Nếu handler không có username (do kết nối riêng từ background listener),
            // lấy username từ data.bidder.username
            if (username == null && dataObj.has("bidder")) {
                JsonObject bidderObj = dataObj.getAsJsonObject("bidder");
                if (bidderObj.has("username")) {
                    username = bidderObj.get("username").getAsString();
                }
            }
        }

        // Fallback nếu data không có auctionId, thử lấy trực tiếp từ bidData (tương thích ngược)
        if (auctionId.isEmpty() && bidData.has("auctionId")) {
            auctionId = bidData.get("auctionId").getAsString();
        }
        if (amount == 0.0 && bidData.has("amount")) {
            amount = bidData.get("amount").getAsDouble();
        }

        Bidder bidder = new Bidder(username, "", "");
        boolean success = AuctionManager.getInstance().placeBid(auctionId, bidder, amount);

        if (success) {
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Dat gia thanh cong\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Gia thap hon gia hien tai hoac phien da ket thuc\"}");
        }
    }
}
