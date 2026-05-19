package models;

import java.util.*;

/**
 * Lớp trừu tượng cơ sở (Base Abstract Class) cho tất cả các đối tượng thực thể (Entities) trong hệ thống eBid.
 * 
 * Các nguyên lý thiết kế và giải pháp kỹ thuật áp dụng:
 * 1. Abstraction (Trừu tượng hóa): Lớp này được khai báo `abstract` để ngăn chặn việc khởi tạo trực tiếp 
 *    (instantiation), đóng vai trò cung cấp khuôn mẫu nền tảng chia sẻ hành vi định danh chung cho các lớp con.
 * 2. Distributed Unique Identity (Định danh duy nhất phân tán): Tích hợp thuật toán `UUID.randomUUID().toString()` 
 *    tự động sinh chuỗi định danh 128-bit ngẫu nhiên khi khởi tạo, đảm bảo tính duy nhất của thực thể 
 *    trong môi trường phân tán Client-Server mà không cần phụ thuộc vào cơ chế Auto-increment của Database SQLite.
 * 3. Protected Field Encapsulation (Đóng gói kế thừa bảo vệ): Thuộc tính `id` sử dụng phạm vi truy cập `protected` 
 *    cho phép các lớp con trực tiếp kế thừa và ghi đè mã tùy chọn khi nạp thực thể từ DB lên.
 */
public abstract class Entity {
    
    // Mã định danh duy nhất (ID) của thực thể (Dùng làm Khóa chính Primary Key trong DB)
    protected String id;

    /**
     * Hàm khởi tạo mặc định của thực thể.
     * Tự động kích hoạt cơ chế sinh mã UUID phiên bản 4 ngẫu nhiên bảo đảm không trùng lặp.
     */
    public Entity(){
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Lấy mã định danh duy nhất của thực thể.
     * 
     * @return Chuỗi ID định danh UUID
     */
    public String getId(){
        return id;
    }

    /**
     * Gán mã định danh duy nhất mới cho thực thể (sử dụng khi đồng bộ dữ liệu từ DB lên).
     * 
     * @param id Chuỗi ID định danh mới cần ghi đè
     */
    public void setId(String id){
        this.id = id;
    }
}
