USE `quanly_detai_db`;

DELIMITER $$

-- 1. STORED PROCEDURE: Tính điểm trung bình cộng của Hội đồng
DROP PROCEDURE IF EXISTS `sp_calculate_final_council_score`$$
CREATE PROCEDURE `sp_calculate_final_council_score`(
    IN p_council_topic_id BIGINT,
    OUT p_final_score DECIMAL(4,2)
)
BEGIN
    DECLARE v_avg DECIMAL(4,2);
    DECLARE v_count INT;

    SELECT COUNT(*), AVG(total_score) 
    INTO v_count, v_avg
    FROM `evaluations`
    WHERE `council_topic_id` = p_council_topic_id;

    IF v_count > 0 THEN
        SET p_final_score = ROUND(v_avg, 2);
        
        -- Cập nhật vào bảng council_topics
        UPDATE `council_topics` 
        SET `final_council_score` = p_final_score 
        WHERE `id` = p_council_topic_id;

        -- Đồng bộ vào bảng student_groups
        UPDATE `student_groups` sg
        JOIN `council_topics` ct ON ct.group_id = sg.id
        SET sg.final_score = p_final_score
        WHERE ct.id = p_council_topic_id;
    ELSE
        SET p_final_score = NULL;
    END IF;
END$$

-- 2. STORED PROCEDURE: Công bố kết quả điểm hội đồng
DROP PROCEDURE IF EXISTS `sp_publish_council_results`$$
CREATE PROCEDURE `sp_publish_council_results`(
    IN p_council_topic_id BIGINT
)
BEGIN
    UPDATE `council_topics`
    SET `is_published` = TRUE
    WHERE `id` = p_council_topic_id;
END$$

-- 3. STORED PROCEDURE: Phân công đề tài vào Hội đồng
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

    UPDATE `student_groups`
    SET `status` = 'DEFENDING'
    WHERE `id` = p_group_id;
END$$

-- 4. TRIGGER: Kiểm tra nhóm không vượt quá 3 thành viên
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
        SET MESSAGE_TEXT = 'Quy định đồ án: Nhóm sinh viên không được vượt quá tối đa 3 thành viên!';
    END IF;
END$$

-- 5. TRIGGER: Ghi nhật ký audit khi trạng thái đề tài thay đổi
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

-- 6. VIEW: Tổng hợp tình trạng đề tài và giảng viên hướng dẫn
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

-- 7. VIEW: Bảng điểm tổng hợp của các Hội đồng bảo vệ
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
GROUP BY ct.id, c.id, c.council_name, c.council_code, ct.defense_order, sg.group_name, t.title, rev.full_name, ct.final_council_score, ct.is_published;
