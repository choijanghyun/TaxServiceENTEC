package com.entec.tax.domain.check.repository;

import com.entec.tax.domain.check.entity.ChkValidationLog;
import com.entec.tax.domain.check.entity.ChkValidationLogId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * CHK_VALIDATION_LOG 테이블 리포지토리.
 * <p>
 * 검증 규칙별 실행 결과 로그 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface ChkValidationLogRepository extends JpaRepository<ChkValidationLog, ChkValidationLogId> {

    /**
     * 요청 ID로 검증 로그 전체 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 검증 로그 목록
     */
    List<ChkValidationLog> findByReqId(String reqId);

    /**
     * 요청 ID 및 실행 결과로 검증 로그 조회.
     *
     * @param reqId  요청 ID
     * @param result 실행 결과 (PASS, FAIL, WARNING, SKIP)
     * @return 해당 조건의 검증 로그 목록
     */
    List<ChkValidationLog> findByReqIdAndResult(String reqId, String result);

    /**
     * 요청 ID로 검증 로그 삭제 (TX-2 재시도 지원).
     *
     * @param reqId 요청 ID
     */
    void deleteByReqId(String reqId);
}
