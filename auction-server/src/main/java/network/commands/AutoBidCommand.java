package network.commands;

import com.google.gson.JsonObject;
import network.ClientHandler;
import services.AuctionManager;
import models.Bidder;
import services.strategies.AutoBiddingStrategy;
import services.strategies.BiddingStrategy;

public class AutoBidCommand implements ServerCommand {
    @Override
    public void execute(ClientHandler handler, JsonObject req) {
        if (handler.getSessionUsername() == null) {
            handler.getOut().println("{\"status\":\"FAILED\",\"message\":\"Chưa đăng nhập\"}");
            return;
        }

        JsonObject data = req.has("data") ? req.getAsJsonObject("data") : req;
        double maxBid    = data.has("maxBid")    ? data.get("maxBid").getAsDouble()    : -1;
        double increment = data.has("increment") ? data.get("increment").getAsDouble() : -1;
        String itemId    = data.has("itemId")    ? data.get("itemId").getAsString()    : null;

        if (maxBid <= 0 || increment <= 0) {
            handler.getOut().println("{\"status\":\"FAILED\",\"message\":\"Thông số không hợp lệ\"}");
            return;
        }

        Bidder bidder = new Bidder(handler.getSessionUsername(), "", "");
        
        if (itemId == null || AuctionManager.getInstance().getAuction(itemId) == null) {
            handler.getOut().println("{\"status\":\"FAILED\",\"message\":\"Không tìm thấy phiên đấu giá\"}");
            return;
        }

        BiddingStrategy strategy = new AutoBiddingStrategy(maxBid, increment);
        AuctionManager.BidResult result = strategy.processBid(itemId, bidder, 0, itemId);

        if (result.success) {
            handler.getOut().println("{\"status\":\"SUCCESS\",\"newPrice\":" + result.newPrice + "}");
            String broadcast = "{\"command\":\"UPDATE_PRICE\",\"itemId\":\"" + itemId
                    + "\",\"price\":" + result.newPrice
                    + ",\"message\":\"[AUTO-BID] " + handler.getSessionUsername()
                    + " đã đăng ký đấu giá tự động\"}";
            ClientHandler.notifyAllObservers(broadcast);
        } else {
            handler.getOut().println("{\"status\":\"FAILED\",\"message\":\"" + result.message + "\"}");
        }
    }
}
