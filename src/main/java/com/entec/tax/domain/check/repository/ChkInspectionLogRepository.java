package com.entec.tax.domain.check.repository;

import com.entec.tax.domain.check.entity.ChkInspectionLog;
import com.entec.tax.domain.check.entity.ChkInspectionLogId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * CHK_INSPECTION_LOG 테이블 리포지토리.
 * <p>
 * 점검 항목별 검사 로그 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface ChkInspectionLogRepository extends JpaRepository<ChkInspectionLog, ChkInspectionLogId> {

    /**
     * 요청 ID로 점검 로그 조회 (표시순서 오름차순).
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 점검 로그 목록 (정렬순)
     */
    List<ChkInspectionLog> findByReqIdOrderBySortOrder(String reqId);

    /**
     * 요청 ID로 점검 로그 삭제 (TX-2 재시도 지원).
     *
     * @param reqId 요청 ID
     */
    void deleteByReqId(String reqId);
}
