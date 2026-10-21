package com.quanlydetai.service;

import com.quanlydetai.entity.Announcement;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.AnnouncementRepository;
import com.quanlydetai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    @Transactional
    public Announcement createAnnouncement(String title, String content, boolean isPinned, User author) {
        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề và nội dung thông báo không được để trống!");
        }

        User persistentAuthor = author;
        if (author != null && author.getId() != null) {
            persistentAuthor = userRepository.findById(author.getId()).orElse(author);
        }

        Announcement announcement = Announcement.builder()
                .title(title.trim())
                .content(content.trim())
                .author(persistentAuthor)
                .isPinned(isPinned)
                .build();

        return announcementRepository.save(announcement);
    }
}
