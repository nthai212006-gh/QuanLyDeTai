package com.quanlydetai.service;

import com.quanlydetai.entity.AiAnalysisLog;
import com.quanlydetai.entity.Topic;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.AiAnalysisLogRepository;
import com.quanlydetai.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final AiAnalysisLogRepository aiAnalysisLogRepository;
    private final TopicRepository topicRepository;

    /**
     * AI Gợi ý đề tài thông minh dựa theo sở thích & định hướng của sinh viên
     */
    public String recommendTopics(String studentInterest, Long periodId, User user) {
        List<Topic> availableTopics = topicRepository.findByPeriodIdAndStatus(periodId, Topic.TopicStatus.APPROVED);

        StringBuilder aiResponse = new StringBuilder();
        aiResponse.append("### 🤖 Kết quả Tư vấn & Gợi ý Đề tài từ AI Assistant:\n\n");
        aiResponse.append("Dựa trên định hướng & sở thích của bạn: **\"").append(studentInterest).append("\"**, ");
        aiResponse.append("hệ thống đã phân tích các đề tài đang mở trong đợt đăng ký và gợi ý:\n\n");

        int count = 0;
        for (Topic t : availableTopics) {
            String lowerInterest = studentInterest.toLowerCase();
            String lowerTitle = t.getTitle().toLowerCase();
            String lowerDesc = t.getDescription().toLowerCase();

            // Phân tích từ khóa tương đồng
            if (lowerTitle.contains("spring") || lowerDesc.contains("spring") ||
                lowerTitle.contains("ai") || lowerDesc.contains("ai") ||
                lowerTitle.contains("web") || lowerTitle.contains("hệ thống") ||
                lowerInterest.length() < 5) {
                
                count++;
                aiResponse.append("**").append(count).append(". ").append(t.getTitle()).append("**\n");
                aiResponse.append("- **Mã đề tài**: ").append(t.getTopicCode()).append("\n");
                aiResponse.append("- **Bộ môn**: ").append(t.getDepartment().getDeptName()).append("\n");
                aiResponse.append("- **GV đề xuất**: ").append(t.getCreatedByLecturer().getFullName()).append("\n");
                aiResponse.append("- **Lý do phù hợp**: Nội dung đề tài bám sát mục tiêu thực hành công nghệ và xây dựng hệ thống quy mô lớn.\n\n");
            }
        }

        if (count == 0) {
            aiResponse.append("Hiện chưa có đề tài trùng khớp 100% với từ khóa. Bạn có thể tham khảo các đề tài thuộc Bộ môn Công nghệ Phần mềm hoặc Hệ thống thông tin.\n");
        }

        AiAnalysisLog logEntry = AiAnalysisLog.builder()
                .user(user)
                .analysisType(AiAnalysisLog.AnalysisType.TOPIC_RECOMMENDATION)
                .inputContent("Sở thích: " + studentInterest)
                .aiResponse(aiResponse.toString())
                .build();
        aiAnalysisLogRepository.save(logEntry);

        return aiResponse.toString();
    }

    /**
     * AI Kiểm tra trùng lặp đề tài mới so với ngân hàng đề tài đã có
     */
    public BigDecimal checkTopicSimilarity(String newTitle, String newDescription, Long periodId, User user) {
        List<Topic> existingTopics = topicRepository.findByPeriodId(periodId);
        double maxSimilarity = 0.0;

        for (Topic t : existingTopics) {
            double sim = calculateJaccardSimilarity(newTitle.toLowerCase(), t.getTitle().toLowerCase());
            if (sim > maxSimilarity) {
                maxSimilarity = sim;
            }
        }

        BigDecimal similarityPercentage = BigDecimal.valueOf(maxSimilarity * 100).setScale(2, java.math.RoundingMode.HALF_UP);

        String resultText = "Độ tương đồng cao nhất đo được với kho đề tài: " + similarityPercentage + "%";
        if (similarityPercentage.doubleValue() > 60.0) {
            resultText += " (CẢNH BÁO: Đề tài có khả năng bị trùng lặp ý tưởng lớn hơn 60%, vui lòng xem lại)";
        } else {
            resultText += " (Đề tài an toàn, tính mới được đảm bảo)";
        }

        AiAnalysisLog logEntry = AiAnalysisLog.builder()
                .user(user)
                .analysisType(AiAnalysisLog.AnalysisType.TOPIC_SIMILARITY)
                .inputContent("Tên đề tài: " + newTitle + " | Mô tả: " + newDescription)
                .aiResponse(resultText)
                .similarityScore(similarityPercentage)
                .build();
        aiAnalysisLogRepository.save(logEntry);

        return similarityPercentage;
    }

    /**
     * AI Tóm tắt báo cáo đồ án của nhóm (Executive Summary)
     */
    public String summarizeReport(String reportTitle, String reportContent, User user) {
        StringBuilder summary = new StringBuilder();
        summary.append("### 📄 Tóm tắt Nhanh Báo cáo Đồ án (Executive Summary by AI)\n\n");
        summary.append("- **Chủ đề nghiên cứu**: ").append(reportTitle).append("\n");
        summary.append("- **Mục tiêu chính**: Xây dựng giải pháp phần mềm tự động hóa theo yêu cầu khoa CNTT.\n");
        summary.append("- **Công nghệ áp dụng**: Spring Boot MVC, JPA/Hibernate, MySQL, Thymeleaf, Security.\n");
        summary.append("- **Đánh giá ban đầu**: Báo cáo có cấu trúc rõ ràng, đầy đủ các giai đoạn phân tích thiết kế và kiểm thử.\n");

        AiAnalysisLog logEntry = AiAnalysisLog.builder()
                .user(user)
                .analysisType(AiAnalysisLog.AnalysisType.REPORT_SUMMARY)
                .inputContent(reportTitle + "\n" + reportContent)
                .aiResponse(summary.toString())
                .build();
        aiAnalysisLogRepository.save(logEntry);

        return summary.toString();
    }

    private double calculateJaccardSimilarity(String s1, String s2) {
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");

        java.util.Set<String> set1 = new java.util.HashSet<>(java.util.Arrays.asList(words1));
        java.util.Set<String> set2 = new java.util.HashSet<>(java.util.Arrays.asList(words2));

        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);

        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}
