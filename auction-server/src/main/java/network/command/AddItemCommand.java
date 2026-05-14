package network.command;

import com.google.gson.JsonObject;
import models.Auction;
import models.Item;
import models.Seller;
import services.AuctionManager;
import services.ItemFactory;
import services.ItemManager;
import network.ClientHandler;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.UUID;

public class AddItemCommand implements Command {
    @Override
    public void execute(JsonObject requestData, PrintWriter out, ClientHandler handler) {
        try {
            JsonObject data = requestData.getAsJsonObject("data");
            String type = data.get("type").getAsString();
            String name = data.get("name").getAsString();
            String desc = data.get("desc").getAsString();
            double price = data.get("price").getAsDouble();
            String extra = data.has("extra") ? data.get("extra").getAsString() : "";
            
            // Lấy tên người bán từ session hoặc request
            String sellerName = (handler.getCurrentUsername() != null) ? 
                               handler.getCurrentUsername() : 
                               (data.has("seller") ? data.get("seller").getAsString() : "Unknown");
                               
            int durationValue = data.has("durationValue") ? data.get("durationValue").getAsInt() : (data.has("duration") ? data.get("duration").getAsInt() : 7);
            String durationUnit = data.has("durationUnit") ? data.get("durationUnit").getAsString() : "Ngày";

            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = startTime;

            switch (durationUnit) {
                case "Phút":
                    endTime = startTime.plusMinutes(durationValue);
                    break;
                case "Giờ":
                    endTime = startTime.plusHours(durationValue);
                    break;
                case "Ngày":
                default:
                    endTime = startTime.plusDays(durationValue);
                    break;
            }

            // Tạo sản phẩm từ Factory
            Item item = ItemFactory.createItem(type, name, desc, price, startTime, endTime, extra);
            item.setImageUrls(extra); // Gắn chuỗi ảnh từ client (trường extra chứa link ảnh từ AddProductController)
            String id = "ITEM-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            item.setId(id);

            Seller seller = new Seller(sellerName, "", "");
            
            // 1. Lưu vào Database
            ItemManager.getInstance().addItem(id, item, seller);

            // 2. Tạo phiên đấu giá trong Memory
            Auction auction = new Auction(item, seller);
            auction.setAuctionId(id);
            AuctionManager.getInstance().addAuction(auction);

            out.println("SUCCESS");
            System.out.println(">>> [SERVER] Đã thêm sản phẩm mới thành công: " + name + " (ID: " + id + ") bởi " + sellerName);

        } catch (Exception e) {
            System.err.println("!!! LỖI khi thêm sản phẩm: " + e.getMessage());
            e.printStackTrace();
            out.println("FAILED");
        }
    }
}
