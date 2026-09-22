package com.quanlydetai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicCreateDto {

    @NotBlank(message = "Mã đề tài không được để trống")
    private String topicCode;

    @NotBlank(message = "Tên đề tài không được để trống")
    @Size(max = 255, message = "Tên đề tài không được vượt quá 255 ký tự")
    private String title;

    private String description;

    private String requirements;

    @NotNull(message = "Vui lòng chọn bộ môn quản lý đề tài")
    private Long departmentId;

    @NotNull(message = "Vui lòng chọn đợt đăng ký")
    private Long periodId;

    // Giảng viên hướng dẫn phụ (tùy chọn)
    private Long coSupervisorId;
}
