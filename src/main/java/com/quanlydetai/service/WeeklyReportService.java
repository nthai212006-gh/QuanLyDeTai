package com.quanlydetai.service;

import com.quanlydetai.aspect.AuditAction;
import com.quanlydetai.entity.StudentGroup;
import com.quanlydetai.entity.User;
import com.quanlydetai.entity.WeeklyProgressReport;
import com.quanlydetai.repository.StudentGroupRepository;
import com.quanlydetai.repository.WeeklyProgressReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final WeeklyProgressReportRepository reportRepository;
    private final StudentGroupRepository groupRepository;

    public List<WeeklyProgressReport> getReportsByGroup(Long groupId) {
        return reportRepository.findByGroupIdOrderByWeekNumberAsc(groupId);
    }

    public WeeklyProgressReport getReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy báo cáo tiến độ ID: " + id));
    }

    @Transactional
    @AuditAction(action = "SUBMIT_WEEKLY_REPORT", entityName = "WeeklyProgressReport")
    public WeeklyProgressReport submitReport(Long groupId, Integer weekNumber, LocalDate start, LocalDate end,
                                            String tasksCompleted, String difficulties, String nextPlan,
                                            String attachmentUrl, User submitter) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm"));

        WeeklyProgressReport report = reportRepository.findByGroupIdAndWeekNumber(groupId, weekNumber)
                .orElse(WeeklyProgressReport.builder()
                        .group(group)
                        .weekNumber(weekNumber)
                        .submittedBy(submitter)
                        .build());

        report.setStartDate(start);
        report.setEndDate(end);
        report.setTasksCompleted(tasksCompleted);
        report.setDifficulties(difficulties);
        report.setNextWeekPlan(nextPlan);
        report.setAttachmentUrl(attachmentUrl);
        report.setStatus(WeeklyProgressReport.ReportStatus.PENDING);
        report.setSubmittedAt(LocalDateTime.now());

        return reportRepository.save(report);
    }

    @Transactional
    @AuditAction(action = "REVIEW_WEEKLY_REPORT", entityName = "WeeklyProgressReport")
    public void reviewReport(Long reportId, String feedback, WeeklyProgressReport.ReportStatus status, User reviewer) {
        WeeklyProgressReport report = getReportById(reportId);
        report.setLecturerFeedback(feedback);
        report.setStatus(status);
        report.setReviewedBy(reviewer);
        report.setReviewedAt(LocalDateTime.now());
        reportRepository.save(report);
    }
}
