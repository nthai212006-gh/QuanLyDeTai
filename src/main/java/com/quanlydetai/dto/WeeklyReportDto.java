package com.quanlydetai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class WeeklyReportDto {

    @NotNull(message = "Tuần báo cáo không được để trống")
    @Min(value = 1, message = "Tuần báo cáo bắt đầu từ 1")
    @Max(value = 20, message = "Tuần báo cáo tối đa là 20")
    private Integer weekNumber;

    @NotBlank(message = "Nội dung công việc đã hoàn thành không được để trống")
    private String tasksCompleted;

    private String issuesFaced;

    @NotBlank(message = "Kế hoạch tuần tiếp theo không được để trống")
    private String nextWeekPlan;

    private String reportFileUrl;
}
