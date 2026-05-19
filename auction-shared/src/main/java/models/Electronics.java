// 1. Khai báo package: Thuộc nhóm models dùng chung.
package models;

// 2. Import thư viện thời gian Java 8
import java.time.LocalDateTime;

/**
 * Lớp Electronics đại diện cho các sản phẩm công nghệ, điện tử trong hệ thống đấu giá.
 * Kế thừa từ lớp cha trừu tượng Item (IS-A) để thừa hưởng toàn bộ thuộc tính cơ bản.
 * Đồng thời mở rộng thêm thuộc tính riêng biệt (warrantyMonths) mà lớp cha không có.
 */
public class Electronics extends Item {
    // 3. Khai báo biến riêng tư warrantyMonths (Tính đóng gói - Encapsulation): 
    // Thời hạn bảo hành của sản phẩm điện tử (tính bằng tháng).
    private int warrantyMonths;

    /**
     * Hàm khởi tạo (Constructor) đầy đủ tham số cho sản phẩm điện tử.
     * 
     * @param name Tên sản phẩm điện tử (ví dụ: Điện thoại, Laptop...)
     * @param description Mô tả chi tiết cấu hình, tình trạng máy
     * @param startingPrice Giá khởi điểm đặt ra ban đầu
     * @param startTime Thời điểm bắt đầu phiên đấu giá
     * @param endTime Thời điểm kết thúc phiên đấu giá
     * @param warrantyMonths Số tháng bảo hành của sản phẩm
     */
    public Electronics(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime, int warrantyMonths) {
        // 4. super(...): Gọi trực tiếp Constructor của lớp cha Item để gán các giá trị dùng chung vào RAM Heap.
        super(name, description, startingPrice, startTime, endTime);
        
        // 5. Gán giá trị bảo hành chuyên biệt được truyền vào cho thuộc tính riêng của lớp con.
        this.warrantyMonths = warrantyMonths;
    }

    /**
     * Triển khai phương thức hiển thị thông tin sản phẩm.
     * Ghi đè phương thức printInfo() của lớp cha Item để in ra các thông số cụ thể của sản phẩm điện tử.
     * Sử dụng kỹ thuật Polymorphism (Đa hình) để tự quyết định cách hành xử khi được đối xử như một Item chung chung.
     */
    @Override
    public void printInfo() {
        System.out.println("[Điện tử] " + getName() + " - Bảo hành: " + warrantyMonths + " tháng - Giá hiện tại: " + getCurrentHighestPrice());
    }

    /**
     * Phương thức Getter để lấy thời gian bảo hành của sản phẩm (Tính đóng gói).
     * 
     * @return Số tháng bảo hành
     */
    public int getWarrantyMonths() {
        return warrantyMonths;
    }
}