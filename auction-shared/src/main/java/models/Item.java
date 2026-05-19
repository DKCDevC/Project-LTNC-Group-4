// 1. Khai báo package: Namespace giúp phân loại nhóm các lớp dữ liệu dùng chung (Shared models).
package models;

// 2. Import lớp LocalDateTime: Giúp quản lý thời gian ngày giờ chính xác cho các hoạt động đấu thầu.
import java.time.LocalDateTime;

/**
 * Lớp trừu tượng (abstract class) đại diện cho một sản phẩm (Item) được đưa ra đấu giá trong hệ thống.
 * Kế thừa từ lớp Entity (IS-A) để thừa hưởng mã định danh duy nhất (id).
 * Chứa toàn bộ các thuộc tính và phương thức cơ bản nhất của một sản phẩm hàng hóa.
 * Do là lớp trừu tượng (abstract), lớp này KHÔNG THỂ khởi tạo đối tượng trực tiếp qua từ khóa new,
 * mà bắt buộc phải khởi tạo thông qua các lớp con cụ thể (như Electronics, Art, Vehicle).
 */
public abstract class Item extends Entity {
    // 3. Khai báo các biến Instance (Thuộc tính đối tượng) có phạm vi truy cập private để bảo vệ dữ liệu (Encapsulation).
    private String name;                // Tên sản phẩm đấu giá
    private String description;         // Mô tả chi tiết về tình trạng, thông số của sản phẩm
    private double startingPrice;       // Giá khởi điểm ban đầu khi bắt đầu phiên đấu giá
    private double currentHighestPrice; // Giá cao nhất hiện tại đã được người đấu giá đặt thành công
    private LocalDateTime startTime;    // Thời điểm bắt đầu cho phép đặt thầu
    private LocalDateTime endTime;      // Thời điểm kết thúc phiên đấu giá sản phẩm này
    
    // 4.protected Seller seller:
    // Sử dụng từ khóa bảo vệ protected để cho phép các lớp con kế thừa trực tiếp truy cập vào thuộc tính người bán.
    protected Seller seller;            // Người bán (Seller) sở hữu sản phẩm này
    
    private String imageUrls;           // Danh sách đường dẫn ảnh của sản phẩm đấu giá (ngăn cách bởi dấu phẩy)

    /**
     * Hàm khởi tạo (Constructor) của lớp trừu tượng Item.
     * Sẽ được gọi gián tiếp thông qua hàm khởi tạo của lớp con (bằng từ khóa super).
     * 
     * @param name Tên sản phẩm
     * @param description Mô tả sản phẩm
     * @param startingPrice Giá khởi điểm
     * @param startTime Thời gian bắt đầu đấu giá
     * @param endTime Thời gian kết thúc đấu giá
     */
    public Item(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(); // 5. Gọi Constructor mặc định của lớp cha Entity để tự tạo một ID duy nhất trên Heap.
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestPrice = startingPrice; // 6. Khởi động: Giá cao nhất hiện tại chính là giá khởi điểm.
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Phương thức trừu tượng (abstract method) hiển thị thông tin cụ thể của từng loại sản phẩm.
     * Phương thức này không có phần thân (body).
     * Các lớp con cụ thể bắt buộc phải ghi đè (Override) phương thức này để tự định nghĩa cách hiển thị thông tin riêng biệt.
     */
    public abstract void printInfo();

    // 7. Các phương thức Getter và Setter giúp đọc và ghi đè an toàn các thuộc tính riêng tư (Encapsulation).
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getCurrentHighestPrice() {
        return currentHighestPrice;
    }
    public void setCurrentHighestPrice(double currentHighestPrice) {
        this.currentHighestPrice = currentHighestPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Seller getSeller(){
        return seller;
    }
    public void setSeller(Seller seller){
        this.seller=seller;
    }

    public String getImageUrls() {
        return imageUrls;
    }
    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }
}