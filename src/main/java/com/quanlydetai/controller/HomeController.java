package com.quanlydetai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("message", "Chào mừng đến với Hệ thống Quản lý Đề tài Sinh viên!");
        return "index";
    }
}