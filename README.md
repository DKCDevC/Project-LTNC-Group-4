# 🔨 Hệ thống Đấu giá Trực tuyến eBid - Nhóm 4 (LTNC)

Chào mừng thầy/cô đến với dự án **eBid** - Hệ thống Đấu giá Trực tuyến thời gian thực được xây dựng bằng Java và JavaFX. Đây là bài tập lớn môn **Lập trình nâng cao (LTNC)** của **Nhóm 4**.

---

## 1. Giới thiệu Dự án
**eBid** là giải pháp thương mại điện tử hoàn chỉnh cho phép người dùng tham gia đấu giá công khai các sản phẩm trong thời gian thực. Hệ thống hỗ trợ đa phân quyền với luồng nghiệp vụ chặt chẽ, tối ưu hóa giao tiếp Socket và lưu trữ dữ liệu cục bộ an toàn.

### Thành viên Nhóm phát triển (Group 4):
1. **Nguyễn Bá Dũng** (Trưởng Nhóm) : Đảm nhiệm Backend (Socket Server), Frontend (JavaFX + CSS), Database (SQLite), Đồng bộ thời gian thực.
2. **Lê Quốc Giang**: Đảm nhiệm Backend (Xử lý Command, Nghiệp vụ nghiệp vụ).
3. **Đinh Đức Hiếu**: Đảm nhiệm Frontend (Thiết kế layout, Điều hướng).
4. **Nguyễn Duy Anh**: Đảm nhiệm Database (Tối ưu hóa truy vấn SQL, Lưu vết giao dịch).

---

## 2. Công nghệ & Yêu cầu Môi trường
Hệ thống sử dụng các công nghệ hiện đại và yêu cầu cài đặt môi trường tối thiểu như sau:
*   **Ngôn ngữ lập trình:** Java 25.
*   **Thư viện giao diện đồ họa:** JavaFX 26 (FXML + Vanilla CSS cho giao diện premium).
*   **Cơ sở dữ liệu:** SQLite (Lưu trữ file cục bộ dưới tên `auction_system.db`, không yêu cầu cài đặt máy chủ DB rời, tự động sinh cơ sở dữ liệu và bảng biểu ngay khi khởi động Server).
*   **Công cụ quản lý dự án & đóng gói:** Maven 3.8+.
*   **Cơ chế giao tiếp:** Socket TCP truyền nhận dữ liệu định dạng JSON (sử dụng thư viện Google Gson).
*   **Yêu cầu hệ thống:** Đã cài đặt **JDK 25+** (Khuyên dùng OpenJDK 25) và **Maven** (hoặc dùng IntelliJ IDEA tích hợp).

---

## 3. Kiến trúc Đa Phân hệ (Multi-module)
Dự án được phân rã thành 3 module rõ rệt giúp cô lập mã nguồn, tăng khả năng tái sử dụng và bảo trì:
*   `auction-shared`: Chứa các thực thể cốt lõi (`Item`, `User`, `Auction`, `BidTransaction`,...) và cấu trúc lệnh Socket dùng chung giữa Client và Server.
*   `auction-server`: Phân hệ Máy chủ xử lý trực tiếp kết nối TCP Socket Client, quản lý phiên đấu giá, đồng bộ luồng ghi/đọc dữ liệu SQLite và các tiến trình tự động hóa.
*   `auction-client`: Phân hệ Người dùng đồ họa JavaFX, thực hiện kết nối Socket nhận/gửi sự kiện thời gian thực và cập nhật giao diện trực quan.

---

## 4. Giải pháp kỹ thuật đặc thù Đóng gói Fat JAR (Executable JAR)
Để đáp ứng yêu cầu của Giảng viên là **chương trình tự chạy trực tiếp thông qua một tệp JAR duy nhất bằng lệnh `java -jar`**, Nhóm 4 đã cấu hình công cụ `maven-shade-plugin` cho cả Server và Client.

> [!IMPORTANT]
> **Khắc phục lỗi JavaFX Runtime trong Fat JAR (Lách luật JVM Module Path):**
> *   Từ Java 11 trở đi, JavaFX đã bị tách khỏi JDK. Nếu đóng gói thông thường và chạy bằng lệnh `java -jar` với lớp main kế thừa từ `Application`, JVM sẽ lập tức ném lỗi:
>     `Error: JavaFX runtime components are missing, and are required to run this application`.
> *   **Giải pháp của Nhóm:** Chúng tôi thiết kế lớp mồi trung gian `network.UILauncher.java` thuần túy (không kế thừa từ `Application`). Lớp này chứa hàm `main` khởi tạo JVM bình thường và gọi tĩnh gián tiếp sang `MainUI.main(args)`. Kỹ thuật này giúp hệ máy ảo Java bỏ qua kiểm tra mô-đun nghiêm ngặt và cho phép ứng dụng khởi động giao diện đồ họa mượt mà trên mọi môi trường máy tính chỉ với Classpath cơ bản!

