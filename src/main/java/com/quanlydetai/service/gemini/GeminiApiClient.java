package com.quanlydetai.service.gemini;

import com.quanlydetai.dto.gemini.GeminiApiRequest;
import com.quanlydetai.dto.gemini.GeminiApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

@Service
@Slf4j
public class GeminiApiClient {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent}")
    private String apiUrl;

    @Value("${gemini.api.timeout-seconds:8}")
    private int timeoutSeconds;

    /**
     * Mốc thời gian (epoch millis) kết thúc lệnh phạt tạm ngắt gọi API (Circuit Breaker).
     * Dùng volatile để đảm bảo tính nhìn thấy (visibility) giữa các thread trong môi trường 1 instance.
     */
    private volatile long circuitBreakerUntil = 0L;

    private final RestClient restClient;

    public GeminiApiClient(@Value("${gemini.api.timeout-seconds:8}") int timeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 8));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Kiểm tra trạng thái sẵn sàng của dịch vụ Gemini.
     * Trả về false nếu chưa có key hoặc đang trong thời gian phạt của Circuit Breaker.
     */
    public boolean isAvailable() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < circuitBreakerUntil) {
            log.debug("Gemini Circuit Breaker đang kích hoạt (còn {}s).", (circuitBreakerUntil - now) / 1000);
            return false;
        }
        return true;
    }

    /**
     * Kích hoạt Circuit Breaker tạm ngưng gọi API trong khoảng thời gian xác định.
     */
    public void tripCircuitBreaker(long cooldownMillis) {
        this.circuitBreakerUntil = System.currentTimeMillis() + cooldownMillis;
        log.warn("Gemini Circuit Breaker đã được kích hoạt trong {} ms.", cooldownMillis);
    }

    /**
     * Gửi yêu cầu sinh nội dung đàm thoại tới Google Gemini REST API.
     */
    public Optional<String> chat(String systemInstructionText, String userQuery) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            GeminiApiRequest requestPayload = GeminiApiRequest.builder()
                    .systemInstruction(GeminiApiRequest.SystemInstruction.of(systemInstructionText))
                    .contents(Collections.singletonList(GeminiApiRequest.Content.user(userQuery)))
                    .generationConfig(GeminiApiRequest.GenerationConfig.builder()
                            .temperature(0.2) // Thống nhất mức sáng tạo 0.2 chống ảo giác
                            .maxOutputTokens(2048) // Đủ dài cho danh sách nhiều nhóm/báo cáo
                            .topP(0.8)
                            .build())
                    .build();

            String fullUrl = apiUrl + (apiUrl.contains("?") ? "&" : "?") + "key=" + apiKey.trim();

            GeminiApiResponse response = restClient.post()
                    .uri(fullUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(GeminiApiResponse.class);

            if (response != null) {
                String reply = response.getFirstCandidateText();
                if (reply != null && !reply.isBlank()) {
                    return Optional.of(reply.trim());
                }
            }

            return Optional.empty();

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Gemini API đạt giới hạn Quota (HTTP 429). Kích hoạt Circuit Breaker trong 60 giây.");
            tripCircuitBreaker(60_000L); // 60s cooldown
            return Optional.empty();

        } catch (HttpServerErrorException.ServiceUnavailable e) {
            log.warn("Gemini API đang quá tải (HTTP 503). Kích hoạt Circuit Breaker trong 20 giây.");
            tripCircuitBreaker(20_000L); // 20s cooldown
            return Optional.empty();

        } catch (ResourceAccessException e) {
            log.warn("Mất kết nối hoặc timeout khi gọi Gemini API ({}s): {}", timeoutSeconds, e.getMessage());
            return Optional.empty();

        } catch (Exception e) {
            log.warn("Lỗi không mong muốn khi gọi Gemini API: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
