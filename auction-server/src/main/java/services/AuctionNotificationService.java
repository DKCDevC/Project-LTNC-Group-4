// 1. Khai báo package: Nằm trong phân hệ dịch vụ nghiệp vụ (Services) của Server.
package services;

/**
 * Interface AuctionNotificationService định nghĩa hợp đồng thông báo sự kiện (Observer Design Pattern).
 * Cho phép AuctionManager hoặc các tác nhân nghiệp vụ phát đi các bản tin thời gian thực 
 * (ví dụ: cập nhật giá thầu mới, thông báo hết giờ...) mà không cần quan tâm đến cách thức gửi tin cụ thể qua Socket/WebSocket.
 * 
 * Ý nghĩa thiết kế:
 * - Decoupling (Tách biệt logic mạng): AuctionManager chỉ tương tác với interface này để đẩy bản tin thầu.
 *   Nó không hề biết sự tồn tại của Socket, ClientHandler, TCP Port hay luồng mạng, tuân thủ nguyên lý Single Responsibility.
 * - Triển khai thực tế: Server hoặc lớp quản lý phiên mạng chính sẽ cài đặt interface này để chuyển tiếp bản tin cho toàn bộ Socket Client.
 */
public interface AuctionNotificationService {
    
    /**
     * Phát đi thông điệp tới toàn bộ các quan sát viên (tất cả các Client đang kết nối trực tuyến).
     * Bất kỳ sự kiện nâng giá hay gia hạn giờ chót nào đều đi qua phương thức hợp đồng này.
     * 
     * @param message Nội dung thông điệp định dạng chuỗi JSON thô cần phát mạng
     */
    void notifyAll(String message);
}
