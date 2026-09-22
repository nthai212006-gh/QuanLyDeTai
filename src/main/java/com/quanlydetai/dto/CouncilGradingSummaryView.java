package com.quanlydetai.dto;

import java.math.BigDecimal;

/**
 * Projection ánh xạ dữ liệu từ MySQL View: v_council_grading_summary
 */
public interface CouncilGradingSummaryView {
    Long getCouncilId();
    String getCouncilName();
    String getCouncilCode();
    Integer getDefenseOrder();
    String getGroupName();
    String getTopicTitle();
    String getReviewerName();
    BigDecimal getFinalCouncilScore();
    Boolean getIsPublished();
    Long getTotalGradedMembers();
}
