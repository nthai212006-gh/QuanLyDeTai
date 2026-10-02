package com.quanlydetai.service;

import com.quanlydetai.aspect.AuditAction;
import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.RegistrationPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PeriodService {

    private final RegistrationPeriodRepository periodRepository;

    public List<RegistrationPeriod> getAllPeriods() {
        return periodRepository.findAllByOrderByCreatedAtDesc();
    }

    public RegistrationPeriod getPeriodById(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký ID: " + id));
    }

    @Transactional
    @AuditAction(action = "CREATE_PERIOD", entityName = "RegistrationPeriod")
    public RegistrationPeriod createPeriod(RegistrationPeriod period, User creator) {
        period.setCreatedBy(creator);
        validateAndSanitizePeriod(period);
        updatePeriodState(period);
        return periodRepository.save(period);
    }

    @Transactional
    @AuditAction(action = "UPDATE_PERIOD", entityName = "RegistrationPeriod")
    public RegistrationPeriod updatePeriod(Long id, RegistrationPeriod updatedData) {
        RegistrationPeriod period = getPeriodById(id);
        period.setPeriodName(updatedData.getPeriodName());
        period.setPeriodType(updatedData.getPeriodType());
        period.setAcademicYear(updatedData.getAcademicYear());
        period.setSemester(updatedData.getSemester());
        period.setGvStartDate(updatedData.getGvStartDate());
        period.setGvEndDate(updatedData.getGvEndDate());
        period.setSvStartDate(updatedData.getSvStartDate());
        period.setSvEndDate(updatedData.getSvEndDate());
        period.setGvpbDeadline(updatedData.getGvpbDeadline());
        period.setCouncilDate(updatedData.getCouncilDate());

        validateAndSanitizePeriod(period);
        updatePeriodState(period);
        return periodRepository.save(period);
    }

    @Transactional
    public void checkAndUpdateAllPeriodStatuses() {
        List<RegistrationPeriod> periods = periodRepository.findAll();
        for (RegistrationPeriod period : periods) {
            updatePeriodState(period);
            periodRepository.save(period);
        }
    }

    private void updatePeriodState(RegistrationPeriod period) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(period.getGvStartDate())) {
            period.setStatus(RegistrationPeriod.PeriodStatus.UPCOMING);
        } else if (now.isBefore(period.getGvEndDate())) {
            period.setStatus(RegistrationPeriod.PeriodStatus.GV_REGISTRATION);
        } else if (now.isBefore(period.getSvStartDate())) {
            period.setStatus(RegistrationPeriod.PeriodStatus.UPCOMING);
        } else if (now.isBefore(period.getSvEndDate())) {
            period.setStatus(RegistrationPeriod.PeriodStatus.SV_REGISTRATION);
        } else if (period.getCouncilDate() != null && now.isAfter(period.getCouncilDate())) {
            period.setStatus(RegistrationPeriod.PeriodStatus.COMPLETED);
        } else {
            period.setStatus(RegistrationPeriod.PeriodStatus.IN_PROGRESS);
        }
    }

    /**
     * Kiểm tra thứ tự ngày hợp lệ và xóa các ngày không phù hợp với loại đợt.
     * Quy tắc:
     *   COURSE_PROJECT, RESEARCH  -> chỉ có 2 giai đoạn GV/SV (không có GVPB, không có hội đồng)
     *   INTERNSHIP (TLCN)          -> có GVPB nhận xét (reviewDeadline), không có ngày bảo vệ hội đồng
     *   GRADUATION_THESIS (KLTN)   -> đầy đủ cả 2 ngày
     */
    private void validateAndSanitizePeriod(RegistrationPeriod period) {
        // 1. Kiểm tra thứ tự ngày GV
        if (period.getGvStartDate() != null && period.getGvEndDate() != null
                && !period.getGvEndDate().isAfter(period.getGvStartDate())) {
            throw new IllegalArgumentException(
                    "Ngày kết thúc giai đoạn GV phải sau ngày bắt đầu GV (topicSubmissionEnd > topicSubmissionStart)");
        }
        // 2. Kiểm tra thứ tự ngày SV
        if (period.getSvStartDate() != null && period.getSvEndDate() != null
                && !period.getSvEndDate().isAfter(period.getSvStartDate())) {
            throw new IllegalArgumentException(
                    "Ngày kết thúc giai đoạn SV phải sau ngày bắt đầu SV (studentRegistrationEnd > studentRegistrationStart)");
        }
        // 3. Ràng buộc hai giai đoạn riêng biệt: SV bắt đầu sau khi GV kết thúc
        if (period.getSvStartDate() != null && period.getGvEndDate() != null
                && period.getSvStartDate().isBefore(period.getGvEndDate())) {
            throw new IllegalArgumentException(
                    "Giai đoạn SV đăng ký đề tài chỉ được mở sau khi Giai đoạn 1 (GV nộp đề tài) đã hoàn thành!");
        }

        // 4. Xóa ngày hoặc kiểm tra ngày bắt buộc theo loại đợt
        RegistrationPeriod.PeriodType type = period.getPeriodType();
        if (type == RegistrationPeriod.PeriodType.COURSE_PROJECT
                || type == RegistrationPeriod.PeriodType.RESEARCH) {
            period.setReviewDeadline(null);
            period.setDefenseDate(null);
        } else if (type == RegistrationPeriod.PeriodType.INTERNSHIP) {
            if (period.getReviewDeadline() == null) {
                throw new IllegalArgumentException("Đợt TLCN bắt buộc phải có Hạn chót GVPB nộp điểm!");
            }
            period.setDefenseDate(null);
        } else if (type == RegistrationPeriod.PeriodType.GRADUATION_THESIS) {
            if (period.getReviewDeadline() == null || period.getDefenseDate() == null) {
                throw new IllegalArgumentException("Đợt KLTN bắt buộc phải có cả Hạn GVPB và Ngày báo cáo hội đồng!");
            }
        }

        // 4. Kiểm tra học kỳ: chỉ chấp nhận Học kỳ 1 hoặc Học kỳ 2, loại bỏ kỳ Hè
        if (period.getSemester() == null || (period.getSemester() != 1 && period.getSemester() != 2)) {
            throw new IllegalArgumentException(
                    "Học kỳ không hợp lệ! Hệ thống chỉ áp dụng cho Học kỳ 1 và Học kỳ 2, không hỗ trợ kỳ Hè.");
        }
    }
}
