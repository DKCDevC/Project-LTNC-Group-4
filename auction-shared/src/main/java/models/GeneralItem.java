package models;

import java.time.LocalDateTime;

/**
 * Lớp GeneralItem đại diện cho các sản phẩm thông thường, phổ thông không thuộc danh mục đặc biệt nào khác.
 * Kế thừa từ lớp Item mà không thêm thuộc tính mở rộng nào.
 */
public class GeneralItem extends Item {

    /**
     * Hàm khởi tạo cơ bản cho sản phẩm phổ thông.
     * @param name Tên sản phẩm
     * @param description Mô tả chi tiết sản phẩm
     * @param startingPrice Giá khởi điểm ban đầu
     * @param startTime Thời điểm bắt đầu đấu giá
     * @param endTime Thời điểm kết thúc đấu giá
     */
    public GeneralItem(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(name, description, startingPrice, startTime, endTime);
    }

    /**
     * Triển khai phương thức hiển thị thông tin sản phẩm.
     * Ghi đè printInfo() để in thông tin sản phẩm phổ thông kèm giá cao nhất hiện tại.
     */
    @Override
    public void printInfo() {
        System.out.println("[Chung] " + getName() + " - Giá hiện tại: " + getCurrentHighestPrice());
    }
}
