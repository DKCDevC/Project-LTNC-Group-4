// 1. Khai báo package: Nằm trong phân hệ dịch vụ nghiệp vụ (Services) của Server.
package services;

// 2. Import các mô hình và thư viện JUnit 5 chuyên dùng để kiểm thử đơn vị
import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp AuctionManagerTest thực hiện kiểm thử tự động (Unit Test) cho động cơ thầu AuctionManager.
 * Sử dụng thư viện JUnit 5 (JUnit Jupiter) để xác minh tính chính xác của các quy tắc đấu giá:
 * - Quy tắc thầu hợp lệ (giá nâng cao hơn).
 * - Quy tắc chặn thầu không hợp lệ (giá thấp hơn giá hiện hành).
 * - Quy tắc tự động gia hạn chống bắn tỉa giờ chót (Anti-sniping protocol verification).
 */
public class AuctionManagerTest {
    // Khai báo các đối tượng giả lập môi trường kiểm thử
    private AuctionManager manager;
    private Auction auction;
    private Bidder bidder1;
    private Bidder bidder2;

    /**
     * Phương thức thiết lập ban đầu (Setup Phase).
     * Được chú thích bằng `@BeforeEach` - JUnit sẽ tự động gọi phương thức này TRƯỚC MỖI phương thức kiểm thử `@Test`
     * để làm sạch môi trường, đảm bảo các ca kiểm thử độc lập hoàn toàn với nhau trên bộ nhớ (không bị nhiễm dữ liệu cũ).
     */
    @BeforeEach
    void setUp() {
        // Lấy thể hiện duy nhất của AuctionManager
        manager = AuctionManager.getInstance();

        // Tạo tài khoản người bán mẫu và sản phẩm mẫu (Điện tử: bảo hành 12 tháng)
        Seller seller = new Seller("seller", "123", "s@test.com");
        Item laptop = new Electronics("Laptop", "Test", 1000,
                LocalDateTime.now(), LocalDateTime.now().plusSeconds(10), 12);

        // Khởi dựng phiên đấu giá mẫu và nạp vào trình quản lý thầu của Server
        auction = new Auction(laptop, seller);
        auction.setAuctionId("test_001");
        manager.addAuction(auction);

        // Khởi tạo 2 tài khoản người mua giả lập tranh thầu
        bidder1 = new Bidder("user1", "123", "u1@test.com");
        bidder2 = new Bidder("user2", "123", "u2@test.com");
    }

    /**
     * Ca kiểm thử 1: Xác minh tính hợp lệ của lệnh thầu có giá trị lớn hơn giá khởi điểm.
     * Kiểm tra hai mặt:
     * - Trả về `true` (chấp thuận thầu).
     * - Giá trị cao nhất của sản phẩm trong RAM được cập nhật khớp chính xác giá vừa thầu.
     */
    @Test
    void testValidBid() {
        // Nâng giá thầu lên 2000 (Lớn hơn giá khởi điểm 1000)
        boolean result = manager.placeBid("test_001", bidder1, 2000);
        
        // Sử dụng Assertion kiểm tra kết quả trả về phải là true
        assertTrue(result, "Giá 2000 phải lớn hơn giá khởi điểm 1000");
        // Kiểm tra giá hiện hành được đồng bộ đúng giá thầu mới
        assertEquals(2000, auction.getItem().getCurrentHighestPrice());
    }

    /**
     * Ca kiểm thử 2: Ngăn chặn thầu không hợp lệ.
     * Đảm bảo hệ thống từ chối (`false`) nếu người thứ hai thầu giá thấp hơn mức thầu hiện tại của người thứ nhất.
     */
    @Test
    void testInvalidLowerBid() {
        // Người thứ nhất đặt 2000 thành công
        manager.placeBid("test_001", bidder1, 2000);
        
        // Người thứ hai cố tình thầu 1500 (Thấp hơn mức 2000)
        boolean result = manager.placeBid("test_001", bidder2, 1500);
        
        // Xác minh kết quả trả về bắt buộc phải là false
        assertFalse(result, "Không được đặt giá 1500 khi giá hiện tại là 2000");
    }

    /**
     * Ca kiểm thử 3: Kiểm tra cơ chế tự động gia hạn thời gian kết thúc (Anti-Sniping Protocol).
     * Khi có người nâng giá sát giờ chót (ở đây phiên thầu chỉ có thời lượng 10 giây từ lúc tạo),
     * thời gian kết thúc thầu mới bắt buộc phải được gia hạn dịch chuyển về sau so với thời gian cũ.
     */
    @Test
    void testAntiSnipingExtension() {
        // Lưu lại mốc thời gian kết thúc thầu cũ
        LocalDateTime oldEndTime = auction.getItem().getEndTime();

        // Thực hiện đặt thầu nâng giá thầu
        manager.placeBid("test_001", bidder1, 3000);

        // Lấy mốc thời gian sau khi thầu
        LocalDateTime newEndTime = auction.getItem().getEndTime();
        
        // Xác minh thời gian mới phải trễ hơn thời gian kết thúc cũ
        assertTrue(newEndTime.isAfter(oldEndTime), "Thời gian kết thúc phải được gia hạn");
    }
}