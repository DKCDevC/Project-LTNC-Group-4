# 🔨 Hệ thống Đấu giá Trực tuyến eBid - Nhóm 4 (LTNC)

Chào mừng Thầy/Cô đến với **eBid** - Hệ thống Đấu giá Trực tuyến thời gian thực (Real-time Online Auction Platform) được thiết kế và phát triển bởi **Nhóm 4** cho học phần **Lập trình nâng cao (LTNC)**. 

Dự án này là kết tinh của việc ứng dụng các nguyên lý lập trình hướng đối tượng (OOP) nâng cao, thiết kế giao thức mạng đồng bộ qua Socket TCP, mô hình cơ sở dữ liệu cục bộ an toàn và giao diện đồ họa người dùng hiện đại, tinh tế.

---

## 📑 Bảng mục lục
1. [Giới thiệu Dự án & Thành viên](#1-giới-thiệu-dự-án--thành-viên)
2. [Đặc tả Kỹ thuật (Tech Stack)](#2-đặc-tả-kỹ-thuật-tech-stack)
3. [Kiến trúc Đa Phân hệ (Multi-module Architecture)](#3-kiến-trúc-đa-phân-hệ-multi-module-architecture)
4. [Sơ đồ Lớp & Thiết kế OOP (Class Diagram)](#4-sơ-đồ-lớp--thiết-kế-oop-class-diagram)
5. [Ứng dụng các Mẫu thiết kế (Design Patterns)](#5-ứng-dụng-các-mẫu-thiết-kế-design-patterns)
6. [Mô hình Cơ sở dữ liệu SQLite (Database Schema)](#6-mô-hình-cơ-sở-dữ-liệu-sqlite-database-schema)
7. [Giao thức truyền thông Custom TCP Socket & JSON](#7-giao-thức-truyền-thông-custom-tcp-socket--json)
8. [Phân tích Thuật toán & Tính năng Nâng cao Cốt lõi](#8-phân-tích-thuật-toán--tính-năng-nâng-cao-cốt-lõi)
9. [Cơ chế Đóng gói Fat JAR & Giải quyết Lỗi JavaFX Runtime](#9-cơ-chế-đóng-gói-fat-jar--giải-quyết-lỗi-javafx-runtime)
10. [Hướng dẫn Biên dịch & Đóng gói (Build Fat JAR)](#10-hướng-dẫn-biên-dịch--đóng-gói-build-fat-jar)
11. [Hướng dẫn Khởi chạy Hệ thống](#11-hướng-dẫn-khởi-chạy-hệ-thống)
12. [Cẩm nang Kiểm thử Hệ thống (Test Cases & Scenarios)](#12-cẩm-nang-kiểm-thử-hệ-thống-test-cases--scenarios)
13. [Cấu trúc thư mục nguồn chi tiết (Directory Tree)](#13-cấu-trúc-thư-mục-nguồn-chi-tiết-directory-tree)
14. [Tài liệu Báo cáo & Video Demo](#14-tài-liệu-báo-cáo--video-demo)

---

## 1. Giới thiệu Dự án & Thành viên

**eBid** cung cấp một sàn đấu giá trực tuyến hoàn chỉnh, nơi Người bán (Seller) có thể đăng bán các sản phẩm và thiết lập thời gian đóng phiên, Người mua (Bidder) có thể duyệt tìm, xem biểu đồ biến động giá và đấu thầu cạnh tranh trực tiếp dưới sự điều phối nghiêm ngặt của Quản trị viên (Admin).

### Thành viên Nhóm phát triển (Group 4):
*   **Nguyễn Bá Dũng** (Trưởng Nhóm): 
    *   *Nhiệm vụ:* Thiết kế kiến trúc tổng thể, xây dựng Socket Server, thiết kế và tối ưu cơ sở dữ liệu SQLite, phát triển toàn bộ UI/UX bằng JavaFX & CSS tùy biến (Custom CSS Styling) cho Client, hiện thực hóa các tính năng cốt lõi (Auto-Bid, Anti-sniping, Real-time Graph).
*   **Lê Quốc Giang**: 
    *   *Nhiệm vụ:* Xây dựng hệ thống Command Handler tại Server, điều phối logic xử lý lệnh của người dùng, kiểm soát tính nhất quán của luồng nghiệp vụ.
*   **Đinh Đức Hiếu**: 
    *   *Nhiệm vụ:* Thiết kế layout các màn hình FXML, quản lý điều hướng giữa các phân vùng giao diện người dùng (Login, SignUp, Dashboard).
*   **Nguyễn Duy Anh**: 
    *   *Nhiệm vụ:* Phát triển phân lớp truy xuất cơ sở dữ liệu (DAO), viết các câu lệnh SQL tối ưu hóa việc truy vấn giao dịch đấu giá và đồng bộ dữ liệu.

---

## 2. Đặc tả Kỹ thuật (Tech Stack)

Hệ thống được phát triển dựa trên những công nghệ chuẩn mực và mạnh mẽ:
*   **Ngôn ngữ chính:** **Java 25** (Tận dụng các tính năng hiện đại như Pattern Matching, Records, Text Blocks nâng cao).
*   **Giao diện người dùng:** **JavaFX 26 (FXML + Vanilla CSS)**. Giao diện được thiết kế theo phong cách tối giản hiện đại (Modern Premium Minimalism) với hiệu ứng kính mờ (Glassmorphism), bóng đổ mềm, bảng màu HSL hài hòa và biểu đồ động.
*   **Cơ sở dữ liệu:** **SQLite 3.45+** (Embedded Serverless DB). Lưu trữ dữ liệu cục bộ dưới dạng tệp đơn độc `auction_system.db`. Không đòi hỏi người vận hành cấu hình máy chủ DB rời, tự động tạo cấu trúc và mồi dữ liệu mẫu (Seeding) ngay khi Server bắt đầu.
*   **Cơ chế truyền thông:** **TCP Socket** nguyên bản kết hợp đa luồng bất đồng bộ (Asynchronous Multi-threading). Dữ liệu được tuần tự hóa (Serialization) sang chuẩn JSON thông qua thư viện **Google Gson 2.10.1** để truyền tải qua mạng.
*   **Build Tool:** **Maven 3.8+** phục vụ quản lý thư viện phụ thuộc và cấu hình quy trình đóng gói đa phân hệ.

---

## 3. Kiến trúc Đa Phân hệ (Multi-module Architecture)

Dự án tuân thủ kiến trúc chia tách trách nhiệm (Separation of Concerns) với mô hình đa module Maven:

```
                  ┌──────────────────┐
                  │  auction-shared  │  <── Chứa Models & Tiện ích chung
                  └────────┬─────────┘
                           │ (Dependency)
             ┌─────────────┴─────────────┐
             ▼                           ▼
┌──────────────────┐       ┌──────────────────┐
│  auction-server  │       │  auction-client  │
│ (SQLite, Socket) │       │ (JavaFX, Client) │
└──────────────────┘       └──────────────────┘
```

*   **`auction-shared`**: Chứa các lớp định nghĩa thực thể mô hình dữ liệu (`User`, `Item`, `Auction`, `BidTransaction`,...) và cấu hình mạng dùng chung. Lớp này được hai module còn lại import làm thư viện phụ thuộc cốt lõi.
*   **`auction-server`**: Bộ não điều khiển trung tâm. Module này khởi chạy cổng TCP lắng nghe các Client, quản lý luồng cơ sở dữ liệu SQLite cục bộ, kiểm soát vòng đời các phiên đấu giá và điều phối các tác vụ ngầm tự động.
*   **`auction-client`**: Lớp biểu diễn tương tác. Module này kết nối Socket đến Server và tiếp nhận phản hồi để hiển thị lên màn hình JavaFX, đồng thời thu thập cử chỉ hành vi của người dùng truyền tải về máy chủ.

---

## 4. Sơ đồ Lớp & Thiết kế OOP (Class Diagram)

Hệ thống được mô hình hóa hướng đối tượng chặt chẽ. Toàn bộ thiết kế cấu trúc lớp được định nghĩa chi tiết trong tệp [online-auction-system.puml](file:///f:/LTNC/Bai7/Project-LTNC-Group-4/online-auction-system.puml) và đã được xuất thành tệp hình ảnh sơ đồ chất lượng cao [online-auction-system.png](file:///f:/LTNC/Bai7/Project-LTNC-Group-4/online-auction-system.png).

### Các điểm nhấn trong thiết kế đối tượng:
*   **Tính kế thừa & Đa hình (Inheritance & Polymorphism):** 
    *   Lớp cha trừu tượng `User` được mở rộng bởi các lớp con cụ thể: `Bidder` (Người thầu), `Seller` (Người bán), và `Admin` (Quản trị viên).
    *   Lớp cha trừu tượng `Item` phân tách thành các danh mục mặt hàng cụ thể: `Electronics` (Điện tử), `Vehicle` (Phương tiện), `Art` (Mỹ thuật), và `GeneralItem` (Tổng hợp).
*   **Tính đóng gói (Encapsulation):** Mọi thuộc tính dữ liệu nhạy cảm đều được bảo vệ nghiêm ngặt bằng phạm vi truy cập `private` và giao tiếp an toàn thông qua các phương thức Getter/Setter tương ứng.

---

## 5. Ứng dụng các Mẫu thiết kế (Design Patterns)

Nhóm 4 đã áp dụng thực tiễn nhiều mẫu thiết kế kinh điển giúp mã nguồn linh hoạt, dễ mở rộng và đạt chuẩn mực công nghiệp:

### 5.1. Singleton Pattern (Mẫu đơn thể)
Áp dụng cho các lớp quản lý bộ nhớ đệm và điều phối nghiệp vụ ở phía Server để đảm bảo tính duy nhất và nhất quán của dữ liệu trên toàn hệ thống:
*   `ItemManager.getInstance()`: Quản lý danh mục sản phẩm duy nhất trong bộ nhớ máy chủ.
*   `UserManager.getInstance()`: Điều phối thông tin các tài khoản đang hoạt động.
*   `AuctionManager.getInstance()`: Đảm bảo chỉ duy nhất một luồng điều phối đấu thầu và đồng hồ đếm ngược phiên đấu giá tồn tại.

### 5.2. Observer Pattern (Mẫu quan sát)
Giải quyết bài toán cập nhật thời gian thực không đồng bộ (Real-time Broadcast):
*   **Subject:** `AuctionSubject` (Được thực hiện bởi `AuctionSocketServer` hoặc `AuctionManager`).
*   **Observer:** `AuctionObserver` (Được định nghĩa qua interface và triển khai bởi `ClientHandler`).
*   **Nguyên lý:** Mỗi khi có một Client mới kết nối hoặc có hành động đấu giá mới phát sinh, hệ thống sẽ đăng ký `ClientHandler` đó vào danh sách quan sát. Khi có biến động giá hoặc thời gian, Server chỉ cần gọi hàm thông báo, toàn bộ các Client đang trực tuyến sẽ lập tức nhận được gói tin cập nhật giao diện mà không cần cơ chế kéo dữ liệu liên tục (Polling).

### 5.3. Command Pattern (Mẫu lệnh)
Được sử dụng làm nền tảng cho giao tiếp mạng Client-Server:
*   Định nghĩa interface `Command` có phương thức `execute(ClientHandler handler, String data)`.
*   Các lớp cụ thể thực thi lệnh: `LoginCommand`, `BidCommand`, `AutoBidCommand`, `CancelAutoBidCommand`, `AddItemCommand`, `DeleteItemCommand`, `AdminCommand`...
*   **Lợi ích:** Hệ thống định tuyến lệnh thông qua `CommandRouter` một cách linh hoạt, loại bỏ hoàn toàn các khối cấu trúc `if-else` lồng nhau phức tạp khi phân tích gói tin Socket, dễ dàng thêm lệnh mới mà không ảnh hưởng tới kiến trúc cũ.

### 5.4. DAO Pattern (Data Access Object)
Cô lập hoàn toàn tầng logic nghiệp vụ khỏi các câu lệnh truy vấn SQL thô:
*   Các lớp `UserDAO`, `ItemDAO`, `OrderDAO` đảm nhận việc mở kết nối JDBC, chuẩn bị câu lệnh `PreparedStatement`, thực thi truy vấn và ánh xạ kết quả (ORM thủ công) thành các đối tượng Java thuần túy.

---

## 6. Mô hình Cơ sở dữ liệu SQLite (Database Schema)

Cơ sở dữ liệu SQLite tự khởi tạo hoàn toàn cục bộ, tự động tạo lập 3 bảng nghiệp vụ chính với cấu trúc như sau:

### 6.1. Bảng Người dùng (`users`)
Lưu trữ định danh người dùng, mật khẩu đăng nhập, email và vai trò phân quyền:
```sql
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    email TEXT NOT NULL,
    role TEXT NOT NULL,         -- 'BIDDER', 'SELLER', 'ADMIN'
    isLocked INTEGER DEFAULT 0, -- 0: Hoạt động, 1: Bị khóa
    isVerified INTEGER DEFAULT 0
);
```

### 6.2. Bảng Sản phẩm (`items`)
Lưu trữ thông tin chi tiết của từng mặt hàng đấu giá và liên kết người bán:
```sql
CREATE TABLE IF NOT EXISTS items (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    starting_price REAL NOT NULL,
    start_time TEXT NOT NULL, -- Định dạng chuẩn ISO 8601 String
    end_time TEXT NOT NULL,
    type TEXT NOT NULL,       -- 'ELECTRONICS', 'VEHICLE', 'ART', 'GENERAL'
    extra_info TEXT,          -- Lưu trữ thông tin đặc thù (vd: Bảo hành, Số dặm...)
    seller_name TEXT,
    image_urls TEXT
);
```

### 6.3. Bảng Đơn hàng thắng cuộc (`orders`)
Lưu trữ thông tin hóa đơn khi phiên đấu giá kết thúc thành công:
```sql
CREATE TABLE IF NOT EXISTS orders (
    order_id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id TEXT,
    seller_name TEXT,
    bidder_name TEXT,
    final_price REAL,
    order_date DATE DEFAULT (DATE('now'))
);
```

### ⚙️ Cơ chế Data Seeding (Tự động nạp dữ liệu mồi)
Để dự án luôn trong trạng thái sẵn sàng chạy thử nghiệm mà không cần thao tác thủ công, `DBConnection.java` và `MainServer.java` thực hiện cơ chế mồi dữ liệu:
*   Nếu bảng `users` hoàn toàn trống, hệ thống sẽ tự động thêm vào 4 tài khoản thử nghiệm chuẩn (`Admin_01`, `Seller_01`, `Bidder_01`, `Bidder_02`).
*   Tại `MainServer`, hệ thống sử dụng cơ chế kiểm tra chống trùng lặp (`Anti-duplication check`) để tự động tạo mới **20 sản phẩm mẫu đa dạng** thuộc quyền sở hữu của người bán `user1` cùng các phiên đấu giá tương ứng.

---

## 7. Giao thức truyền thông Custom TCP Socket & JSON

Hệ thống giao tiếp thông qua Socket TCP bất đồng bộ. Mọi gói tin truyền tải qua mạng đều được đóng gói dưới định dạng JSON thống nhất:

### Định dạng gói tin gửi từ Client lên Server:
```json
{
  "command": "TÊN_LỆNH",
  "data": "NỘI_DUNG_DỮ_LIỆU_DẠNG_JSON_STRING_HOẶC_TEXT"
}
```

### 🔬 Ví dụ minh họa luồng truyền nhận gói tin thực tế:

#### 1. Yêu cầu Đặt giá thầu thường (BidCommand):
*   **Client gửi:**
    ```json
    {
      "command": "BID",
      "data": "{\"auctionId\":\"item_user1_1\",\"bidderName\":\"Bidder_01\",\"amount\":1200000.0}"
    }
    ```
*   **Server xử lý & kiểm tra bước giá:** 
    Nếu hợp lệ, Server cập nhật bộ nhớ đệm và SQLite, sau đó phát quảng bá (Broadcast) gói tin cập nhật tới **tất cả** Client đang online:
    ```json
    {
      "command": "AUCTION_UPDATE",
      "data": "{\"auctionId\":\"item_user1_1\",\"currentPrice\":1200000.0,\"bidsCount\":1,\"lastBidder\":\"Bidder_01\",\"endTime\":\"2026-05-21T12:00:00\"}"
    }
    ```

---

## 8. Phân tích Thuật toán & Tính năng Nâng cao Cốt lõi

Nhóm 4 đã nghiên cứu và triển khai thành công 3 thuật toán phức tạp phục vụ trực tiếp cho hoạt động thực tế của sàn đấu giá:

### 8.1. Thuật toán Đặt thầu tự động thông minh (Auto-Bidding)
Người mua có thể ủy quyền cho một robot AI cắm chốt đấu giá thay thế họ.
*   **Tham số đầu vào:** `Max Budget` (Giá tối đa chịu đựng) và `Bid Increment` (Bước nhảy thầu mong muốn).
*   **Nguyên lý vận hành:**
    1. Khi một Bidder khác đặt giá mới $P_{new}$ trên sản phẩm, hệ thống lập tức kích hoạt luồng quét kiểm tra xem sản phẩm đó có Robot Auto-Bid nào đang hoạt động hay không.
    2. Nếu có, robot sẽ tính toán mức giá phản công: $P_{counter} = P_{new} + \text{Bid Increment}$.
    3. Hệ thống kiểm tra điều kiện bảo vệ: Nếu $P_{counter} \le \text{Max Budget}$, hệ thống tự động thực thi đặt giá $P_{counter}$ thay cho người dùng sở hữu robot.
    4. **Tranh chấp đa Robot (Robot vs Robot):** Nếu có nhiều Robot Auto-Bid cùng cài cắm trên 1 sản phẩm, máy chủ sẽ thực hiện mô phỏng đấu thầu vòng lặp tức thời giữa các robot cho đến khi một bên chạm ngưỡng ngân sách tối đa, bên còn lại sẽ giành chiến thắng ở bước giá tối thiểu tiếp theo.

```
[Giá thầu mới từ Bidder X: P_new]
              │
              ▼
[Tìm kiếm Robot Auto-Bid đăng ký] ──(Không có)──> [Giữ nguyên giá P_new]
              │
          (Có robot)
              ▼
[Tính toán: P_counter = P_new + Increment]
              │
      ┌───────┴───────┐
      ▼               ▼
(P_counter <= Max)  (P_counter > Max)
      │               │
      ▼               ▼
[Tự động đặt thầu]  [Hủy Auto-Bid của Robot]
[Broadcast cập nhật] [Gửi cảnh báo vượt ngân sách]
```

### 8.2. Thuật toán Chống bắn tỉa phút chót (Anti-sniping)
Ngăn chặn hành vi đầu cơ phá hoại của các "Sniper" (người dùng đợi sát giây cuối cùng để đặt giá khiến người khác không kịp phản ứng).
*   **Nguyên lý vận hành:**
    1. Khi có một thầu hợp lệ được đặt, hệ thống tính toán khoảng thời gian còn lại đến khi đóng phiên: $\Delta t = T_{end} - T_{current}$.
    2. Nếu $\Delta t \le 30 \text{ giây}$, thuật toán sẽ tự động gia hạn thời gian kết thúc phiên đấu giá: $T_{new\_end} = T_{current} + 30 \text{ giây}$.
    3. Hệ thống lưu thời gian mới vào database SQLite và lập tức phát thông báo đổi lịch đóng phiên tới tất cả Client. Biểu đồ đếm ngược trên UI của toàn bộ người dùng sẽ tự động kéo dài thêm 30 giây một cách mượt mà.

### 8.3. Đảm bảo an toàn Đa luồng & Tranh chấp thầu (Concurrency Safety)
*   **Thách thức:** Khi hàng chục Client cùng nhấn nút đặt thầu ở cùng một mili-giây, máy chủ có thể bị tranh chấp tài nguyên (Race Condition), dẫn đến ghi nhận sai giá thầu hoặc một người mua bị mất lượt oan uổng.
*   **Giải pháp:** Mọi hành vi đặt thầu trong `AuctionManager` được bao bọc bởi từ khóa đồng bộ hóa **`synchronized`** trên chính đối tượng phiên đấu giá (`Auction`). Điều này ép các luồng mạng từ các Client khác nhau phải xếp hàng tuần tự. Máy chủ xử lý giá thầu đến trước, cập nhật giá mới làm mốc, và từ chối các giá thầu đến sau nếu chúng thấp hơn hoặc bằng mức giá mới cập nhật này.

---

## 9. Cơ chế Đóng gói Fat JAR & Giải quyết Lỗi JavaFX Runtime

Quy trình đóng gói sử dụng `maven-shade-plugin` nhằm xuất bản ra các tệp JAR hoàn chỉnh, độc lập và chạy được ngay.

### 9.1. Workaround tránh lỗi JavaFX Runtime: Lớp mồi `UILauncher`
Nhóm phát triển đã tách biệt hoàn toàn điểm khởi chạy máy ảo Java:
*   Thông thường, nếu chỉ định lớp chính của tệp JAR chạy thẳng vào `MainUI` (kế thừa từ `javafx.application.Application`), JVM khi quét Classpath lúc khởi động sẽ tìm kiếm cấu trúc mô-đun JavaFX và báo lỗi crash hệ thống ngay lập tức.
*   **Giải pháp xử lý:** Chúng tôi tạo lớp `network.UILauncher` không hề kế thừa từ bất cứ lớp nào của JavaFX:
    ```java
    package network;
    public class UILauncher {
        public static void main(String[] args){
            MainUI.main(args); // Gọi gián tiếp sang lớp ứng dụng thực tế
        }
    }
    ```
*   Khi chạy lệnh `java -jar`, JVM sẽ quét `UILauncher` và coi đây là một ứng dụng Java thuần túy (Standard Java App), cho phép khởi động máy ảo thành công. Lớp này sau đó mồi lệnh kích hoạt JavaFX Runtime tĩnh, lách qua rào cản kiểm soát mô-đun của JVM và khởi chạy giao diện đồ họa eBid mượt mà.

---

## 10. Hướng dẫn Biên dịch & Đóng gói (Build Fat JAR)

### Cách 1: Thao tác trực tiếp trên giao diện IntelliJ IDEA (Khuyên dùng)
Nếu máy của bạn chưa cấu hình biến môi trường Maven (`mvn`), hãy thực hiện qua IDE:
1.  Khởi chạy **IntelliJ IDEA** và mở thư mục dự án `Project-LTNC-Group-4`.
2.  Nhấp chuột vào tab công cụ **Maven** ở phía bên phải màn hình.
3.  Mở rộng mục gốc **online-auction-system** -> **Lifecycle**.
4.  Nhấp đúp chuột vào tác vụ **clean** để dọn dẹp các tệp tạm cũ.
5.  Nhấp đúp chuột vào tác vụ **package** để chạy quá trình biên dịch toàn diện.
6.  Đợi cho đến khi cửa sổ log biên dịch in ra dòng chữ báo thành công **`BUILD SUCCESS`**.

### Cách 2: Sử dụng Dòng lệnh (Terminal)
Nếu máy bạn đã cài sẵn Maven độc lập, mở Terminal tại thư mục gốc dự án và thực thi lệnh:
```bash
mvn clean package -DskipTests
```

---

## 11. Hướng dẫn Khởi chạy Hệ thống

Sau khi quá trình đóng gói hoàn tất, các tệp Fat JAR sẽ nằm ở thư mục `target` của các module Server và Client. Bạn tiến hành khởi chạy theo các bước bắt buộc sau:

### 🚀 Bước 1: Khởi động Server (Bắt buộc chạy trước)
Mở một cửa sổ Terminal (PowerShell/CMD) tại thư mục gốc của dự án và chạy:
```bash
java -jar auction-server/target/auction-server-1.0-SNAPSHOT.jar
```
*   **Chức năng ngầm tự thực hiện:** Server nạp driver SQLite, tạo tệp cơ sở dữ liệu `auction_system.db`, khởi tạo cấu trúc 3 bảng dữ liệu, tạo 4 tài khoản thử nghiệm mặc định, sinh ngẫu nhiên 20 sản phẩm mẫu đấu giá sinh động và bắt đầu lắng nghe cổng mạng Socket `9999`.

### 💻 Bước 2: Khởi động Client
Mở một cửa sổ Terminal mới (chạy song song và không tắt Terminal Server) và chạy:
```bash
java -jar auction-client/target/auction-client-1.0-SNAPSHOT.jar
```
*   **Kết quả:** Cửa sổ đăng nhập eBid hiện lên. Bạn có thể lặp lại bước 2 này trên nhiều Terminal khác nhau để mở nhiều Client đồng thời, mô phỏng quá trình tranh chấp đặt giá thầu thời gian thực giữa nhiều người mua.

---

## 12. Cẩm nang Kiểm thử Hệ thống (Test Cases & Scenarios)

Dưới đây là danh sách tài khoản thử nghiệm tích hợp sẵn trong mã nguồn và quy trình kiểm thử chi tiết từng chức năng nghiệp vụ để Thầy/Cô và bạn dễ dàng đánh giá:

### 🔑 Danh sách tài khoản có sẵn:
*   **Quản trị viên (Admin):** Username `Admin_01` | Mật khẩu `admin123`
*   **Người bán (Seller):** Username `Seller_01` (hoặc `user1`) | Mật khẩu `pass123`
*   **Người mua 1 (Bidder 1):** Username `Bidder_01` | Mật khẩu `pass123`
*   **Người mua 2 (Bidder 2):** Username `Bidder_02` | Mật khẩu `pass123`

---

### 🧪 Kịch bản kiểm thử 1: Đấu thầu thời gian thực & Chống bắn tỉa (Anti-sniping)
1.  Khởi động **Server** và mở **2 Client** song song.
2.  Trên **Client 1**, đăng nhập bằng tài khoản `Bidder_01`.
3.  Trên **Client 2**, đăng nhập bằng tài khoản `Bidder_02`.
4.  Cả hai tài khoản cùng truy cập vào chi tiết sản phẩm **"Sản phẩm mẫu 1"** (nhấp chọn sản phẩm ở trang chủ).
5.  **Bidder_01** nhập một mức giá cao hơn giá hiện tại và nhấn nút **Đặt giá (Place Bid)**.
    *   *Hiện tượng:* Mức giá mới và tên người dẫn đầu (`Bidder_01`) lập tức được cập nhật trên màn hình của cả **Bidder_01** và **Bidder_02** mà không cần tải lại trang. Biểu đồ đường Lịch sử biến động giá vẽ thêm một điểm mốc mới.
6.  **Kiểm tra Anti-sniping:** Quan sát đồng hồ đếm ngược của sản phẩm mẫu này trên màn hình. Khi đồng hồ chỉ thời gian còn lại dưới 30 giây, hãy cho **Bidder_02** nhấn đặt thầu giá mới hợp lệ.
    *   *Hiện tượng:* Thời gian kết thúc phiên đấu giá lập tức tự động cộng thêm và đặt mốc đếm ngược về lại 30 giây trên giao diện của cả hai người dùng, ngăn chặn tuyệt đối tình trạng phá hoại ở giây cuối.

---

### 🧪 Kịch bản kiểm thử 2: Đua thầu Robot AI tự động (Auto-Bidding)
1.  **Client 1** (`Bidder_01`) truy cập vào sản phẩm mẫu bất kỳ.
2.  Tại khu vực thiết lập **Tự động đấu giá (Auto Bid Setup)**:
    *   Nhập **Giá tối đa (Max Budget):** `2,000,000` ₫.
    *   Nhập **Bước giá nhảy (Increment):** `50,000` ₫.
    *   Nhấn **Kích hoạt tự động đặt thầu (Set Auto Bid)**.
3.  Trên **Client 2** (`Bidder_02`), thực hiện đặt giá thủ công ở mức `1,500,000` ₫.
    *   *Hiện tượng:* Ngay sau khi `Bidder_02` đặt thầu, Robot của `Bidder_01` lập tức tự động phản công bằng cách tăng giá thầu lên thành `1,550,000` ₫ (cộng thêm bước giá 50,000 ₫). Tên người dẫn đầu vẫn tiếp tục duy trì là `Bidder_01`.
4.  **Kiểm thử chạm ngưỡng giới hạn:** Cho `Bidder_02` đặt giá vượt qua ngưỡng giới hạn của robot, ví dụ: `2,100,000` ₫.
    *   *Hiện tượng:* Robot đấu giá của `Bidder_01` sẽ tự động ngừng hoạt động vì đã vượt quá ngân sách tối đa cho phép (`2,000,000` ₫). Quyền dẫn đầu thuộc về `Bidder_02` và Client 1 nhận được thông báo robot dừng thầu.

---

### 🧪 Kịch bản kiểm thử 3: Quản trị viên xử lý tranh chấp & Khóa tài khoản
1.  Mở một Client mới và đăng nhập bằng tài khoản Quản trị viên `Admin_01` (Mật khẩu: `admin123`).
2.  Giao diện **Admin Dashboard** xuất hiện.
3.  **Kiểm tra hủy đấu giá lỗi:**
    *   Chuyển sang tab **Quản lý Phiên Đấu giá (Auctions)**.
    *   Tìm "Sản phẩm mẫu 1" trong danh sách và nhấn nút **Hủy phiên (Cancel)**.
    *   *Hiện tượng:* Trạng thái phiên đấu giá chuyển lập tức thành `CANCELLED`. Toàn bộ các Client đang duyệt xem sản phẩm này sẽ nhận được thông báo phiên đấu giá đã bị Admin hủy bỏ.
4.  **Kiểm tra khóa tài khoản vi phạm:**
    *   Chuyển sang tab **Quản lý Người dùng (Users)**.
    *   Tìm kiếm người dùng `Bidder_02` và nhấn nút **Khóa tài khoản (Lock)**.
    *   *Hiện tượng:* Trạng thái chuyển thành `Locked`.
    *   Thử dùng một Client khác đăng nhập bằng `Bidder_02` (Mật khẩu: `pass123`).
    *   *Hiện tượng:* Đăng nhập thất bại và hệ thống hiển thị cảnh báo: *"Tài khoản của bạn đã bị khóa bởi Admin!"*.

---

### 🧪 Kịch bản kiểm thử 4: Người bán đăng hàng và theo dõi dòng tiền
1.  Đăng nhập bằng tài khoản Người bán `Seller_01` (Mật khẩu: `pass123`).
2.  Giao diện chuyên biệt của **Seller Dashboard** sẽ hiển thị.
3.  **Đăng bán sản phẩm mới:**
    *   Nhấn nút **Thêm sản phẩm mới (Add Product)**.
    *   Nhập tên: "Laptop Gaming Dell", chọn danh mục "Điện tử", nhập giá khởi điểm `15,000,000` ₫, mô tả sản phẩm và chọn ảnh minh họa.
    *   Thiết lập thời gian đấu giá (Ví dụ kết thúc sau 5 phút để thử nghiệm nhanh).
    *   Nhấn **Đăng bán (Submit)**.
    *   *Hiện tượng:* Sản phẩm mới lập tức xuất hiện trong danh sách hiển thị của Seller, đồng thời toàn bộ các Bidder đang online sẽ lập tức nhìn thấy sản phẩm này xuất hiện trên màn hình mua sắm của họ ở thời gian thực.
4.  **Xem doanh thu & Hóa đơn:** Khi phiên đấu giá kết thúc thành công (hết giờ đếm ngược và có người mua thắng thầu), Seller Dashboard sẽ tự động cập nhật thống kê doanh thu bán hàng và hiển thị thông tin hóa đơn thắng cuộc của người mua tương ứng để tiến hành giao dịch.

---

## 13. Cấu trúc thư mục nguồn chi tiết (Directory Tree)

Dưới đây là cây thư mục phân bố mã nguồn chi tiết của toàn bộ dự án eBid để Thầy/Cô dễ dàng theo dõi cấu trúc tổ chức mã nguồn:

```
Project-LTNC-Group-4/
│
├── pom.xml                               # Tệp cấu hình Maven gốc quản lý đa module
├── README.md                             # Hướng dẫn vận hành chi tiết siêu cấp
├── online-auction-system.puml            # File mã nguồn sơ đồ lớp PlantUML
├── online-auction-system.png             # Hình ảnh sơ đồ lớp trích xuất
│
├── auction-shared/                       # Module các lớp dùng chung giữa Client & Server
│   ├── pom.xml
│   └── src/main/java/
│       ├── models/
│       │   ├── User.java                 # Lớp cha trừu tượng đại diện người dùng
│       │   ├── Bidder.java               # Thực thể Người mua kế thừa User
│       │   ├── Seller.java               # Thực thể Người bán kế thừa User
│       │   ├── Admin.java                # Thực thể Quản trị viên kế thừa User
│       │   ├── Item.java                 # Lớp cha trừu tượng sản phẩm đấu giá
│       │   ├── Electronics.java          # Mặt hàng điện tử kế thừa Item
│       │   ├── Vehicle.java              # Mặt hàng xe cộ kế thừa Item
│       │   ├── Art.java                  # Mặt hàng mỹ thuật kế thừa Item
│       │   ├── GeneralItem.java          # Mặt hàng tổng hợp kế thừa Item
│       │   ├── Auction.java              # Mô tả phòng/phiên đấu giá
│       │   ├── BidTransaction.java       # Lưu vết giao dịch đặt giá thầu
│       │   ├── AutoBid.java              # Cài đặt Robot đặt thầu tự động
│       │   ├── AuctionStatus.java        # Enum trạng thái: ACTIVE, COMPLETED...
│       │   └── AuctionRequest.java       # Cấu trúc gói tin Socket giao tiếp
│       └── utils/
│           └── GsonConfig.java           # Định cấu hình tuần tự hóa Gson LocalDate
│
├── auction-server/                       # Module xử lý Máy chủ (Backend)
│   ├── pom.xml                           # Cấu hình đóng gói maven-shade-plugin Server
│   └── src/main/java/
│       ├── MainServer.java               # Entry Point chính khởi chạy Máy chủ
│       ├── dao/
│       │   ├── UserDAO.java              # Truy xuất bảng users trong SQLite
│       │   ├── ItemDAO.java              # Truy xuất bảng items trong SQLite
│       │   ├── OrderDAO.java             # Truy xuất bảng orders trong SQLite
│       │   ├── UserRepository.java
│       │   └── AdminJDBCRepository.java  # Tương tác SQLite dành cho tính năng Admin
│       ├── network/
│       │   ├── AuctionSocketServer.java  # Quản lý cổng nghe ServerSocket TCP
│       │   ├── ClientHandler.java        # Điều phối giao tiếp từng luồng Client riêng biệt
│       │   ├── AuctionObserver.java      # Giao diện phục vụ Observer Pattern
│       │   ├── AuctionSubject.java       # Điều hướng phát tin Broadcast
│       │   └── command/                  # Các Handler giải quyết lệnh từ mạng gửi lên
│       │       ├── Command.java          # Interface Command chuẩn
│       │       ├── CommandRouter.java    # Bộ định tuyến ánh xạ lệnh thông minh
│       │       ├── LoginCommand.java     # Xử lý đăng nhập / đăng ký
│       │       ├── BidCommand.java       # Xử lý thầu thường & Anti-sniping
│       │       ├── AutoBidCommand.java   # Kích hoạt robot Auto-Bid
│       │       ├── CancelAutoBidCommand.java # Hủy robot Auto-Bid
│       │       ├── AddItemCommand.java   # Xử lý Seller đăng sản phẩm mới
│       │       ├── DeleteItemCommand.java# Xử lý Seller gỡ sản phẩm
│       │       ├── GetSellerDashboardCommand.java
│       │       └── AdminCommand.java     # Xử lý khóa tài khoản / hủy phiên thầu
│       ├── services/
│       │   ├── UserManager.java          # Bộ nhớ cache & logic tài khoản
│       │   ├── ItemManager.java          # Quản lý vòng đời sản phẩm
│       │   ├── ItemFactory.java          # Factory Method tạo mới Item
│       │   ├── AuctionManager.java       # Đồng bộ hóa đấu thầu và đồng hồ đếm ngược
│       │   └── DashboardService.java     # Kết xuất thống kê đồ thị doanh thu
│       └── utils/
│           └── DBConnection.java         # Tiện ích kết nối & khởi tạo bảng SQLite
│
└── auction-client/                       # Module giao diện người dùng (Frontend)
    ├── pom.xml                           # Cấu hình đóng gói maven-shade-plugin Client
    └── src/main/
        ├── java/
        │   ├── network/
        │   │   ├── UILauncher.java       # Indirect Entry Point lách lỗi JavaFX Runtime
        │   │   └── MainUI.java           # Khởi tạo giao diện chính của JavaFX Application
        │   └── controllers/              # Điều khiển tương tác mã nguồn với FXML
        │       ├── LoginController.java  # Quản lý màn hình Đăng nhập
        │       ├── SignUpController.java # Quản lý màn hình Đăng ký tài khoản
        │       ├── DashboardController.java # Màn hình mua sắm chính của Bidder (Thầu tự động, Biểu đồ...)
        │       ├── SellerDashboardController.java # Giao diện bán hàng và xem doanh thu của Seller
        │       ├── AddProductController.java    # Cửa sổ đăng sản phẩm mới của Seller
        │       └── AdminDashboardController.java # Giao diện giám sát vận hành của Admin
        └── resources/
            └── views/                    # Các file cấu trúc bố cục giao diện XML
                ├── Login.fxml
                ├── SignUp.fxml
                ├── Dashboard.fxml
                ├── SellerDashboard.fxml
                ├── AddProduct.fxml
                └── admin_dashboard.fxml
```

---

## 14. Tài liệu Báo cáo & Video Demo

Thầy/Cô và các bạn có thể truy cập các tài nguyên báo cáo chi tiết và tài liệu hướng dẫn trực quan của dự án theo các đường dẫn bên dưới:

*   **Bản Báo Cáo Dự Án PDF chính thức:**
    [Tải xuống hoặc xem trực tuyến tại đây](https://drive.google.com/file/d/105jQROPfv_Ck2M7fVZclfoA-Q7Y3C6Qv/view?usp=sharing)
*   **Video Demo Hoạt động Hệ thống trực quan:**
    [Xem Video Demo tại đây](https://drive.google.com/file/d/105jQROPfv_Ck2M7fVZclfoA-Q7Y3C6Qv/view?usp=sharing)

---

Một lần nữa, **Nhóm 4** xin chân thành cảm ơn Thầy/Cô đã dành thời gian quý báu để trải nghiệm và chấm điểm ứng dụng **eBid**! Kính chúc Thầy/Cô nhiều sức khỏe và có những giờ phút thực nghiệm tuyệt vời cùng hệ thống của chúng em!
