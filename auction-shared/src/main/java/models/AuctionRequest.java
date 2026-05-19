package models;

import java.util.Objects;

/**
 * Lớp AuctionRequest định nghĩa cấu trúc gói tin (Data Transfer Object - DTO) 
 * dùng để trao đổi thông điệp qua lại giữa Client và Server thông qua giao thức Socket TCP.
 * 
 * Các nguyên lý thiết kế hệ thống phân tán:
 * 1. Data Transfer Object (DTO) Pattern: Lớp này đóng vai trò là một container chứa dữ liệu thuần túy, 
 *    không mang logic nghiệp vụ, tối ưu cho việc truyền tải trạng thái qua môi trường mạng mạng.
 * 2. Loose Coupling (Ghép nối lỏng): Thuộc tính `data` được khai báo dưới kiểu cha `Object` 
 *    cho phép đóng gói bất kỳ loại thực thể nào (User, Item, BidTransaction, JsonObject...) 
 *    giúp giao thức tin nhắn linh hoạt, dễ mở rộng lệnh mới mà không cần sửa đổi cấu trúc DTO.
 * 3. Serialization/Deserialization Compatibility: Cung cấp hàm khởi tạo mặc định không tham số 
 *    để các thư viện Parser như GSON có thể khởi tạo đối tượng bằng kỹ thuật Java Reflection 
 *    trước khi đổ dữ liệu JSON vào các trường.
 */
public class AuctionRequest {
    
    // Tên lệnh chính hoặc hành động yêu cầu (ví dụ: "LOGIN", "BID", "AUTO_BID", "ADMIN"...)
    private String command;
    
    // Dữ liệu đính kèm kèm theo lệnh, có thể là bất kỳ kiểu đối tượng nào (Đa hình hóa dạng Object)
    private Object data;

    /**
     * Hàm khởi tạo mặc định không tham số.
     * Bắt buộc có để hỗ trợ cơ chế Deserialize bằng kỹ thuật Reflection của thư viện Gson.
     */
    public AuctionRequest() {}

    /**
     * Hàm khởi tạo đầy đủ tham số để đóng gói gói tin gửi đi nhanh.
     * 
     * @param command Tên lệnh yêu cầu hành động gửi đến máy chủ
     * @param data Dữ liệu thực tải (Payload) đính kèm đi theo lệnh
     */
    public AuctionRequest(String command, Object data) {
        this.command = command;
        this.data = data;
    }

    /**
     * Lấy tên lệnh yêu cầu.
     * @return Chuỗi tên lệnh
     */
    public String getCommand() { 
        return command; 
    }
    
    /**
     * Lấy dữ liệu đính kèm của gói tin thầu.
     * @return Đối tượng dữ liệu thực tải dạng Object
     */
    public Object getData() { 
        return data; 
    }
}