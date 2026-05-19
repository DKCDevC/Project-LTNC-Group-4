// 1. Khai báo package: Định nghĩa không gian tên chứa lớp AutoBid để tổ chức code gọn gàng, tránh xung đột tên.
package models;

// 2. Import lớp LocalDateTime: Dùng để ghi nhận ngày giờ hệ thống chính xác đến từng mili-giây.
import java.time.LocalDateTime;

/**
 * Lớp AutoBid đại diện cho cấu hình tính năng Đặt giá tự động (Auto-bid) của một người dùng cụ thể.
 * Thiết kế theo nguyên lý Bất biến (Immutability): Một khi đối tượng được tạo ra từ constructor,
 * các giá trị của nó không thể bị thay đổi từ bên ngoài (vì không có các phương thức setter), đảm bảo tính an toàn dữ liệu.
 */
public class AutoBid {
    
    // 3. Khai báo biến bidder: Lưu trữ đối tượng Người đặt giá sở hữu cấu hình tự động này (Composition - quan hệ HAS-A).
    private Bidder bidder;
    
    // 4. Khai báo biến maxBid: Số tiền giới hạn tối đa mà người dùng này sẵn sàng trả cho sản phẩm đấu giá.
    private double maxBid;
    
    // 5. Khai báo biến increment: Bước giá nhảy tự động cộng thêm mỗi khi robot nâng giá thầu để tranh giành sản phẩm.
    private double increment;
    
    // 6. Khai báo biến registerTime: Thời điểm người dùng đăng ký cấu hình này (được dùng để ưu tiên robot đăng ký trước).
    private LocalDateTime registerTime;

    /**
     * Hàm khởi tạo (Constructor) để thiết lập một cấu hình Auto-bid hoàn chỉnh.
     * 
     * @param bidder Đối tượng người đấu giá thiết lập cấu hình
     * @param maxBid Số tiền giới hạn tối đa có thể trả
     * @param increment Bước giá tự động cộng thêm mỗi lần nhảy
     */
    public AutoBid(Bidder bidder, double maxBid, double increment) {
        // 7. Gán giá trị biến tham chiếu bidder truyền vào cho biến instance của class.
        this.bidder = bidder;
        
        // 8. Gán giá trị giới hạn tối đa truyền vào cho biến instance maxBid.
        this.maxBid = maxBid;
        
        // 9. Gán bước giá tự động truyền vào cho biến instance increment.
        this.increment = increment;
        
        // 10. Tự động lấy ngày giờ hiện tại chính xác của hệ thống làm dấu mốc thời gian đăng ký robot.
        this.registerTime = LocalDateTime.now(); 
    }

    // 11. Các phương thức Getter: Chỉ cho phép đọc các thông số của cấu hình Robot, tuyệt đối không cho ghi đè (đảm bảo tính đóng gói).
    public Bidder getBidder() { return bidder; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
    public LocalDateTime getRegisterTime() { return registerTime; }
}