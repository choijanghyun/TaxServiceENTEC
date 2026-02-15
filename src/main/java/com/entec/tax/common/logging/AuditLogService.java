package com.entec.tax.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 감사 로그 서비스
 * - 주요 업무 처리 이력을 DB에 기록
 * - 별도의 트랜잭션(REQUIRES_NEW)으로 처리하여 본 업무 트랜잭션에 영향 없음
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 감사 로그 기록
     *
     * @param userId      사용자 ID
     * @param userName    사용자명
     * @param actionType  작업 유형 (CREATE, READ, UPDATE, DELETE)
     * @param targetTable 대상 테이블명
     * @param targetId    대상 데이터 ID
     * @param description 작업 설명
     * @param request     HTTP 요청 객체
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeLog(String userId, String userName, String actionType,
                         String targetTable, String targetId, String description,
                         HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUserName(userName);
            auditLog.setActionType(actionType);
            auditLog.setTargetTable(targetTable);
            auditLog.setTargetId(targetId);
            auditLog.setDescription(description);
            auditLog.setResultStatus("SUCCESS");
            auditLog.setCreatedAt(LocalDateTime.now());

            if (request != null) {
                auditLog.setIpAddress(getClientIp(request));
                auditLog.setRequestUrl(request.getRequestURI());
                auditLog.setHttpMethod(request.getMethod());
            }

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("[AuditLog] 감사 로그 기록 실패 - userId={}, action={}, error={}",
                    userId, actionType, e.getMessage());
        }
    }

    /**
     * 데이터 변경 이력이 포함된 감사 로그 기록
     *
     * @param userId      사용자 ID
     * @param userName    사용자명
     * @param actionType  작업 유형
     * @param targetTable 대상 테이블명
     * @param targetId    대상 데이터 ID
     * @param description 작업 설명
     * @param beforeData  변경 전 데이터 객체
     * @param afterData   변경 후 데이터 객체
     * @param request     HTTP 요청 객체
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeLogWithData(String userId, String userName, String actionType,
                                 String targetTable, String targetId, String description,
                                 Object beforeData, Object afterData,
                                 HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUserName(userName);
            auditLog.setActionType(actionType);
            auditLog.setTargetTable(targetTable);
            auditLog.setTargetId(targetId);
            auditLog.setDescription(description);
            auditLog.setResultStatus("SUCCESS");
            auditLog.setCreatedAt(LocalDateTime.now());

            if (beforeData != null) {
                auditLog.setBeforeData(objectMapper.writeValueAsString(beforeData));
            }
            if (afterData != null) {
                auditLog.setAfterData(objectMapper.writeValueAsString(afterData));
            }
            if (request != null) {
                auditLog.setIpAddress(getClientIp(request));
                auditLog.setRequestUrl(request.getRequestURI());
                auditLog.setHttpMethod(request.getMethod());
            }

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("[AuditLog] 감사 로그 기록 실패 - userId={}, action={}, error={}",
                    userId, actionType, e.getMessage());
        }
    }

    /**
     * 실패 감사 로그 기록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeFailLog(String userId, String userName, String actionType,
                             String targetTable, String targetId, String description,
                             String errorMessage, HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUserName(userName);
            auditLog.setActionType(actionType);
            auditLog.setTargetTable(targetTable);
            auditLog.setTargetId(targetId);
            auditLog.setDescription(description);
            auditLog.setResultStatus("FAIL");
            auditLog.setErrorMessage(errorMessage);
            auditLog.setCreatedAt(LocalDateTime.now());

            if (request != null) {
                auditLog.setIpAddress(getClientIp(request));
                auditLog.setRequestUrl(request.getRequestURI());
                auditLog.setHttpMethod(request.getMethod());
            }

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("[AuditLog] 실패 감사 로그 기록 실패 - userId={}, action={}, error={}",
                    userId, actionType, e.getMessage());
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     * - 프록시 환경 고려 (X-Forwarded-For, X-Real-IP)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 여러 프록시를 거친 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
