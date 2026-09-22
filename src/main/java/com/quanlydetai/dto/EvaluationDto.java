package com.quanlydetai.dto;

import com.quanlydetai.entity.Evaluation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDto {

    @NotNull(message = "Mã đề tài không được để trống")
    private Long topicId;

    private Long councilId;

    @NotNull(message = "Loại đánh giá không được để trống")
    private Evaluation.EvaluationType evaluationType;

    @NotNull(message = "Điểm số không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm số không được nhỏ hơn 0")
    @DecimalMax(value = "10.0", message = "Điểm số tối đa là 10")
    private BigDecimal score;

    private String comments;
}
