package utils;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import models.*;

/**
 * Cấu hình thư viện Gson phục vụ tuần tự hóa (Serialization) và khử tuần tự hóa (Deserialization) dữ liệu JSON.
 * Xử lý đặc biệt cho kiểu dữ liệu Java 8 LocalDateTime và hỗ trợ đa hình (Polymorphism) cho lớp trừu tượng Item.
 */
public class GsonConfig {
    // Định dạng ngày giờ chuẩn ISO_LOCAL_DATE_TIME dùng chung cho cả Client và Server
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Tạo một đối tượng Gson được cấu hình tùy chỉnh để xử lý LocalDateTime và lớp trừu tượng Item.
     * @return Đối tượng Gson sẵn sàng sử dụng
     */
    public static Gson createGson() {
        return new GsonBuilder()
                // 1. Cấu hình chuyển đổi LocalDateTime sang chuỗi JSON (Serializer)
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(formatter)))
                
                // 2. Cấu hình chuyển đổi ngược từ chuỗi JSON sang đối tượng LocalDateTime (Deserializer)
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsString(), formatter))
                
                // 3. Giải quyết vấn đề đa hình của lớp trừu tượng Item (Deserializer)
                // Khi Gson nhận được một chuỗi JSON của sản phẩm (Item), nó không biết nên chuyển thành
                // Electronics, Art hay Vehicle vì Item là lớp trừu tượng.
                // Hàm này sẽ kiểm tra các thuộc tính đặc trưng trong chuỗi JSON để quyết định lớp cụ thể thích hợp.
                .registerTypeAdapter(Item.class, (JsonDeserializer<Item>) (json, typeOfT, context) -> {
                    JsonObject jsonObject = json.getAsJsonObject();
                    
                    // Nếu JSON có thuộc tính "warrantyMonths" -> Đây là sản phẩm điện tử (Electronics)
                    if (jsonObject.has("warrantyMonths")) {
                        return context.deserialize(json, Electronics.class);
                    // Nếu JSON có thuộc tính "artistName" -> Đây là tác phẩm nghệ thuật (Art)
                    } else if (jsonObject.has("artistName")) {
                        return context.deserialize(json, Art.class);
                    // Nếu JSON có thuộc tính "brand" -> Đây là phương tiện giao thông (Vehicle)
                    } else if (jsonObject.has("brand")) {
                        return context.deserialize(json, Vehicle.class);
                    }
                    
                    // Mặc định chuyển về Electronics nếu không khớp các thuộc tính đặc trưng trên
                    return context.deserialize(json, Electronics.class); 
                })
                .create();
    }
}