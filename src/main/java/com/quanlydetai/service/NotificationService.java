package com.quanlydetai.service;

import com.quanlydetai.entity.Notification;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    public void sendNotification(User recipient, String title, String message, 
                                Notification.NotificationType type, String refUrl, boolean sendEmail) {
        Notification notif = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .referenceUrl(refUrl)
                .isRead(false)
                .emailSent(false)
                .build();

        notificationRepository.save(notif);

        if (sendEmail && recipient.getEmail() != null) {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(recipient.getEmail());
                mail.setSubject("[FIT-NOTIFICATION] " + title);
                mail.setText(message);
                mailSender.send(mail);
                notif.setEmailSent(true);
                notificationRepository.save(notif);
                log.info("Sent notification email to {}", recipient.getEmail());
            } catch (Exception e) {
                log.warn("Could not send email to {}: {}", recipient.getEmail(), e.getMessage());
            }
        }
    }
}
