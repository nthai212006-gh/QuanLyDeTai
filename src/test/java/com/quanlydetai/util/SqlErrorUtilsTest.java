package com.quanlydetai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class SqlErrorUtilsTest {

    @Test
    @DisplayName("Should extract group size limit trigger message")
    void testExtractGroupSizeMessage() {
        SQLException sqlEx = new SQLException("Quy định đề án: Nhóm sinh viên không được vượt quá tối đa 3 thành viên!", "45000");
        RuntimeException wrappedEx = new RuntimeException("Hibernate exception", sqlEx);

        String friendlyMessage = SqlErrorUtils.extractFriendlyMessage(wrappedEx);
        assertThat(friendlyMessage).isEqualTo("Quy định đề án: Nhóm sinh viên không được vượt quá tối đa 3 thành viên!");
    }

    @Test
    @DisplayName("Should extract leader submission trigger message")
    void testExtractLeaderSubmissionMessage() {
        SQLException sqlEx = new SQLException("Lỗi phân quyền: Việc nộp báo cáo đồ án chỉ được phép thực hiện bởi Nhóm trưởng!", "45000");
        RuntimeException wrappedEx = new RuntimeException("DB error", sqlEx);

        String friendlyMessage = SqlErrorUtils.extractFriendlyMessage(wrappedEx);
        assertThat(friendlyMessage).isEqualTo("Lỗi phân quyền: Việc nộp báo cáo đồ án chỉ được phép thực hiện bởi Nhóm trưởng!");
    }

    @Test
    @DisplayName("Should extract supervisor grading trigger message")
    void testExtractSupervisorGradingMessage() {
        SQLException sqlEx = new SQLException("Lỗi vi phạm quy chế: Giảng viên không được phép chấm đề tài do chính mình đang hướng dẫn!", "45000");
        RuntimeException wrappedEx = new RuntimeException("PersistenceException", sqlEx);

        String friendlyMessage = SqlErrorUtils.extractFriendlyMessage(wrappedEx);
        assertThat(friendlyMessage).isEqualTo("Lỗi vi phạm quy chế: Giảng viên không được phép chấm đề tài do chính mình đang hướng dẫn!");
    }

    @Test
    @DisplayName("Should fallback gracefully when no specific trigger message found")
    void testFallbackMessage() {
        RuntimeException ex = new RuntimeException("Dữ liệu nhập không hợp lệ");
        String friendlyMessage = SqlErrorUtils.extractFriendlyMessage(ex);
        assertThat(friendlyMessage).isEqualTo("Dữ liệu nhập không hợp lệ");
    }

    @Test
    @DisplayName("Should handle null exception gracefully")
    void testNullException() {
        String friendlyMessage = SqlErrorUtils.extractFriendlyMessage(null);
        assertThat(friendlyMessage).isEqualTo("Đã xảy ra lỗi không xác định!");
    }
}
