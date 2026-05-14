# Hệ thống Đấu giá Trực tuyến eBid (Project-LTNC-Group-4)

Chào mừng bạn đến với hệ thống đấu giá trực tuyến eBid. Đây là đồ án môn học Lập trình nâng cao, được phát triển bởi Nhóm 4.

## 📌 Tổng quan dự án
Hệ thống bao gồm 3 module chính:
- **auction-shared**: Chứa các mô hình dữ liệu (Models) dùng chung cho cả Client và Server.
- **auction-server**: Xử lý logic đấu giá, quản lý người dùng và lưu trữ cơ sở dữ liệu (SQLite).
- **auction-client**: Giao diện người dùng (JavaFX) dành cho người mua và người bán.

## 🛠 Yêu cầu hệ thống
- **Java**: JDK 25 hoặc mới hơn.
- **Maven**: 3.8+
- **Hệ điều hành**: Windows/macOS/Linux.

## 🏗 Cách Build chương trình (Fat JAR)
Dự án sử dụng `maven-shade-plugin` để đóng gói toàn bộ thư viện vào một file JAR duy nhất (Uber JAR).

1. Mở terminal tại thư mục gốc của dự án.
2. Chạy lệnh build:
   ```bash
   mvn clean package
   ```
3. Sau khi build thành công, các file executable JAR sẽ nằm ở:
   - Server: `auction-server/target/auction-server-1.0-SNAPSHOT.jar`
   - Client: `auction-client/target/auction-client-1.0-SNAPSHOT.jar`

## 🚀 Cách Chạy chương trình

### Bước 1: Khởi động Server
Mở terminal mới và chạy lệnh:
```bash
java -jar auction-server/target/auction-server-1.0-SNAPSHOT.jar
```
*Lưu ý: Server cần được chạy trước để Client có thể kết nối.*

### Bước 2: Khởi động Client
Mở terminal khác và chạy lệnh:
```bash
java -jar auction-client/target/auction-client-1.0-SNAPSHOT.jar
```

## 🔑 Tài khoản mặc định (Test)
Bạn có thể dùng các tài khoản sau để trải nghiệm:
- **Người bán (Seller)**: `Seller_01` / `pass123`
- **Người mua (Bidder)**: `bidder1` / `password123`
- **Quản trị viên (Admin)**: `admin` / `admin123`

## ✨ Các tính năng nổi bật
- **Cuộn vô hạn (Infinite Scroll)**: Tự động tải thêm sản phẩm khi cuộn trang.
- **Biểu đồ doanh thu**: Phân tích hiệu quả kinh doanh cho người bán.
- **Tự động đấu giá (Auto-bid)**: Đặt giá thầu tự động thông minh.
- **Giao diện Modern UI**: Thiết kế theo phong cách hiện đại, responsive.

---
**Nhóm phát triển:** Group 4 - LTNC
**Hạn nộp:** 31/05/2026
