package com.quanlydetai.controller;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.StudentGroup;
import com.quanlydetai.entity.WeeklyProgressReport;
import com.quanlydetai.service.GroupService;
import com.quanlydetai.service.WeeklyReportService;
import com.quanlydetai.util.SqlErrorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/weekly-reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService reportService;
    private final GroupService groupService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public String myReports(@AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        Long studentId = userDetails.getId();
        java.util.Optional<Long> activePeriodId = groupService.findActivePeriodIdByStudent(studentId);
        if (activePeriodId.isEmpty()) {
            activePeriodId = groupService.findLatestPeriodIdByStudent(studentId);
        }
        if (activePeriodId.isPresent()) {
            java.util.Optional<StudentGroup> group = groupService.getGroupByStudentAndPeriod(studentId, activePeriodId.get());
            if (group.isPresent()) {
                return "redirect:/weekly-reports/group/" + group.get().getId();
            }
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa tham gia nhóm sinh viên nào để xem báo cáo tuần.");
        return "redirect:/groups/my-group";
    }

    @GetMapping("/group/{groupId}")
    public String listReports(@PathVariable("groupId") Long groupId, Model model,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        StudentGroup group = groupService.getGroupById(groupId);
        List<WeeklyProgressReport> reports = reportService.getReportsByGroup(groupId);

        model.addAttribute("group", group);
        model.addAttribute("reports", reports);
        model.addAttribute("currentUser", userDetails.getUser());
        return "weekly-reports/list";
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public String submitWeeklyReport(@RequestParam("groupId") Long groupId,
                                     @RequestParam("weekNumber") Integer weekNumber,
                                     @RequestParam("startDate") String startDate,
                                     @RequestParam("endDate") String endDate,
                                     @RequestParam("tasksCompleted") String tasks,
                                     @RequestParam(value = "difficulties", required = false) String difficulties,
                                     @RequestParam("nextWeekPlan") String nextPlan,
                                     @RequestParam(value = "attachmentUrl", required = false) String attachmentUrl,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        try {
            reportService.submitReport(groupId, weekNumber, LocalDate.parse(startDate), LocalDate.parse(endDate),
                    tasks, difficulties, nextPlan, attachmentUrl, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Nộp báo cáo tiến độ tuần " + weekNumber + " thành công!");
            return "redirect:/weekly-reports/group/" + groupId + "?submitted=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", SqlErrorUtils.extractFriendlyMessage(e));
            return "redirect:/weekly-reports/group/" + groupId;
        }
    }

    @PostMapping("/review")
    @PreAuthorize("hasAnyRole('LECTURER', 'HEAD_OF_DEPT', 'DEAN', 'ADMIN')")
    public String reviewReport(@RequestParam("reportId") Long reportId,
                               @RequestParam("groupId") Long groupId,
                               @RequestParam("feedback") String feedback,
                               @RequestParam("status") WeeklyProgressReport.ReportStatus status,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            reportService.reviewReport(reportId, feedback, status, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Đánh giá nhận xét báo cáo thành công!");
            return "redirect:/weekly-reports/group/" + groupId + "?reviewed=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", SqlErrorUtils.extractFriendlyMessage(e));
            return "redirect:/weekly-reports/group/" + groupId;
        }
    }
}
