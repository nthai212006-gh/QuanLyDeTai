package com.quanlydetai.controller;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.entity.StudentGroup;
import com.quanlydetai.entity.Topic;
import com.quanlydetai.repository.CouncilTopicRepository;
import com.quanlydetai.repository.RegistrationPeriodRepository;
import com.quanlydetai.service.GroupService;
import com.quanlydetai.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final TopicService topicService;
    private final RegistrationPeriodRepository periodRepository;
    private final CouncilTopicRepository councilTopicRepository;

    @GetMapping
    public String listGroups(@RequestParam(value = "periodId", required = false) Long periodId,
                             Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RegistrationPeriod> periods = periodRepository.findAllByOrderByCreatedAtDesc();
        Long activePeriodId = periodId != null ? periodId : (periods.isEmpty() ? null : periods.get(0).getId());

        List<StudentGroup> groups = activePeriodId != null ? groupService.getGroupsByPeriod(activePeriodId) : List.of();

        model.addAttribute("groups", groups);
        model.addAttribute("periods", periods);
        model.addAttribute("selectedPeriodId", activePeriodId);
        model.addAttribute("currentUser", userDetails.getUser());
        return "groups/list";
    }

    @GetMapping("/my-group")
    @PreAuthorize("hasRole('STUDENT')")
    public String myGroup(@RequestParam(value = "periodId", required = false) Long periodId,
                          Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RegistrationPeriod> periods = periodRepository.findAllByOrderByCreatedAtDesc();
        
        Long activePeriodId = null;
        if (periodId != null && periodRepository.existsById(periodId)) {
            activePeriodId = periodId;
        } else {
            activePeriodId = groupService.findActivePeriodIdByStudent(userDetails.getId())
                    .or(() -> groupService.findLatestPeriodIdByStudent(userDetails.getId()))
                    .orElseGet(() -> periods.isEmpty() ? null : periods.get(0).getId());
        }

        Optional<StudentGroup> myGroup = Optional.empty();
        if (activePeriodId != null) {
            myGroup = groupService.getGroupByStudentAndPeriod(userDetails.getId(), activePeriodId);
        }

        List<Topic> availableTopics = activePeriodId != null ? 
                topicService.getAvailableTopicsForRegistration(activePeriodId) : List.of();

        model.addAttribute("group", myGroup.orElse(null));
        model.addAttribute("periods", periods);
        model.addAttribute("selectedPeriodId", activePeriodId);
        model.addAttribute("joinedPeriodIds", groupService.getJoinedPeriodIdsByStudent(userDetails.getId()));
        model.addAttribute("availableTopics", availableTopics);
        model.addAttribute("currentUser", userDetails.getUser());
        if (myGroup.isPresent()) {
            model.addAttribute("submissions", groupService.getSubmissionsByGroup(myGroup.get().getId()));
            // Gửi kết quả hội đồng nếu đã được công bố
            councilTopicRepository.findByGroupId(myGroup.get().getId())
                    .filter(ct -> Boolean.TRUE.equals(ct.getIsPublished()))
                    .ifPresent(ct -> model.addAttribute("councilTopic", ct));
        }

        return "groups/my-group";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('STUDENT')")
    public String createGroup(@RequestParam("groupName") String groupName,
                              @RequestParam("periodId") Long periodId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            groupService.createGroup(groupName, periodId, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Tạo nhóm thành công! Bạn là nhóm trưởng.");
            return "redirect:/groups/my-group?periodId=" + periodId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
            return "redirect:/groups/my-group?periodId=" + periodId;
        }
    }

    @PostMapping("/{id}/add-member")
    @PreAuthorize("hasRole('STUDENT')")
    public String addMember(@PathVariable("id") Long groupId,
                            @RequestParam("studentCode") String studentCode,
                            @RequestParam(value = "periodId", required = false) Long periodId,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Long targetPeriodId = periodId;
        try {
            if (targetPeriodId == null) {
                targetPeriodId = groupService.getPeriodIdByGroupId(groupId);
            }
            groupService.addMember(groupId, studentCode);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thành viên vào nhóm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
        }
        return "redirect:/groups/my-group" + (targetPeriodId != null ? "?periodId=" + targetPeriodId : "");
    }

    @PostMapping("/{id}/register-topic")
    @PreAuthorize("hasRole('STUDENT')")
    public String registerTopic(@PathVariable("id") Long groupId,
                                @RequestParam("topicId") Long topicId,
                                @RequestParam(value = "periodId", required = false) Long periodId,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Long targetPeriodId = periodId;
        try {
            if (targetPeriodId == null) {
                targetPeriodId = groupService.getPeriodIdByGroupId(groupId);
            }
            groupService.registerTopic(groupId, topicId, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký đề tài thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
        }
        return "redirect:/groups/my-group" + (targetPeriodId != null ? "?periodId=" + targetPeriodId : "");
    }

    @PostMapping("/{id}/submit-report")
    @PreAuthorize("hasRole('STUDENT')")
    public String submitReport(@PathVariable("id") Long groupId,
                               @RequestParam("title") String title,
                               @RequestParam("fileUrl") String fileUrl,
                               @RequestParam(value = "sourceCodeUrl", required = false) String sourceCodeUrl,
                               @RequestParam(value = "note", required = false) String note,
                               @RequestParam(value = "periodId", required = false) Long periodId,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Long targetPeriodId = periodId;
        try {
            if (targetPeriodId == null) {
                targetPeriodId = groupService.getPeriodIdByGroupId(groupId);
            }
            groupService.submitReport(groupId, userDetails.getUser(), title, fileUrl, sourceCodeUrl, note);
            redirectAttributes.addFlashAttribute("successMessage", "Nộp báo cáo tiến độ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
        }
        return "redirect:/groups/my-group" + (targetPeriodId != null ? "?periodId=" + targetPeriodId : "");
    }

    @PostMapping("/{id}/remove-member")
    @PreAuthorize("hasAnyRole('HEAD_OF_DEPT', 'DEAN', 'ADMIN')")
    public String removeMember(@PathVariable("id") Long groupId,
                               @RequestParam("studentId") Long studentId,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            groupService.removeMember(groupId, studentId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sinh viên khỏi nhóm thành công!");
            return "redirect:/groups";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
            return "redirect:/groups";
        }
    }
}
