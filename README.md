# Hệ Thống Quản Lý Đề Tài Nghiên Cứu Khoa Học & Khóa Luận Tốt Nghiệp

Dự án phát triển trên nền tảng **Spring Boot 3.2.5** (Java 21), **Spring Security**, **Spring Data JPA** và **MySQL 8.0**, giao diện **Thymeleaf + Bootstrap 5**.

---

## 1. Yêu Cầu Môi Trường
- **Java**: JDK 21 trở lên
- **MySQL**: MySQL Server 8.0 (Port mặc định: `3306`)
- **Maven**: Đã tích hợp sẵn qua `mvnw.cmd` (không cần cài riêng Maven)

---

## 2. Thiết Lập Cơ Sở Dữ Liệu (Single Source of Truth)

Nguồn cơ sở dữ liệu chuẩn và duy nhất của hệ thống được lưu tại file:
`database/full_database_dump.sql`

> [!IMPORTANT]
> - Hệ thống sử dụng chế độ `spring.jpa.hibernate.ddl-auto=validate` để bảo đảm tính toàn vẹn tuyệt đối của schema, trigger, stored procedure và dữ liệu mẫu.
> - Tuyệt đối không chuyển sang `update` hoặc `create` để tránh làm biến dạng schema cơ sở dữ liệu.

### Các bước khởi tạo MySQL:
1. Đăng nhập vào MySQL Server:
   ```bash
   mysql -u root -p
   ```
2. Tạo cơ sở dữ liệu `quanly_detai_db`:
   ```sql
   DROP DATABASE IF EXISTS quanly_detai_db;
   CREATE DATABASE quanly_detai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Import file `database/full_database_dump.sql`:
   ```bash
   mysql -u root -p quanly_detai_db < database/full_database_dump.sql
   ```

---

## 3. Cấu Hình Ứng Dụng (`src/main/resources/application.properties`)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/quanly_detai_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Chế độ kiểm tra schema chuẩn theo DB
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
```

---

## 4. Danh Sách Tài Khoản Đăng Nhập Mẫu

Tất cả tài khoản trong hệ thống sử dụng mật khẩu mặc định là: **`123456`**
(Tham khảo chi tiết tại `database/07_Danh_Sach_Tai_Khoan_Dang_Nhap_He_Thong.txt`)

| Phân hệ / Vai trò | Username | Mật khẩu | Chức vụ / Ghi chú |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `ADMIN01` | `123456` | Quản trị viên hệ thống |
| **Trưởng khoa** | `TK001` | `123456` | Trưởng khoa CNTT |
| **Giảng viên** | `GV001` | `123456` | Giảng viên hướng dẫn / Hội đồng |
| **Giảng viên** | `GV002` | `123456` | Giảng viên hướng dẫn / Phản biện |
| **Sinh viên** | `SV001` | `123456` | Sinh viên (Trưởng nhóm đề tài) |
| **Sinh viên** | `SV002` | `123456` | Sinh viên (Thành viên nhóm) |

---

## 5. Hướng Dẫn Chạy Dự Án & Kiểm Thử

### Chạy ứng dụng:
```powershell
.\mvnw.cmd spring-boot:run
```
Sau khi khởi động thành công, truy cập trình duyệt tại địa chỉ:
`http://localhost:8080` (hoặc `http://localhost:8080/login`)

### Chạy bộ kiểm thử (TDD Test Suite):
```powershell
.\mvnw.cmd test
```
Kiểm thử riêng phân hệ xác thực:
```powershell
.\mvnw.cmd test -Dtest=AuthenticationIntegrationTest
```
