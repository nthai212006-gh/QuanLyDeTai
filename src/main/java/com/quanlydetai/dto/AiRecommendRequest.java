package com.quanlydetai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendRequest {

    @NotBlank(message = "Sở thích hoặc định hướng công nghệ không được để trống")
    private String interest;

    @NotNull(message = "Vui lòng chọn đợt đăng ký")
    private Long periodId;
}
