package com.entec.tax.common.logging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 감사 로그 Repository
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime from, LocalDateTime to);

    List<AuditLog> findByTargetTableAndTargetId(String targetTable, String targetId);

    List<AuditLog> findByActionTypeAndCreatedAtBetween(String actionType, LocalDateTime from, LocalDateTime to);
}
