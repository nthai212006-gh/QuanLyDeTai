package com.quanlydetai.controller;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.service.AiService;
import com.quanlydetai.dto.AiRecommendRequest;
import com.quanlydetai.dto.AiSimilarityRequest;
import com.quanlydetai.dto.AiSimilarityResponse;
import com.quanlydetai.dto.AiSummaryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import com.quanlydetai.entity.User;
import com.quanlydetai.service.AiChatbotService;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiService aiService;
    private final AiChatbotService aiChatbotService;

    @GetMapping("/chatbot")
    public String chatbotPage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        String salutation = aiChatbotService.getNormalizedSalutation(user);
        boolean isStaff = aiChatbotService.isLecturerOrStaff(user);
        String welcomeGreeting = (isStaff ? "Kính chào " : "Xin chào ") + salutation + ", hệ thống sẵn sàng hỗ trợ tra cứu thông tin học vụ.";
        model.addAttribute("currentUser", user);
        model.addAttribute("welcomeGreeting", welcomeGreeting);
        model.addAttribute("chatHistory", aiChatbotService.getChatHistory(user.getId()));
        return "ai/chatbot";
    }

    @GetMapping("/assistant")
    public String assistantPage() {
        return "redirect:/ai/chatbot";
    }

    @PostMapping("/chat")
    @ResponseBody
    public ResponseEntity<?> chatMessage(@RequestBody Map<String, String> payload,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        String message = payload.get("message");
        String reply = aiChatbotService.processChat(message, userDetails.getUser());
        String timeNow = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        return ResponseEntity.ok(Map.of(
                "reply", reply,
                "timestamp", timeNow
        ));
    }

    @PostMapping("/clear-chat")
    @ResponseBody
    public ResponseEntity<?> clearChat(@AuthenticationPrincipal CustomUserDetails userDetails) {
        aiChatbotService.clearChatHistory(userDetails.getUser().getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/recommend")
    @ResponseBody
    public ResponseEntity<?> recommendTopics(@Valid @RequestBody AiRecommendRequest request,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        String result = aiService.recommendTopics(request.getInterest(), request.getPeriodId(), userDetails.getUser());
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/check-similarity")
    @ResponseBody
    public ResponseEntity<AiSimilarityResponse> checkSimilarity(@Valid @RequestBody AiSimilarityRequest request,
                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        BigDecimal score = aiService.checkTopicSimilarity(request.getTitle(), request.getDescription(), request.getPeriodId(), userDetails.getUser());
        boolean isDuplicate = score.doubleValue() > 60.0;
        String message = isDuplicate ?
                "Cảnh báo: Tỷ lệ tương đồng " + score + "% với kho đề tài cũ!" :
                "Đề tài đạt chuẩn tính mới (Tương đồng " + score + "%)";

        return ResponseEntity.ok(AiSimilarityResponse.builder()
                .score(score)
                .isDuplicate(isDuplicate)
                .message(message)
                .build());
    }

    @PostMapping("/summarize")
    @ResponseBody
    public ResponseEntity<?> summarizeReport(@Valid @RequestBody AiSummaryRequest request,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        String summary = aiService.summarizeReport(request.getTitle(), request.getContent(), userDetails.getUser());
        return ResponseEntity.ok(Map.of("summary", summary));
    }
}
