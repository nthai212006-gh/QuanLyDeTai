package com.quanlydetai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PeriodScheduler {

    private final PeriodService periodService;

    /**
     * Tự động quét và cập nhật trạng thái đợt đăng ký mỗi 1 giờ hoặc lúc 00:00 hàng ngày
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void schedulePeriodStateCheck() {
        log.info("[CRON JOB] Bắt đầu quét và tự động cập nhật trạng thái các đợt đăng ký đề tài...");
        try {
            periodService.checkAndUpdateAllPeriodStatuses();
            log.info("[CRON JOB] Hoàn tất cập nhật trạng thái các đợt đăng ký.");
        } catch (Exception e) {
            log.error("[CRON JOB] Lỗi trong quá trình cập nhật trạng thái: {}", e.getMessage());
        }
    }
}
