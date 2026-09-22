package com.quanlydetai.view;

import com.quanlydetai.dto.CouncilGradingSummaryView;
import com.quanlydetai.dto.TopicRegistrationStatusView;
import com.quanlydetai.repository.CouncilRepository;
import com.quanlydetai.repository.TopicRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatabaseViewsIntegrationTest {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private CouncilRepository councilRepository;

    @Test
    @DisplayName("Should query view v_topic_registration_status successfully")
    @Transactional(readOnly = true)
    void testTopicRegistrationStatusView() {
        List<TopicRegistrationStatusView> results = topicRepository.findAllTopicRegistrationStatus();
        assertThat(results).isNotEmpty();

        TopicRegistrationViewItem:
        for (TopicRegistrationStatusView item : results) {
            assertThat(item.getTopicId()).isNotNull();
            assertThat(item.getTopicCode()).isNotBlank();
            assertThat(item.getTopicTitle()).isNotBlank();
        }
    }

    @Test
    @DisplayName("Should query view v_council_grading_summary successfully")
    @Transactional(readOnly = true)
    void testCouncilGradingSummaryView() {
        List<CouncilGradingSummaryView> results = councilRepository.findAllGradingSummaries();
        assertThat(results).isNotEmpty();

        CouncilGradingSummaryView first = results.get(0);
        assertThat(first.getCouncilId()).isNotNull();
        assertThat(first.getCouncilName()).isNotBlank();
        assertThat(first.getGroupName()).isNotBlank();
        assertThat(first.getTopicTitle()).isNotBlank();
    }
}
