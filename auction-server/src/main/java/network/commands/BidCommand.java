package network.commands;

import com.google.gson.JsonObject;
import network.ClientHandler;
import services.AuctionManager;
import models.Bidder;

public class BidCommand implements ServerCommand {
    @Override
    public void execute(ClientHandler handler, JsonObject req) {
        if (handler.getSessionUsername() == null) {
            handler.getOut().println("{\"status\":\"FAILED\",\"message\":\"Chưa đăng nhập\"}");
            return;
        }

        JsonObject data = req.has("data") ? req.getAsJsonObject("data") : req;
        double bidAmount = data.has("amount") ? data.get("amount").getAsDouble() : -1;
        if (bidAmount <= 0) {
            handler.getOut().println("{\"status\":\"FAILED\",\"message\":\"Giá không hợp lệ\"}");
            return;
        }

        Bidder bidder = new Bidder(handler.getSessionUsername(), "", "");
        String itemId = data.has("itemId") ? data.get("itemId").getAsString() : null;
        
        // Simple resolution: if itemId is an auction ID
        if (itemId == null || AuctionManager.getInstance().getAuction(itemId) == null) {
            handler.getOut().println("{\"status\":\"FAILED\",\"message\":\"Không tìm thấy phiên đấu giá\"}");
            return;
        }

        services.strategies.BiddingStrategy strategy = new services.strategies.ManualBiddingStrategy();
        AuctionManager.BidResult result = strategy.processBid(itemId, bidder, bidAmount, itemId);

        if (result.success) {
            handler.getOut().println("{\"status\":\"SUCCESS\",\"newPrice\":" + result.newPrice + "}");
            String broadcast = "{\"command\":\"UPDATE_PRICE\",\"itemId\":\"" + itemId
                    + "\",\"price\":" + result.newPrice
                    + ",\"winner\":\"" + handler.getSessionUsername()
                    + "\",\"message\":\"" + handler.getSessionUsername() + " đặt giá "
                    + String.format("%,.0f", result.newPrice) + " đ\"}";
            ClientHandler.notifyAllObservers(broadcast);
        } else {
            handler.getOut().println("{\"status\":\"FAILED\",\"message\":\"" + result.message + "\"}");
        }
    }
}
