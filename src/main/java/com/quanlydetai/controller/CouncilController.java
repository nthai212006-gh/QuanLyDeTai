package com.quanlydetai.controller;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.Council;
import com.quanlydetai.entity.CouncilMember;
import com.quanlydetai.entity.Evaluation;
import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.CouncilMemberRepository;
import com.quanlydetai.repository.RegistrationPeriodRepository;
import com.quanlydetai.repository.StudentGroupRepository;
import com.quanlydetai.repository.UserRepository;
import com.quanlydetai.service.CouncilService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/councils")
@RequiredArgsConstructor
public class CouncilController {

    private final CouncilService councilService;
    private final CouncilMemberRepository councilMemberRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final UserRepository userRepository;
    private final StudentGroupRepository groupRepository;

    @GetMapping
    public String listCouncils(@RequestParam(value = "periodId", required = false) Long periodId,
                              Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RegistrationPeriod> periods = periodRepository.findAllByOrderByCreatedAtDesc();
        List<Council> councils = periodId != null ? councilService.getCouncilsByPeriod(periodId) : councilService.getAllCouncils();

        model.addAttribute("councils", councils);
        model.addAttribute("periods", periods);
        model.addAttribute("selectedPeriodId", periodId);
        model.addAttribute("currentUser", userDetails.getUser());
        return "councils/list";
    }

    @GetMapping("/{id}")
    public String councilDetail(@PathVariable("id") Long id, Model model,
                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        Council council = councilService.getCouncilById(id);
        List<User> lecturers = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_LECTURER"))
                .toList();

        model.addAttribute("council", council);
        model.addAttribute("lecturers", lecturers);
        model.addAttribute("availableGroups", groupRepository.findAvailableForCouncilAssignment(council.getPeriod().getId()));
        model.addAttribute("positions", CouncilMember.CouncilPosition.values());
        model.addAttribute("currentUser", userDetails.getUser());
        return "councils/detail";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('DEAN', 'ADMIN')")
    public String createCouncil(@RequestParam("councilCode") String code,
                               @RequestParam("councilName") String name,
                               @RequestParam("periodId") Long periodId,
                               @RequestParam("defenseDate") String defenseDateStr,
                               @RequestParam("location") String location,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime defenseDate;
            try {
                defenseDate = LocalDateTime.parse(defenseDateStr);
            } catch (Exception parseEx) {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                defenseDate = LocalDateTime.parse(defenseDateStr, formatter);
            }
            councilService.createCouncil(code, name, periodId, defenseDate, location);
            redirectAttributes.addFlashAttribute("successMessage", "Thành lập hội đồng phản biện mới thành công.");
            return "redirect:/councils?periodId=" + periodId + "&created=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
            return "redirect:/councils?periodId=" + periodId;
        }
    }

    @PostMapping("/{id}/add-member")
    @PreAuthorize("hasAnyRole('DEAN', 'ADMIN')")
    public String addMember(@PathVariable("id") Long councilId,
                            @RequestParam("lecturerId") Long lecturerId,
                            @RequestParam("position") CouncilMember.CouncilPosition position,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            councilService.addCouncilMember(councilId, lecturerId, position);
            redirectAttributes.addFlashAttribute("successMessage", "Thành viên đã được thêm vào hội đồng thành công.");
            return "redirect:/councils/" + councilId + "?memberAdded=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
            return "redirect:/councils/" + councilId;
        }
    }

    @PostMapping("/{id}/remove-member/{lecturerId}")
    @PreAuthorize("hasAnyRole('DEAN', 'ADMIN')")
    public String removeMember(@PathVariable("id") Long councilId,
                              @PathVariable("lecturerId") Long lecturerId,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            councilService.removeCouncilMember(councilId, lecturerId);
            long remaining = councilMemberRepository.countByCouncilId(councilId);
            if (remaining < 3) {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Đã xóa thành viên khỏi hội đồng. Lưu ý: Hội đồng hiện chỉ còn " + remaining + "/3 GV tối thiểu, cần bổ sung đủ trước khi chấm điểm.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thành viên khỏi hội đồng thành công.");
            }
            return "redirect:/councils/" + councilId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
            return "redirect:/councils/" + councilId;
        }
    }

    @PostMapping("/{id}/assign-topic")
    @PreAuthorize("hasAnyRole('DEAN', 'ADMIN')")
    public String assignTopic(@PathVariable("id") Long councilId,
                             @RequestParam("groupId") Long groupId,
                             @RequestParam(value = "reviewerId", required = false) Long reviewerId,
                             @RequestParam(value = "order", defaultValue = "1") Integer order,
                             org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            councilService.assignTopicToCouncil(councilId, groupId, reviewerId, order);
            redirectAttributes.addFlashAttribute("successMessage", "Phân công đề tài vào hội đồng thành công.");
            return "redirect:/councils/" + councilId + "?topicAssigned=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
            return "redirect:/councils/" + councilId;
        }
    }

    @PostMapping("/grade")
    @PreAuthorize("hasAnyRole('LECTURER', 'HEAD_OF_DEPT', 'DEAN', 'ADMIN')")
    public String gradeTopic(@ModelAttribute com.quanlydetai.dto.CouncilGradeDto gradeDto,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            councilService.gradeTopic(
                    gradeDto.getCouncilTopicId(),
                    userDetails.getUser(),
                    gradeDto.getEvalType(),
                    gradeDto.getC1(),
                    gradeDto.getC2(),
                    gradeDto.getC3(),
                    gradeDto.getComments()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Chấm điểm đề tài thành công!");
            return "redirect:/councils/" + gradeDto.getCouncilId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
            return "redirect:/councils/" + gradeDto.getCouncilId();
        }
    }

    @PostMapping("/publish/{councilTopicId}")
    @PreAuthorize("hasAnyRole('DEAN', 'HEAD_OF_DEPT', 'ADMIN')")
    public String publishResult(@PathVariable("councilTopicId") Long councilTopicId,
                                @RequestParam("councilId") Long councilId,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            councilService.publishResults(councilTopicId);
            redirectAttributes.addFlashAttribute("successMessage", "Kết quả điểm bảo vệ đã được công bố chính thức cho sinh viên tra cứu.");
            return "redirect:/councils/" + councilId + "?published=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
            return "redirect:/councils/" + councilId;
        }
    }
}