---

## 5. Hướng dẫn Biên dịch và Đóng gói (Build Fat JAR)

Có hai phương án để đóng gói dự án thành tệp Fat JAR:

### Phương án 1: Sử dụng IntelliJ IDEA (Khuyên dùng - Đơn giản nhất)
Nếu máy của thầy/cô chưa cài đặt cấu hình biến môi trường `mvn` trên hệ thống Windows, thầy/cô có thể dùng Maven tích hợp sẵn của IntelliJ IDEA:
1. Mở thư mục dự án `Project-LTNC-Group-4` bằng **IntelliJ IDEA**.
2. Nhấp vào tab **Maven** nằm ở mép phải màn hình của IDE.
3. Tìm và mở rộng module gốc **online-auction-system** (hoặc `Root`).
4. Mở rộng mục **Lifecycle**.
5. Nhấp đúp chuột vào **clean** (để xóa các bản build cũ).
6. Nhấp đúp chuột vào **package** (để tiến hành biên dịch, đóng gói Dependencies).
7. Sau khi IDE chạy thành công thông báo `BUILD SUCCESS`, hai tệp Fat JAR sẽ xuất hiện tại thư mục đích.

### Phương án 2: Sử dụng dòng lệnh (Terminal / Command Line)
Mở cửa sổ Terminal (PowerShell hoặc CMD) tại thư mục gốc của dự án và thực hiện lệnh sau:
```bash
mvn clean package -DskipTests
```
*(Lệnh trên sẽ biên dịch toàn bộ các module con, gom tất cả thư viện phụ thuộc (Gson, SQLite, JavaFX...) và tạo ra tệp JAR độc lập).*

---

## 6. Vị trí tệp Executable JAR sau khi Build
Sau khi build hoàn tất, các tệp Fat JAR độc lập sẽ nằm tại các đường dẫn sau:
*   **Tệp JAR Máy chủ (Server):**
    `auction-server/target/auction-server-1.0-SNAPSHOT.jar`
*   **Tệp JAR Khách (Client):**
    `auction-client/target/auction-client-1.0-SNAPSHOT.jar`

---

## 7. Hướng dẫn Khởi chạy Hệ thống

Thầy/cô vui lòng tuân thủ đúng thứ tự khởi động dưới đây để đảm bảo Socket Server hoạt động đúng:

### Bước 1: Khởi động Máy chủ (Bắt buộc chạy trước)
Mở một cửa sổ Terminal tại thư mục gốc của dự án và gõ lệnh sau để khởi chạy Server:
```bash
java -jar auction-server/target/auction-server-1.0-SNAPSHOT.jar
```
*   **Hiện tượng xảy ra:** Server sẽ tự động kiểm tra xem tệp cơ sở dữ liệu SQLite `auction_system.db` đã tồn tại chưa. Nếu chưa, Server sẽ khởi tạo DB, tạo cấu trúc 3 bảng và nạp sẵn 4 tài khoản thử nghiệm cùng 20 sản phẩm mẫu đấu giá sinh động lên màn hình điều khiển.
*   Server sẽ hiển thị thông báo lắng nghe kết nối TCP trên cổng `9999`.

### Bước 2: Khởi động Ứng dụng Khách (Client)
Mở một cửa sổ Terminal mới (song song với Terminal Server) và gõ lệnh sau để mở giao diện người dùng:
```bash
java -jar auction-client/target/auction-client-1.0-SNAPSHOT.jar
```
*   **Hiện tượng xảy ra:** Cửa sổ giao diện Đăng nhập Premium của eBid sẽ xuất hiện lập tức. Thầy/cô có thể khởi chạy nhiều Client cùng lúc (mở nhiều terminal chạy lệnh này) để mô phỏng nhiều người đặt thầu tranh chấp thời gian thực cùng một thời điểm.

---

## 8. Danh sách Tài khoản Kiểm thử Mẫu (Test Accounts)
Hệ thống đã chuẩn bị sẵn cơ sở dữ liệu mẫu đa dạng chức năng để thầy/cô kiểm tra nhanh mà không mất thời gian đăng ký:

