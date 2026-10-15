@echo off
title He Thong Quan Ly De Tai Sinh Vien - FIT THESIS
chcp 65001 > nul
cd /D "%~dp0"

echo ===================================================
echo   HỆ THỐNG QUẢN LÝ ĐỀ TÀI SINH VIÊN (SPRING BOOT)
echo ===================================================
echo.

:: 1. Kiểm tra và khởi động MySQL nếu chưa chạy
echo [1/3] Đang kiểm tra dịch vụ MySQL...
tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [OK] MySQL đang hoạt động.
) else (
    echo [!] MySQL chưa chạy. Đang tự động khởi động MySQL từ XAMPP...
    if exist "C:\xampp\mysql\bin\mysqld.exe" (
        start "" /B "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --standalone
        timeout /t 3 > nul
        echo [OK] Đã khởi động MySQL.
    ) else (
        echo [CẢNH BÁO] Vui lòng mở XAMPP và bật MySQL trước khi tiếp tục!
    )
)

:: 2. Khởi chạy ứng dụng Spring Boot
echo.
echo [2/3] Đang khởi chạy ứng dụng Spring Boot trên cổng 8088...
if exist "target\QuanLyDeTai-0.0.1-SNAPSHOT.jar" (
    start "" cmd /c "java -jar target\QuanLyDeTai-0.0.1-SNAPSHOT.jar"
) else (
    echo Đang biên dịch và chạy bằng Maven...
    start "" cmd /c "mvn spring-boot:run"
)

:: 3. Đợi vài giây và mở trình duyệt
echo.
echo [3/3] Đang mở trình duyệt web: http://localhost:8088 ...
timeout /t 6 > nul
start http://localhost:8088

echo.
echo ===================================================
echo   KHỞI CHẠY THÀNH CÔNG!
echo   Website: http://localhost:8088
echo   Tài khoản mẫu: TK001 (Trưởng khoa) / 123456
echo ===================================================
pause
