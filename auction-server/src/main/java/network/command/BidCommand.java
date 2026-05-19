// 1. Khai báo package: Nằm trong phân hệ command quản lý các lớp lệnh mạng của Server.
package network.command;

// 2. Import thư viện Gson xử lý gói tin dạng JsonObject.
import com.google.gson.JsonObject;
// 3. Import trình quản lý đấu giá AuctionManager (Tầng nghiệp vụ).
import services.AuctionManager;
// 4. Import thực thể Bidder đại diện cho người đặt thầu.
import models.Bidder;
// 5. Import ClientHandler đại diện cho session kết nối mạng.
import network.ClientHandler;
// 6. Import thư viện ghi dữ liệu mạng văn bản PrintWriter.
import java.io.PrintWriter;

/**
 * Lớp BidCommand chịu trách nhiệm xử lý yêu cầu đặt thầu thủ công (Manual Bidding) từ Client (Người mua).
 * Triển khai interface Command (Command Design Pattern).
 * Lớp này tách biệt hoàn toàn logic mạng (chuyển tiếp Socket) khỏi tầng logic nghiệp vụ cốt lõi (AuctionManager).
 */
public class BidCommand implements Command {
    
    /**
     * Thực thi nghiệp vụ đặt thầu thủ công.
     * Trích xuất thông tin phiên đấu giá (auctionId) và số tiền đặt thầu (amount) từ gói tin JSON.
     * Xác thực thông tin người mua (Bidder) từ session kết nối Socket hoặc fallback data.
     * Chuyển tiếp yêu cầu xuống hệ thống AuctionManager để thực hiện thuật toán thầu đồng thời (concurrency-safe).
     * 
     * @param bidData Dữ liệu JSON dạng gói tin gửi lên từ Client
     * @param out Luồng ghi dữ liệu mạng gửi phản hồi về client
     * @param handler Đối tượng quản lý session kết nối mạng của client này
     */
    @Override
    public void execute(JsonObject bidData, PrintWriter out, ClientHandler handler) {
        // 7. Lấy tên tài khoản người đặt thầu trực tiếp từ session đang lưu trong đối tượng ClientHandler.
        // Đây là kỹ thuật State Session Tracking (Theo dõi trạng thái phiên): Đảm bảo người dùng không thể giả mạo
        // danh tính tài khoản của người khác khi đặt giá thầu bằng cách trích xuất trực tiếp thông tin đã xác thực lúc login.
        String username = handler.getCurrentUsername();

        // Khởi tạo các giá trị mặc định cho gói thầu
        String auctionId = "";
        double amount = 0.0;

        // 8. Trích xuất dữ liệu từ cấu trúc gói tin thầu chuẩn:
        // Gói tin chuẩn gửi từ ClientFX: {"command":"BID","data":{"auctionId":"...","amount":...,"bidder":{"username":"..."}}}
        if (bidData.has("data")) {
            JsonObject dataObj = bidData.getAsJsonObject("data");
            auctionId = dataObj.has("auctionId") ? dataObj.get("auctionId").getAsString() : "";
            amount = dataObj.has("amount") ? dataObj.get("amount").getAsDouble() : 0.0;
            
            // 9. Phòng thủ từ xa (Fallback Authentication):
            // Nếu ClientHandler hiện tại chưa xác nhận đăng nhập (ví dụ: kết nối phụ kết nối ngầm),
            // thử lấy trực tiếp từ cấu trúc gói tin thầu 'bidder' do client khai báo.
            if (username == null && dataObj.has("bidder")) {
                JsonObject bidderObj = dataObj.getAsJsonObject("bidder");
                if (bidderObj.has("username")) {
                    username = bidderObj.get("username").getAsString();
                }
            }
        }

        // 10. Tương thích ngược (Backward Compatibility):
        // Nếu gói tin không có cấu trúc lồng phức tạp, thử lấy trực tiếp từ lớp ngoài cùng của Json.
        if (auctionId.isEmpty() && bidData.has("auctionId")) {
            auctionId = bidData.get("auctionId").getAsString();
        }
        if (amount == 0.0 && bidData.has("amount")) {
            amount = bidData.get("amount").getAsDouble();
        }

        // 11. Đúc (Instantiate) đối tượng Bidder đại diện cho người đặt thầu trên RAM Heap.
        Bidder bidder = new Bidder(username, "", "");
        
        // 12. Chuyển giao luồng xử lý xuống tầng nghiệp vụ AuctionManager:
        // placeBid() là phương thức đồng bộ hóa luồng (synchronized) giúp bảo vệ an toàn giao dịch,
        // ngăn chặn xung đột thầu nếu có nhiều người đặt thầu cùng một tích tắc.
        boolean success = AuctionManager.getInstance().placeBid(auctionId, bidder, amount);

        // 13. Phản hồi kết quả (Response Protocol):
        // Gửi kết quả đặt thầu lập tức về Socket Client dưới định dạng chuỗi JSON thô để Client giải mã hiển thị.
        if (success) {
            out.println("{\"status\":\"SUCCESS\", \"message\":\"Dat gia thanh cong\"}");
        } else {
            out.println("{\"status\":\"FAILED\", \"message\":\"Gia thap hon gia hien tai hoac phien da ket thuc\"}");
        }
    }
}
