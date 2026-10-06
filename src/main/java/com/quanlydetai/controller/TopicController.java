package com.quanlydetai.controller;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.Department;
import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.entity.Topic;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.DepartmentRepository;
import com.quanlydetai.repository.RegistrationPeriodRepository;
import com.quanlydetai.repository.UserRepository;
import com.quanlydetai.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;
    private final DepartmentRepository departmentRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String listTopics(@RequestParam(value = "periodId", required = false) Long periodId,
                             Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RegistrationPeriod> periods = periodRepository.findAllByOrderByCreatedAtDesc();
        List<Topic> topics;
        if (periodId != null) {
            topics = topicService.getTopicsByPeriod(periodId);
        } else {
            topics = topicService.getAllTopics();
        }

        model.addAttribute("topics", topics);
        model.addAttribute("periods", periods);
        model.addAttribute("selectedPeriodId", periodId);
        model.addAttribute("currentUser", userDetails.getUser());
        return "topics/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('LECTURER', 'HEAD_OF_DEPT', 'DEAN', 'ADMIN')")
    public String showCreateForm(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!model.containsAttribute("topic")) {
            model.addAttribute("topic", new Topic());
        }
        List<RegistrationPeriod> periods = periodRepository.findAllByOrderByCreatedAtDesc();
        Long firstOpenPeriodId = periods.stream()
                .filter(RegistrationPeriod::isTopicSubmissionOpen)
                .map(RegistrationPeriod::getId)
                .findFirst()
                .orElse(null);
        model.addAttribute("firstOpenPeriodId", firstOpenPeriodId);
        model.addAttribute("hasOpenPeriod", firstOpenPeriodId != null);
        model.addAttribute("periods", periods);
        model.addAttribute("departments", departmentRepository.findAll());
        
        // Danh sách giảng viên để chọn GVHD phụ
        List<User> lecturers = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_LECTURER") && !u.getId().equals(userDetails.getId()))
                .toList();
        model.addAttribute("lecturers", lecturers);
        model.addAttribute("currentUser", userDetails.getUser());
        return "topics/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('LECTURER', 'HEAD_OF_DEPT', 'DEAN', 'ADMIN')")
    public String createTopic(@ModelAttribute("topic") Topic topic,
                              @RequestParam("departmentId") Long departmentId,
                              @RequestParam("periodId") Long periodId,
                              @RequestParam(value = "coSupervisorId", required = false) Long coSupervisorId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            topicService.proposeTopic(topic, departmentId, periodId, userDetails.getUser(), coSupervisorId);
            redirectAttributes.addFlashAttribute("successMessage", "Đề xuất đề tài thành công và đã được gửi chờ phê duyệt!");
            return "redirect:/topics?periodId=" + periodId;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("topic", topic);
            return "redirect:/topics/create";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
            redirectAttributes.addFlashAttribute("topic", topic);
            return "redirect:/topics/create";
        }
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasAnyRole('DEAN', 'HEAD_OF_DEPT', 'ADMIN')")
    public String approveTopic(@PathVariable("id") Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        topicService.approveTopic(id, userDetails.getUser());
        return "redirect:/topics?approved=true";
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasAnyRole('DEAN', 'HEAD_OF_DEPT', 'ADMIN')")
    public String rejectTopic(@PathVariable("id") Long id,
                              @RequestParam("reason") String reason,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        topicService.rejectTopic(id, reason, userDetails.getUser());
        return "redirect:/topics?rejected=true";
    }
}
