// 1. Khai báo package: Nằm trong phân hệ tiện ích dùng chung (shared utils) của cả Client và Server.
package utils;

// 2. Import các công cụ phân giải JSON mạnh mẽ từ thư viện Google Gson
import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import models.*;

/**
 * Cấu hình thư viện Gson phục vụ tuần tự hóa (Serialization) và khử tuần tự hóa (Deserialization) dữ liệu JSON.
 * Xử lý đặc biệt cho kiểu dữ liệu Java 8 LocalDateTime và hỗ trợ đa hình (Polymorphism) cho lớp trừu tượng Item.
 * 
 * Ý nghĩa thiết kế:
 * - Serialization (Tuần tự hóa): Biến đổi cây đối tượng trong Heap RAM thành chuỗi ký tự dẹt (Flat JSON String) 
 *   để truyền đi qua luồng mạng socket (Blocking IO stream).
 * - Deserialization (Khử tuần tự hóa): Đọc chuỗi JSON nhận được từ socket mạng và tái dựng lại thành đối tượng Java trên RAM.
 * - Custom TypeAdapter: Giải quyết bài toán ép kiểu dữ liệu phức tạp của Java 8 LocalDateTime và vấn đề đa hình đối tượng 
 *   cha trừu tượng `Item` khi chuyển ngữ JSON.
 */
public class GsonConfig {
    // Định dạng ngày giờ chuẩn ISO_LOCAL_DATE_TIME dùng chung cho cả Client và Server (Ví dụ: 2026-05-18T11:00:00)
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Tạo một đối tượng Gson được cấu hình tùy chỉnh để xử lý LocalDateTime và lớp trừu tượng Item.
     * Áp dụng Builder Design Pattern (`GsonBuilder`) để cấu hình linh hoạt từng TypeAdapter.
     * 
     * @return Đối tượng Gson sẵn sàng sử dụng được tối ưu hóa
     */
    public static Gson createGson() {
        return new GsonBuilder()
                // 1. Cấu hình chuyển đổi LocalDateTime sang chuỗi JSON (Serializer):
                // Java 8 LocalDateTime không phải là một kiểu nguyên thủy (primitive), do đó Gson thô mặc định 
                // sẽ phân giải nó thành một cụm đối tượng ngày, tháng, năm vô cùng phức tạp và cồng kềnh.
                // Lambda ở đây đóng vai trò ép kiểu LocalDateTime sang một phần tử chuỗi dẹt (`JsonPrimitive`) dạng ISO.
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(formatter)))
                
                // 2. Cấu hình chuyển đổi ngược từ chuỗi JSON sang đối tượng LocalDateTime (Deserializer):
                // Đọc chuỗi JSON và phân tích ngược lại (parse) thành thực thể LocalDateTime tương thích Java 8.
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsString(), formatter))
                
                // 3. Giải quyết vấn đề đa hình của lớp trừu tượng Item (Polymorphic Deserialization adapter):
                // Khi Gson nhận được một chuỗi JSON của sản phẩm (Item), nó không thể tự động khởi tạo đối tượng 
                // vì `Item` là một lớp trừu tượng (abstract class) - không thể gọi từ khóa `new Item()`.
                // Adapter này hoạt động như một bộ giải mã đặc hữu (discriminator-based parser):
                // Nó quét thủ công (inspect) các thuộc tính đặc trưng trong chuỗi JSON để xác định đúng kiểu thực thể cụ thể.
                .registerTypeAdapter(Item.class, (JsonDeserializer<Item>) (json, typeOfT, context) -> {
                    JsonObject jsonObject = json.getAsJsonObject();
                    
                    // Nếu JSON có thuộc tính "warrantyMonths" -> Đây là sản phẩm điện tử (Electronics class)
                    if (jsonObject.has("warrantyMonths")) {
                        return context.deserialize(json, Electronics.class);
                    // Nếu JSON có thuộc tính "artistName" -> Đây là tác phẩm nghệ thuật (Art class)
                    } else if (jsonObject.has("artistName")) {
                        return context.deserialize(json, Art.class);
                    // Nếu JSON có thuộc tính "brand" -> Đây là phương tiện giao thông (Vehicle class)
                    } else if (jsonObject.has("brand")) {
                        return context.deserialize(json, Vehicle.class);
                    }
                    
                    // Mặc định chuyển về Electronics nếu không khớp các thuộc tính đặc trưng trên để phòng thủ lỗi
                    return context.deserialize(json, Electronics.class); 
                })
                .create();
    }
}