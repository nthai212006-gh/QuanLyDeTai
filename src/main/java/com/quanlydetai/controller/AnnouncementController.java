package com.quanlydetai.controller;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.Announcement;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.AnnouncementRepository;
import com.quanlydetai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('DEAN', 'ADMIN')")
    public String createAnnouncement(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "isPinned", defaultValue = "false") boolean isPinned,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tiêu đề và nội dung thông báo không được để trống!");
            return "redirect:/dashboard";
        }

        User author = userRepository.findById(userDetails.getId()).orElse(userDetails.getUser());

        Announcement announcement = Announcement.builder()
                .title(title.trim())
                .content(content.trim())
                .author(author)
                .isPinned(isPinned)
                .build();

        announcementRepository.save(announcement);
        redirectAttributes.addFlashAttribute("successMessage", "Đã đăng thông báo mới thành công!");
        return "redirect:/dashboard";
    }
}
