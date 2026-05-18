package models;

import java.time.LocalDateTime;

/**
 * Lớp Art đại diện cho các sản phẩm tác phẩm nghệ thuật, hội họa, điêu khắc trong hệ thống đấu giá.
 * Kế thừa từ lớp Item và bổ sung thêm thuộc tính tên tác giả sáng tác.
 */
public class Art extends Item {
    // Tên của nghệ sĩ, tác giả sáng tác ra tác phẩm nghệ thuật này
    private String artistName;

    /**
     * Hàm khởi tạo đầy đủ cho sản phẩm nghệ thuật.
     * @param name Tên tác phẩm nghệ thuật
     * @param description Mô tả về chất liệu, kích thước, ý nghĩa tác phẩm
     * @param startingPrice Giá khởi điểm đặt ra ban đầu
     * @param startTime Thời điểm bắt đầu phiên đấu giá
     * @param endTime Thời điểm kết thúc phiên đấu giá
     * @param artistName Tên của tác giả/nghệ sĩ
     */
    public Art(String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime, String artistName) {
        super(name, description, startingPrice, startTime, endTime);
        this.artistName = artistName;
    }

    /**
     * Triển khai phương thức hiển thị thông tin sản phẩm.
     * Ghi đè phương thức printInfo() để in thông tin tác phẩm kèm theo tên tác giả và giá hiện tại.
     */
    @Override
    public void printInfo() {
        System.out.println("[Nghệ thuật] " + getName() + " của tác giả " + artistName + " - Giá hiện tại: " + getCurrentHighestPrice());
    }

    /**
     * Lấy tên của tác giả tác phẩm nghệ thuật.
     * @return Chuỗi tên tác giả
     */
    public String getArtistName() {
        return artistName;
    }
}