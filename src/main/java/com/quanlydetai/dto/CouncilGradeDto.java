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
public class CouncilGradeDto {

    @NotNull(message = "Mã phân công đề tài không được để trống")
    private Long councilTopicId;

    @NotNull(message = "Mã hội đồng không được để trống")
    private Long councilId;

    @NotNull(message = "Loại đánh giá không được để trống")
    private Evaluation.EvaluationType evalType;

    @NotNull(message = "Điểm tiêu chí 1 không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm không thể âm")
    @DecimalMax(value = "10.0", message = "Điểm tối đa là 10.0")
    private BigDecimal c1;

    @NotNull(message = "Điểm tiêu chí 2 không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm không thể âm")
    @DecimalMax(value = "10.0", message = "Điểm tối đa là 10.0")
    private BigDecimal c2;

    @NotNull(message = "Điểm tiêu chí 3 không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm không thể âm")
    @DecimalMax(value = "10.0", message = "Điểm tối đa là 10.0")
    private BigDecimal c3;

    private String comments;
}
