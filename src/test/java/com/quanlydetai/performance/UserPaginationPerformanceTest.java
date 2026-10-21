package com.quanlydetai.performance;

import com.quanlydetai.entity.User;
import com.quanlydetai.repository.UserRepository;
import com.quanlydetai.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserPaginationPerformanceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    /**
     * TEST 1 - XÁC THỰC CÔNG CỤ ĐO (POSITIVE / NEGATIVE VERIFICATION):
     * Cố tình tạo điều kiện sai: Specification có FETCH JOIN collection roles kết hợp Pageable.
     * Khẳng định ListAppender thực sự bắt được cảnh báo in-memory pagination (HHH90003004 / HHH000104).
     * Nếu test này fail -> công cụ đo không đáng tin!
     */
    @Test
    @DisplayName("Test 11.1 - Positive Verification: ListAppender must capture in-memory pagination warning when collection fetch is misconfigured with Pageable")
    void listAppenderMustCaptureWarningWhenMisconfigured() {
        Specification<User> badSpec = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("roles", jakarta.persistence.criteria.JoinType.LEFT); // Cố ý fetch collection
            }
            return cb.conjunction();
        };

        ch.qos.logback.classic.Logger hibernateRootLogger = 
            (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.hibernate");
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        hibernateRootLogger.addAppender(appender);

        try {
            userRepository.findAll(badSpec, PageRequest.of(0, 5)); // Cố ý gây cảnh báo

            // Trong Hibernate 6.x, mã là HHH90003004 (cũ là HHH000104): "firstResult/maxResults specified with collection fetch; applying in memory"
            boolean captured = appender.list.stream()
                .anyMatch(e -> e.getFormattedMessage() != null && 
                    (e.getFormattedMessage().contains("HHH90003004") 
                     || e.getFormattedMessage().contains("HHH000104") 
                     || e.getFormattedMessage().contains("applying in memory")));
            assertTrue(captured, 
                "ListAppender phải bắt được cảnh báo in-memory pagination khi cấu hình sai!");
        } finally {
            hibernateRootLogger.detachAppender(appender);
        }
    }

    /**
     * TEST 2 - KIỂM CHỨNG PRODUCTION CODE THẬT:
     * Chạy Two-phase Pagination chuẩn.
     * Khẳng định 100% KHÔNG có bất kỳ cảnh báo in-memory pagination nào xuất hiện trong log!
     */
    @Test
    @DisplayName("Test 11.1 - Production Verification: searchUsers must NOT produce in-memory pagination warning in Hibernate log")
    void searchUsers_MustNotProduceHHH000104Warning() {
        ch.qos.logback.classic.Logger hibernateRootLogger = 
            (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.hibernate");
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        hibernateRootLogger.addAppender(appender);

        try {
            // Chạy phương thức UserService chuẩn Two-phase
            Page<User> result = userService.searchUsers(null, null, null, PageRequest.of(0, 10));
            assertNotNull(result);
            assertFalse(result.isEmpty(), "Danh sách kết quả không được rỗng");

            boolean hasWarning = appender.list.stream()
                .anyMatch(e -> e.getFormattedMessage() != null && 
                    (e.getFormattedMessage().contains("HHH90003004") 
                     || e.getFormattedMessage().contains("HHH000104") 
                     || e.getFormattedMessage().contains("applying in memory")));
            assertFalse(hasWarning, 
                "Lỗi nghiêm trọng: Phát hiện cảnh báo in-memory pagination trong log Hibernate!");
        } finally {
            hibernateRootLogger.detachAppender(appender);
        }
    }
}
