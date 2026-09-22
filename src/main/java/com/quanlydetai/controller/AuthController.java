package com.quanlydetai.controller;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.Announcement;
import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.entity.Topic;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.AnnouncementRepository;
import com.quanlydetai.repository.RegistrationPeriodRepository;
import com.quanlydetai.repository.StudentGroupRepository;
import com.quanlydetai.repository.TopicRepository;
import com.quanlydetai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AnnouncementRepository announcementRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final TopicRepository topicRepository;
    private final StudentGroupRepository groupRepository;

    private final UserRepository userRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User currentUser = userRepository.findById(userDetails.getId()).orElse(userDetails.getUser());
        List<Announcement> announcements = announcementRepository.findAllByOrderByIsPinnedDescCreatedAtDesc();
        List<RegistrationPeriod> periods = periodRepository.findAllByOrderByCreatedAtDesc();
        long totalTopics = topicRepository.count();
        long totalGroups = groupRepository.count();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("announcements", announcements);
        model.addAttribute("periods", periods);
        model.addAttribute("totalTopics", totalTopics);
        model.addAttribute("totalGroups", totalGroups);

        return "dashboard";
    }
}
