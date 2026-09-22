package com.quanlydetai.controller;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.repository.RegistrationPeriodRepository;
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
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiService aiService;
    private final RegistrationPeriodRepository periodRepository;

    @GetMapping("/assistant")
    public String assistantPage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RegistrationPeriod> periods = periodRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("periods", periods);
        model.addAttribute("currentUser", userDetails.getUser());
        return "ai/assistant";
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
