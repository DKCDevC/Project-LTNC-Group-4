package models;

import java.util.*;

/**
 * Lớp trừu tượng cơ sở (Base Abstract Class) cho tất cả các đối tượng thực thể trong hệ thống eBid.
 * Lớp này tự động sinh mã định danh duy nhất (UUID) cho mỗi thực thể khi khởi tạo.
 */
public abstract class Entity {
    // Mã định danh duy nhất (ID) của thực thể, dùng làm khóa chính hoặc phân biệt giữa các đối tượng
    protected String id;

    /**
     * Hàm khởi tạo mặc định.
     * Tự động tạo ngẫu nhiên một chuỗi UUID duy nhất gán cho thuộc tính id.
     */
    public Entity(){
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Lấy mã định danh duy nhất của thực thể.
     * @return Chuỗi ID dạng UUID hoặc mã tùy chỉnh
     */
    public String getId(){
        return id;
    }

    /**
     * Gán mã định danh duy nhất mới cho thực thể.
     * @param id Chuỗi ID mới cần gán
     */
    public void setId(String id){
        this.id = id;
    }
}
