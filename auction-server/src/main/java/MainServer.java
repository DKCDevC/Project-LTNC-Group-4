// 1. Import các thực thể (Models) dùng chung để mô tả phiên đấu giá, người dùng
import models.Auction;
import models.Electronics;
import models.Item;
import models.Seller;
// 2. Import các cấu phần mạng và điều phối của Server
import network.AuctionSocketServer;
import services.AuctionManager;
// 3. Import kết nối Database SQLite
import utils.DBConnection; 

import java.time.LocalDateTime;

/**
 * Lớp MainServer đóng vai trò là Điểm khởi chạy (Entry Point) của toàn bộ hệ thống Đấu giá Máy chủ.
 * Chứa hàm main() để hệ điều hành chạy luồng chính, nạp Database, tạo dữ liệu mồi,
 * khởi động Timer quét thời gian và mở cổng Socket TCP 9999 lắng nghe Client.
 */
public class MainServer {
    public static void main(String[] args) {
        // 4. In ra màn hình console thông báo khởi động hệ thống.
        System.out.println("=== KHỞI ĐỘNG HỆ THỐNG ĐẤU GIÁ ===");

        // --- KHỞI TẠO CƠ SỞ DỮ LIỆU ---
        System.out.println(">>> Đang kết nối và kiểm tra Database...");
        // 5. Gọi hàm getConnection() của DBConnection. 
        // Lệnh này đảm bảo file SQLite `auction_system.db` được tạo ra, cấu trúc 3 bảng (users, items, orders) 
        // được xây dựng, và 4 tài khoản mặc định được nạp đầy đủ ngay khi Server bật lên.
        DBConnection.getConnection();
        // -------------------------------------------------------

        // --- TẠO DỮ LIỆU NGƯỜI DÙNG MẪU ---
        // 6. Khởi tạo một thực thể Seller (Người bán) "user1" trên bộ nhớ Heap.
        // Đối tượng này sẽ đại diện cho người bán sở hữu 20 sản phẩm mẫu được tạo dưới đây.
        Seller user1 = new Seller("user1", "pass123", "user1@test.com");

        // --- TẠO DANH SÁCH 20 SẢN PHẨM & PHIÊN ĐẤU GIÁ MẪU ---
        // 7. Vòng lặp for chạy từ 1 đến 20 để tự động hóa việc phát sinh dữ liệu mồi phục vụ kiểm thử.
        for (int i = 1; i <= 20; i++) {
            // 8. Định nghĩa mã định danh ID và Tên riêng biệt cho từng sản phẩm dựa trên biến chạy `i`.
            String itemId = "item_user1_" + i;
            String itemName = "Sản phẩm mẫu " + i + " của user1";
            
            // 9. Khởi tạo đối tượng Item (sử dụng lớp con Electronics - Hàng điện tử).
            // Đa hình (Polymorphism) / Upcasting: Biến kiểu Item trỏ tới thực thể Electronics.
            // Truyền các thông số: Tên, Mô tả, Giá khởi điểm (tăng tiến theo i),
            // Ngày bắt đầu (ngay bây giờ), Ngày kết thúc (bây giờ + 2 ngày), Thời gian bảo hành (12 tháng).
            Item sampleItem = new Electronics(itemName, "Mô tả cho sản phẩm " + i, 1000000 + (i * 100000),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(2), 12);
            
            // 10. Gán mã ID duy nhất cho sản phẩm
            sampleItem.setId(itemId);

            // --- KIỂM TRA TRÁNH LẶP DỮ LIỆU (ANTI-DUPLICATION) ---
            // 11. Dùng Stream API để kiểm tra xem trong kho bộ nhớ tĩnh của ItemManager đã tồn tại sản phẩm trùng ID này chưa.
            // stream().noneMatch(...) duyệt mảng cực nhanh, trả về true nếu không có phần tử nào trùng ID.
            // Giúp ngăn ngừa lỗi khóa chính duy nhất (UNIQUE constraint/Duplicate Key) trong SQLite khi khởi động server nhiều lần.
            if (services.ItemManager.getInstance().getAllItems().stream().noneMatch(item -> item.getId().equals(sampleItem.getId()))) {
                // 12. Nếu chưa có, nạp sản phẩm vào ItemManager và lưu vết người bán (user1) sở hữu nó.
                services.ItemManager.getInstance().addItem(sampleItem.getId(), sampleItem, user1);
                System.out.println(">>> Đã lưu sản phẩm mẫu mới vào Database: " + itemName);
            } else {
                System.out.println(">>> Sản phẩm mẫu đã tồn tại trong Database, bỏ qua bước lưu: " + itemName);
            }

            // --- TẠO PHIÊN ĐẤU GIÁ TƯƠNG ỨNG ---
            // 13. Đúc một đối tượng Đấu trường (Auction), liên kết sản phẩm mẫu với người bán.
            Auction sampleAuction = new Auction(sampleItem, user1);
            sampleAuction.setAuctionId(itemId); // Đặt ID phiên trùng ID sản phẩm để đồng bộ hóa tuyệt đối.
            
            // 14. Đăng ký phiên đấu giá mẫu này vào Trình quản lý AuctionManager.
            // AuctionManager sẽ giám sát trạng thái, giá thầu hiện tại, danh sách robot cắm chốt của phòng này.
            AuctionManager.getInstance().addAuction(sampleAuction);
        }
        
        System.out.println("=== ĐẠO TẠO VÀ LƯU 20 PHIÊN ĐẤU GIÁ MẪU CHO user1 ===");

        // --- BẮT ĐẦU VÒNG LẶP KIỂM TRA THỜI GIAN ĐẤU GIÁ ---
        // 15. Kích hoạt bộ định thì thời gian (Timer) chạy ngầm của AuctionManager.
        // Timer này sử dụng một luồng ngầm định kỳ (Background Thread) quét liên tục mỗi giây để xem các phiên đấu giá
        // nào đã quá giờ `end_time` chưa. Nếu rồi, tự động đóng phiên, ghi nhận người chiến thắng, tạo đơn hàng và thông báo về client.
        AuctionManager.getInstance().startAuctionTimer();

        // --- KHỞI ĐỘNG MÁY CHỦ MẠNG (SOCKET SERVER) ---
        // 16. Thiết lập cổng mạng lắng nghe mặc định là 9999.
        int port = 9999;
        System.out.println(">>> Đang mở cổng mạng " + port + "...");
        AuctionSocketServer server = new AuctionSocketServer(port);
        
        // 17. Khởi chạy Server Socket:
        // Vòng lặp vô tận ServerSocket.accept() chặn luồng chính (blocking) để luôn trong trạng thái chờ các Client mới bắt tay (handshake).
        server.startServer();
    }
}