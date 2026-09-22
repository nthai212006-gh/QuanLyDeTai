package com.quanlydetai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSimilarityResponse {
    private BigDecimal score;
    private boolean isDuplicate;
    private String message;
}
