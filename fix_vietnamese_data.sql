vUSE `quanly_detai_db`;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. Cập nhật Roles
UPDATE `roles` SET `description` = 'Trưởng khoa CNTT (Tạo đợt, xem toàn bộ đề tài, công bố điểm)' WHERE `id` = 1;
UPDATE `roles` SET `description` = 'Trưởng bộ môn (Duyệt đề tài của bộ môn)' WHERE `id` = 2;
UPDATE `roles` SET `description` = 'Giảng viên (Đăng ký đề tài, hướng dẫn SV, phản biện/chấm điểm)' WHERE `id` = 3;
UPDATE `roles` SET `description` = 'Sinh viên (Thành lập nhóm, đăng ký đề tài, nộp báo cáo, xem điểm)' WHERE `id` = 4;
UPDATE `roles` SET `description` = 'Quản trị viên hệ thống' WHERE `id` = 5;

-- 2. Cập nhật Departments
UPDATE `departments` SET `dept_name` = 'Công nghệ phần mềm', `description` = 'Bộ môn phụ trách các môn lập trình, kiến trúc phần mềm, công nghệ web/mobile' WHERE `id` = 1;
UPDATE `departments` SET `dept_name` = 'Hệ thống thông tin', `description` = 'Bộ môn phụ trách cơ sở dữ liệu, phân tích thiết kế hệ thống, ERP' WHERE `id` = 2;
UPDATE `departments` SET `dept_name` = 'Mạng máy tính & Truyền thông', `description` = 'Bộ môn phụ trách an toàn thông tin, mạng máy tính, điện toán đám mây' WHERE `id` = 3;
UPDATE `departments` SET `dept_name` = 'Khoa học máy tính', `description` = 'Bộ môn phụ trách AI, xử lý ảnh, học máy, cấu trúc dữ liệu thuật toán' WHERE `id` = 4;

-- 3. Cập nhật Users
UPDATE `users` SET `full_name` = 'GS. TS. Nguyễn Văn Khoa', `academic_rank` = 'GS. TS' WHERE `id` = 1;
UPDATE `users` SET `full_name` = 'PGS. TS. Trần Công Phần Mềm', `academic_rank` = 'PGS. TS' WHERE `id` = 2;
UPDATE `users` SET `full_name` = 'TS. Lê Hướng Dẫn', `academic_rank` = 'TS' WHERE `id` = 3;
UPDATE `users` SET `full_name` = 'ThS. Phạm Phản Biện', `academic_rank` = 'ThS' WHERE `id` = 4;
UPDATE `users` SET `full_name` = 'TS. Vũ Hội Đồng', `academic_rank` = 'TS' WHERE `id` = 5;
UPDATE `users` SET `full_name` = 'Nguyễn Nhóm Trưởng', `class_name` = 'DHCNTT17A' WHERE `id` = 6;
UPDATE `users` SET `full_name` = 'Trần Thành Viên 1', `class_name` = 'DHCNTT17A' WHERE `id` = 7;
UPDATE `users` SET `full_name` = 'Lê Thành Viên 2', `class_name` = 'DHCNTT17A' WHERE `id` = 8;
UPDATE `users` SET `full_name` = 'Quản trị viên IT' WHERE `id` = 9;

-- 4. Cập nhật Registration Periods
UPDATE `registration_periods` SET 
`period_name` = 'Đợt Khóa Luận Tốt Nghiệp Học Kỳ 1 2026-2027' 
WHERE `id` = 1;

-- 5. Cập nhật Topics
UPDATE `topics` SET 
`title` = 'Xây dựng Hệ thống Quản lý Đề tài Sinh viên sử dụng Spring Boot & Microservices',
`description` = 'Nghiên cứu quy trình quản lý đề tài khoa CNTT, tự động hóa từ khâu đăng ký, phân công GVHD/GVPB đến bảo vệ hội đồng.',
`requirements` = 'Thành thạo Java Spring Boot, MySQL/PostgreSQL, Spring Security JWT, Docker, Frontend React/Vue.'
WHERE `id` = 1;

UPDATE `topics` SET 
`title` = 'Ứng dụng Trí tuệ nhân tạo (AI) trong Hỗ trợ Phát hiện Đạo văn Đồ án Sinh viên',
`description` = 'Xây dựng mô hình xử lý ngôn ngữ tự nhiên (NLP) so khớp tài liệu báo cáo đồ án sinh viên để đánh giá mức độ tương đồng.',
`requirements` = 'Python, FastAPI, Spring Boot API Gateway, PyTorch/TensorFlow.'
WHERE `id` = 2;

-- 6. Cập nhật Student Groups
UPDATE `student_groups` SET `group_name` = 'Nhóm Alpha Dev' WHERE `id` = 1;

-- 7. Cập nhật Topic Submissions
UPDATE `topic_submissions` SET 
`title` = 'Báo cáo tiến độ đồ án giai đoạn 1',
`note` = 'Nhóm đã hoàn thành thiết kế DB và luồng đăng ký.'
WHERE `id` = 1;

-- 8. Cập nhật Councils
UPDATE `councils` SET 
`council_name` = 'Hội đồng Bảo vệ Khóa Luận Tốt Nghiệp - Bộ Môn CNPM 01',
`location` = 'Phòng B204 - Giảng đường B'
WHERE `id` = 1;

-- 9. Cập nhật Announcements
UPDATE `announcements` SET 
`title` = 'Thông báo mở đợt đăng ký Khóa Luận Tốt Nghiệp HK1 2026-2027',
`content` = 'Khoa CNTT thông báo bắt đầu mở hệ thống cho Giảng viên nộp đề tài từ ngày 01/09/2026 đến ngày 15/09/2026. Sinh viên chuẩn bị lập nhóm (tối đa 3 SV) để đăng ký đề tài từ ngày 16/09/2026.'
WHERE `id` = 1;

UPDATE `announcements` SET 
`title` = 'Quy định về việc nộp báo cáo và phản biện đề tài tốt nghiệp',
`content` = 'Tất cả các nhóm sinh viên lưu ý: Việc nộp báo cáo chỉ được thực hiện bởi Nhóm trưởng. Giảng viên hướng dẫn không được tham gia chấm điểm phản biện cho đề tài mình hướng dẫn.'
WHERE `id` = 2;

-- 10. Cập nhật Weekly Reports
UPDATE `weekly_progress_reports` SET
`tasks_completed` = 'Thu thập tài liệu và khảo sát quy trình quản lý đề tài khoa CNTT. Thiết kế cơ sở dữ liệu trên MySQL.',
`difficulties` = 'Chưa nắm rõ quy định về số lượng GV trong hội đồng phản biện.',
`next_week_plan` = 'Thiết kế khung dự án Spring Boot và các Entity JPA.',
`lecturer_feedback` = 'Ý tưởng tốt, cần bám sát quy định đồ án từ 3-5 GV cho hội đồng.'
WHERE `id` = 1;
