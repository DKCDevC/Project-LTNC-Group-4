package models;

import java.time.LocalDateTime;

/**
 * Lớp AutoBid đại diện cho cấu hình tính năng Đặt giá tự động (Auto-bid) của một người dùng.
 * Cho phép người đấu giá cài đặt một số tiền tối đa có thể trả và bước giá tự động tăng lên
 * mỗi khi có người dùng khác đặt giá cao hơn mình.
 */
public class AutoBid {
    // Đối tượng người đấu giá (Bidder) thiết lập cấu hình tự động này
    private Bidder bidder;
    
    // Số tiền tối đa mà người dùng sẵn sàng chi trả cho sản phẩm này
    private double maxBid;
    
    // Bước giá tăng lên mỗi lần nâng giá tự động (ví dụ: mỗi lần tăng thêm 10,000đ)
    private double increment;
    
    // Thời điểm người dùng đăng ký cấu hình Auto-bid này (dùng để ưu tiên bot đăng ký trước)
    private LocalDateTime registerTime;

    /**
     * Hàm khởi tạo cấu hình Auto-bid.
     * @param bidder Người đấu giá thiết lập
     * @param maxBid Giới hạn giá tối đa
     * @param increment Bước giá tự động cộng thêm
     */
    public AutoBid(Bidder bidder, double maxBid, double increment) {
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registerTime = LocalDateTime.now(); // Tự động ghi nhận thời điểm đăng ký hiện tại
    }

    public Bidder getBidder() { return bidder; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
    public LocalDateTime getRegisterTime() { return registerTime; }
}