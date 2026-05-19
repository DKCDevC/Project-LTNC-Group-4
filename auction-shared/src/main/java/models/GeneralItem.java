// 1. Khai báo package: Nằm chung nhóm với các model dùng chung cho cả Client và Server.
package models;

// 2. Import thư viện ngày giờ hệ thống Java 8
import java.time.LocalDateTime;

/**
 * Lớp GeneralItem đại diện cho các sản phẩm thông thường, phổ thông không thuộc danh mục đặc biệt nào khác.
 * Kế thừa trực tiếp từ lớp trừu tượng cha Item (Inheritance - quan hệ IS-A).
 * Do lớp cha Item chứa phương thức trừu tượng printInfo(), lớp con GeneralItem KHÔNG THỂ trốn tránh,
 * bắt buộc phải cụ thể hóa bằng cách triển khai mã xử lý cho phương thức này.
 */
public class GeneralItem extends Item {

    /**
     * Hàm khởi tạo cơ bản cho sản phẩm phổ thông.
     * Thực hiện kỹ thuật Chaining Constructor (Liên kết chuỗi hàm tạo).
     * 
     * @param name Tên sản phẩm
     * @param description Mô tả chi tiết sản phẩm
     * @param startingPrice Giá khởi điểm ban đầu
     * @param startTime Thời điểm bắt đầu đấu giá
     * @param endTime Thời điểm kết thúc đấu giá
     */
    public GeneralItem(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        // 3. super(...): Gọi trực tiếp đến hàm khởi tạo đầy đủ tham số của lớp cha Item để gán các giá trị ban đầu vào RAM.
        super(name, description, startingPrice, startTime, endTime);
    }

    /**
     * Triển khai phương thức hiển thị thông tin sản phẩm.
     * Sử dụng Annotation @Override để báo hiệu cho trình biên dịch (Compiler) kiểm tra xem
     * phương thức này có khớp chữ ký (signature) với phương thức trừu tượng ở lớp cha Item không.
     * Nếu không khớp, Compiler sẽ báo lỗi ngay lập tức lúc biên dịch (Compile-time error).
     */
    @Override
    public void printInfo() {
        // 4. Gọi getName() và getCurrentHighestPrice() thừa hưởng từ lớp cha để in ra màn hình Console.
        System.out.println("[Chung] " + getName() + " - Giá hiện tại: " + getCurrentHighestPrice());
    }
}
