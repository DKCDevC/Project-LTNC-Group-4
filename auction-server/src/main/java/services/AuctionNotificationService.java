package services;

/**
 * Interface AuctionNotificationService định nghĩa hợp đồng thông báo sự kiện (Observer Pattern).
 * Cho phép AuctionManager hoặc các tác nhân nghiệp vụ phát đi các bản tin thời gian thực 
 * (ví dụ: cập nhật giá thầu mới, thông báo hết giờ...) mà không cần quan tâm đến cách thức gửi tin cụ thể qua Socket/WebSocket.
 */
public interface AuctionNotificationService {
    
    /**
     * Phát đi thông điệp tới toàn bộ các quan sát viên (tất cả các Client đang kết nối trực tuyến).
     * @param message Nội dung thông điệp cần phát
     */
    void notifyAll(String message);
}
