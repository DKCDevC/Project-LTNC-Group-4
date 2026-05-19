// 1. Khai báo package: Thuộc phân hệ network quản lý kết nối socket của Server.
package network;

/**
 * Interface AuctionObserver đại diện cho "Quan sát viên" trong mẫu thiết kế Observer (Observer Design Pattern).
 * Định nghĩa phương thức hợp đồng (contract method) cập nhật dữ liệu để gửi trực tiếp thông điệp mạng thời gian thực về phía Client.
 * 
 * Ý nghĩa thiết kế của Interface:
 * - Loose Coupling (Liên kết lỏng): Giúp chủ thể phát tin (`AuctionSubject`) không cần quan tâm chi tiết kết nối 
 *   mạng hay giao thức Socket bên dưới được xử lý như thế nào. Nó chỉ cần gọi hàm `updateClient()` trên bất kỳ đối tượng nào
 *   triển khai (implement) interface này.
 * - Triển khai thực tế: Lớp `ClientHandler` sẽ implement interface này để làm nhiệm vụ chuyển tiếp gói tin qua luồng TCP.
 */
public interface AuctionObserver {
    
    /**
     * Phương thức callback (Abstract Method) bắt buộc các lớp triển khai phải định nghĩa logic.
     * Được gọi bởi Chủ thể (Subject) khi có thông tin đấu giá mới cần cập nhật lập tức tới Client.
     * 
     * @param message Chuỗi thông tin định dạng JSON chứa giá thầu mới hoặc thay đổi trạng thái cần gửi qua Socket
     */
    void updateClient(String message);
}