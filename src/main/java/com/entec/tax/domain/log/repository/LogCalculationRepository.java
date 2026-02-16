package com.entec.tax.domain.log.repository;

import com.entec.tax.domain.log.entity.LogCalculation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * LOG_CALCULATION 테이블 리포지토리.
 * <p>
 * 감사추적 로그 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface LogCalculationRepository extends JpaRepository<LogCalculation, Long> {

    /**
     * 요청 ID로 감사추적 로그 조회 (실행일시 오름차순).
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 감사추적 로그 목록 (시간순)
     */
    List<LogCalculation> findByReqIdOrderByExecutedAtAsc(String reqId);

    /**
     * 요청 ID 및 계산 단계로 감사추적 로그 조회.
     *
     * @param reqId    요청 ID
     * @param calcStep 계산 단계 (STEP0, STEP1, M1-03 등)
     * @return 해당 조건의 감사추적 로그 목록
     */
    List<LogCalculation> findByReqIdAndCalcStep(String reqId, String calcStep);

    /**
     * 추적 ID로 감사추적 로그 조회.
     *
     * @param traceId API 요청 단위 추적 ID
     * @return 해당 추적 ID의 감사추적 로그 목록
     */
    List<LogCalculation> findByTraceId(String traceId);
}
