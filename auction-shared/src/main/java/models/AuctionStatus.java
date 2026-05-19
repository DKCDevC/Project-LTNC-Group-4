package models;

/**
 * Enum định nghĩa các trạng thái vòng đời có thể có của một phiên đấu giá trong hệ thống eBid.
 * 
 * Các nguyên lý thiết kế và kỹ thuật áp dụng:
 * 1. Type Safety (An toàn kiểu dữ liệu): Sử dụng Java `enum` thay vì hằng số Chuỗi (String constants) 
 *    hoặc số nguyên (Integers) để tránh lỗi gán sai giá trị lúc chạy, tăng cường kiểm tra cú pháp khi biên dịch.
 * 2. Finite State Machine (Máy trạng thái hữu hạn - FSM): Biểu diễn cấu trúc trạng thái của phiên thầu:
 *    OPEN -> RUNNING -> FINISHED -> PAID hoặc CANCELED.
 */
public enum AuctionStatus {
    
    // Phiên đấu giá mới được khởi tạo thành công trên hệ thống, chờ đến giờ chính thức mở thầu
    OPEN,
    
    // Phiên đấu giá đang diễn ra tích cực thời gian thực, cho phép các Bidder đặt thầu cạnh tranh
    RUNNING,
    
    // Phiên đấu giá đã chạm mốc thời gian kết thúc (Hết giờ), ngừng nhận thầu và khóa sổ
    FINISHED,
    
    // Hóa đơn đơn hàng thắng cuộc đã được thanh toán an toàn, kết thúc toàn bộ vòng đời sản phẩm
    PAID,
    
    // Phiên đấu giá bị cưỡng chế hủy thầu do vi phạm quy chế bởi Admin hoặc Người bán thu hồi tin
    CANCELED
}