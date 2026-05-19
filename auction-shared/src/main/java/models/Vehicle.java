package models;

import java.time.LocalDateTime;

/**
 * Lớp Vehicle đại diện cho các sản phẩm phương tiện giao thông (như ô tô, xe máy, xe điện) trong hệ thống đấu giá eBid.
 * 
 * Các nguyên lý kỹ thuật hướng đối tượng (OOP):
 * 1. Polymorphism (Đa hình kế thừa): Kế thừa lớp trừu tượng `Item` và bổ sung trường thuộc tính đặc thù 
 *    cho phương tiện giao thông là `brand` (Thương hiệu/Hãng sản xuất).
 * 2. Encapsulation (Đóng gói): Các trường dữ liệu `brand` được đóng gói private, tương tác thông qua 
 *    tổ hợp getter/setter công khai.
 * 3. Abstraction Implementation (Hiện thực hóa trừu tượng): Override phương thức trừu tượng `printInfo()` 
 *    để in thông tin chi tiết đặc thù của xe thầu.
 */
public class Vehicle extends Item {
    
    // Thương hiệu hoặc hãng sản xuất của phương tiện (Đóng gói an toàn)
    private String brand;

    /**
     * Hàm khởi tạo đầy đủ cho sản phẩm phương tiện giao thông (Vehicle).
     * 
     * @param name Tên phương tiện (ví dụ: Tesla Model S)
     * @param description Mô tả về tình trạng, số km đã đi, màu sắc, động cơ
     * @param startingPrice Giá khởi điểm đặt ra ban đầu của xe
     * @param startTime Thời điểm bắt đầu phiên đấu giá xe thầu
     * @param endTime Thời điểm kết thúc phiên đấu giá gõ búa xe thầu
     * @param brand Thương hiệu/hãng xe sáng chế độc quyền
     */
    public Vehicle(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime, String brand) {
        super(name, description, startingPrice, startTime, endTime);
        this.brand = brand;
    }

    /**
     * Triển khai phương thức hiển thị thông tin sản phẩm xe thầu.
     * Ghi đè phương thức printInfo() từ lớp cha Item.
     */
    @Override
    public void printInfo() {
        System.out.println("[Phương Tiện] " + getName() + " | Hãng: " + brand + " | Giá khởi điểm: " + getStartingPrice());
    }

    /**
     * Lấy thương hiệu/hãng sản xuất của phương tiện.
     * 
     * @return Chuỗi tên thương hiệu
     */
    public String getBrand() { 
        return brand; 
    }
    
    /**
     * Thiết lập thương hiệu/hãng sản xuất mới cho phương tiện.
     * 
     * @param brand Hãng sản xuất mới cần thay đổi
     */
    public void setBrand(String brand) { 
        this.brand = brand; 
    }
}