package com.quanlydetai.controller;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.service.PeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/periods")
@RequiredArgsConstructor
public class PeriodController {

    private final PeriodService periodService;

    @GetMapping
    public String listPeriods(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("periods", periodService.getAllPeriods());
        model.addAttribute("currentUser", userDetails.getUser());
        return "periods/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('DEAN', 'ADMIN')")
    public String showCreateForm(Model model) {
        model.addAttribute("period", new RegistrationPeriod());
        model.addAttribute("periodTypes", RegistrationPeriod.PeriodType.values());
        return "periods/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('DEAN', 'ADMIN')")
    public String createPeriod(@ModelAttribute("period") RegistrationPeriod period,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        try {
            periodService.createPeriod(period, userDetails.getUser());
            return "redirect:/periods?success=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("period", period);
            model.addAttribute("periodTypes", RegistrationPeriod.PeriodType.values());
            model.addAttribute("errorMessage", e.getMessage());
            return "periods/create";
        }
    }
}
