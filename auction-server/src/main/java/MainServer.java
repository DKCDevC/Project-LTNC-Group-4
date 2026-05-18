import models.Auction;
import models.Electronics;
import models.Item;
import models.Seller;
import network.AuctionSocketServer;
import services.AuctionManager;
import utils.DBConnection; // --- ĐÃ THÊM: Import file kết nối DB ---

import java.time.LocalDateTime;

public class MainServer {
    public static void main(String[] args) {
        // In ra màn hình console thông báo hệ thống bắt đầu khởi động
        System.out.println("=== KHỞI ĐỘNG HỆ THỐNG ĐẤU GIÁ ===");

        // --- KHỞI TẠO CƠ SỞ DỮ LIỆU ---
        // Gọi đến DBConnection để lấy đối tượng kết nối (Connection).
        // Nếu Database chưa tồn tại hoặc chưa có bảng, phương thức này có thể được cấu hình
        // để tự động tạo file database SQLite và tạo các bảng cần thiết (Users, Items, Auctions...).
        System.out.println(">>> Đang kết nối và kiểm tra Database...");
        DBConnection.getConnection();
        // -------------------------------------------------------

        // --- TẠO DỮ LIỆU NGƯỜI DÙNG MẪU ---
        // Khởi tạo một đối tượng Seller (Người bán) với username là "user1", password là "pass123".
        // Đối tượng này sẽ đại diện cho người dùng đăng bán các sản phẩm mẫu dưới đây.
        Seller user1 = new Seller("user1", "pass123", "user1@test.com");

        // --- TẠO DANH SÁCH 20 SẢN PHẨM & PHIÊN ĐẤU GIÁ MẪU ---
        // Vòng lặp này giúp tạo nhanh 20 sản phẩm để giả lập dữ liệu cho hệ thống
        for (int i = 1; i <= 20; i++) {
            // 1. Khởi tạo mã ID và tên riêng biệt cho từng sản phẩm
            String itemId = "item_user1_" + i;
            String itemName = "Sản phẩm mẫu " + i + " của user1";
            
            // 2. Tạo đối tượng Item (ở đây dùng lớp con Electronics - Hàng điện tử)
            // Cài đặt tên, mô tả, giá khởi điểm (tăng dần theo i), thời gian bắt đầu (hiện tại)
            // và thời gian kết thúc (hiện tại + 2 ngày), thời gian bảo hành (12 tháng).
            Item sampleItem = new Electronics(itemName, "Mô tả cho sản phẩm " + i, 1000000 + (i * 100000),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(2), 12);
            // 3. Gán mã ID cho sản phẩm vừa tạo
            sampleItem.setId(itemId);

            // 4. KIỂM TRA VÀ LƯU SẢN PHẨM VÀO DATABASE
            // Kiểm tra trong danh sách tất cả sản phẩm (từ ItemManager) xem ID của sản phẩm này đã tồn tại chưa.
            // Việc này nhằm tránh lỗi trùng lặp khóa chính (UNIQUE constraint) khi chạy server nhiều lần.
            if (services.ItemManager.getInstance().getAllItems().stream().noneMatch(item -> item.getId().equals(sampleItem.getId()))) {
                services.ItemManager.getInstance().addItem(sampleItem.getId(), sampleItem, user1);
                System.out.println(">>> Đã lưu sản phẩm mẫu mới vào Database: " + itemName);
            } else {
                System.out.println(">>> Sản phẩm mẫu đã tồn tại trong Database, bỏ qua bước lưu: " + itemName);
            }

            // 5. TẠO PHIÊN ĐẤU GIÁ CHO SẢN PHẨM
            // Khởi tạo một phiên đấu giá (Auction) liên kết với sản phẩm và người bán (user1)
            Auction sampleAuction = new Auction(sampleItem, user1);
            sampleAuction.setAuctionId(itemId); // Đặt ID phiên đấu giá giống với ID sản phẩm để dễ quản lý
            
            // 6. Thêm phiên đấu giá này vào Trình quản lý (AuctionManager)
            // AuctionManager sẽ chịu trách nhiệm theo dõi giá, người đấu giá và trạng thái của phiên này.
            AuctionManager.getInstance().addAuction(sampleAuction);
        }
        
        System.out.println("=== ĐÃ TẠO VÀ LƯU 20 PHIÊN ĐẤU GIÁ MẪU CHO user1 ===");

        // --- BẮT ĐẦU VÒNG LẶP KIỂM TRA THỜI GIAN ĐẤU GIÁ ---
        // Timer này sẽ chạy ngầm định kỳ (thường là mỗi giây), quét qua tất cả các phiên đấu giá
        // để xem phiên nào đã đến giờ kết thúc chưa, từ đó đóng phiên và xác định người chiến thắng.
        AuctionManager.getInstance().startAuctionTimer();

        // --- KHỞI ĐỘNG MÁY CHỦ MẠNG (SOCKET SERVER) ---
        // Thiết lập cổng (port) 9999 để lắng nghe các kết nối TCP từ phía Client (người dùng).
        int port = 9999;
        AuctionSocketServer server = new AuctionSocketServer(port);
        // startServer() thường là một vòng lặp vô tận dùng ServerSocket.accept() để nhận kết nối mới
        server.startServer();
    }
}