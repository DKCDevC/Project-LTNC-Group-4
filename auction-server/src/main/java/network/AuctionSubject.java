package network;

/**
 * Interface AuctionSubject đại diện cho "Chủ thể được quan sát" trong mẫu thiết kế Quan sát viên (Observer Pattern).
 * Khai báo các hợp đồng quản lý danh sách quan sát viên (Client connections) và phát đi thông điệp mạng.
 */
public interface AuctionSubject {
    
    /**
     * Đăng ký một Quan sát viên mới (ví dụ: ClientHandler của một Client mới kết nối).
     * @param observer Quan sát viên cần đăng ký
     */
    void registerObserver(AuctionObserver observer);
    
    /**
     * Hủy đăng ký một Quan sát viên (ví dụ: khi Client bị mất kết nối hoặc ngắt mạng).
     * @param observer Quan sát viên cần gỡ bỏ
     */
    void removeObserver(AuctionObserver observer);
    
    /**
     * Phát đi thông báo thời gian thực đến toàn bộ các Quan sát viên đang trực tuyến trong danh sách.
     * @param message Nội dung bản tin phát sóng dạng chuỗi JSON
     */
    void notifyAllObservers(String message);
}