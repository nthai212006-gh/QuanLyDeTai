package com.quanlydetai.aspect;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.SystemAuditLog;
import com.quanlydetai.repository.SystemAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final SystemAuditLogRepository auditLogRepository;

    @AfterReturning(pointcut = "@annotation(auditAction)", returning = "result")
    public void logAudit(JoinPoint joinPoint, AuditAction auditAction, Object result) {
        try {
            Long userId = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                userId = userDetails.getId();
            }

            String ipAddress = "127.0.0.1";
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getRemoteAddr();
            }

            String details = "Method: " + joinPoint.getSignature().getName() + 
                             ", Args: " + Arrays.toString(joinPoint.getArgs());
            if (details.length() > 600) {
                details = details.substring(0, 600) + "...";
            }

            SystemAuditLog audit = SystemAuditLog.builder()
                    .userId(userId)
                    .action(auditAction.action())
                    .entityName(auditAction.entityName())
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(audit);
            log.info("[AUDIT LOG] User {} executed action: {}", userId, auditAction.action());
        } catch (Exception e) {
            log.error("Error saving audit log: {}", e.getMessage());
        }
    }
}
