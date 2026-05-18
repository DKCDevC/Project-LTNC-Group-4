package models;

import java.time.LocalDateTime;

/**
 * Lớp trừu tượng đại diện cho một sản phẩm (Item) được đưa ra đấu giá trong hệ thống.
 * Kế thừa từ lớp Entity để thừa hưởng mã định danh duy nhất (id).
 */
public abstract class Item extends Entity {
    // Tên sản phẩm đấu giá
    private String name;
    
    // Mô tả chi tiết về tình trạng, thông số của sản phẩm
    private String description;
    
    // Giá khởi điểm ban đầu khi bắt đầu phiên đấu giá
    private double startingPrice;
    
    // Giá cao nhất hiện tại đã được người đấu giá đặt
    private double currentHighestPrice;
    
    // Thời điểm bắt đầu cho phép đặt thầu
    private LocalDateTime startTime;
    
    // Thời điểm kết thúc phiên đấu giá sản phẩm này
    private LocalDateTime endTime;
    
    // Người bán (Seller) sở hữu sản phẩm này
    protected Seller seller;
    
    // Danh sách đường dẫn ảnh của sản phẩm đấu giá (ngăn cách bởi dấu phẩy hoặc dạng JSON)
    private String imageUrls;

    /**
     * Hàm khởi tạo đầy đủ các thuộc tính cơ bản của một sản phẩm.
     * @param name Tên sản phẩm
     * @param description Mô tả sản phẩm
     * @param startingPrice Giá khởi điểm
     * @param startTime Thời gian bắt đầu đấu giá
     * @param endTime Thời gian kết thúc đấu giá
     */
    public Item(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestPrice = startingPrice; // Lúc đầu, giá cao nhất chính là giá khởi điểm
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Phương thức trừu tượng hiển thị thông tin cụ thể của từng loại sản phẩm.
     * Các lớp con kế thừa (như Electronics, Art, Vehicle) bắt buộc phải triển khai phương thức này.
     */
    public abstract void printInfo();

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