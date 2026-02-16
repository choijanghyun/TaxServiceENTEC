package com.entec.tax.domain.output.repository;

import com.entec.tax.domain.output.entity.OutEmployeeSummary;
import com.entec.tax.domain.output.entity.OutEmployeeSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * OUT_EMPLOYEE_SUMMARY 테이블 리포지토리.
 * <p>
 * 상시근로자 산정 결과 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface OutEmployeeSummaryRepository extends JpaRepository<OutEmployeeSummary, OutEmployeeSummaryId> {

    /**
     * 요청 ID로 전체 연도 구분의 상시근로자 산정 결과 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 상시근로자 산정 목록
     */
    List<OutEmployeeSummary> findByReqId(String reqId);

    /**
     * 요청 ID 및 연도 구분으로 상시근로자 산정 결과 조회.
     *
     * @param reqId    요청 ID
     * @param yearType 연도 구분 (PRIOR, CURRENT)
     * @return 해당 조건의 상시근로자 산정 결과 (없으면 Optional.empty)
     */
    Optional<OutEmployeeSummary> findByReqIdAndYearType(String reqId, String yearType);

    /**
     * 요청 ID로 상시근로자 산정 결과 삭제 (TX-2 재시도 지원).
     *
     * @param reqId 요청 ID
     */
    void deleteByReqId(String reqId);
}
