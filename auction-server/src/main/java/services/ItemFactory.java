// 1. Khai báo package: Nằm trong phân hệ dịch vụ nghiệp vụ (Services) của Server.
package services;

// 2. Import các mô hình cụ thể của sản phẩm đa hình
import models.Art;
import models.Electronics;
import models.GeneralItem;
import models.Item;
import models.Vehicle;
import java.time.LocalDateTime;

/**
 * Lớp ItemFactory triển khai mẫu thiết kế Nhà máy (Factory Design Pattern).
 * Đóng vai trò là điểm duy nhất (Single Point of Instantiation) chịu trách nhiệm khởi tạo các loại sản phẩm cụ thể 
 * (Electronics, Art, Vehicle, GeneralItem) dựa trên tham số loại (type) đầu vào.
 * 
 * Ý nghĩa thiết kế của Factory Pattern:
 * - Encapsulation (Đóng gói việc tạo đối tượng): Che giấu chi tiết cấu trúc phức tạp của các lớp con (subclasses).
 *   Client chỉ cần gọi phương thức tĩnh `createItem()` với một tham số chuỗi đơn giản, không cần biết lớp con nào 
 *   được khởi dựng bằng từ khóa `new` hay nó cần những tham số đặc hữu gì.
 * - Polymorphism Promotion (Thúc đẩy tính đa hình): Giúp trả về kiểu dữ liệu trừu tượng cha (`Item`),
 *   cho phép phía gọi xử lý tất cả các loại mặt hàng một cách đồng nhất, giảm thiểu liên kết cứng (Tight Coupling).
 */
public class ItemFactory {

    /**
     * Phương thức tĩnh (Static Factory Method) tạo và trả về đối tượng sản phẩm cụ thể.
     * 
     * Quy trình xử lý (Input -> Process -> Output):
     * 1. Đầu vào (Input): Nhận kiểu sản phẩm ("type"), các thuộc tính cơ bản (name, desc, price, times),
     *    và một thuộc tính mở rộng chuyên biệt (`extraInfo`).
     * 2. Xử lý (Process): 
     *    - Sử dụng cấu trúc `switch-case` trên phiên bản chữ hoa của kiểu dữ liệu.
     *    - Đối với "ELECTRONICS": Thử giải mã (parse) số tháng bảo hành từ `extraInfo` một cách phòng thủ (Defensive Parsing),
     *      nếu xảy ra lỗi định dạng NumberFormatException thì tự động gán mặc định là 0 tháng để hệ thống không bị crash.
     *    - Đối với "ART": Gán trực tiếp tên nghệ sĩ/danh họa.
     *    - Đối với "VEHICLE": Gán trực tiếp hãng sản xuất xe.
     *    - Đối với "GENERAL": Trả về lớp sản phẩm thông thường.
     * 3. Đầu ra (Output): Một thực thể `Item` đa hình con hoàn chỉnh.
     * 
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
                    // Chuyển đổi thông tin bảo hành từ chuỗi sang số nguyên phòng thủ (Defensive Parsing)
                    warranty = Integer.parseInt(extraInfo); 
                } catch (NumberFormatException e) { 
                    /* Nếu lỗi định dạng hoặc trống, để mặc định là 0 tháng, tránh crash hệ thống */ 
                }
                return new Electronics(name, description, startingPrice, startTime, endTime, warranty);

            case "ART":
                // Đối với tác phẩm nghệ thuật, extraInfo lưu tên danh họa/nghệ sĩ đặc thù
                return new Art(name, description, startingPrice, startTime, endTime, extraInfo);

            case "VEHICLE":
                // Đối với phương tiện xe cộ, extraInfo lưu thương hiệu hãng xe đặc thù
                return new Vehicle(name, description, startingPrice, startTime, endTime, extraInfo);

            case "GENERAL":
                // Sản phẩm thông thường không yêu cầu thông tin mở rộng đặc biệt nào khác
                return new GeneralItem(name, description, startingPrice, startTime, endTime);

            default:
                // Ném ngoại lệ Runtime phòng vệ nếu truyền sai loại sản phẩm cấu hình
                throw new IllegalArgumentException("Lỗi: Loại sản phẩm không được hỗ trợ (" + type + ")");
        }
    }
}