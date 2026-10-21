package com.quanlydetai.service;

import com.quanlydetai.service.gemini.GeminiApiClient;
import com.quanlydetai.service.gemini.GeminiContextBuilder;

import com.quanlydetai.entity.*;
import com.quanlydetai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiChatbotService {

    private final RegistrationPeriodRepository periodRepository;
    private final StudentGroupRepository groupRepository;
    private final WeeklyProgressReportRepository weeklyReportRepository;
    private final TopicSubmissionRepository submissionRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final AiAnalysisLogRepository aiAnalysisLogRepository;
    private final TopicRepository topicRepository;
    private final CouncilRepository councilRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GeminiApiClient geminiApiClient;
    private final GeminiContextBuilder geminiContextBuilder;

    public AiChatbotService(
            RegistrationPeriodRepository periodRepository,
            StudentGroupRepository groupRepository,
            WeeklyProgressReportRepository weeklyReportRepository,
            TopicSubmissionRepository submissionRepository,
            CouncilMemberRepository councilMemberRepository,
            CouncilTopicRepository councilTopicRepository,
            AiAnalysisLogRepository aiAnalysisLogRepository,
            TopicRepository topicRepository,
            CouncilRepository councilRepository,
            GroupMemberRepository groupMemberRepository,
            GeminiApiClient geminiApiClient,
            GeminiContextBuilder geminiContextBuilder) {
        this.periodRepository = periodRepository;
        this.groupRepository = groupRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.submissionRepository = submissionRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.councilTopicRepository = councilTopicRepository;
        this.aiAnalysisLogRepository = aiAnalysisLogRepository;
        this.topicRepository = topicRepository;
        this.councilRepository = councilRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.geminiApiClient = geminiApiClient;
        this.geminiContextBuilder = geminiContextBuilder;
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Xử lý câu hỏi của người dùng và trả về phản hồi từ AI Chatbot
     */
    @Transactional
    public String processChat(String message, User user) {
        if (message == null || message.trim().isEmpty()) {
            return "Xin chào! Bạn có thể đặt câu hỏi về các đợt đăng ký, đề tài, nhóm hướng dẫn, báo cáo tiến độ tuần, hoặc hội đồng bảo vệ.";
        }

        String rawQuery = message.trim();

        // 1. ƯU TIÊN 1: Gọi Google Gemini Generative AI Engine nếu khả dụng
        if (geminiApiClient != null && geminiApiClient.isAvailable()) {
            try {
                String scopedPrompt = geminiContextBuilder.buildScopedContext(rawQuery, user);
                Optional<String> geminiResponse = geminiApiClient.chat(scopedPrompt, rawQuery);

                if (geminiResponse.isPresent() && !geminiResponse.get().isBlank()) {
                    String cleanReply = sanitizeAcademicReply(geminiResponse.get());
                    saveChatLog(user, rawQuery, cleanReply, "CHAT_GEMINI");
                    return cleanReply;
                }
            } catch (Exception e) {
                log.warn("Gemini Engine không phản hồi, tự động chuyển sang Fallback: {}", e.getMessage());
            }
        }

        // 2. ƯU TIÊN 2 (FALLBACK): Chạy Rule-based 18 Intent sẵn có
        String fallbackReply = processRuleBasedChat(rawQuery, user);
        saveChatLog(user, rawQuery, fallbackReply, "CHAT_FALLBACK");
        return fallbackReply;
    }

    private void saveChatLog(User user, String query, String reply, String actionType) {
        try {
            AiAnalysisLog logEntry = AiAnalysisLog.builder()
                    .user(user)
                    .actionType(actionType)
                    .queryInput(query)
                    .aiOutput(reply)
                    .build();
            aiAnalysisLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Không thể lưu nhật ký chat AI ({}): {}", actionType, e.getMessage());
        }
    }

    private String sanitizeAcademicReply(String text) {
        if (text == null) return "";
        return text.replaceAll("[\ud800\udc00-\udbff\udfff\u2600-\u26ff\u2700-\u27bf]", "").trim();
    }

    /**
     * Engine xử lý 18 Intent Rule-based dự phòng (Zero-Downtime Fallback)
     */
    public String processRuleBasedChat(String rawQuery, User user) {
        String lower = rawQuery.toLowerCase();
        String response;

        // 1. Chi tiết đề tài cụ thể / Thông tin về đề tài
        if (matchesAny(lower, "thông tin về đề tài", "thong tin ve de tai", "thông tin đề tài", "thong tin de tai", 
                              "chi tiết đề tài", "chi tiet de tai", "nội dung đề tài", "noi dung de tai", 
                              "mô tả đề tài", "mo ta de tai")) {
            response = handleTopicDetail(rawQuery);
        }
        // 2. Số lượng đề tài trong đợt
        else if (matchesAny(lower, "bao nhiêu đề tài", "bao nhieu de tai", "mấy đề tài", "may de tai", 
                                  "số lượng đề tài", "so luong de tai", "tổng số đề tài", "tong so de tai")) {
            response = handleTopicsCountInPeriod(rawQuery);
        }
        // 3. Đề tài chưa có nhóm đăng ký / còn trống
        else if (matchesAny(lower, "chưa có nhóm", "chua co nhom", "chưa ai đăng ký", "chua ai dang ky", 
                                  "còn trống", "con trong", "đề tài trống", "de tai trong", "chưa được nhận", "chua duoc nhan")) {
            response = handleAvailableTopicsInPeriod(rawQuery);
        }
        // 4. Danh sách đề tài trong đợt
        else if (matchesAny(lower, "đề tài trong đợt", "de tai trong dot", "đề tài nào trong đợt", "de tai nao trong dot", 
                                  "những đề tài trong đợt", "nhung de tai trong dot", "danh sách đề tài", "danh sach de tai", 
                                  "các đề tài đợt", "cac de tai dot", "đề tài của đợt", "de tai cua dot", "các đề tài trong đợt", "cac de tai trong dot")) {
            response = handleTopicsInPeriod(rawQuery);
        }
        // 5. Hội đồng sắp tới cần tham gia là khi nào
        else if ((matchesAny(lower, "sắp tới", "sap toi", "tiếp theo", "tiep theo", "gần nhất", "gan nhat") && matchesAny(lower, "hội đồng", "hoi dong"))
                 || matchesAny(lower, "khi nào tham gia hội đồng", "khi nao tham gia hoi dong", "khi nào bảo vệ", "khi nao bao ve", 
                                     "khi nào chấm hội đồng", "khi nao cham hoi dong", "lịch hội đồng", "lich hoi dong", 
                                     "lịch bảo vệ của tôi", "lich bao ve cua toi", "lịch chấm hội đồng", "lich cham hoi dong")) {
            response = handleUpcomingCouncil(user);
        }
        // 6. Danh sách Nhóm & đề tài trong hội đồng (sắp tới hoặc cụ thể)
        else if (matchesAny(lower, "nhóm và đề tài trong hội đồng", "nhom va de tai trong hoi dong", 
                                  "nhóm trong hội đồng", "nhom trong hoi dong", "đề tài trong hội đồng", "de tai trong hoi dong", 
                                  "danh sách nhóm hội đồng", "danh sach nhom hoi dong", "các nhóm chấm trong hội đồng", "cac nhom cham trong hoi dong", 
                                  "nhóm bảo vệ trong hội đồng", "nhom bao ve trong hoi dong", "danh sách nhóm trong hội đồng", "danh sach nhom trong hoi dong")) {
            response = handleCouncilAssignedGroupsAndTopics(rawQuery, user);
        }
        // 7. Thông tin chi tiết về đợt đăng ký
        else if (matchesAny(lower, "thông tin về đợt", "thong tin ve dot", "thông tin đợt", "thong tin dot", 
                                  "chi tiết đợt", "chi tiet dot", "thời gian của đợt", "thoi gian cua dot", 
                                  "kế hoạch của đợt", "ke hoach cua dot", "thời hạn đợt", "thoi han dot")) {
            response = handlePeriodDetail(rawQuery);
        }
        // 8. Thông tin nhóm sinh viên / thành viên nhóm
        else if (matchesAny(lower, "thành viên nhóm", "thanh vien nhom", "sinh viên trong nhóm", "sinh vien trong nhom", 
                                  "thông tin nhóm", "thong tin nhom")) {
            response = handleGroupDetail(rawQuery, user);
        }
        // 9. Kiểm tra ý định: Đợt đăng ký chung
        else if (matchesAny(lower, "đợt đăng ký", "dot dang ky", "đợt nào đang diễn ra", "dot nao dang dien ra", 
                              "đợt đang mở", "dot dang mo", "kế hoạch đăng ký", "ke hoach dang ky",
                              "đang có đợt nào", "dang co dot nao", "có những đợt nào", "co nhung dot nao")) {
            response = handleRegistrationPeriods();
        }
        // 10. Kiểm tra ý định: Số lượng nhóm đang hướng dẫn
        else if (matchesAny(lower, "bao nhiêu nhóm", "bao nhieu nhom", "mấy nhóm", "may nhom", 
                                  "số lượng nhóm hướng dẫn", "so luong nhom huong dan", "hướng dẫn bao nhiêu", "huong dan bao nhieu")) {
            response = handleSupervisedGroupsCount(user);
        }
        // 11. Kiểm tra ý định: Danh sách các nhóm đang hướng dẫn
        else if (matchesAny(lower, "những nhóm nào", "nhung nhom nao", "nhóm nào", "nhom nao", "hướng dẫn", "huong dan", 
                                  "danh sách nhóm", "danh sach nhom", "nhóm tôi", "nhom toi", "nhóm đang hướng dẫn") 
                 && matchesAny(lower, "hướng dẫn", "huong dan", "của tôi", "cua toi", "tôi đang", "toi dang", "thầy", "cô")) {
            response = handleSupervisedGroupsList(user);
        }
        // 12. Kiểm tra ý định: Báo cáo tuần đang chờ duyệt
        else if (matchesAny(lower, "chờ duyệt", "cho duyet", "chưa duyệt", "chua duyet") 
                 && matchesAny(lower, "báo cáo", "bao cao", "tuần", "tuan")) {
            response = handlePendingWeeklyReports(user);
        }
        // 13. Kiểm tra ý định: Nhóm nào đã nộp báo cáo tuần
        else if (matchesAny(lower, "đã nộp", "da nop", "nộp báo cáo", "nop bao cao") 
                 && matchesAny(lower, "báo cáo tuần", "bao cao tuan", "tiến độ tuần", "tien do tuan", "tuần", "tuan")) {
            response = handleSubmittedWeeklyReports(user);
        }
        // 14. Kiểm tra ý định: Nhóm nào đã nộp báo cáo hoàn chỉnh / toàn văn
        else if (matchesAny(lower, "hoàn chỉnh", "hoan chinh", "toàn văn", "toan van", "khóa luận hoàn chỉnh", "khoa luan hoan chinh")
                 || (matchesAny(lower, "báo cáo", "bao cao") && matchesAny(lower, "hoàn chỉnh", "hoan chinh", "cuối kỳ", "cuoi ky", "bảo vệ", "bao ve"))) {
            response = handleFinalThesisSubmissions(user);
        }
        // 15. Kiểm tra ý định: Tham gia bao nhiêu hội đồng
        else if (matchesAny(lower, "bao nhiêu hội đồng", "bao nhieu hoi dong", "mấy hội đồng", "may hoi dong", "số lượng hội đồng", "so luong hoi dong")) {
            response = handleCouncilsCount(user);
        }
        // 16. Kiểm tra ý định: Tham gia những hội đồng nào (chủ tịch, thư ký, phản biện, ủy viên)
        else if (matchesAny(lower, "hội đồng", "hoi dong", "chủ tịch", "chu tich", "thư ký", "thu ky", "phản biện", "phan bien", "ủy viên", "uy vien")) {
            response = handleCouncilsList(user, lower);
        }
        // 17. Lời chào hỏi
        else if (matchesAny(lower, "xin chào", "xin chao", "chào", "chao", "hello", "hi", "alo", "hey")) {
            response = handleGreeting(user);
        }
        // 18. Trợ giúp / Hướng dẫn
        else if (matchesAny(lower, "giúp", "giup", "làm được gì", "lam duoc gi", "chức năng", "chuc nang", "hướng dẫn", "huong dan", "bạn là ai", "ban la ai")) {
            response = handleHelp(user);
        }
        // 19. Mặc định / Chưa nhận diện được câu hỏi cụ thể
        else {
            response = handleFallback(rawQuery, user);
        }

        return response;
    }

    /**
     * Lấy lịch sử hội thoại gần nhất của người dùng (Bao gồm cả tin nhắn cũ, Gemini và Fallback)
     */
    public List<AiAnalysisLog> getChatHistory(Long userId) {
        return aiAnalysisLogRepository.findTop30ByUserIdAndActionTypeInOrderByCreatedAtAsc(
                userId, Arrays.asList("CHAT_MESSAGE", "CHAT_GEMINI", "CHAT_FALLBACK"));
    }

    /**
     * Xóa toàn bộ lịch sử chat của người dùng
     */
    @Transactional
    public void clearChatHistory(Long userId) {
        aiAnalysisLogRepository.deleteByUserIdAndActionTypeIn(
                userId, Arrays.asList("CHAT_MESSAGE", "CHAT_GEMINI", "CHAT_FALLBACK"));
    }

    // =========================================================================
    // CÁC HÀM XỬ LÝ NGHIỆP VỤ MỚI BỔ SUNG
    // =========================================================================

    /**
     * A. Có những đề tài nào trong đợt ...?
     */
    private String handleTopicsInPeriod(String query) {
        RegistrationPeriod period = findPeriodFromQuery(query);
        if (period == null) {
            return "### Danh Sách Đề Tài\n\nHiện tại hệ thống **chưa ghi nhận đợt đăng ký nào**.";
        }

        List<Topic> topics = topicRepository.findByPeriodId(period.getId());
        if (topics.isEmpty()) {
            return "### Danh Sách Đề Tài - " + period.getPeriodName() + "\n\n" +
                   "Hiện tại trong đợt này **chưa có đề tài nào** được tạo.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### Danh Sách Đề Tài Trong Đợt: **").append(period.getPeriodName()).append("**\n\n");
        sb.append("Tổng số đề tài: **").append(topics.size()).append("** đề tài\n\n");

        int index = 1;
        for (Topic t : topics) {
            String statusBadge = translateTopicStatusBadge(t.getStatus());
            String supervisor = t.getSupervisorsDisplayString();
            String deptName = t.getDepartment() != null ? t.getDepartment().getDeptName() : "N/A";

            sb.append("**").append(index++).append(". ").append(t.getTitle()).append("**\n");
            sb.append("- **Mã đề tài**: `").append(t.getTopicCode()).append("`\n");
            sb.append("- **GV hướng dẫn**: ").append(supervisor).append("\n");
            sb.append("- **Bộ môn**: ").append(deptName).append("\n");
            sb.append("- **Trạng thái**: ").append(statusBadge).append("\n\n");
        }

        sb.append(" *Thầy/Cô có thể hỏi: \"Thông tin về đề tài [Mã đề tài]\" để xem mục tiêu & yêu cầu chi tiết.*");
        return sb.toString();
    }

    /**
     * B. Có bao nhiêu đề tài trong đợt ...?
     */
    private String handleTopicsCountInPeriod(String query) {
        RegistrationPeriod period = findPeriodFromQuery(query);
        if (period == null) {
            return "### Thống Kê Số Lượng Đề Tài\n\nHiện tại hệ thống **chưa ghi nhận đợt đăng ký nào**.";
        }

        long totalCount = topicRepository.countByPeriodId(period.getId());
        long approvedCount = topicRepository.countByPeriodIdAndStatus(period.getId(), Topic.TopicStatus.APPROVED);
        long pendingCount = topicRepository.countByPeriodIdAndStatus(period.getId(), Topic.TopicStatus.PENDING_APPROVAL);
        long assignedCount = topicRepository.countByPeriodIdAndStatus(period.getId(), Topic.TopicStatus.ASSIGNED);
        long rejectedCount = topicRepository.countByPeriodIdAndStatus(period.getId(), Topic.TopicStatus.REJECTED);
        long completedCount = topicRepository.countByPeriodIdAndStatus(period.getId(), Topic.TopicStatus.COMPLETED);

        List<Topic> availableTopics = topicRepository.findAvailableTopicsForRegistration(period.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("### Thống Kê Số Lượng Đề Tài Trong Đợt: **").append(period.getPeriodName()).append("**\n\n");
        sb.append("-  **Tổng số đề tài đã đề xuất**: **").append(totalCount).append("** đề tài\n");
        sb.append("-  **Đã được phê duyệt (APPROVED)**: **").append(approvedCount).append("** đề tài\n");
        sb.append("-  **Đang chờ bộ môn duyệt (PENDING)**: **").append(pendingCount).append("** đề tài\n");
        sb.append("-  **Đã có nhóm nhận (ASSIGNED)**: **").append(assignedCount).append("** đề tài\n");
        if (completedCount > 0) {
            sb.append("-  **Đã hoàn tất bảo vệ (COMPLETED)**: **").append(completedCount).append("** đề tài\n");
        }
        if (rejectedCount > 0) {
            sb.append("-  **Bị từ chối duyệt (REJECTED)**: **").append(rejectedCount).append("** đề tài\n");
        }
        sb.append("-  **Đề tài còn trống sinh viên có thể đăng ký**: **").append(availableTopics.size()).append("** đề tài\n");

        return sb.toString();
    }

    /**
     * C. Đề tài chưa có nhóm đăng ký / còn trống trong đợt
     */
    private String handleAvailableTopicsInPeriod(String query) {
        RegistrationPeriod period = findPeriodFromQuery(query);
        if (period == null) {
            return "### Đề Tài Còn Trống\n\nHiện tại hệ thống **chưa ghi nhận đợt đăng ký nào**.";
        }

        List<Topic> availableTopics = topicRepository.findAvailableTopicsForRegistration(period.getId());
        if (availableTopics.isEmpty()) {
            return "### Đề Tài Còn Trống - " + period.getPeriodName() + "\n\n" +
                   "Hiện tại **không còn đề tài nào trống** trong đợt này (Tất cả đề tài đã được các nhóm đăng ký hoặc chưa được duyệt).";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### Danh Sách Đề Tài Còn Trống Trong Đợt: **").append(period.getPeriodName()).append("**\n\n");
        sb.append("Có **").append(availableTopics.size()).append("** đề tài đã được duyệt và chưa có nhóm đăng ký:\n\n");

        int index = 1;
        for (Topic t : availableTopics) {
            sb.append("**").append(index++).append(". ").append(t.getTitle()).append("**\n");
            sb.append("- **Mã đề tài**: `").append(t.getTopicCode()).append("`\n");
            sb.append("- **GV hướng dẫn**: ").append(t.getSupervisorsDisplayString()).append("\n");
            sb.append("- **Bộ môn**: ").append(t.getDepartment() != null ? t.getDepartment().getDeptName() : "N/A").append("\n");
            sb.append("- **Số nhóm tối đa**: ").append(t.getMaxGroups() != null ? t.getMaxGroups() : 1).append(" nhóm\n\n");
        }

        return sb.toString();
    }

    /**
     * D. Hội đồng sắp tới cần tham gia là khi nào?
     */
    private String handleUpcomingCouncil(User user) {
        List<CouncilMember> memberships = councilMemberRepository.findByLecturerId(user.getId());
        Map<Long, CouncilMember.CouncilPosition> roleMap = new HashMap<>();
        for (CouncilMember cm : memberships) {
            if (cm.getCouncil() != null) {
                roleMap.put(cm.getCouncil().getId(), cm.getPosition());
            }
        }

        List<CouncilTopic> reviewerAssignments = councilTopicRepository.findByReviewerLecturerId(user.getId());
        for (CouncilTopic ct : reviewerAssignments) {
            if (ct.getCouncil() != null && !roleMap.containsKey(ct.getCouncil().getId())) {
                roleMap.put(ct.getCouncil().getId(), CouncilMember.CouncilPosition.REVIEWER);
            }
        }

        if (roleMap.isEmpty()) {
            return "### ️ Lịch Hội Đồng Sắp Tới\n\nHiện tại Thầy/Cô **chưa được phân công vào hội đồng nào** trong hệ thống.";
        }

        List<Council> allCouncils = councilRepository.findAllById(roleMap.keySet());

        // Ưu tiên các hội đồng chưa kết thúc, xếp theo ngày bảo vệ gần nhất
        List<Council> upcomingCouncils = allCouncils.stream()
                .filter(c -> c.getStatus() != Council.CouncilStatus.COMPLETED)
                .sorted((c1, c2) -> {
                    if (c1.getDefenseDate() == null && c2.getDefenseDate() == null) return 0;
                    if (c1.getDefenseDate() == null) return 1;
                    if (c2.getDefenseDate() == null) return -1;
                    return c1.getDefenseDate().compareTo(c2.getDefenseDate());
                })
                .collect(Collectors.toList());

        if (upcomingCouncils.isEmpty()) {
            return "### ️ Lịch Hội Đồng Sắp Tới\n\n" +
                   "Hiện tại Thầy/Cô **không có hội đồng nào sắp tới** (Các hội đồng được phân công trước đây đều đã hoàn tất bảo vệ).";
        }

        Council nextCouncil = upcomingCouncils.get(0);
        CouncilMember.CouncilPosition pos = roleMap.get(nextCouncil.getId());
        String posName = translateCouncilPosition(pos);
        String defenseTime = (nextCouncil.getDefenseDate() != null) 
                ? nextCouncil.getDefenseDate().format(DATE_TIME_FORMATTER) 
                : "Chưa ấn định thời gian cụ thể";
        String location = (nextCouncil.getLocation() != null && !nextCouncil.getLocation().trim().isEmpty()) 
                ? nextCouncil.getLocation() 
                : "Chưa xếp phòng bảo vệ";
        int topicCount = (nextCouncil.getAssignedTopics() != null) ? nextCouncil.getAssignedTopics().size() : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("### ️ Hội Đồng Sắp Tới Thầy/Cô Cần Tham Gia\n\n");
        sb.append("Hội đồng gần nhất của Thầy/Cô là: **").append(nextCouncil.getCouncilName()).append("**\n\n");
        sb.append("- **Mã hội đồng**: `").append(nextCouncil.getCouncilCode()).append("`\n");
        sb.append("- **Vai trò của Thầy/Cô**: **").append(posName).append("**\n");
        sb.append("- ⏰ **Thời gian bảo vệ**: `").append(defenseTime).append("`\n");
        sb.append("-  **Địa điểm / Phòng**: **").append(location).append("**\n");
        sb.append("-  **Số nhóm đề tài báo cáo**: **").append(topicCount).append(" nhóm**\n");
        sb.append("-  **Trạng thái**: `").append(nextCouncil.getStatus()).append("`\n\n");

        if (upcomingCouncils.size() > 1) {
            sb.append("#### Các Hội Đồng Khác Cần Tham Gia Tiếp Theo:\n");
            for (int i = 1; i < upcomingCouncils.size(); i++) {
                Council oc = upcomingCouncils.get(i);
                String dt = (oc.getDefenseDate() != null) ? oc.getDefenseDate().format(DATE_TIME_FORMATTER) : "Chưa có lịch";
                String ocPos = translateCouncilPosition(roleMap.get(oc.getId()));
                sb.append("- **").append(oc.getCouncilName()).append("** (`").append(oc.getCouncilCode()).append("`): ")
                  .append(ocPos).append(" | Ngày: `").append(dt).append("` | Phòng: ").append(oc.getLocation() != null ? oc.getLocation() : "TBA").append("\n");
            }
            sb.append("\n");
        }

        sb.append(" *Thầy/Cô có thể hỏi: \"Danh sách nhóm và đề tài trong hội đồng sắp tới\" để xem chi tiết.*");
        return sb.toString();
    }

    /**
     * E. Danh sách Nhóm & đề tài trong hội đồng sắp tới (hoặc hội đồng cụ thể)
     */
    private String handleCouncilAssignedGroupsAndTopics(String query, User user) {
        String lower = query.toLowerCase();
        List<Council> councils = councilRepository.findAll();

        Council targetCouncil = null;

        // Kiểm tra xem người dùng có gõ mã hoặc tên hội đồng cụ thể không
        for (Council c : councils) {
            if (lower.contains(c.getCouncilCode().toLowerCase()) || lower.contains(c.getCouncilName().toLowerCase())) {
                targetCouncil = c;
                break;
            }
        }

        // Nếu không chỉ định, lấy hội đồng sắp tới của user
        if (targetCouncil == null) {
            List<CouncilMember> memberships = councilMemberRepository.findByLecturerId(user.getId());
            Set<Long> userCouncilIds = memberships.stream().map(cm -> cm.getCouncil().getId()).collect(Collectors.toSet());
            for (CouncilTopic ct : councilTopicRepository.findByReviewerLecturerId(user.getId())) {
                if (ct.getCouncil() != null) userCouncilIds.add(ct.getCouncil().getId());
            }

            if (!userCouncilIds.isEmpty()) {
                targetCouncil = councils.stream()
                        .filter(c -> userCouncilIds.contains(c.getId()))
                        .filter(c -> c.getStatus() != Council.CouncilStatus.COMPLETED)
                        .sorted((c1, c2) -> {
                            if (c1.getDefenseDate() == null && c2.getDefenseDate() == null) return 0;
                            if (c1.getDefenseDate() == null) return 1;
                            if (c2.getDefenseDate() == null) return -1;
                            return c1.getDefenseDate().compareTo(c2.getDefenseDate());
                        })
                        .findFirst()
                        .orElse(null);
            }
        }

        if (targetCouncil == null && !councils.isEmpty()) {
            targetCouncil = councils.get(0);
        }

        if (targetCouncil == null) {
            return "### ️ Danh Sách Nhóm & Đề Tài Trong Hội Đồng\n\nHiện tại hệ thống **chưa có hội đồng nào** được tạo.";
        }

        List<CouncilTopic> assignedTopics = councilTopicRepository.findByCouncilId(targetCouncil.getId());
        if (assignedTopics.isEmpty()) {
            return "### ️ Danh Sách Nhóm & Đề Tài - " + targetCouncil.getCouncilName() + "\n\n" +
                   "Hội đồng này hiện tại **chưa được phân công nhóm đề tài nào**.";
        }

        assignedTopics.sort(Comparator.comparing(ct -> ct.getDefenseOrder() != null ? ct.getDefenseOrder() : 999));

        StringBuilder sb = new StringBuilder();
        sb.append("### ️ Danh Sách Nhóm & Đề Tài Thuộc Hội Đồng: **").append(targetCouncil.getCouncilName()).append("**\n\n");
        sb.append("- **Mã hội đồng**: `").append(targetCouncil.getCouncilCode()).append("`\n");
        sb.append("- **Thời gian bảo vệ**: `").append(targetCouncil.getDefenseDate() != null ? targetCouncil.getDefenseDate().format(DATE_TIME_FORMATTER) : "Chưa có lịch").append("`\n");
        sb.append("- **Địa điểm**: ").append(targetCouncil.getLocation() != null ? targetCouncil.getLocation() : "Chưa xếp phòng").append("\n");
        sb.append("- **Tổng số nhóm báo cáo**: **").append(assignedTopics.size()).append(" nhóm**\n\n");

        int index = 1;
        for (CouncilTopic ct : assignedTopics) {
            StudentGroup g = ct.getGroup();
            String groupName = g != null ? g.getGroupName() : "Nhóm #" + ct.getId();
            String leaderName = (g != null && g.getLeader() != null) ? g.getLeader().getFullName() : "N/A";
            Topic t = (g != null) ? g.getTopic() : null;
            String topicTitle = t != null ? t.getTitle() : "Chưa đăng ký đề tài";
            String topicCode = t != null ? t.getTopicCode() : "N/A";
            String supervisor = t != null ? t.getSupervisorsDisplayString() : "N/A";
            String reviewer = ct.getReviewerLecturer() != null ? ct.getReviewerLecturer().getFullName() : "Chưa chỉ định";
            String score = ct.getFinalCouncilScore() != null ? ct.getFinalCouncilScore().toString() + " điểm" : "Chưa chấm điểm";

            sb.append("**").append(index++).append(". ").append(groupName).append(" - ").append(topicTitle).append("**\n");
            sb.append("   - **Mã đề tài**: `").append(topicCode).append("`\n");
            sb.append("   - **Nhóm trưởng**: ").append(leaderName).append("\n");
            sb.append("   - **GV Hướng dẫn**: ").append(supervisor).append("\n");
            sb.append("   - **GV Phản biện**: ").append(reviewer).append("\n");
            sb.append("   - **Thứ tự báo cáo**: Thứ ").append(ct.getDefenseOrder() != null ? ct.getDefenseOrder() : index - 1).append("\n");
            sb.append("   - **Điểm tổng kết HĐ**: `").append(score).append("`\n\n");
        }

        return sb.toString();
    }

    /**
     * F. Thông tin chi tiết về đợt đăng ký ...
     */
    private String handlePeriodDetail(String query) {
        RegistrationPeriod p = findPeriodFromQuery(query);
        if (p == null) {
            return "### Chi Tiết Đợt Đăng Ký\n\nKhông tìm thấy đợt đăng ký nào theo yêu cầu của bạn.";
        }

        String typeName = getPeriodTypeName(p.getPeriodType());
        boolean isActive = Boolean.TRUE.equals(p.getIsActive());

        StringBuilder sb = new StringBuilder();
        sb.append("### Thông Tin Chi Tiết Đợt Đăng Ký: **").append(p.getPeriodName()).append("**\n\n");
        sb.append("- **Mã đợt / ID**: `#").append(p.getId()).append("`\n");
        sb.append("- **Loại hình**: ").append(typeName).append("\n");
        sb.append("- **Năm học**: **").append(p.getAcademicYear()).append("**, Học kỳ: **").append(p.getSemester()).append("**\n");
        sb.append("- **Trạng thái**: ").append(isActive ? "` Đang kích hoạt (Active)`" : "`⚪ Đã đóng (Inactive)`").append("\n\n");

        sb.append("#### ⏰ Mốc Thời Gian & Tiến Độ Chi Tiết:\n");
        sb.append("-  **Giảng viên đề xuất / nộp đề tài**: Từ `")
          .append(p.getTopicSubmissionStart() != null ? p.getTopicSubmissionStart().format(DATE_TIME_FORMATTER) : "N/A")
          .append("` đến `")
          .append(p.getTopicSubmissionEnd() != null ? p.getTopicSubmissionEnd().format(DATE_TIME_FORMATTER) : "N/A").append("`\n");

        sb.append("-  **Sinh viên chọn & đăng ký đề tài**: Từ `")
          .append(p.getStudentRegistrationStart() != null ? p.getStudentRegistrationStart().format(DATE_TIME_FORMATTER) : "N/A")
          .append("` đến `")
          .append(p.getStudentRegistrationEnd() != null ? p.getStudentRegistrationEnd().format(DATE_TIME_FORMATTER) : "N/A").append("`\n");

        sb.append("- ⏳ **Hạn chót nộp báo cáo / Phản biện**: `")
          .append(p.getReviewDeadline() != null ? p.getReviewDeadline().format(DATE_TIME_FORMATTER) : "Chưa có").append("`\n");

        sb.append("- ️ **Ngày dự kiến tổ chức Hội đồng bảo vệ**: `")
          .append(p.getDefenseDate() != null ? p.getDefenseDate().format(DATE_FORMATTER) : "Chưa ấn định").append("`\n\n");

        sb.append("#### Quy Định Chung:\n");
        sb.append("- **Số sinh viên mỗi nhóm**: Tối đa 3 SV/nhóm theo quy định đào tạo.\n");
        sb.append("- **Đề tài mỗi giảng viên**: Tối đa 5 đề tài theo phân công bộ môn.\n");

        return sb.toString();
    }

    /**
     * G. Thông tin chi tiết về đề tài ...
     */
    private String handleTopicDetail(String query) {
        String cleaned = query.replaceAll("(?i)(thông tin về đề tài|thông tin đề tài|chi tiết đề tài|nội dung đề tài|mô tả đề tài|thong tin ve de tai|thong tin de tai|chi tiet de tai|noi dung de tai|mo ta de tai|đề tài|de tai|mã|ma)", "")
                .trim();

        List<Topic> allTopics = topicRepository.findAll();
        Topic matchedTopic = null;

        if (!cleaned.isEmpty()) {
            for (Topic t : allTopics) {
                if (t.getTopicCode().equalsIgnoreCase(cleaned)) {
                    matchedTopic = t;
                    break;
                }
            }
            if (matchedTopic == null) {
                for (Topic t : allTopics) {
                    if (t.getTopicCode().toLowerCase().contains(cleaned.toLowerCase())) {
                        matchedTopic = t;
                        break;
                    }
                }
            }
            if (matchedTopic == null) {
                for (Topic t : allTopics) {
                    if (t.getTitle().toLowerCase().contains(cleaned.toLowerCase())) {
                        matchedTopic = t;
                        break;
                    }
                }
            }
        }

        if (matchedTopic == null) {
            if (allTopics.isEmpty()) {
                return "### Thông Tin Đề Tài\n\nHiện tại hệ thống **chưa có đề tài nào**.";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("### Thông Tin Đề Tài\n\n");
            sb.append("Không tìm thấy đề tài tương ứng với từ khóa `\"").append(cleaned).append("\"`. Bạn vui lòng cung cấp mã đề tài hoặc tên đề tài cụ thể.\n\n");
            sb.append("**Một số đề tài mẫu trong hệ thống:**\n");
            int count = 0;
            for (Topic t : allTopics) {
                sb.append("- `").append(t.getTopicCode()).append("`: **").append(t.getTitle()).append("**\n");
                if (++count >= 5) break;
            }
            return sb.toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### Thông Tin Chi Tiết Đề Tài: **").append(matchedTopic.getTitle()).append("**\n\n");
        sb.append("- **Mã đề tài**: `").append(matchedTopic.getTopicCode()).append("`\n");
        sb.append("- **Đợt đăng ký**: ").append(matchedTopic.getPeriod() != null ? matchedTopic.getPeriod().getPeriodName() : "N/A").append("\n");
        sb.append("- **Bộ môn phụ trách**: ").append(matchedTopic.getDepartment() != null ? matchedTopic.getDepartment().getDeptName() : "N/A").append("\n");
        sb.append("- **Giảng viên hướng dẫn**: **").append(matchedTopic.getSupervisorsDisplayString()).append("**\n");
        sb.append("- **Trạng thái duyệt**: ").append(translateTopicStatusBadge(matchedTopic.getStatus())).append("\n");
        sb.append("- **Số nhóm tối đa**: ").append(matchedTopic.getMaxGroups() != null ? matchedTopic.getMaxGroups() : 1).append(" nhóm\n\n");

        sb.append("#### Mô Tả & Mục Tiêu Nghiên Cứu:\n");
        sb.append(matchedTopic.getDescription() != null && !matchedTopic.getDescription().trim().isEmpty() ? matchedTopic.getDescription() : "Chưa có mô tả chi tiết.").append("\n\n");

        if (matchedTopic.getRequirements() != null && !matchedTopic.getRequirements().trim().isEmpty()) {
            sb.append("#### ⚙️ Yêu Cầu Kỹ Thuật & Kiến Thức:\n");
            sb.append(matchedTopic.getRequirements()).append("\n\n");
        }

        List<StudentGroup> allGroups = groupRepository.findAll();
        final Long targetTopicId = matchedTopic.getId();
        List<StudentGroup> assignedGroups = allGroups.stream()
                .filter(g -> g.getTopic() != null && g.getTopic().getId().equals(targetTopicId))
                .collect(Collectors.toList());

        sb.append("#### Tình Trạng Nhóm Đăng Ký:\n");
        if (assignedGroups.isEmpty()) {
            sb.append(" **Đề tài hiện đang còn trống**, chưa có nhóm sinh viên nào đăng ký.\n");
        } else {
            for (StudentGroup g : assignedGroups) {
                String leaderName = g.getLeader() != null ? g.getLeader().getFullName() : "N/A";
                sb.append("- **").append(g.getGroupName()).append("** (Trưởng nhóm: ").append(leaderName)
                  .append(" - Trạng thái: `").append(translateGroupStatus(g.getStatus())).append("`)\n");
            }
        }

        return sb.toString();
    }

    /**
     * H. Thông tin nhóm sinh viên & danh sách thành viên
     */
    private String handleGroupDetail(String query, User user) {
        String lower = query.toLowerCase();
        List<StudentGroup> groups = groupRepository.findAll();

        StudentGroup matchedGroup = null;
        for (StudentGroup g : groups) {
            if (lower.contains(g.getGroupName().toLowerCase()) || 
                (g.getGroupCode() != null && lower.contains(g.getGroupCode().toLowerCase()))) {
                matchedGroup = g;
                break;
            }
        }

        if (matchedGroup == null) {
            for (StudentGroup g : groups) {
                if (lower.contains("nhóm " + g.getId()) || lower.contains("nhom " + g.getId())) {
                    matchedGroup = g;
                    break;
                }
            }
        }

        if (matchedGroup == null) {
            return "### Thông Tin Nhóm Sinh Viên\n\n" +
                   "Không tìm thấy nhóm sinh viên tương ứng với câu hỏi của Thầy/Cô. " +
                   "Vui lòng chỉ định rõ tên nhóm hoặc mã nhóm (Ví dụ: *\"Thông tin nhóm Nhóm 1\"*).";
        }

        List<GroupMember> members = groupMemberRepository.findByGroupId(matchedGroup.getId());
        Topic topic = matchedGroup.getTopic();

        StringBuilder sb = new StringBuilder();
        sb.append("### Thông Tin Chi Tiết: **").append(matchedGroup.getGroupName()).append("**\n\n");
        sb.append("- **Mã nhóm**: `").append(matchedGroup.getGroupCode()).append("`\n");
        sb.append("- **Đợt đăng ký**: ").append(matchedGroup.getPeriod() != null ? matchedGroup.getPeriod().getPeriodName() : "N/A").append("\n");
        sb.append("- **Trạng thái thực hiện**: `").append(translateGroupStatus(matchedGroup.getStatus())).append("`\n");
        sb.append("- **Đề tài đảm nhận**: **").append(topic != null ? topic.getTitle() : "Chưa chọn đề tài").append("**\n");
        if (topic != null) {
            sb.append("- **GV hướng dẫn**: ").append(topic.getSupervisorsDisplayString()).append("\n");
        }
        sb.append("\n");

        sb.append("#### ‍ Danh Sách Thành Viên (").append(members.size()).append(" SV):\n");
        int idx = 1;
        for (GroupMember gm : members) {
            User s = gm.getStudent();
            String roleTag = gm.getRoleInGroup() == GroupMember.RoleInGroup.LEADER ? "⭐ **Nhóm trưởng**" : "Thành viên";
            sb.append(idx++).append(". **").append(s.getFullName()).append("** (MSSV: `").append(s.getUserCode()).append("`) - ").append(roleTag);
            if (s.getEmail() != null) {
                sb.append(" | Email: ").append(s.getEmail());
            }
            sb.append("\n");
        }

        List<WeeklyProgressReport> reports = weeklyReportRepository.findByGroupIdOrderByWeekNumberAsc(matchedGroup.getId());
        sb.append("\n#### Tình Hình Báo Cáo Tiến Độ Tuần:\n");
        if (reports.isEmpty()) {
            sb.append("Nhóm chưa nộp báo cáo tuần nào.\n");
        } else {
            WeeklyProgressReport latest = reports.get(reports.size() - 1);
            sb.append("Nhóm đã nộp tổng cộng **").append(reports.size()).append(" báo cáo tuần**. Báo cáo gần nhất: **Tuần ")
              .append(latest.getWeekNumber()).append("** (").append(translateFeedbackStatus(latest.getFeedbackStatus())).append(")\n");
        }

        return sb.toString();
    }

    // =========================================================================
    // CÁC HÀM XỬ LÝ NGHIỆP VỤ BAN ĐẦU
    // =========================================================================

    /**
     * 1. Hiện tại đang có những đợt đăng ký nào diễn ra?
     */
    private String handleRegistrationPeriods() {
        List<RegistrationPeriod> periods = periodRepository.findAllByOrderByCreatedAtDesc();
        LocalDateTime now = LocalDateTime.now();

        if (periods.isEmpty()) {
            return "### Thông Báo Đợt Đăng Ký\n\nHiện tại hệ thống **chưa ghi nhận đợt đăng ký nào** được kích hoạt.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### Danh Sách Các Đợt Đăng Ký Hiện Tại\n\n");
        sb.append("Dưới đây là các đợt đăng ký trong hệ thống cùng mốc thời gian chi tiết:\n\n");

        int activeCount = 0;
        for (RegistrationPeriod p : periods) {
            boolean isActive = Boolean.TRUE.equals(p.getIsActive());
            String typeName = getPeriodTypeName(p.getPeriodType());
            
            String currentPhase;
            String phaseBadge;
            if (!isActive) {
                currentPhase = "Đã đóng đợt";
                phaseBadge = "⚪ Đã kết thúc";
            } else if (p.getTopicSubmissionStart() != null && now.isBefore(p.getTopicSubmissionStart())) {
                currentPhase = "Sắp bắt đầu mở nộp đề tài";
                phaseBadge = " Sắp diễn ra";
            } else if (p.isTopicSubmissionOpen()) {
                currentPhase = "Giảng viên đang trong thời gian đề xuất đề tài";
                phaseBadge = " Đang mở cho Giảng viên";
            } else if (p.isStudentRegistrationOpen()) {
                currentPhase = "Sinh viên đang trong thời gian chọn đề tài";
                phaseBadge = " Đang mở cho Sinh viên";
            } else if (p.getReviewDeadline() != null && now.isBefore(p.getReviewDeadline())) {
                currentPhase = "Các nhóm đang thực hiện đề tài & tiến hành phản biện";
                phaseBadge = " Đang thực hiện & Phản biện";
            } else if (p.getDefenseDate() != null) {
                currentPhase = "Giai đoạn tổ chức hội đồng bảo vệ khóa luận";
                phaseBadge = " Bảo vệ hội đồng";
            } else {
                currentPhase = "Đang diễn ra";
                phaseBadge = " Đang hoạt động";
            }

            if (isActive) activeCount++;

            sb.append("**").append(p.getPeriodName()).append("** (`").append(typeName).append(" - Năm học: ").append(p.getAcademicYear()).append("`)\n");
            sb.append("- **Trạng thái**: ").append(phaseBadge).append(" (").append(currentPhase).append(")\n");
            sb.append("- **Thời gian GV nộp đề tài**: `")
              .append(p.getTopicSubmissionStart() != null ? p.getTopicSubmissionStart().format(DATE_TIME_FORMATTER) : "N/A")
              .append("` ➜ `")
              .append(p.getTopicSubmissionEnd() != null ? p.getTopicSubmissionEnd().format(DATE_TIME_FORMATTER) : "N/A").append("`\n");
            sb.append("- **Thời gian SV đăng ký**: `")
              .append(p.getStudentRegistrationStart() != null ? p.getStudentRegistrationStart().format(DATE_TIME_FORMATTER) : "N/A")
              .append("` ➜ `")
              .append(p.getStudentRegistrationEnd() != null ? p.getStudentRegistrationEnd().format(DATE_TIME_FORMATTER) : "N/A").append("`\n");
            if (p.getReviewDeadline() != null) {
                sb.append("- **Hạn phản biện**: `").append(p.getReviewDeadline().format(DATE_TIME_FORMATTER)).append("`\n");
            }
            if (p.getDefenseDate() != null) {
                sb.append("- **Ngày bảo vệ dự kiến**: `").append(p.getDefenseDate().format(DATE_FORMATTER)).append("`\n");
            }
            sb.append("\n");
        }

        sb.append("> **Tổng kết:** Hiện có **").append(activeCount).append("** đợt đăng ký đang trong trạng thái hoạt động.");
        return sb.toString();
    }

    /**
     * 2. Hiện tại tôi đang là giảng viên hướng dẫn của bao nhiêu nhóm?
     */
    private String handleSupervisedGroupsCount(User user) {
        List<StudentGroup> supervisedGroups = groupRepository.findSupervisedGroupsByLecturerId(user.getId());
        int count = supervisedGroups.size();

        if (count == 0) {
            return "### Thống Kê Nhóm Hướng Dẫn\n\n" +
                   "Hiện tại Thầy/Cô **chưa là giảng viên hướng dẫn của nhóm sinh viên nào** trong hệ thống.";
        }

        Map<StudentGroup.GroupStatus, Long> statusCounts = supervisedGroups.stream()
                .collect(Collectors.groupingBy(StudentGroup::getStatus, Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("### Thống Kê Nhóm Hướng Dẫn\n\n");
        sb.append("Hiện tại Thầy/Cô đang là Giảng viên hướng dẫn của **").append(count).append(" nhóm sinh viên**:\n\n");

        for (Map.Entry<StudentGroup.GroupStatus, Long> entry : statusCounts.entrySet()) {
            sb.append("- ").append(translateGroupStatus(entry.getKey())).append(": **")
              .append(entry.getValue()).append(" nhóm**\n");
        }

        sb.append("\n *Thầy/Cô có thể hỏi tiếp: \"Hiện tại tôi đang là giảng viên hướng dẫn của những nhóm nào?\" để xem danh sách chi tiết.*");
        return sb.toString();
    }

    /**
     * 3. Hiện tại tôi đang là giảng viên hướng dẫn của những nhóm nào?
     */
    private String handleSupervisedGroupsList(User user) {
        List<StudentGroup> supervisedGroups = groupRepository.findSupervisedGroupsByLecturerId(user.getId());

        if (supervisedGroups.isEmpty()) {
            return "### Danh Sách Nhóm Hướng Dẫn\n\n" +
                   "Hiện tại Thầy/Cô **chưa có nhóm sinh viên nào** được phân công hướng dẫn.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### Danh Sách Các Nhóm Thầy/Cô Đang Hướng Dẫn\n\n");
        sb.append("Thầy/Cô đang hướng dẫn tổng cộng **").append(supervisedGroups.size()).append(" nhóm**:\n\n");

        int index = 1;
        for (StudentGroup g : supervisedGroups) {
            String topicTitle = (g.getTopic() != null) ? g.getTopic().getTitle() : "Chưa đăng ký đề tài";
            String topicCode = (g.getTopic() != null) ? g.getTopic().getTopicCode() : "N/A";
            String leaderName = (g.getLeader() != null) ? g.getLeader().getFullName() : "N/A";
            String leaderCode = (g.getLeader() != null) ? g.getLeader().getUserCode() : "N/A";
            String periodName = (g.getPeriod() != null) ? g.getPeriod().getPeriodName() : "N/A";
            String statusName = translateGroupStatus(g.getStatus());

            sb.append("**").append(index++).append(". ").append(g.getGroupName())
              .append("** (`").append(g.getGroupCode()).append("`)\n");
            sb.append("- **Đề tài**: ").append(topicTitle).append(" [Mã: `").append(topicCode).append("`]\n");
            sb.append("- **Nhóm trưởng**: ").append(leaderName).append(" (MSSV: `").append(leaderCode).append("`)\n");
            sb.append("- **Đợt đăng ký**: ").append(periodName).append("\n");
            sb.append("- **Trạng thái**: `").append(statusName).append("`\n\n");
        }

        return sb.toString();
    }

    /**
     * 4. Cho tôi biết nhóm nào đang chờ duyệt báo cáo tuần
     */
    private String handlePendingWeeklyReports(User user) {
        List<StudentGroup> supervisedGroups = groupRepository.findSupervisedGroupsByLecturerId(user.getId());
        if (supervisedGroups.isEmpty()) {
            return "### ⏳ Báo Cáo Tuần Chờ Duyệt\n\nThầy/Cô hiện không hướng dẫn nhóm nào nên không có báo cáo tuần cần duyệt.";
        }

        List<Long> groupIds = supervisedGroups.stream().map(StudentGroup::getId).collect(Collectors.toList());
        List<WeeklyProgressReport> pendingReports = weeklyReportRepository.findByGroupIdInAndFeedbackStatusOrderBySubmittedAtDesc(
                groupIds, WeeklyProgressReport.FeedbackStatus.PENDING);

        if (pendingReports.isEmpty()) {
            return "### ⏳ Báo Cáo Tuần Chờ Duyệt\n\n" +
                   " Hiện tại **không có báo cáo tuần nào đang chờ duyệt**! Tất cả các báo cáo của nhóm Thầy/Cô hướng dẫn đã được phản hồi.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### ⏳ Danh Sách Báo Cáo Tuần Đang Chờ Duyệt\n\n");
        sb.append("Có **").append(pendingReports.size()).append(" báo cáo tuần** đang chờ Thầy/Cô nhận xét & duyệt:\n\n");

        int index = 1;
        for (WeeklyProgressReport r : pendingReports) {
            String groupName = (r.getGroup() != null) ? r.getGroup().getGroupName() : "Nhóm #" + r.getGroup().getId();
            String submittedTime = (r.getSubmittedAt() != null) ? r.getSubmittedAt().format(DATE_TIME_FORMATTER) : "N/A";
            String tasksDone = (r.getTasksCompleted() != null && !r.getTasksCompleted().trim().isEmpty())
                    ? r.getTasksCompleted().replace("\n", " ").trim()
                    : "Không có mô tả";
            if (tasksDone.length() > 90) tasksDone = tasksDone.substring(0, 90) + "...";

            sb.append("**").append(index++).append(". ").append(groupName).append(" - Tuần ").append(r.getWeekNumber()).append("**\n");
            sb.append("- **Thời gian nộp**: `").append(submittedTime).append("`\n");
            sb.append("- **Công việc đã làm**: ").append(tasksDone).append("\n");
            sb.append("- **Trạng thái**: ` Đang chờ GVHD duyệt`\n\n");
        }

        sb.append(" Thầy/Cô có thể vào mục **Tiến độ đề tài ➜ Duyệt báo cáo tuần** để gửi phản hồi cho sinh viên.");
        return sb.toString();
    }

    /**
     * 5. Cho tôi biết những nhóm nào đã nộp báo cáo tuần
     */
    private String handleSubmittedWeeklyReports(User user) {
        List<StudentGroup> supervisedGroups = groupRepository.findSupervisedGroupsByLecturerId(user.getId());
        if (supervisedGroups.isEmpty()) {
            return "### Danh Sách Báo Cáo Tuần\n\nThầy/Cô hiện không hướng dẫn nhóm nào.";
        }

        List<Long> groupIds = supervisedGroups.stream().map(StudentGroup::getId).collect(Collectors.toList());
        List<WeeklyProgressReport> reports = weeklyReportRepository.findByGroupIdInOrderBySubmittedAtDesc(groupIds);

        if (reports.isEmpty()) {
            return "### Danh Sách Báo Cáo Tuần\n\n" +
                   "Hiện tại **chưa có nhóm nào nộp báo cáo tuần** trong số các nhóm Thầy/Cô hướng dẫn.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### Tình Hình Báo Cáo Tiến Độ Tuần\n\n");
        sb.append("Tổng số báo cáo tuần đã nộp từ các nhóm hướng dẫn: **").append(reports.size()).append(" báo cáo**.\n\n");

        Map<Long, List<WeeklyProgressReport>> reportsByGroup = reports.stream()
                .collect(Collectors.groupingBy(r -> r.getGroup().getId()));

        int index = 1;
        for (StudentGroup g : supervisedGroups) {
            List<WeeklyProgressReport> groupReports = reportsByGroup.get(g.getId());
            sb.append("**").append(index++).append(". ").append(g.getGroupName()).append("** (`").append(g.getGroupCode()).append("`)\n");
            
            if (groupReports == null || groupReports.isEmpty()) {
                sb.append("- *Chưa nộp báo cáo tuần nào.*\n\n");
            } else {
                sb.append("- Tổng số báo cáo đã nộp: **").append(groupReports.size()).append(" tuần**\n");
                WeeklyProgressReport latest = groupReports.get(0);
                String submitDate = (latest.getSubmittedAt() != null) ? latest.getSubmittedAt().format(DATE_TIME_FORMATTER) : "N/A";
                sb.append("- Báo cáo gần nhất: **Tuần ").append(latest.getWeekNumber())
                  .append("** (Nộp lúc: `").append(submitDate).append("` | Trạng thái: ").append(translateFeedbackStatus(latest.getFeedbackStatus())).append(")\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 6. Nhóm nào đã nộp báo cáo hoàn chỉnh / toàn văn?
     */
    private String handleFinalThesisSubmissions(User user) {
        List<StudentGroup> supervisedGroups = groupRepository.findSupervisedGroupsByLecturerId(user.getId());
        if (supervisedGroups.isEmpty()) {
            return "### Báo Cáo Khóa Luận Hoàn Chỉnh\n\nThầy/Cô hiện không hướng dẫn nhóm nào.";
        }

        List<Long> groupIds = supervisedGroups.stream().map(StudentGroup::getId).collect(Collectors.toList());
        List<TopicSubmission> finalSubmissions = submissionRepository.findByGroupIdInAndSubmissionTypeOrderBySubmittedAtDesc(
                groupIds, TopicSubmission.SubmissionType.FINAL_THESIS);

        if (finalSubmissions.isEmpty()) {
            return "### Báo Cáo Khóa Luận Hoàn Chỉnh\n\n" +
                   "Hiện tại **chưa có nhóm nào nộp báo cáo hoàn chỉnh (FINAL_THESIS)** trong số các nhóm Thầy/Cô hướng dẫn.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### Tình Hình Nộp Báo Cáo Hoàn Chỉnh (Toàn Văn Khóa Luận)\n\n");
        sb.append("Đã có **").append(finalSubmissions.size()).append(" lượt nộp báo cáo hoàn chỉnh**:\n\n");

        int index = 1;
        for (TopicSubmission sub : finalSubmissions) {
            String groupName = (sub.getGroup() != null) ? sub.getGroup().getGroupName() : "Nhóm #" + sub.getGroup().getId();
            String topicTitle = (sub.getGroup() != null && sub.getGroup().getTopic() != null) ? sub.getGroup().getTopic().getTitle() : "N/A";
            String submitTime = (sub.getSubmittedAt() != null) ? sub.getSubmittedAt().format(DATE_TIME_FORMATTER) : "N/A";

            sb.append("**").append(index++).append(". ").append(groupName).append("**\n");
            sb.append("- **Đề tài**: ").append(topicTitle).append("\n");
            sb.append("- **Tiêu đề báo cáo**: ").append(sub.getTitle()).append("\n");
            sb.append("- **Thời gian nộp**: `").append(submitTime).append("`\n");
            if (sub.getFileUrl() != null && !sub.getFileUrl().trim().isEmpty()) {
                sb.append("- **Đường dẫn tệp**: `").append(sub.getFileUrl()).append("`\n");
            }
            if (sub.getNote() != null && !sub.getNote().trim().isEmpty()) {
                sb.append("- **Ghi chú**: ").append(sub.getNote()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 7. Tôi đang làm giảng viên phản biện/chủ tịch/thư ký/ủy viên cho những hội đồng nào?
     */
    private String handleCouncilsList(User user, String lowerQuery) {
        List<CouncilMember> memberships = councilMemberRepository.findByLecturerId(user.getId());
        List<CouncilTopic> reviewerAssignments = councilTopicRepository.findByReviewerLecturerId(user.getId());

        boolean askChair = lowerQuery.contains("chủ tịch") || lowerQuery.contains("chu tich");
        boolean askSecretary = lowerQuery.contains("thư ký") || lowerQuery.contains("thu ky");
        boolean askReviewer = lowerQuery.contains("phản biện") || lowerQuery.contains("phan bien");
        boolean askMember = lowerQuery.contains("ủy viên") || lowerQuery.contains("uy vien");
        boolean isSpecificRole = askChair || askSecretary || askReviewer || askMember;

        if (memberships.isEmpty() && reviewerAssignments.isEmpty()) {
            return "### ️ Thông Tin Hội Đồng\n\n" +
                   "Hiện tại Thầy/Cô **chưa được phân công tham gia hội đồng nào** trong hệ thống.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### ️ Danh Sách Hội Đồng Thầy/Cô Đang Tham Gia\n\n");

        int count = 0;
        for (CouncilMember cm : memberships) {
            Council c = cm.getCouncil();
            if (c == null) continue;

            CouncilMember.CouncilPosition pos = cm.getPosition();
            if (isSpecificRole) {
                if (askChair && pos != CouncilMember.CouncilPosition.CHAIR) continue;
                if (askSecretary && pos != CouncilMember.CouncilPosition.SECRETARY) continue;
                if (askReviewer && pos != CouncilMember.CouncilPosition.REVIEWER) continue;
                if (askMember && pos != CouncilMember.CouncilPosition.MEMBER) continue;
            }

            count++;
            String posName = translateCouncilPosition(pos);
            String defenseTime = (c.getDefenseDate() != null) ? c.getDefenseDate().format(DATE_TIME_FORMATTER) : "Chưa có lịch";
            String location = (c.getLocation() != null && !c.getLocation().trim().isEmpty()) ? c.getLocation() : "Chưa xếp phòng";
            String periodName = (c.getPeriod() != null) ? c.getPeriod().getPeriodName() : "N/A";
            int topicCount = (c.getAssignedTopics() != null) ? c.getAssignedTopics().size() : 0;

            sb.append("**").append(count).append(". ").append(c.getCouncilName())
              .append("** [Mã: `").append(c.getCouncilCode()).append("`]\n");
            sb.append("- **Vai trò của Thầy/Cô**: **").append(posName).append("**\n");
            sb.append("- **Đợt bảo vệ**: ").append(periodName).append("\n");
            sb.append("- **Thời gian bảo vệ**: `").append(defenseTime).append("`\n");
            sb.append("- **Địa điểm**: ").append(location).append("\n");
            sb.append("- **Số đề tài/nhóm phân công vào HĐ**: ").append(topicCount).append(" nhóm\n\n");
        }

        if ((askReviewer || !isSpecificRole) && !reviewerAssignments.isEmpty()) {
            sb.append("#### Các Nhóm Đề Tài Được Phân Công Chấm Phản Biện Riêng:\n");
            for (CouncilTopic ct : reviewerAssignments) {
                String groupName = (ct.getGroup() != null) ? ct.getGroup().getGroupName() : "Nhóm #" + ct.getId();
                String topicTitle = (ct.getGroup() != null && ct.getGroup().getTopic() != null) ? ct.getGroup().getTopic().getTitle() : "N/A";
                String councilName = (ct.getCouncil() != null) ? ct.getCouncil().getCouncilName() : "N/A";

                sb.append("- **").append(groupName).append("**: ").append(topicTitle)
                  .append(" *(Thuộc: ").append(councilName).append(")*\n");
            }
            sb.append("\n");
        }

        if (count == 0 && reviewerAssignments.isEmpty()) {
            return "### ️ Thông Tin Hội Đồng\n\n" +
                   "Không tìm thấy hội đồng nào phù hợp với chức vụ Thầy/Cô vừa hỏi.";
        }

        return sb.toString();
    }

    /**
     * 8. Tôi đang tham gia vào bao nhiêu hội đồng?
     */
    private String handleCouncilsCount(User user) {
        List<CouncilMember> memberships = councilMemberRepository.findByLecturerId(user.getId());
        Set<Long> uniqueCouncilIds = memberships.stream()
                .map(cm -> cm.getCouncil().getId())
                .collect(Collectors.toSet());

        List<CouncilTopic> reviewerAssignments = councilTopicRepository.findByReviewerLecturerId(user.getId());
        for (CouncilTopic ct : reviewerAssignments) {
            if (ct.getCouncil() != null) {
                uniqueCouncilIds.add(ct.getCouncil().getId());
            }
        }

        int count = uniqueCouncilIds.size();

        if (count == 0) {
            return "### ️ Thống Kê Hội Đồng Tham Gia\n\n" +
                   "Hiện tại Thầy/Cô **chưa tham gia vào hội đồng nào** trong hệ thống.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### ️ Thống Kê Hội Đồng Tham Gia\n\n");
        sb.append("Hiện tại Thầy/Cô đang tham gia vào **").append(count).append(" hội đồng** đánh giá & bảo vệ khóa luận:\n\n");

        int index = 1;
        for (CouncilMember cm : memberships) {
            Council c = cm.getCouncil();
            if (c != null) {
                sb.append(index++).append(". **").append(c.getCouncilName()).append("** (`").append(c.getCouncilCode()).append("`): ")
                  .append("**").append(translateCouncilPosition(cm.getPosition())).append("**\n");
            }
        }

        return sb.toString();
    }

    /**
     * 9. Lời chào hỏi
     */
    private String handleGreeting(User user) {
        String salutation = getNormalizedSalutation(user);
        boolean isStaff = isLecturerOrStaff(user);
        String greetingPrefix = isStaff ? "Kính chào **" : "Xin chào **";

        return greetingPrefix + salutation + "**.\n\n" +
               "Tôi là Trợ lý AI hỗ trợ công tác học vụ và quản lý đề tài tốt nghiệp Khoa CNTT.\n\n" +
               "Hệ thống sẵn sàng hỗ trợ tra cứu các thông tin sau:\n" +
               "- Tra cứu tiến độ và hạn nộp các đợt đăng ký đề tài\n" +
               "- Danh mục đề tài theo chuyên ngành và tình trạng phân công\n" +
               "- Tình trạng nộp và duyệt báo cáo tiến độ tuần\n" +
               "- Lịch và danh sách hội đồng đánh giá khóa luận";
    }

    /**
     * 10. Trợ giúp & Giới thiệu tính năng
     */
    private String handleHelp(User user) {
        return "### Trợ Lý Tra Cứu Học Vụ - Khoa CNTT\n\n" +
               "Tôi được thiết kế để hỗ trợ tra cứu thông tin học vụ nhanh chóng. Một số câu hỏi mẫu có thể tra cứu:\n\n" +
               "1. **\"Hiện tại đang có những đợt đăng ký nào diễn ra?\"**\n" +
               "2. **\"Thông tin về đợt đăng ký [Tên đợt]\"**\n" +
               "3. **\"Có những đề tài nào trong đợt này?\"**\n" +
               "4. **\"Có bao nhiêu đề tài trong đợt 1?\"**\n" +
               "5. **\"Có những đề tài nào chưa có nhóm đăng ký?\"**\n" +
               "6. **\"Thông tin về đề tài DT001\"**\n" +
               "7. **\"Hiện tại tôi đang là giảng viên hướng dẫn của những nhóm nào?\"**\n" +
               "8. **\"Cho tôi biết những nhóm nào đã nộp báo cáo tuần\"**\n" +
               "9. **\"Cho tôi biết nhóm nào đang chờ duyệt báo cáo tuần\"**\n" +
               "10. **\"Nhóm nào đã nộp báo cáo hoàn chỉnh\"**\n" +
               "11. **\"Hội đồng sắp tới cần tham gia là khi nào?\"**\n" +
               "12. **\"Danh sách Nhóm và đề tài trong hội đồng sắp tới\"**\n" +
               "13. **\"Tôi đang tham gia vào bao nhiêu hội đồng?\"**\n" +
               "14. **\"Thông tin nhóm sinh viên Nhóm 1\"**\n\n" +
               "Bạn có thể nhập trực tiếp câu hỏi hoặc lựa chọn các chủ đề gợi ý trên giao diện.";
    }

    /**
     * 11. Xử lý câu hỏi không thuộc mẫu cố định (Fallback)
     */
    private String handleFallback(String rawQuery, User user) {
        boolean isStaff = isLecturerOrStaff(user);
        String salutation = isStaff ? "Thầy/Cô" : "bạn";
        return "Xin lỗi " + salutation + ", tôi chưa nhận diện được yêu cầu này hoặc thông tin cần truy vấn nằm ngoài phạm vi quản lý đề tài.\n\n" +
               "**Gợi ý các câu hỏi phù hợp:**\n" +
               "- *Hiện tại đang có những đợt đăng ký nào diễn ra?*\n" +
               "- *Có những đề tài nào trong đợt này?*\n" +
               "- *Có bao nhiêu đề tài trong đợt 1?*\n" +
               "- *Có những đề tài nào chưa có nhóm đăng ký?*\n" +
               "- *Thông tin về đề tài DT001*\n" +
               "- *Hội đồng sắp tới cần tham gia là khi nào?*\n" +
               "- *Danh sách Nhóm và đề tài trong hội đồng sắp tới*\n" +
               "- *Hiện tại tôi đang là giảng viên hướng dẫn của những nhóm nào?*\n" +
               "- *Cho tôi biết nhóm nào đang chờ duyệt báo cáo tuần.*";
    }

    /**
     * Chuẩn hóa danh xưng người dùng bằng Regex 2 chiều chống NPE và tránh lặp học vị.
     */
    public String getNormalizedSalutation(User user) {
        if (user == null) {
            return "bạn";
        }
        String fullName = user.getFullName() != null ? user.getFullName().trim() : "";
        String rank = user.getAcademicRank() != null ? user.getAcademicRank().trim() : "";

        // Bóc sạch tiền tố học vị nếu fullName đã chứa sẵn (vd: "TS. Lê Anh Tuấn" -> "Lê Anh Tuấn")
        String cleanName = fullName.replaceAll("^(?i)(GS\\.?\\s*TS\\.?|PGS\\.?\\s*TS\\.?|TS\\.?|ThS\\.?|KS\\.?|CN\\.?|Ths\\.?)\\s*", "").trim();
        if (cleanName.isEmpty()) {
            cleanName = fullName;
        }

        boolean isStaff = isLecturerOrStaff(user);

        if (!rank.isEmpty()) {
            // Chuẩn hóa định dạng học vị
            String cleanRank = rank.replaceAll("^(?i)(Ths\\.?|ThS\\.?)$", "ThS.")
                                   .replaceAll("^(?i)(Ts\\.?|TS\\.?)$", "TS.")
                                   .replaceAll("^(?i)(PGS\\.?\\s*TS\\.?|PGS\\.?\\s*TS)$", "PGS. TS.")
                                   .replaceAll("^(?i)(GS\\.?\\s*TS\\.?|GS\\.?\\s*TS)$", "GS. TS.");
            return cleanRank + " " + cleanName;
        } else if (isStaff) {
            String prefix = "Thầy/Cô";
            if (user.getGender() != null) {
                if (user.getGender() == User.Gender.MALE) {
                    prefix = "Thầy";
                } else if (user.getGender() == User.Gender.FEMALE) {
                    prefix = "Cô";
                }
            }
            return prefix + " " + cleanName;
        } else {
            return cleanName;
        }
    }

    /**
     * Kiểm tra người dùng có phải là giảng viên hoặc cán bộ khoa hay không
     */
    public boolean isLecturerOrStaff(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream().anyMatch(r -> 
            r.getRoleName() != null && !"ROLE_STUDENT".equalsIgnoreCase(r.getRoleName()));
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private RegistrationPeriod findPeriodFromQuery(String query) {
        List<RegistrationPeriod> periods = periodRepository.findAllByOrderByCreatedAtDesc();
        if (periods.isEmpty()) return null;

        String lower = query.toLowerCase();

        for (RegistrationPeriod p : periods) {
            String nameLower = p.getPeriodName().toLowerCase();
            if (lower.contains(nameLower)) {
                return p;
            }
            if (lower.contains("đợt " + p.getId()) || lower.contains("dot " + p.getId()) || lower.contains("#" + p.getId())) {
                return p;
            }
        }

        for (RegistrationPeriod p : periods) {
            if (p.getSemester() != null && (lower.contains("đợt " + p.getSemester()) || lower.contains("dot " + p.getSemester()) || lower.contains("học kỳ " + p.getSemester()) || lower.contains("hk" + p.getSemester()))) {
                return p;
            }
        }

        if (matchesAny(lower, "kltn", "khóa luận", "khoa luan")) {
            for (RegistrationPeriod p : periods) {
                if (p.getPeriodType() == RegistrationPeriod.PeriodType.GRADUATION_THESIS) return p;
            }
        }
        if (matchesAny(lower, "đồ án", "do an", "môn học", "mon hoc")) {
            for (RegistrationPeriod p : periods) {
                if (p.getPeriodType() == RegistrationPeriod.PeriodType.COURSE_PROJECT) return p;
            }
        }
        if (matchesAny(lower, "nckh", "nghiên cứu", "nghien cuu")) {
            for (RegistrationPeriod p : periods) {
                if (p.getPeriodType() == RegistrationPeriod.PeriodType.RESEARCH) return p;
            }
        }
        if (matchesAny(lower, "thực tập", "thuc tap")) {
            for (RegistrationPeriod p : periods) {
                if (p.getPeriodType() == RegistrationPeriod.PeriodType.INTERNSHIP) return p;
            }
        }

        for (RegistrationPeriod p : periods) {
            if (Boolean.TRUE.equals(p.getIsActive())) {
                return p;
            }
        }

        return periods.get(0);
    }

    private boolean matchesAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private String getPeriodTypeName(RegistrationPeriod.PeriodType type) {
        if (type == null) return "Khóa luận tốt nghiệp";
        return switch (type) {
            case GRADUATION_THESIS -> "Khóa luận tốt nghiệp";
            case COURSE_PROJECT -> "Đồ án môn học / Chuyên ngành";
            case RESEARCH -> "Nghiên cứu khoa học (NCKH)";
            case INTERNSHIP -> "Thực tập tốt nghiệp";
        };
    }

    private String translateGroupStatus(StudentGroup.GroupStatus status) {
        if (status == null) return "Chưa xác định";
        return switch (status) {
            case PENDING_APPROVAL -> "Chờ duyệt đề tài";
            case ASSIGNED -> "Đã gán đề tài";
            case IN_PROGRESS -> "Đang thực hiện đề tài";
            case DEFENDING -> "Đang trong danh sách bảo vệ";
            case COMPLETED -> "Đã hoàn thành";
            case DISQUALIFIED -> "Không đủ điều kiện";
        };
    }

    private String translateTopicStatusBadge(Topic.TopicStatus status) {
        if (status == null) return "`Chưa xác định`";
        return switch (status) {
            case APPROVED -> "` Đã phê duyệt (APPROVED)`";
            case PENDING_APPROVAL -> "` Chờ bộ môn duyệt (PENDING)`";
            case ASSIGNED -> "` Đã có nhóm nhận (ASSIGNED)`";
            case REJECTED -> "` Bị từ chối (REJECTED)`";
            case COMPLETED -> "` Đã hoàn thành (COMPLETED)`";
        };
    }

    private String translateFeedbackStatus(WeeklyProgressReport.FeedbackStatus status) {
        if (status == null) return "` Đang chờ duyệt`";
        return switch (status) {
            case PENDING -> "` Đang chờ duyệt`";
            case ACCEPTED -> "` Đã duyệt & Đồng ý`";
            case NEEDS_REVISION -> "` Yêu cầu chỉnh sửa lại`";
        };
    }

    private String translateCouncilPosition(CouncilMember.CouncilPosition pos) {
        if (pos == null) return "Thành viên hội đồng";
        return switch (pos) {
            case CHAIR -> " Chủ tịch Hội đồng";
            case SECRETARY -> "✍️ Thư ký Hội đồng";
            case REVIEWER -> " Giảng viên Phản biện";
            case MEMBER -> " Ủy viên Hội đồng";
        };
    }
}
