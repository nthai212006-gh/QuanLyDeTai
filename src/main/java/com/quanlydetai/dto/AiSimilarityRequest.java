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
public class AiSimilarityRequest {

    @NotBlank(message = "Tên đề tài không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Mã đợt đăng ký không được để trống")
    private Long periodId;
}
