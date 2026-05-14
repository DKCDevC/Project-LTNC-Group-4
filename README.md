# Hệ thống Đấu giá Trực tuyến eBid - Nhóm 4

## 1. Giới thiệu dự án
Hệ thống đấu giá trực tuyến eBid là giải pháp thương mại điện tử cho phép người dùng tham gia mua bán sản phẩm thông qua hình thức đấu giá công khai. 
- **Phạm vi**: Hệ thống hỗ trợ đa vai trò (Người mua, Người bán, Admin), đấu giá thời gian thực, tự động hóa việc đặt giá và quản lý phiên đấu giá một cách minh bạch, an toàn.

## 2. Công nghệ và Yêu cầu cài đặt
- **Ngôn ngữ**: Java 25.
- **Giao diện**: JavaFX 26 (FXML + CSS).
- **Cơ sở dữ liệu**: SQLite (Lưu trữ cục bộ, không cần cài đặt Server DB).
- **Build Tool**: Maven 3.8+.
- **Giao tiếp**: Socket (TCP) + JSON (Gson).
- **Yêu cầu môi trường**: Đã cài đặt JDK 25+ và Maven.

## 3. Cấu trúc Module
Dự án được thiết kế theo kiến trúc đa module (Multi-module Maven):
- `auction-shared`: Chứa các định nghĩa Class Models (Item, User, Auction,...) dùng chung.
- `auction-server`: Backend xử lý logic nghiệp vụ, quản lý Socket Server và Database.
- `auction-client`: Frontend JavaFX, xử lý giao diện và kết nối tới Server.

## 4. Vị trí file Executable JAR
Sau khi thực hiện lệnh build, các file JAR sẽ được tạo ra tại:
- **Server**: `auction-server/target/auction-server-1.0-SNAPSHOT.jar`
- **Client**: `auction-client/target/auction-client-1.0-SNAPSHOT.jar`

## 5. Hướng dẫn khởi chạy
Vui lòng tuân thủ thứ tự sau để đảm bảo hệ thống hoạt động chính xác:

### Bước 1: Build dự án
Mở terminal tại thư mục gốc và chạy:
```bash
mvn clean package -DskipTests
```

### Bước 2: Chạy Server (Bắt buộc chạy trước)
```bash
java -jar auction-server/target/auction-server-1.0-SNAPSHOT.jar
```

### Bước 3: Chạy Client
```bash
java -jar auction-client/target/auction-client-1.0-SNAPSHOT.jar
```

## 6. Danh sách chức năng đã hoàn thành
### Chức năng bắt buộc:
- [x] Đăng ký, Đăng nhập (Phân quyền Bidder/Seller/Admin).
- [x] Quản lý sản phẩm (Thêm/Sửa/Xóa, phân loại mặt hàng).
- [x] Đấu giá thời gian thực (Real-time Bidding).
- [x] Tự động đóng phiên và xác định người thắng khi hết giờ.
- [x] Xử lý lỗi, ngoại lệ (Giá thầu không hợp lệ, lỗi kết nối).
- [x] Giao diện người dùng hiện đại, responsive.

### Chức năng nâng cao:
- [x] **Auto-Bidding**: Tự động nâng giá thầu thông minh.
- [x] **Anti-sniping**: Tự động gia hạn phiên đấu giá khi có thầu phút cuối.
- [x] **Concurrent Bidding**: Xử lý nhiều người đặt giá cùng lúc an toàn.
- [x] **Real-time Price Curve**: Biểu đồ giá trực quan sinh động.
- [x] **Infinite Scroll**: Cuộn trang vô hạn cho danh sách sản phẩm.

## 7. Tài liệu và Video
- **Báo cáo PDF**: [Link báo cáo tại đây (Vui lòng cập nhật link)]
- **Video Demo**: [Link video tại đây (Vui lòng cập nhật link)]

---
**Nhóm phát triển (Group 4 - LTNC):**
1. **Nguyễn Bá Dũng** (Trưởng Nhóm)
2. **Lê Quốc Giang**
3. **Đinh Đức Hiếu**
4. **Nguyễn Duy Anh**

