package models;

import java.time.LocalDateTime;

/**
 * Lớp Electronics đại diện cho các sản phẩm công nghệ, điện tử trong hệ thống đấu giá.
 * Kế thừa từ lớp Item và mở rộng thêm thuộc tính thời gian bảo hành.
 */
public class Electronics extends Item {
    // Thời hạn bảo hành của sản phẩm điện tử (tính bằng tháng)
    private int warrantyMonths;

    /**
     * Hàm khởi tạo đầy đủ cho sản phẩm điện tử.
     * @param name Tên sản phẩm điện tử (ví dụ: Điện thoại, Laptop...)
     * @param description Mô tả chi tiết cấu hình, tình trạng máy
     * @param startingPrice Giá khởi điểm đặt ra ban đầu
     * @param startTime Thời điểm bắt đầu phiên đấu giá
     * @param endTime Thời điểm kết thúc phiên đấu giá
     * @param warrantyMonths Số tháng bảo hành của sản phẩm
     */
    public Electronics(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime, int warrantyMonths) {
        super(name, description, startingPrice, startTime, endTime);
        this.warrantyMonths = warrantyMonths;
    }

    /**
     * Triển khai phương thức hiển thị thông tin sản phẩm.
     * Ghi đè phương thức printInfo() của lớp Item để in ra các thông số cụ thể của sản phẩm điện tử.
     */
    @Override
    public void printInfo() {
        System.out.println("[Điện tử] " + getName() + " - Bảo hành: " + warrantyMonths + " tháng - Giá hiện tại: " + getCurrentHighestPrice());
    }

    /**
     * Lấy thời gian bảo hành của sản phẩm.
     * @return Số tháng bảo hành
     */
    public int getWarrantyMonths() {
            return warrantyMonths;
    }
}