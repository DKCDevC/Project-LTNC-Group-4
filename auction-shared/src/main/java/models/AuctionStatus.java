package models;

/**
 * Enum định nghĩa các trạng thái có thể có của một phiên đấu giá trong hệ thống eBid.
 */
public enum AuctionStatus {
    // Phiên đấu giá mới được mở, chưa chính thức cho phép đấu giá hoặc chờ duyệt
    OPEN,
    
    // Phiên đấu giá đang diễn ra tích cực, cho phép người dùng đặt giá thầu
    RUNNING,
    
    // Phiên đấu giá đã kết thúc (hết thời gian) và đã xác định kết quả người chiến thắng
    FINISHED,
    
    // Phiên đấu giá đã được người thắng cuộc thanh toán đơn hàng thành công
    PAID,
    
    // Phiên đấu giá đã bị hủy bỏ bởi Người bán hoặc Quản trị viên
    CANCELED,
}