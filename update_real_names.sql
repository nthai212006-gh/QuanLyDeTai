USE `quanly_detai_db`;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Cập nhật họ và tên chuẩn đời thực (Việt Nam)
UPDATE `users` SET `full_name` = 'GS. TS. Nguyễn Minh Tuấn' WHERE `user_code` = 'TK001';
UPDATE `users` SET `full_name` = 'PGS. TS. Trần Huy Hoàng' WHERE `user_code` = 'TBM001';
UPDATE `users` SET `full_name` = 'TS. Lê Anh Tuấn' WHERE `user_code` = 'GV001';
UPDATE `users` SET `full_name` = 'ThS. Phạm Quốc Hoàng' WHERE `user_code` = 'GV002';
UPDATE `users` SET `full_name` = 'TS. Vũ Đức Thắng' WHERE `user_code` = 'GV003';

UPDATE `users` SET `full_name` = 'Nguyễn Tuấn Anh' WHERE `user_code` = 'SV001';
UPDATE `users` SET `full_name` = 'Trần Minh Hoàng' WHERE `user_code` = 'SV002';
UPDATE `users` SET `full_name` = 'Lê Thanh Tùng' WHERE `user_code` = 'SV003';

UPDATE `users` SET `full_name` = 'Quản Trị Viên Hệ Thống' WHERE `user_code` = 'ADMIN';

-- Cập nhật tên nhóm sinh viên chuẩn chỉ
UPDATE `student_groups` SET `group_name` = 'Nhóm Nghiên Cứu Phần Mềm' WHERE `id` = 1;
