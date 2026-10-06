-- ====================================================================
-- Script vá lỗi dữ liệu: Bổ sung an toàn User 10 (SV005) và User 12 (GV004)
-- Sử dụng INSERT IGNORE để không ghi đè dữ liệu test tay đã có
-- ====================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. Bổ sung sinh viên ID 10 (Trưởng nhóm Nhóm 2)
INSERT IGNORE INTO `users` (`id`, `user_code`, `password`, `full_name`, `email`, `phone`, `academic_rank`, `class_name`, `department_id`) VALUES
(10, 'SV005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Trịnh Hoàng Long', 'sv005@student.edu.vn', '0912345682', NULL, 'DHCNTT17B', 4);

INSERT IGNORE INTO `user_roles` (`user_id`, `role_id`) VALUES
(10, 4);

-- 2. Bổ sung giảng viên ID 12 (GV004)
INSERT IGNORE INTO `users` (`id`, `user_code`, `password`, `full_name`, `email`, `phone`, `academic_rank`, `class_name`, `department_id`) VALUES
(12, 'GV004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ThS. Nguyễn Văn D', 'gv004@fit.edu.vn', '0906789012', 'ThS', NULL, 1);

INSERT IGNORE INTO `user_roles` (`user_id`, `role_id`) VALUES
(12, 3);
