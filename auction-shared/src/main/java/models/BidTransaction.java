package models;

import java.time.LocalDateTime;

/**
 * Lớp BidTransaction đại diện cho một lượt đặt giá (giao dịch đấu giá) cụ thể trong hệ thống eBid.
 * 
 * Các nguyên lý thiết kế và kỹ thuật áp dụng:
 * 1. Transactional Immutability (Tính bất biến của Giao dịch): Một khi lượt đặt giá thầu đã được tạo ra 
 *    và ghi nhận, các thông số tài chính (Số tiền, Người thầu, Thời gian) không được phép thay đổi (read-only).
 * 2. Immutable Log Pattern (Mẫu nhật ký kiểm toán): Lưu vết lịch sử tăng giá sản phẩm làm cơ sở đối chiếu tranh chấp 
 *    hoặc vẽ biểu đồ biến động giá LineChart tại Client.
 * 3. Temporal Tracking (Ghi vết thời gian): Sử dụng `LocalDateTime.now()` để ghi vết mốc thời gian chính xác 
 *    đến từng mili giây khi lượt đấu thầu được máy chủ Server chấp thuận hợp lệ.
 * 4. Entity Inheritance (Kế thừa thực thể định danh): Kế thừa lớp `Entity` để tự động sinh UUID chuỗi 
 *    làm khóa chính định danh duy nhất cho bản ghi giao dịch trong cơ sở dữ liệu SQLite.
 */
public class BidTransaction extends Entity {
    
    // Người đấu giá (Bidder) thực hiện lượt đặt giá thầu này
    private Bidder bidder;
    
    // Số tiền đặt thầu của giao dịch này (Bắt buộc phải cao hơn giá cao nhất hiện tại)
    private double amount;
    
    // Mốc thời gian chính xác khi lượt đặt thầu này được hệ thống phê duyệt thành công
    private LocalDateTime timestamp;

    /**
     * Hàm khởi tạo một giao dịch đặt giá thầu (BidTransaction).
     * 
     * @param bidder Đối tượng người đặt giá (Bidder)
     * @param amount Số tiền đặt thầu thực tế (VND)
     */
    public BidTransaction(Bidder bidder, double amount) {
        super(); // Gọi hàm dựng Entity để sinh mã định danh giao dịch UUID ngẫu nhiên
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now(); // Tự động chốt mốc thời điểm ghi nhận hiện tại
    }

    /**
     * Lấy thông tin người đấu thầu.
     * @return Đối tượng người thầu
     */
    public Bidder getBidder() { 
        return bidder; 
    }
    
    /**
     * Lấy số tiền đặt thầu.
     * @return Số tiền đặt thầu (double)
     */
    public double getAmount() { 
        return amount; 
    }
    
    /**
     * Lấy mốc thời gian chốt đặt thầu.
     * @return Thời điểm LocalDateTime
     */
    public LocalDateTime getTimestamp() { 
        return timestamp; 
    }
}