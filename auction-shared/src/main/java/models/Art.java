package models;

import java.time.LocalDateTime;

/**
 * Lớp Art đại diện cho các sản phẩm tác phẩm nghệ thuật, hội họa, điêu khắc trong hệ thống đấu giá eBid.
 * 
 * Các nguyên lý kỹ thuật hướng đối tượng (OOP):
 * 1. Polymorphism (Đa hình kế thừa): Art kế thừa lớp trừu tượng `Item` và bổ sung trường thuộc tính riêng biệt 
 *    đặc thù cho lĩnh vực mỹ thuật là `artistName` (Tên nghệ sĩ sáng tác).
 * 2. Abstraction Implementation (Hiện thực hóa trừu tượng): Cụ thể hóa phương thức trừu tượng `printInfo()` 
 *    định nghĩa tại lớp cha để kết xuất thông tin phù hợp với ngữ cảnh tranh vẽ nghệ thuật.
 * 3. Encapsulation (Đóng gói): Bảo vệ trường thông tin `artistName` bằng tầm vực private, 
 *    chỉ cho phép truy xuất gián tiếp qua phương thức getter công khai.
 */
public class Art extends Item {
    
    // Tên của nghệ sĩ, tác giả sáng tác ra tác phẩm nghệ thuật này (Đóng gói bảo mật)
    private String artistName;

    /**
     * Hàm khởi tạo đầy đủ cho sản phẩm nghệ thuật (Art).
     * 
     * @param name Tên tác phẩm nghệ thuật độc bản
     * @param description Mô tả về chất liệu, kích thước, ý nghĩa, năm sáng tác
     * @param startingPrice Giá khởi điểm đặt ra ban đầu của tác phẩm
     * @param startTime Thời điểm bắt đầu chính thức mở thầu phiên đấu giá
     * @param endTime Thời điểm kết thúc gõ búa chốt phiên đấu giá
     * @param artistName Tên của tác giả/nghệ sĩ sáng lập độc quyền
     */
    public Art(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime, String artistName) {
        super(name, description, startingPrice, startTime, endTime);
        this.artistName = artistName;
    }

    /**
     * Triển khai phương thức hiển thị thông tin sản phẩm nghệ thuật.
     * Ghi đè phương thức trừu tượng printInfo() từ lớp cha Item.
     */
    @Override
    public void printInfo() {
        System.out.println("[Nghệ thuật] " + getName() + " của tác giả " + artistName + " - Giá hiện tại: " + getCurrentHighestPrice());
    }

    /**
     * Lấy tên của tác giả tác phẩm nghệ thuật.
     * 
     * @return Chuỗi tên tác giả nghệ sĩ sáng tác
     */
    public String getArtistName() {
        return artistName;
    }
}