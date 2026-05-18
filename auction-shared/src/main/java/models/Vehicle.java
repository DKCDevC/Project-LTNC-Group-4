package models;

import java.time.LocalDateTime;

/**
 * Lớp Vehicle đại diện cho các sản phẩm phương tiện giao thông (như ô tô, xe máy, xe điện) trong hệ thống đấu giá.
 * Kế thừa từ lớp Item và mở rộng thêm thuộc tính thương hiệu/hãng sản xuất.
 */
public class Vehicle extends Item {
    // Thương hiệu hoặc hãng sản xuất của phương tiện (ví dụ: Toyota, Honda, Tesla...)
    private String brand;

    /**
     * Hàm khởi tạo đầy đủ cho sản phẩm phương tiện giao thông.
     * @param name Tên phương tiện
     * @param description Mô tả về tình trạng, số km đã đi, màu sắc...
     * @param startingPrice Giá khởi điểm đặt ra ban đầu
     * @param startTime Thời điểm bắt đầu phiên đấu giá
     * @param endTime Thời điểm kết thúc phiên đấu giá
     * @param brand Thương hiệu/hãng xe
     */
    public Vehicle(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime, String brand) {
        super(name, description, startingPrice, startTime, endTime);
        this.brand = brand;
    }

    /**
     * Triển khai phương thức hiển thị thông tin sản phẩm.
     * Ghi đè phương thức printInfo() để in thông tin phương tiện kèm hãng sản xuất và giá khởi điểm.
     */
    @Override
    public void printInfo() {
        System.out.println("[Phương Tiện] " + getName() + " | Hãng: " + brand + " | Giá khởi điểm: " + getStartingPrice());
    }

    /**
     * Lấy thương hiệu/hãng sản xuất của phương tiện.
     * @return Chuỗi tên thương hiệu
     */
    public String getBrand() { return brand; }
    
    /**
     * Thiết lập thương hiệu/hãng sản xuất mới cho phương tiện.
     * @param brand Hãng sản xuất cần đổi
     */
    public void setBrand(String brand) { this.brand = brand; }
}