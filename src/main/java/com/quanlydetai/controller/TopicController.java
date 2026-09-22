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
        model.addAttribute("topic", new Topic());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("periods", periodRepository.findAllByOrderByCreatedAtDesc());
        
        // Danh sách giảng viên để chọn GVHD phụ
        List<User> lecturers = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_LECTURER") && !u.getId().equals(userDetails.getId()))
                .toList();
        model.addAttribute("lecturers", lecturers);
        return "topics/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('LECTURER', 'HEAD_OF_DEPT', 'DEAN', 'ADMIN')")
    public String createTopic(@ModelAttribute("topic") Topic topic,
                              @RequestParam("departmentId") Long departmentId,
                              @RequestParam("periodId") Long periodId,
                              @RequestParam(value = "coSupervisorId", required = false) Long coSupervisorId,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        topicService.proposeTopic(topic, departmentId, periodId, userDetails.getUser(), coSupervisorId);
        return "redirect:/topics?periodId=" + periodId + "&success=true";
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
