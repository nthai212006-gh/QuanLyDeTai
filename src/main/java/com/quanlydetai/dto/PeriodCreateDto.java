package com.quanlydetai.dto;

import com.quanlydetai.entity.RegistrationPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodCreateDto {

    @NotBlank(message = "Tên đợt đăng ký không được để trống")
    private String name;

    @NotBlank(message = "Năm học không được để trống (VD: 2025-2026)")
    private String academicYear;

    @NotNull(message = "Vui lòng chọn học kỳ")
    @jakarta.validation.constraints.Min(value = 1, message = "Học kỳ chỉ có thể là 1 hoặc 2")
    @jakarta.validation.constraints.Max(value = 2, message = "Học kỳ chỉ có thể là 1 hoặc 2")
    private Integer semester;

    @NotNull(message = "Vui lòng chọn loại đợt (Môn học, NCKH, TLCN, KLTN)")
    private RegistrationPeriod.PeriodType periodType;

    @NotNull(message = "Hạn bắt đầu nộp đề tài không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime topicSubmissionStart;

    @NotNull(message = "Hạn kết thúc nộp đề tài không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime topicSubmissionEnd;

    @NotNull(message = "Hạn bắt đầu SV đăng ký không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime studentRegistrationStart;

    @NotNull(message = "Hạn kết thúc SV đăng ký không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime studentRegistrationEnd;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime reviewDeadline;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate defenseDate;
}
