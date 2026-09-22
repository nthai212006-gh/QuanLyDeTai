package com.quanlydetai.dto;

/**
 * Projection ánh xạ dữ liệu từ MySQL View: v_topic_registration_status
 */
public interface TopicRegistrationStatusView {
    Long getTopicId();
    String getTopicCode();
    String getTopicTitle();
    String getDepartmentName();
    String getPeriodName();
    String getPrimarySupervisor();
    String getTopicStatus();
    Long getAssignedGroupId();
    String getAssignedGroupName();
}
