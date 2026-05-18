package network.command;

import com.google.gson.JsonObject;
import services.AuctionManager;
import models.Bidder;
import network.ClientHandler;
import java.io.PrintWriter;

/**
 * Lớp BidCommand chịu trách nhiệm xử lý yêu cầu đặt thầu thủ công (Manual Bidding) từ Client (Người mua).
 * Triển khai interface Command.
 */
public class BidCommand implements Command {
    
    /**
     * Thực thi nghiệp vụ đặt thầu thủ công.
     * Trích xuất thông tin phiên đấu giá (auctionId) và số tiền đặt thầu (amount) từ JSON.
     * Xác thực thông tin người mua (Bidder) từ session kết nối Socket hoặc fallback data.
     * Chuyển tiếp yêu cầu xuống hệ thống AuctionManager để thực hiện thuật toán an toàn thầu.
     */
    @Override
    public void execute(JsonObject bidData, PrintWriter out, ClientHandler handler) {
        // Lấy tên tài khoản người đặt thầu trực tiếp từ session đang lưu trong ClientHandler
        String username = handler.getCurrentUsername();

        // Gói tin thầu chuẩn: {"command":"BID","data":{"auctionId":"...","amount":...,"bidder":{"username":"..."}}}
        String auctionId = "";
        double amount = 0.0;

        if (bidData.has("data")) {
            JsonObject dataObj = bidData.getAsJsonObject("data");
            auctionId = dataObj.has("auctionId") ? dataObj.get("auctionId").getAsString() : "";
            amount = dataObj.has("amount") ? dataObj.get("amount").getAsDouble() : 0.0;
            
            // Nếu ClientHandler hiện tại chưa xác nhận đăng nhập (ví dụ: kết nối phụ kết nối ngầm),
            // thử lấy trực tiếp từ cấu trúc gói tin thầu 'bidder' gửi lên
            if (username == null && dataObj.has("bidder")) {
                JsonObject bidderObj = dataObj.getAsJsonObject("bidder");
                if (bidderObj.has("username")) {
                    username = bidderObj.get("username").getAsString();
                }
            }
        }

        // Tương thích ngược: Fallback nếu data không có cấu trúc lồng, thử lấy trực tiếp ở lớp ngoài cùng
        if (auctionId.isEmpty() && bidData.has("auctionId")) {
            auctionId = bidData.get("auctionId").getAsString();
        }
        if (amount == 0.0 && bidData.has("amount")) {
            amount = bidData.get("amount").getAsDouble();
        }

        // Khởi tạo đối tượng Bidder đại diện cho người đặt thầu
        Bidder bidder = new Bidder(username, "", "");
        
        // Gọi AuctionManager xử lý đặt giá thầu thời gian thực
        boolean success = AuctionManager.getInstance().placeBid(auctionId, bidder, amount);

        // Gửi kết quả đặt thầu lập tức về Socket Client
        if (success) {
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Dat gia thanh cong\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Gia thap hon gia hien tai hoac phien da ket thuc\"}");
        }
    }
}
