package com.quanlydetai.dto.gemini;

import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiApiRequest {

    private SystemInstruction systemInstruction;
    private List<Content> contents;
    private GenerationConfig generationConfig;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemInstruction {
        private List<Part> parts;

        public static SystemInstruction of(String text) {
            return SystemInstruction.builder()
                    .parts(Collections.singletonList(new Part(text)))
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Content {
        private String role;
        private List<Part> parts;

        public static Content user(String text) {
            return Content.builder()
                    .role("user")
                    .parts(Collections.singletonList(new Part(text)))
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerationConfig {
        private Double temperature;
        private Integer maxOutputTokens;
        private Double topP;
    }
}
