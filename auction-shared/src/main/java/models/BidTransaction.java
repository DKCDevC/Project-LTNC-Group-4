package models;

import java.time.LocalDateTime;

/**
 * Lớp BidTransaction đại diện cho một lượt đặt giá (giao dịch đấu giá) cụ thể.
 * Ghi lại thông tin người đấu giá, số tiền đặt thầu và mốc thời gian diễn ra giao dịch.
 * Kế thừa từ lớp Entity để có mã định danh giao dịch riêng biệt.
 */
public class BidTransaction extends Entity {
    // Người đấu giá (Bidder) thực hiện lượt đặt giá này
    private Bidder bidder;
    
    // Số tiền đặt thầu của giao dịch này (phải cao hơn giá cao nhất trước đó)
    private double amount;
    
    // Mốc thời gian chính xác khi lượt đặt thầu này được hệ thống ghi nhận
    private LocalDateTime timestamp;

    /**
     * Hàm khởi tạo một giao dịch đặt giá thầu.
     * @param bidder Người đặt giá
     * @param amount Số tiền đặt thầu
     */
    public BidTransaction(Bidder bidder, double amount) {
        super();
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now(); // Tự động lấy thời điểm hiện tại
    }

    public Bidder getBidder() { return bidder; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}