-- ====================================================================
-- TOÀN BỘ MÃ NGUỒN CƠ SỞ DỮ LIỆU MYSQL: QUANLY_DETAI_DB
-- Hệ Thống Quản Lý Đề Tài Sinh Viên - Khoa Công Nghệ Thông Tin (Fit-Thesis)
-- Bao gồm: Database Init, 19 Bảng, 2 Views, 3 Stored Procedures, 5 Triggers, Seed Data
-- ====================================================================

-- ====================================================================
-- FIT THESIS PORTAL - DATABASE INITIALIZATION SCRIPT
-- ====================================================================
DROP DATABASE IF EXISTS `quanly_detai_db`;
CREATE DATABASE IF NOT EXISTS `quanly_detai_db`
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE `quanly_detai_db`;

-- Thiết lập phiên làm việc hỗ trợ tiếng Việt có dấu đầy đủ
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET foreign_key_checks = 0;


CREATE TABLE `roles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `role_name` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `departments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `dept_code` VARCHAR(20) NOT NULL UNIQUE,
    `dept_name` VARCHAR(150) NOT NULL,
    `description` TEXT NULL,
    `head_of_dept_id` BIGINT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_code` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `phone` VARCHAR(20) NULL,
    `academic_rank` VARCHAR(50) NULL,
    `class_name` VARCHAR(50) NULL,
    `department_id` BIGINT NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_users_dept` FOREIGN KEY (`department_id`) 
        REFERENCES `departments` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) 
        REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) 
        REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `registration_periods` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `period_name` VARCHAR(150) NOT NULL,
    `academic_year` VARCHAR(20) NOT NULL,
    `semester` INT NOT NULL CHECK (`semester` IN (1, 2)),
    `period_type` ENUM('COURSE_PROJECT', 'RESEARCH', 'GRADUATION_THESIS', 'INTERNSHIP') NOT NULL,
    `topic_submission_start` DATETIME NOT NULL,
    `topic_submission_end` DATETIME NOT NULL,
    `student_registration_start` DATETIME NOT NULL,
    `student_registration_end` DATETIME NOT NULL,
    `review_deadline` DATETIME NULL,
    `defense_date` DATE NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_by_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_periods_creator` FOREIGN KEY (`created_by_id`) 
        REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `topics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `topic_code` VARCHAR(50) NOT NULL UNIQUE,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT NOT NULL,
    `requirements` TEXT NULL,
    `department_id` BIGINT NOT NULL,
    `period_id` BIGINT NOT NULL,
    `max_groups` INT NOT NULL DEFAULT 1,
    `status` ENUM('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'ASSIGNED', 'COMPLETED') 
        NOT NULL DEFAULT 'PENDING_APPROVAL',
    `reject_reason` TEXT NULL,
    `created_by_lecturer_id` BIGINT NOT NULL,
    `approved_by_id` BIGINT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_topics_dept` FOREIGN KEY (`department_id`) 
        REFERENCES `departments` (`id`),
    CONSTRAINT `fk_topics_period` FOREIGN KEY (`period_id`) 
        REFERENCES `registration_periods` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_topics_creator` FOREIGN KEY (`created_by_lecturer_id`) 
        REFERENCES `users` (`id`),
    CONSTRAINT `fk_topics_approver` FOREIGN KEY (`approved_by_id`) 
        REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `topic_supervisors` (
    `topic_id` BIGINT NOT NULL,
    `lecturer_id` BIGINT NOT NULL,
    `is_primary` BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (`topic_id`, `lecturer_id`),
    CONSTRAINT `fk_ts_topic` FOREIGN KEY (`topic_id`) 
        REFERENCES `topics` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ts_lecturer` FOREIGN KEY (`lecturer_id`) 
        REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `student_groups` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_name` VARCHAR(100) NOT NULL,
    `period_id` BIGINT NOT NULL,
    `topic_id` BIGINT NULL,
    `leader_id` BIGINT NOT NULL,
    `status` ENUM('PENDING_APPROVAL', 'ASSIGNED', 'IN_PROGRESS', 'DEFENDING', 'COMPLETED', 'DISQUALIFIED') 
        NOT NULL DEFAULT 'PENDING_APPROVAL',
    `final_score` DECIMAL(4,2) NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_groups_period` FOREIGN KEY (`period_id`) 
        REFERENCES `registration_periods` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_groups_topic` FOREIGN KEY (`topic_id`) 
        REFERENCES `topics` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_groups_leader` FOREIGN KEY (`leader_id`) 
        REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `group_members` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `period_id` BIGINT NOT NULL,
    `role_in_group` ENUM('LEADER', 'MEMBER') NOT NULL DEFAULT 'MEMBER',
    `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_period` (`student_id`, `period_id`),
    CONSTRAINT `fk_gm_group` FOREIGN KEY (`group_id`) 
        REFERENCES `student_groups` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_gm_student` FOREIGN KEY (`student_id`) 
        REFERENCES `users` (`id`),
    CONSTRAINT `fk_gm_period` FOREIGN KEY (`period_id`) 
        REFERENCES `registration_periods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `topic_submissions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `submitted_by_id` BIGINT NOT NULL,
    `submission_type` ENUM('PROPOSAL', 'PROGRESS', 'FINAL_THESIS', 'SOURCE_CODE') NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `file_url` VARCHAR(255) NULL,
    `note` TEXT NULL,
    `submission_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_sub_group` FOREIGN KEY (`group_id`) 
        REFERENCES `student_groups` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sub_user` FOREIGN KEY (`submitted_by_id`) 
        REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `councils` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `council_code` VARCHAR(50) NOT NULL UNIQUE,
    `council_name` VARCHAR(150) NOT NULL,
    `period_id` BIGINT NOT NULL,
    `defense_date` DATETIME NULL,
    `location` VARCHAR(150) NULL,
    `status` ENUM('CREATED', 'IN_SESSION', 'COMPLETED') NOT NULL DEFAULT 'CREATED',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_councils_period` FOREIGN KEY (`period_id`) 
        REFERENCES `registration_periods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `council_members` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `council_id` BIGINT NOT NULL,
    `lecturer_id` BIGINT NOT NULL,
    `position` ENUM('CHAIR', 'SECRETARY', 'MEMBER', 'REVIEWER') NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_council_lecturer` (`council_id`, `lecturer_id`),
    CONSTRAINT `fk_cm_council` FOREIGN KEY (`council_id`) 
        REFERENCES `councils` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cm_lecturer` FOREIGN KEY (`lecturer_id`) 
        REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `council_topics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `council_id` BIGINT NOT NULL,
    `group_id` BIGINT NOT NULL UNIQUE,
    `reviewer_lecturer_id` BIGINT NULL,
    `defense_order` INT NOT NULL DEFAULT 1,
    `final_council_score` DECIMAL(4,2) NULL,
    `is_published` BOOLEAN NOT NULL DEFAULT FALSE,
    `published_at` DATETIME NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_ct_council` FOREIGN KEY (`council_id`) 
        REFERENCES `councils` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ct_group` FOREIGN KEY (`group_id`) 
        REFERENCES `student_groups` (`id`),
    CONSTRAINT `fk_ct_reviewer` FOREIGN KEY (`reviewer_lecturer_id`) 
        REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `evaluations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `council_topic_id` BIGINT NOT NULL,
    `evaluator_lecturer_id` BIGINT NOT NULL,
    `evaluation_type` ENUM('INSTRUCTOR', 'REVIEWER', 'COUNCIL_MEMBER', 'COUNCIL_CHAIR') NOT NULL,
    `criteria_1_score` DECIMAL(4,2) NULL COMMENT 'Báo cáo & Tài liệu (Hệ số 30%)',
    `criteria_2_score` DECIMAL(4,2) NULL COMMENT 'Sản phẩm & Demo công nghệ (Hệ số 40%)',
    `criteria_3_score` DECIMAL(4,2) NULL COMMENT 'Thuyết trình & Phản biện (Hệ số 30%)',
    `total_score` DECIMAL(4,2) NOT NULL,
    `comments` TEXT NULL,
    `graded_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_evaluator_topic` (`council_topic_id`, `evaluator_lecturer_id`),
    CONSTRAINT `fk_eval_ct` FOREIGN KEY (`council_topic_id`) 
        REFERENCES `council_topics` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_eval_lecturer` FOREIGN KEY (`evaluator_lecturer_id`) 
        REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `weekly_progress_reports` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `week_number` INT NOT NULL,
    `tasks_completed` TEXT NOT NULL,
    `issues_faced` TEXT NULL,
    `next_week_plan` TEXT NOT NULL,
    `report_file_url` VARCHAR(255) NULL,
    `submitted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `lecturer_feedback` TEXT NULL,
    `feedback_status` ENUM('PENDING', 'ACCEPTED', 'NEEDS_REVISION') NOT NULL DEFAULT 'PENDING',
    `feedback_at` DATETIME NULL,
    `reviewed_by_id` BIGINT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_week` (`group_id`, `week_number`),
    CONSTRAINT `fk_wpr_group` FOREIGN KEY (`group_id`) 
        REFERENCES `student_groups` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_wpr_reviewer` FOREIGN KEY (`reviewed_by_id`) 
        REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `announcements` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `author_id` BIGINT NOT NULL,
    `is_pinned` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_ann_author` FOREIGN KEY (`author_id`) 
        REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `notifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `link` VARCHAR(255) NULL,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) 
        REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `ai_analysis_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `action_type` VARCHAR(50) NOT NULL COMMENT 'RECOMMEND, SIMILARITY_CHECK, SUMMARIZE',
    `query_input` TEXT NOT NULL,
    `ai_output` TEXT NOT NULL,
    `similarity_score` DECIMAL(5,2) NULL,
    `period_id` BIGINT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_ai_user` FOREIGN KEY (`user_id`) 
        REFERENCES `users` (`id`),
    CONSTRAINT `fk_ai_period` FOREIGN KEY (`period_id`) 
        REFERENCES `registration_periods` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `system_audit_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NULL,
    `action` VARCHAR(100) NOT NULL,
    `entity_name` VARCHAR(50) NOT NULL,
    `entity_id` BIGINT NULL,
    `details` TEXT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`) 
        REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE OR REPLACE VIEW `v_topic_registration_status` AS
SELECT 
    t.id AS topic_id,
    t.topic_code,
    t.title AS topic_title,
    d.dept_name AS department_name,
    rp.period_name,
    u.full_name AS primary_supervisor,
    t.status AS topic_status,
    sg.id AS assigned_group_id,
    sg.group_name AS assigned_group_name
FROM `topics` t
LEFT JOIN `departments` d ON t.department_id = d.id
LEFT JOIN `registration_periods` rp ON t.period_id = rp.id
LEFT JOIN `users` u ON t.created_by_lecturer_id = u.id
LEFT JOIN `student_groups` sg ON sg.topic_id = t.id;


CREATE OR REPLACE VIEW `v_council_grading_summary` AS
SELECT 
    c.id AS council_id,
    c.council_name,
    c.council_code,
    ct.defense_order,
    sg.group_name,
    t.title AS topic_title,
    rev.full_name AS reviewer_name,
    ct.final_council_score,
    ct.is_published,
    COUNT(e.id) AS total_graded_members
FROM `councils` c
JOIN `council_topics` ct ON ct.council_id = c.id
JOIN `student_groups` sg ON ct.group_id = sg.id
JOIN `topics` t ON sg.topic_id = t.id
LEFT JOIN `users` rev ON ct.reviewer_lecturer_id = rev.id
LEFT JOIN `evaluations` e ON e.council_topic_id = ct.id
GROUP BY 
    ct.id, c.id, c.council_name, c.council_code, ct.defense_order, 
    sg.group_name, t.title, rev.full_name, ct.final_council_score, ct.is_published;


DELIMITER $$
DROP PROCEDURE IF EXISTS `sp_calculate_final_council_score`$$
CREATE PROCEDURE `sp_calculate_final_council_score`(
    IN p_council_topic_id BIGINT,
    OUT p_final_score DECIMAL(4,2)
)
BEGIN
    DECLARE v_avg DECIMAL(4,2);
    DECLARE v_count INT;

    -- Tính số lượng GV đã chấm và điểm trung bình cộng
    SELECT COUNT(*), AVG(total_score) 
    INTO v_count, v_avg
    FROM `evaluations`
    WHERE `council_topic_id` = p_council_topic_id;

    IF v_count > 0 THEN
        SET p_final_score = ROUND(v_avg, 2);
        
        -- Cập nhật vào bảng phân công đề tài hội đồng
        UPDATE `council_topics` 
        SET `final_council_score` = p_final_score 
        WHERE `id` = p_council_topic_id;

        -- Đồng bộ vào bảng nhóm sinh viên
        UPDATE `student_groups` sg
        JOIN `council_topics` ct ON ct.group_id = sg.id
        SET sg.final_score = p_final_score
        WHERE ct.id = p_council_topic_id;
    ELSE
        SET p_final_score = NULL;
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP FUNCTION IF EXISTS `fn_is_grading_started`$$
CREATE FUNCTION `fn_is_grading_started`(p_council_id BIGINT)
RETURNS BOOLEAN
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_count INT DEFAULT 0;
    SELECT COUNT(*) INTO v_count
    FROM `evaluations` e
    JOIN `council_topics` ct ON e.council_topic_id = ct.id
    WHERE ct.council_id = p_council_id;
    RETURN v_count > 0;
END$$
DELIMITER ;


DELIMITER $$
DROP PROCEDURE IF EXISTS `sp_publish_council_results`$$
CREATE PROCEDURE `sp_publish_council_results`(
    IN p_council_topic_id BIGINT
)
BEGIN
    DECLARE v_final_score DECIMAL(4,2);
    DECLARE v_eval_count INT;
    DECLARE v_member_count INT;
    DECLARE v_council_id BIGINT;

    -- 1. Lấy thông tin hội đồng và số lượng thành viên thực tế
    SELECT council_id, final_council_score 
    INTO v_council_id, v_final_score 
    FROM `council_topics` 
    WHERE id = p_council_topic_id;

    SELECT COUNT(*) INTO v_member_count 
    FROM `council_members` 
    WHERE council_id = v_council_id;

    -- 2. Đếm số thành viên đã nộp điểm đánh giá
    SELECT COUNT(*) INTO v_eval_count 
    FROM `evaluations` 
    WHERE council_topic_id = p_council_topic_id;

    -- 3. Bắt buộc TẤT CẢ thành viên của hội đồng phải chấm điểm xong
    IF v_final_score IS NULL OR v_eval_count < v_member_count THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Đề tài chưa hoàn tất chấm điểm bởi TẤT CẢ thành viên hội đồng, không thể công bố!';
    END IF;

    -- 4. Đánh dấu đã công bố và thời điểm
    UPDATE `council_topics`
    SET `is_published` = TRUE, `published_at` = NOW()
    WHERE `id` = p_council_topic_id;

    -- 5. Đồng bộ trạng thái Nhóm sang COMPLETED
    UPDATE `student_groups` sg
    JOIN `council_topics` ct ON ct.group_id = sg.id
    SET sg.status = 'COMPLETED'
    WHERE ct.id = p_council_topic_id;

    -- 6. Đồng bộ trạng thái Đề tài sang COMPLETED (Đúng RTM)
    UPDATE `topics` t
    JOIN `student_groups` sg ON sg.topic_id = t.id
    JOIN `council_topics` ct ON ct.group_id = sg.id
    SET t.status = 'COMPLETED'
    WHERE ct.id = p_council_topic_id;
END$$
DELIMITER ;


DELIMITER $$
DROP PROCEDURE IF EXISTS `sp_assign_topic_to_council`$$
CREATE PROCEDURE `sp_assign_topic_to_council`(
    IN p_council_id BIGINT,
    IN p_group_id BIGINT,
    IN p_reviewer_id BIGINT,
    IN p_defense_order INT
)
BEGIN
    INSERT INTO `council_topics` (`council_id`, `group_id`, `reviewer_lecturer_id`, `defense_order`, `is_published`)
    VALUES (p_council_id, p_group_id, p_reviewer_id, p_defense_order, FALSE);

    -- Cập nhật trạng thái của nhóm sinh viên sang giai đoạn DEFENDING
    UPDATE `student_groups`
    SET `status` = 'DEFENDING'
    WHERE `id` = p_group_id;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_prevent_supervisor_grading`$$
CREATE TRIGGER `trg_prevent_supervisor_grading`
BEFORE INSERT ON `evaluations`
FOR EACH ROW
BEGIN
    DECLARE v_topic_id BIGINT;
    DECLARE v_is_supervisor INT DEFAULT 0;

    -- Lấy mã đề tài tương ứng với lượt báo cáo này
    SELECT sg.topic_id INTO v_topic_id
    FROM `council_topics` ct
    JOIN `student_groups` sg ON ct.group_id = sg.id
    WHERE ct.id = NEW.council_topic_id;

    -- Kiểm tra xem giảng viên chấm có nằm trong danh sách GVHD của đề tài không
    SELECT COUNT(*) INTO v_is_supervisor
    FROM `topic_supervisors` ts
    WHERE ts.topic_id = v_topic_id AND ts.lecturer_id = NEW.evaluator_lecturer_id;

    IF v_is_supervisor > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lỗi vi phạm quy chế: Giảng viên không được phép chấm đề tài do chính mình đang hướng dẫn!';
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_prevent_supervisor_grading_update`$$
CREATE TRIGGER `trg_prevent_supervisor_grading_update`
BEFORE UPDATE ON `evaluations`
FOR EACH ROW
BEGIN
    DECLARE v_topic_id BIGINT;
    DECLARE v_is_supervisor INT DEFAULT 0;

    SELECT sg.topic_id INTO v_topic_id
    FROM `council_topics` ct
    JOIN `student_groups` sg ON ct.group_id = sg.id
    WHERE ct.id = NEW.council_topic_id;

    SELECT COUNT(*) INTO v_is_supervisor
    FROM `topic_supervisors` ts
    WHERE ts.topic_id = v_topic_id AND ts.lecturer_id = NEW.evaluator_lecturer_id;

    IF v_is_supervisor > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lỗi vi phạm quy chế: Giảng viên không được phép chỉnh sửa điểm đề tài mình đang hướng dẫn!';
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_check_submission_by_leader`$$
CREATE TRIGGER `trg_check_submission_by_leader`
BEFORE INSERT ON `topic_submissions`
FOR EACH ROW
BEGIN
    DECLARE v_leader_id BIGINT;

    -- Lấy leader_id của nhóm
    SELECT leader_id INTO v_leader_id
    FROM `student_groups`
    WHERE id = NEW.group_id;

    IF v_leader_id <> NEW.submitted_by_id THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lỗi phân quyền: Việc nộp báo cáo đồ án chỉ được phép thực hiện bởi Nhóm trưởng!';
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_check_group_size`$$
CREATE TRIGGER `trg_check_group_size`
BEFORE INSERT ON `group_members`
FOR EACH ROW
BEGIN
    DECLARE v_current_members INT DEFAULT 0;

    SELECT COUNT(*) INTO v_current_members
    FROM `group_members`
    WHERE `group_id` = NEW.group_id;

    IF v_current_members >= 3 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Quy định đề án: Nhóm sinh viên không được vượt quá tối đa 3 thành viên!';
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_check_topic_supervisors_rules_insert`$$
CREATE TRIGGER `trg_check_topic_supervisors_rules_insert`
BEFORE INSERT ON `topic_supervisors`
FOR EACH ROW
BEGIN
    DECLARE v_count INT;
    DECLARE v_primary_count INT;

    SELECT COUNT(*) INTO v_count FROM `topic_supervisors` WHERE topic_id = NEW.topic_id;
    IF v_count >= 2 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Quy chế: Mỗi đề tài chỉ được hướng dẫn bởi tối đa 2 giảng viên!';
    END IF;

    IF NEW.is_primary = TRUE THEN
        SELECT COUNT(*) INTO v_primary_count FROM `topic_supervisors` WHERE topic_id = NEW.topic_id AND is_primary = TRUE;
        IF v_primary_count >= 1 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Quy chế: Mỗi đề tài chỉ được có duy nhất 1 giảng viên hướng dẫn chính!';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_check_topic_supervisors_rules_update`$$
CREATE TRIGGER `trg_check_topic_supervisors_rules_update`
BEFORE UPDATE ON `topic_supervisors`
FOR EACH ROW
BEGIN
    DECLARE v_other_primary INT;

    IF NEW.is_primary = TRUE AND OLD.is_primary = FALSE THEN
        SELECT COUNT(*) INTO v_other_primary FROM `topic_supervisors` 
        WHERE topic_id = NEW.topic_id AND lecturer_id <> NEW.lecturer_id AND is_primary = TRUE;
        IF v_other_primary >= 1 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Quy chế: Mỗi đề tài chỉ được có duy nhất 1 giảng viên hướng dẫn chính!';
        END IF;
    END IF;

    IF OLD.is_primary = TRUE AND NEW.is_primary = FALSE THEN
        SELECT COUNT(*) INTO v_other_primary FROM `topic_supervisors` 
        WHERE topic_id = NEW.topic_id AND lecturer_id <> NEW.lecturer_id AND is_primary = TRUE;
        IF v_other_primary = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Quy chế: Đề tài phải có ít nhất 1 giảng viên hướng dẫn chính!';
        END IF;
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_audit_topic_status_change`$$
CREATE TRIGGER `trg_audit_topic_status_change`
AFTER UPDATE ON `topics`
FOR EACH ROW
BEGIN
    IF OLD.status <> NEW.status THEN
        INSERT INTO `system_audit_logs` (`user_id`, `action`, `entity_name`, `entity_id`, `details`, `created_at`)
        VALUES (
            NEW.approved_by_id,
            'CHANGE_TOPIC_STATUS',
            'Topic',
            NEW.id,
            CONCAT('Trạng thái đề tài đổi từ [', OLD.status, '] sang [', NEW.status, ']'),
            NOW()
        );
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_check_period_date_rules`$$
CREATE TRIGGER `trg_check_period_date_rules`
BEFORE INSERT ON `registration_periods`
FOR EACH ROW
BEGIN
    -- 1. Thứ tự nội bộ từng giai đoạn
    IF NEW.topic_submission_end <= NEW.topic_submission_start THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lỗi: Thời gian kết thúc nộp đề tài phải sau thời gian bắt đầu!';
    END IF;

    IF NEW.student_registration_end <= NEW.student_registration_start THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lỗi: Thời gian kết thúc đăng ký của SV phải sau thời gian bắt đầu!';
    END IF;

    -- 2. ĐẶC TẢ HAI GIAI ĐOẠN RIÊNG BIỆT: SV đăng ký chỉ mở khi GV nộp đề tài đã kết thúc
    IF NEW.student_registration_start < NEW.topic_submission_end THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Quy chế: Giai đoạn SV đăng ký đề tài chỉ được mở sau khi Giai đoạn 1 (GV nộp đề tài) đã hoàn thành!';
    END IF;

    -- 3. Môn học/NCKH: Không có review_deadline và defense_date
    IF NEW.period_type IN ('COURSE_PROJECT', 'RESEARCH') THEN
        IF NEW.review_deadline IS NOT NULL OR NEW.defense_date IS NOT NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đợt Môn học/NCKH không được thiết lập Hạn GVPB hoặc Ngày hội đồng!';
        END IF;
    END IF;

    -- 4. TLCN: Bắt buộc review_deadline, không có defense_date
    IF NEW.period_type = 'INTERNSHIP' THEN
        IF NEW.review_deadline IS NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đợt TLCN bắt buộc phải có Hạn chót GVPB nộp điểm!';
        END IF;
        IF NEW.defense_date IS NOT NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đợt TLCN không có Ngày báo cáo hội đồng!';
        END IF;
    END IF;

    -- 5. KLTN: Bắt buộc cả hai mốc ngày
    IF NEW.period_type = 'GRADUATION_THESIS' THEN
        IF NEW.review_deadline IS NULL OR NEW.defense_date IS NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đợt KLTN bắt buộc phải có cả Hạn GVPB và Ngày báo cáo hội đồng!';
        END IF;
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_check_period_date_rules_update`$$
CREATE TRIGGER `trg_check_period_date_rules_update`
BEFORE UPDATE ON `registration_periods`
FOR EACH ROW
BEGIN
    IF NEW.topic_submission_end <= NEW.topic_submission_start THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lỗi: Thời gian kết thúc nộp đề tài phải sau thời gian bắt đầu!';
    END IF;

    IF NEW.student_registration_end <= NEW.student_registration_start THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lỗi: Thời gian kết thúc đăng ký của SV phải sau thời gian bắt đầu!';
    END IF;

    IF NEW.student_registration_start < NEW.topic_submission_end THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Quy chế: Giai đoạn SV đăng ký đề tài chỉ được mở sau khi Giai đoạn 1 (GV nộp đề tài) đã hoàn thành!';
    END IF;

    IF NEW.period_type IN ('COURSE_PROJECT', 'RESEARCH') THEN
        IF NEW.review_deadline IS NOT NULL OR NEW.defense_date IS NOT NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đợt Môn học/NCKH không được thiết lập Hạn GVPB hoặc Ngày hội đồng!';
        END IF;
    END IF;

    IF NEW.period_type = 'INTERNSHIP' THEN
        IF NEW.review_deadline IS NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đợt TLCN bắt buộc phải có Hạn chót GVPB nộp điểm!';
        END IF;
        IF NEW.defense_date IS NOT NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đợt TLCN không có Ngày báo cáo hội đồng!';
        END IF;
    END IF;

    IF NEW.period_type = 'GRADUATION_THESIS' THEN
        IF NEW.review_deadline IS NULL OR NEW.defense_date IS NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đợt KLTN bắt buộc phải có cả Hạn GVPB và Ngày báo cáo hội đồng!';
        END IF;
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_check_council_member_rules`$$
CREATE TRIGGER `trg_check_council_member_rules`
BEFORE INSERT ON `council_members`
FOR EACH ROW
BEGIN
    DECLARE v_count INT;
    DECLARE v_pos_count INT;
    DECLARE v_dummy BIGINT;

    -- PESSIMISTIC ROW-LOCK: Khóa dòng cha councils để tuần tự hóa với luồng chấm điểm (chặn TOCTOU race)
    IF NEW.council_id IS NOT NULL THEN
        SELECT id INTO v_dummy FROM `councils` WHERE id = NEW.council_id FOR UPDATE;
    END IF;

    -- 1. KHÓA CỨNG: Gọi function tái dùng để chặn thêm thành viên khi đã chấm
    IF fn_is_grading_started(NEW.council_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Hội đồng đã bắt đầu chấm điểm, không được phép thêm thành viên mới!';
    END IF;

    -- 2. Giới hạn tối đa 5 giảng viên
    SELECT COUNT(*) INTO v_count FROM `council_members` WHERE council_id = NEW.council_id;
    IF v_count >= 5 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Quy chế: Mỗi hội đồng chỉ gồm từ 03 đến tối đa 05 giảng viên!';
    END IF;

    -- 3. Duy nhất 1 Chủ tịch và 1 Thư ký
    IF NEW.position IN ('CHAIR', 'SECRETARY') THEN
        SELECT COUNT(*) INTO v_pos_count 
        FROM `council_members` 
        WHERE council_id = NEW.council_id AND position = NEW.position;
        IF v_pos_count > 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Mỗi hội đồng chỉ được có duy nhất 1 Chủ tịch và 1 Thư ký!';
        END IF;
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_prevent_council_member_update_after_grading`$$
CREATE TRIGGER `trg_prevent_council_member_update_after_grading`
BEFORE UPDATE ON `council_members`
FOR EACH ROW
BEGIN
    DECLARE v_dummy BIGINT;

    -- PESSIMISTIC ROW-LOCK
    IF NEW.council_id IS NOT NULL THEN
        SELECT id INTO v_dummy FROM `councils` WHERE id = NEW.council_id FOR UPDATE;
    END IF;

    IF fn_is_grading_started(NEW.council_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Hội đồng đã bắt đầu chấm điểm, không được phép chỉnh sửa thông tin thành viên!';
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_prevent_council_member_delete_after_grading`$$
CREATE TRIGGER `trg_prevent_council_member_delete_after_grading`
BEFORE DELETE ON `council_members`
FOR EACH ROW
BEGIN
    DECLARE v_dummy BIGINT;

    -- PESSIMISTIC ROW-LOCK
    IF OLD.council_id IS NOT NULL THEN
        SELECT id INTO v_dummy FROM `councils` WHERE id = OLD.council_id FOR UPDATE;
    END IF;

    IF fn_is_grading_started(OLD.council_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Hội đồng đã bắt đầu chấm điểm, không được phép xóa thành viên hội đồng!';
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_check_min_council_before_grading`$$
CREATE TRIGGER `trg_check_min_council_before_grading`
BEFORE INSERT ON `evaluations`
FOR EACH ROW
BEGIN
    DECLARE v_council_id BIGINT;
    DECLARE v_member_count INT;
    DECLARE v_has_chair INT;
    DECLARE v_has_secretary INT;
    DECLARE v_dummy BIGINT;

    SELECT ct.council_id INTO v_council_id FROM `council_topics` ct WHERE ct.id = NEW.council_topic_id;

    -- PESSIMISTIC ROW-LOCK: Khóa độc quyền dòng cha councils để tuần tự hóa với các luồng sửa thành viên
    IF v_council_id IS NOT NULL THEN
        SELECT id INTO v_dummy FROM `councils` WHERE id = v_council_id FOR UPDATE;
    END IF;

    SELECT COUNT(*) INTO v_member_count FROM `council_members` WHERE council_id = v_council_id;
    SELECT COUNT(*) INTO v_has_chair FROM `council_members` WHERE council_id = v_council_id AND position = 'CHAIR';
    SELECT COUNT(*) INTO v_has_secretary FROM `council_members` WHERE council_id = v_council_id AND position = 'SECRETARY';

    -- ĐÚNG SPEC GỐC: 3-5 GV, gồm 1 Chủ tịch và 1 Thư ký
    IF v_member_count < 3 OR v_has_chair = 0 OR v_has_secretary = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Hội đồng chưa đủ điều kiện chấm điểm (Cần từ 3 đến 5 GV, bao gồm 1 Chủ tịch và 1 Thư ký)!';
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_after_eval_insert_recalc`$$
CREATE TRIGGER `trg_after_eval_insert_recalc`
AFTER INSERT ON `evaluations`
FOR EACH ROW
BEGIN
    DECLARE v_avg DECIMAL(4,2);
    SELECT ROUND(AVG(total_score), 2) INTO v_avg 
    FROM `evaluations` WHERE `council_topic_id` = NEW.council_topic_id;
    
    UPDATE `council_topics` SET `final_council_score` = v_avg WHERE `id` = NEW.council_topic_id;
    UPDATE `student_groups` sg JOIN `council_topics` ct ON ct.group_id = sg.id 
    SET sg.final_score = v_avg WHERE ct.id = NEW.council_topic_id;
END$$

DROP TRIGGER IF EXISTS `trg_after_eval_update_recalc`$$
CREATE TRIGGER `trg_after_eval_update_recalc`
AFTER UPDATE ON `evaluations`
FOR EACH ROW
BEGIN
    DECLARE v_avg DECIMAL(4,2);
    SELECT ROUND(AVG(total_score), 2) INTO v_avg 
    FROM `evaluations` WHERE `council_topic_id` = NEW.council_topic_id;
    
    UPDATE `council_topics` SET `final_council_score` = v_avg WHERE `id` = NEW.council_topic_id;
    UPDATE `student_groups` sg JOIN `council_topics` ct ON ct.group_id = sg.id 
    SET sg.final_score = v_avg WHERE ct.id = NEW.council_topic_id;
END$$

DROP TRIGGER IF EXISTS `trg_after_eval_delete_recalc`$$
CREATE TRIGGER `trg_after_eval_delete_recalc`
AFTER DELETE ON `evaluations`
FOR EACH ROW
BEGIN
    DECLARE v_avg DECIMAL(4,2);
    DECLARE v_count INT;
    
    SELECT COUNT(*), ROUND(AVG(total_score), 2) INTO v_count, v_avg 
    FROM `evaluations` WHERE `council_topic_id` = OLD.council_topic_id;
    
    -- Nếu xóa hết điểm, điểm tổng kết trở về NULL (tránh treo dữ liệu mồ côi)
    IF v_count = 0 THEN
        SET v_avg = NULL;
    END IF;
    
    UPDATE `council_topics` SET `final_council_score` = v_avg WHERE `id` = OLD.council_topic_id;
    UPDATE `student_groups` sg JOIN `council_topics` ct ON ct.group_id = sg.id 
    SET sg.final_score = v_avg WHERE ct.id = OLD.council_topic_id;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_check_group_topic_rules`$$
CREATE TRIGGER `trg_check_group_topic_rules`
BEFORE INSERT ON `student_groups`
FOR EACH ROW
BEGIN
    DECLARE v_max INT;
    DECLARE v_current INT;
    DECLARE v_status VARCHAR(50);

    IF NEW.topic_id IS NOT NULL THEN
        SELECT `status`, `max_groups` INTO v_status, v_max FROM `topics` WHERE `id` = NEW.topic_id;

        IF v_status <> 'APPROVED' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Sinh viên chỉ được đăng ký đề tài đã được phê duyệt (APPROVED)!';
        END IF;

        SELECT COUNT(*) INTO v_current FROM `student_groups` WHERE `topic_id` = NEW.topic_id;
        IF v_current >= v_max THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đề tài này đã đạt số lượng nhóm đăng ký tối đa cho phép!';
        END IF;
    END IF;
END$$
DELIMITER ;


DELIMITER $$
DROP TRIGGER IF EXISTS `trg_check_group_topic_rules_update`$$
CREATE TRIGGER `trg_check_group_topic_rules_update`
BEFORE UPDATE ON `student_groups`
FOR EACH ROW
BEGIN
    DECLARE v_max INT;
    DECLARE v_current INT;
    DECLARE v_status VARCHAR(50);

    -- FIX CHUẨN: CHỈ validate khi topic_id THỰC SỰ thay đổi!
    IF NEW.topic_id IS NOT NULL AND (OLD.topic_id IS NULL OR OLD.topic_id <> NEW.topic_id) THEN
        SELECT `status`, `max_groups` INTO v_status, v_max FROM `topics` WHERE `id` = NEW.topic_id;

        IF v_status <> 'APPROVED' THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Sinh viên chỉ được đăng ký đề tài đã được phê duyệt (APPROVED)!';
        END IF;

        SELECT COUNT(*) INTO v_current FROM `student_groups` WHERE `topic_id` = NEW.topic_id;
        IF v_current >= v_max THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Đề tài này đã đạt số lượng nhóm đăng ký tối đa cho phép!';
        END IF;
    END IF;
END$$
DELIMITER ;


-- ====================================================================
-- SEED DATA MẪU KHỞI TẠO CHO HỆ THỐNG FIT-THESIS
-- ====================================================================

-- 1. Nạp vai trò (Roles)
INSERT INTO `roles` (`id`, `role_name`, `description`) VALUES
(1, 'ROLE_DEAN', 'Trưởng khoa CNTT (Tạo đợt, xem toàn bộ đề tài, công bố điểm)'),
(2, 'ROLE_HEAD_OF_DEPT', 'Trưởng bộ môn (Duyệt đề tài của bộ môn)'),
(3, 'ROLE_LECTURER', 'Giảng viên (Đăng ký đề tài, hướng dẫn SV, phản biện/chấm điểm)'),
(4, 'ROLE_STUDENT', 'Sinh viên (Thành lập nhóm, đăng ký đề tài, nộp báo cáo, xem điểm)'),
(5, 'ROLE_ADMIN', 'Quản trị viên hệ thống');

-- 2. Nạp bộ môn (Departments)
INSERT INTO `departments` (`id`, `dept_code`, `dept_name`, `description`) VALUES
(1, 'CNPM', 'Công nghệ phần mềm', 'Bộ môn phụ trách lập trình, kiến trúc phần mềm, công nghệ web/mobile'),
(2, 'HTTT', 'Hệ thống thông tin', 'Bộ môn phụ trách cơ sở dữ liệu, phân tích thiết kế hệ thống, ERP'),
(3, 'MMT', 'Mạng máy tính & Truyền thông', 'Bộ môn phụ trách an toàn thông tin, mạng máy tính, điện toán đám mây'),
(4, 'KHMT', 'Khoa học máy tính', 'Bộ môn phụ trách AI, xử lý ảnh, học máy, cấu trúc dữ liệu thuật toán');

-- 3. Nạp người dùng (Mật khẩu mã hóa BCrypt cho '123456')
-- BCrypt hash của '123456': $2a$10$w8TfJ3jCqL.Hk0kZ9U2pEOQ3LhVzL0Yt8YV0/vQkH5LhVzL0Yt8YV
INSERT INTO `users` (`id`, `user_code`, `password`, `full_name`, `email`, `phone`, `academic_rank`, `class_name`, `department_id`) VALUES
(1, 'TK001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'GS. TS. Nguyễn Minh Tuấn', 'truongkhoa@fit.edu.vn', '0901234567', 'GS. TS', NULL, 1),
(2, 'TBM001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'PGS. TS. Trần Huy Hoàng', 'tbm_cnpm@fit.edu.vn', '0902345678', 'PGS. TS', NULL, 1),
(3, 'GV001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'TS. Lê Anh Tuấn', 'gv_huongdan@fit.edu.vn', '0903456789', 'TS', NULL, 1),
(4, 'GV002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ThS. Phạm Quốc Hoàng', 'gv_phanbien@fit.edu.vn', '0904567890', 'ThS', NULL, 1),
(5, 'GV003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'TS. Vũ Đức Thắng', 'gv_hoidong@fit.edu.vn', '0905678901', 'TS', NULL, 1),
(6, 'SV001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Nguyễn Tuấn Anh', 'truongnhom@student.edu.vn', '0912345678', NULL, 'DHCNTT17A', 1),
(7, 'SV002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Trần Minh Hoàng', 'thanhvien1@student.edu.vn', '0912345679', NULL, 'DHCNTT17A', 1),
(8, 'SV003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Lê Thanh Tùng', 'thanhvien2@student.edu.vn', '0912345680', NULL, 'DHCNTT17A', 1),
(9, 'ADMIN01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Lê Đình Hoàng', 'admin@fit.edu.vn', '0999999999', NULL, NULL, NULL);

-- 4. Gán quyền (User Roles)
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES
(1, 1), (1, 3), -- Trưởng khoa có quyền DEAN và LECTURER
(2, 2), (2, 3), -- Trưởng BM có quyền HEAD_OF_DEPT và LECTURER
(3, 3), (4, 3), (5, 3), -- Giảng viên
(6, 4), (7, 4), (8, 4), -- Sinh viên
(9, 5); -- Admin

-- 5. Nạp đợt đăng ký đồ án
INSERT INTO `registration_periods` (`id`, `period_name`, `academic_year`, `semester`, `period_type`, `topic_submission_start`, `topic_submission_end`, `student_registration_start`, `student_registration_end`, `review_deadline`, `defense_date`, `created_by_id`) VALUES
(1, 'Đợt Khóa Luận Tốt Nghiệp Học Kỳ 1 2026-2027', '2026-2027', 1, 'GRADUATION_THESIS', '2026-09-01 08:00:00', '2026-09-20 17:00:00', '2026-09-21 08:00:00', '2026-10-05 17:00:00', '2026-11-20 17:00:00', '2026-12-05', 1);

-- 6. Nạp đề tài nghiên cứu
INSERT INTO `topics` (`id`, `topic_code`, `title`, `description`, `requirements`, `department_id`, `period_id`, `status`, `created_by_lecturer_id`, `approved_by_id`) VALUES
(1, 'DT_KLTN_2026_01', 'Xây dựng Hệ thống Quản lý Đề tài Sinh viên sử dụng Spring Boot & Microservices', 'Nghiên cứu quy trình quản lý đề tài khoa CNTT, tự động hóa từ khâu đăng ký, phân công GVHD/GVPB đến bảo vệ hội đồng.', 'Thành thạo Java Spring Boot, MySQL, Spring Security, Docker, Frontend React/Thymeleaf.', 1, 1, 'APPROVED', 3, 2),
(2, 'DT_KLTN_2026_02', 'Ứng dụng Trí tuệ nhân tạo (AI) trong Hỗ trợ Phát hiện Đạo văn Đồ án Sinh viên', 'Xây dựng mô hình xử lý ngôn ngữ tự nhiên (NLP) so khớp tài liệu báo cáo đồ án sinh viên để đánh giá mức độ tương đồng.', 'Python, FastAPI, Spring Boot API Gateway, PyTorch/TensorFlow.', 4, 1, 'APPROVED', 4, 2);

-- 7. Nạp phân công GVHD
INSERT INTO `topic_supervisors` (`topic_id`, `lecturer_id`, `is_primary`) VALUES
(1, 3, TRUE),  -- GV001 là GVHD chính cho Topic 1
(1, 4, FALSE), -- GV002 là GVHD phụ cho Topic 1
(2, 4, TRUE);  -- GV002 là GVHD chính cho Topic 2

-- 8. Nạp nhóm sinh viên & thành viên nhóm
INSERT INTO `users` (`id`, `user_code`, `password`, `full_name`, `email`, `phone`, `academic_rank`, `class_name`, `department_id`) VALUES
(10, 'SV005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Trịnh Hoàng Long', 'sv005@student.edu.vn', '0912345682', NULL, 'DHCNTT17B', 4),
(11, 'SV004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Phạm Minh Đức', 'sv004@student.edu.vn', '0912345681', NULL, 'DHCNTT17B', 4),
(12, 'GV004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ThS. Nguyễn Văn D', 'gv004@fit.edu.vn', '0906789012', 'ThS', NULL, 1);

INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES
(10, 4),
(11, 4),
(12, 3);

INSERT INTO `student_groups` (`id`, `group_name`, `period_id`, `topic_id`, `leader_id`, `status`) VALUES
(1, 'Nhóm Nghiên Cứu Phần Mềm', 1, 1, 6, 'DEFENDING'),
(2, 'Nhóm Trí Tuệ Nhân Tạo & NLP', 1, 2, 10, 'DEFENDING');

INSERT INTO `group_members` (`id`, `group_id`, `student_id`, `period_id`, `role_in_group`) VALUES
(1, 1, 6, 1, 'LEADER'),
(2, 1, 7, 1, 'MEMBER'),
(3, 1, 8, 1, 'MEMBER'),
(4, 2, 10, 1, 'LEADER'),
(5, 2, 11, 1, 'MEMBER');

-- 9. Nạp Hội đồng bảo vệ & phân công
-- HỘI ĐỒNG 01: Test case tiêu cực (GV002 là thành viên HĐ nhưng là GVHD nên bị chặn chấm)
INSERT INTO `councils` (`id`, `council_code`, `council_name`, `period_id`, `defense_date`, `location`, `status`) VALUES
(1, 'HD_KLTN_CNPM_01', 'Hội đồng Bảo vệ Khóa Luận Tốt Nghiệp - Bộ Môn CNPM 01', 1, '2026-12-05 08:30:00', 'Phòng B204 - Giảng đường B', 'CREATED'),
(2, 'HD_KLTN_CNPM_02', 'Hội đồng Bảo vệ Khóa Luận Tốt Nghiệp - Hội Đồng 02', 1, '2026-12-05 14:00:00', 'Phòng B205 - Giảng đường B', 'CREATED');

-- Thành viên Hội đồng 01
INSERT INTO `council_members` (`id`, `council_id`, `lecturer_id`, `position`) VALUES
(1, 1, 2, 'CHAIR'),     -- TBM001: Chủ tịch
(2, 1, 5, 'SECRETARY'), -- GV003: Thư ký
(3, 1, 4, 'MEMBER');    -- GV002: Ủy viên

-- Thành viên Hội đồng 02: Test case tích cực (TBM001, GV001, GV003 không ai hướng dẫn Đề tài 2 -> Chấm điểm thành công 100%)
INSERT INTO `council_members` (`id`, `council_id`, `lecturer_id`, `position`) VALUES
(4, 2, 2, 'CHAIR'),     -- TBM001: Chủ tịch
(5, 2, 3, 'SECRETARY'), -- GV001: Thư ký (TS. Lê Anh Tuấn)
(6, 2, 5, 'MEMBER');    -- GV003: Ủy viên (TS. Vũ Đức Thắng)

INSERT INTO `council_topics` (`id`, `council_id`, `group_id`, `reviewer_lecturer_id`, `defense_order`, `is_published`) VALUES
(1, 1, 1, 4, 1, FALSE),
(2, 2, 2, 5, 1, FALSE);

-- 10. Nạp thông báo mẫu của Khoa
INSERT INTO `announcements` (`id`, `title`, `content`, `author_id`, `is_pinned`) VALUES
(1, 'Thông báo mở cổng Đăng ký Đề tài Khóa Luận Tốt Nghiệp Học Kỳ 1 2026-2027', 'Khoa Công nghệ Thông tin thông báo đến toàn thể sinh viên năm cuối về kế hoạch đăng ký đề tài KLTN...', 1, TRUE);

SET foreign_key_checks = 1;

