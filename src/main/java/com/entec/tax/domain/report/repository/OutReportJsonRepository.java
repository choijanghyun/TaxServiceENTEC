package com.entec.tax.domain.report.repository;

import com.entec.tax.domain.report.entity.OutReportJson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * OUT_REPORT_JSON 테이블 리포지토리.
 * <p>
 * 최종 보고서 JSON 저장/조회를 담당한다.
 * </p>
 */
public interface OutReportJsonRepository extends JpaRepository<OutReportJson, String> {

    /**
     * 요청 ID로 보고서 JSON 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 보고서 JSON (없으면 Optional.empty)
     */
    Optional<OutReportJson> findByReqId(String reqId);

    /**
     * 요청 ID로 보고서 JSON 삭제 (TX-2 재시도 지원).
     *
     * @param reqId 요청 ID
     */
    void deleteByReqId(String reqId);
}
