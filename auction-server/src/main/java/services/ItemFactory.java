package services;

import models.Art;
import models.Electronics;
import models.GeneralItem;
import models.Item;
import models.Vehicle;

import java.time.LocalDateTime;

/**
 * Lớp ItemFactory triển khai mẫu thiết kế Nhà máy (Factory Pattern).
 * Đóng vai trò là điểm duy nhất chịu trách nhiệm khởi tạo các loại sản phẩm cụ thể 
 * (Electronics, Art, Vehicle, GeneralItem) dựa trên tham số loại (type) đầu vào.
 */
public class ItemFactory {

    /**
     * Phương thức tĩnh (Static Factory Method) tạo và trả về đối tượng sản phẩm cụ thể.
     * @param type Kiểu sản phẩm dạng chuỗi (ví dụ: "ELECTRONICS", "ART", "VEHICLE")
     * @param name Tên sản phẩm
     * @param description Mô tả chi tiết sản phẩm
     * @param startingPrice Giá thầu khởi điểm
     * @param startTime Thời gian bắt đầu đấu giá
     * @param endTime Thời gian kết thúc đấu giá
     * @param extraInfo Thông tin bổ sung đặc trưng cho từng loại sản phẩm (ví dụ: số tháng bảo hành, tên nghệ sĩ, hãng xe)
     * @return Đối tượng Item con cụ thể
     * @throws IllegalArgumentException Nếu loại sản phẩm truyền vào không nằm trong danh sách hỗ trợ
     */
    public static Item createItem(String type, String name, String description, double startingPrice,
                                  LocalDateTime startTime, LocalDateTime endTime, String extraInfo) {

        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                int warranty = 0;
                try { 
                    // Chuyển đổi thông tin bảo hành từ chuỗi sang số nguyên
                    warranty = Integer.parseInt(extraInfo); 
                } catch (NumberFormatException e) { 
                    /* Nếu lỗi định dạng hoặc trống, để mặc định là 0 tháng */ 
                }
                return new Electronics(name, description, startingPrice, startTime, endTime, warranty);

            case "ART":
                // Đối với tác phẩm nghệ thuật, extraInfo lưu tên danh họa/nghệ sĩ
                return new Art(name, description, startingPrice, startTime, endTime, extraInfo);

            case "VEHICLE":
                // Đối với phương tiện xe cộ, extraInfo lưu thương hiệu hãng xe
                return new Vehicle(name, description, startingPrice, startTime, endTime, extraInfo);

            case "GENERAL":
                // Sản phẩm thông thường không yêu cầu thông tin mở rộng đặc biệt nào khác
                return new GeneralItem(name, description, startingPrice, startTime, endTime);

            default:
                throw new IllegalArgumentException("Lỗi: Loại sản phẩm không được hỗ trợ (" + type + ")");
        }
    }
}