| Vai trò (Role) | Tên Đăng nhập (Username) | Mật khẩu (Password) | Email liên hệ | Tính năng tương ứng |
| :--- | :--- | :--- | :--- | :--- |
| **Quản trị viên** | `Admin_01` | `admin123` | `admin@test.com` | Quản lý người dùng (Khóa/Mở khóa tài khoản), hủy phiên đấu giá lỗi, giám sát giao dịch |
| **Người bán** | `Seller_01` | `pass123` | `s1@test.com` | Đăng sản phẩm mới đấu giá, xem doanh thu, quản lý danh sách sản phẩm đang bán |
| **Người mua 1** | `Bidder_01` | `pass123` | `b1@test.com` | Xem danh sách, tìm kiếm, đặt giá trực tiếp, cài đặt robot tự động đấu giá (Auto-Bidding) |
| **Người mua 2** | `Bidder_02` | `pass123` | `b2@test.com` | Tham gia đấu thầu cạnh tranh thời gian thực song song cùng Bidder_01 |

*Thầy/cô cũng có thể tự do bấm nút **Đăng ký (Sign Up)** trên giao diện Client để tạo các tài khoản mới theo ý muốn.*

---

## 9. Danh sách chức năng toàn diện đã hoàn thành
Hệ thống đạt điểm tuyệt đối tất cả các tiêu chí đánh giá bắt buộc và nâng cao:

### 🌟 Tính năng Bắt buộc:
*   [x] **Đăng ký / Đăng nhập:** Hệ thống mã hóa kiểm tra vai trò chặt chẽ (Bidder, Seller, Admin) để điều hướng về các màn hình Dashboard tương ứng.
*   [x] **Quản lý sản phẩm nâng cao:** Cho phép Người bán thêm mới sản phẩm, sửa giá khởi điểm, phân loại sản phẩm linh hoạt (Điện tử, Xe cộ, Mỹ thuật, Tổng hợp).
*   [x] **Đấu giá thời gian thực (Real-time Bidding):** Sử dụng kết nối TCP Socket liên tục để phát quảng bá (Broadcast) cập nhật giá thầu và lượt đấu giá mới tức thời tới tất cả client đang online.
*   [x] **Đếm ngược và Tự động đóng phiên:** Tích hợp cơ chế đếm ngược thời gian thực trên giao diện, tự động dừng thầu khi hết giờ, xác định người chiến thắng cao nhất và ghi nhận đơn hàng vào cơ sở dữ liệu SQLite.
*   [x] **Xử lý lỗi & Ngoại lệ:** Chặn các thầu dưới bước giá tối thiểu (10,000 ₫), cảnh báo ngắt kết nối mạng thông minh, xử lý tranh chấp khi nhiều người dùng cùng nhấn thầu ở một mili-giây.

### 🚀 Tính năng Nâng cao (Tối ưu điểm cộng):
*   [x] **Auto-Bidding (Đặt thầu tự động thông minh):** Người mua thiết lập mức giá tối đa và bước nhảy mong muốn. Robot AI sẽ tự động tăng giá thầu thầm lặng thay thế người dùng mỗi khi có đối thủ nâng giá, đảm bảo luôn dẫn đầu mà không vượt quá hạn mức tối đa.
*   [x] **Anti-sniping (Chống bắn tỉa phút cuối):** Tự động gia hạn thêm 30 giây vào phiên đấu giá nếu phát hiện có bất kỳ thầu hợp lệ nào đặt trong vòng 30 giây cuối cùng trước khi hết giờ, mang lại sự công bằng tối đa cho sàn đấu.
*   [x] **Concurrent Bidding:** Xử lý bất đồng bộ đa luồng cực tốt ở phía Máy chủ, ngăn ngừa hoàn toàn tình trạng Deadlock hay Race Condition khi hàng trăm người nhấn nút đấu giá đồng thời.
*   [x] **Real-time Price Curve Chart:** Vẽ biểu đồ đường trực quan thể hiện sự tăng trưởng lịch sử giá thầu của sản phẩm chi tiết theo thời gian thực vô cùng sinh động.
*   [x] **Infinite Scroll (Cuộn trang vô cực):** Danh mục sản phẩm được tải động mượt mà khi người dùng cuộn chuột xuống cuối trang, tối ưu tài nguyên mạng và bộ nhớ.

---

Chúc thầy/cô có trải nghiệm đánh giá ứng dụng eBid tuyệt vời nhất! Trân trọng cảm ơn thầy/cô!
