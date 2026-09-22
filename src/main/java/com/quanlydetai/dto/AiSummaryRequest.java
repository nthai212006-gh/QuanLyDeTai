package com.quanlydetai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSummaryRequest {

    @NotBlank(message = "Tên đề tài không được để trống")
    private String title;

    @NotBlank(message = "Nội dung báo cáo không được để trống")
    private String content;
}
