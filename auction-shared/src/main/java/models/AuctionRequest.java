package models;

import java.util.Objects;

/**
 * Lớp AuctionRequest định nghĩa cấu trúc gói tin (Data Transfer Object - DTO) 
 * dùng để trao đổi dữ liệu qua lại giữa Client và Server qua giao thức Socket.
 * Thường được chuyển đổi sang định dạng JSON bằng thư viện Gson trước khi gửi qua mạng.
 */
public class AuctionRequest {
    // Tên lệnh hoặc hành động yêu cầu (ví dụ: "LOGIN", "PLACE_BID", "ADD_ITEM"...)
    private String command;
    
    // Dữ liệu đính kèm kèm theo lệnh, có thể là bất kỳ kiểu đối tượng nào (ví dụ: User, Item, BidTransaction)
    private Object data;

    /**
     * Hàm khởi tạo mặc định không tham số (cần thiết cho quá trình khử tuần tự hóa - Deserialization của Gson).
     */
    public AuctionRequest() {}

    /**
     * Hàm khởi tạo đầy đủ tham số.
     * @param command Tên lệnh yêu cầu
     * @param data Dữ liệu đi kèm lệnh
     */
    public AuctionRequest(String command, Object data) {
        this.command = command;
        this.data = data;
    }

    public String getCommand() { return command; }
    public Object getData() { return data; }
}