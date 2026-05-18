package network;

/**
 * Interface AuctionObserver đại diện cho "Quan sát viên" trong mẫu thiết kế Observer Pattern.
 * Định nghĩa phương thức cập nhật dữ liệu để gửi trực tiếp thông điệp mạng thời gian thực về phía Client.
 */
public interface AuctionObserver {
    
    /**
     * Phương thức callback được gọi bởi Chủ thể khi có thông tin đấu giá mới cần cập nhật tới Client.
     * @param message Chuỗi thông tin định dạng JSON cần gửi qua Socket
     */
    void updateClient(String message);
}