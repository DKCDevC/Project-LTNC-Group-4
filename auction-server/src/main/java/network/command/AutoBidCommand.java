package network.command;

import com.google.gson.JsonObject;
import models.Auction;
import models.AutoBid;
import models.Bidder;
import network.ClientHandler;
import services.AuctionManager;
import java.io.PrintWriter;

public class AutoBidCommand implements Command {
    private static final double MIN_INCREMENT = 10000.0;

    @Override
    public void execute(JsonObject requestJson, PrintWriter out, ClientHandler handler) {
        if (!requestJson.has("data")) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Thiếu dữ liệu\"}");
            return;
        }

        JsonObject data = requestJson.getAsJsonObject("data");
        String auctionId = data.get("auctionId").getAsString();
        String username = data.get("username").getAsString();
        double maxBid = data.get("maxBid").getAsDouble();
        double increment = data.get("increment").getAsDouble();

        if (increment < MIN_INCREMENT) {
            out.println("{\"status\":\"FAILED\", \"message\":\"Bước giá tối thiểu phải là " + String.format("%,.0f", MIN_INCREMENT) + " ₫\"}");
            return;
        }

        Auction auction = AuctionManager.getInstance().getAuction(auctionId);
        if (auction != null) {
            Bidder bidder = new Bidder(username, "", "");
            AutoBid autoBid = new AutoBid(bidder, maxBid, increment);
            
            // Hủy auto bid cũ nếu có
            AuctionManager.getInstance().removeAutoBid(auctionId, username);
            
            // Đăng ký auto bid mới
            auction.registerAutoBid(autoBid);
            
            // Kích hoạt ngay lập tức nếu cần
            AuctionManager.getInstance().checkAndTriggerAutoBids(auctionId);
            
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Đã kích hoạt Auto Bid\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Không tìm thấy phiên đấu giá\"}");
        }
    }
}
