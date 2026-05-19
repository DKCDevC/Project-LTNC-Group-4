// 1. Khai báo package: Thuộc phân hệ network quản lý kết nối socket của Server.
package network;

/**
 * Interface AuctionSubject đại diện cho "Chủ thể được quan sát" (Observable/Subject) trong mẫu thiết kế Observer (Observer Design Pattern).
 * Khai báo các hợp đồng (contracts) quản lý danh sách quan sát viên (Client connections) và phát đi thông điệp mạng thời gian thực.
 * 
 * Ý nghĩa thiết kế của Subject:
 * - Định hình một cổng quản lý tập trung toàn bộ các phiên kết nối mạng của Client trực tuyến.
 * - Cho phép các Client động đăng ký (Subscribe) hoặc hủy đăng ký (Unsubscribe) theo cơ chế Runtime.
 * - Hỗ trợ phát thông tin đồng loạt (Broadcast) tức thời khi có biến động giá.
 */
public interface AuctionSubject {
    
    /**
     * Đăng ký một Quan sát viên mới (ví dụ: ClientHandler của một Client mới kết nối bắt tay mạng thành công).
     * 
     * @param observer Quan sát viên cần đăng ký vào danh sách quản lý
     */
    void registerObserver(AuctionObserver observer);
    
    /**
     * Hủy đăng ký một Quan sát viên (ví dụ: khi Client bị mất kết nối đột ngột hoặc ngắt mạng chủ động).
     * Việc hủy đăng ký này cực kỳ quan trọng để giải phóng bộ nhớ (RAM Heap) và ngăn chặn việc gửi dữ liệu 
     * vào các socket đã đóng, tránh lỗi Broken Pipe hoặc Connection Reset.
     * 
     * @param observer Quan sát viên cần gỡ bỏ khỏi danh sách quản lý
     */
    void removeObserver(AuctionObserver observer);
    
    /**
     * Phát đi thông báo thời gian thực đến toàn bộ các Quan sát viên đang trực tuyến trong danh sách.
     * Duyệt qua danh sách observers và gọi callback `updateClient()` trên từng phần tử.
     * 
     * @param message Nội dung bản tin phát sóng dạng chuỗi JSON thô chứa biến động đấu giá
     */
    void notifyAllObservers(String message);
}