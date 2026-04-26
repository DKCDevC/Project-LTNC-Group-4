package network.command;

import com.google.gson.JsonObject;
import services.AuctionManager;
import models.Bidder;
import network.ClientHandler;
import java.io.PrintWriter;

public class BidCommand implements Command {
    @Override
    public void execute(JsonObject bidData, PrintWriter out, ClientHandler handler) {
        // Sử dụng username từ session nếu có, nếu không lấy từ request (để tương thích ngược)
        String username = (handler.getCurrentUsername() != null) ? 
                          handler.getCurrentUsername() : 
                          (bidData.has("username") ? bidData.get("username").getAsString() : "Unknown");
        
        String auctionId = bidData.has("auctionId") ? bidData.get("auctionId").getAsString() : "";
        double amount = bidData.has("amount") ? bidData.get("amount").getAsDouble() : 0.0;

        Bidder bidder = new Bidder(username, "", "");
        boolean success = AuctionManager.getInstance().placeBid(auctionId, bidder, amount);

        if (success) {
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Dat gia thanh cong\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Gia thap hon gia hien tai hoac phien da ket thuc\"}");
        }
    }
}
