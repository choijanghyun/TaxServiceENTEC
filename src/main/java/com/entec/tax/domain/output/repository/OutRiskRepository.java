package com.entec.tax.domain.output.repository;

import com.entec.tax.domain.output.entity.OutRisk;
import com.entec.tax.domain.output.entity.OutRiskId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * OUT_RISK 테이블 리포지토리.
 * <p>
 * 사후관리/리스크 평가 결과 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface OutRiskRepository extends JpaRepository<OutRisk, OutRiskId> {

    /**
     * 요청 ID로 리스크 평가 결과 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 리스크 평가 목록
     */
    List<OutRisk> findByReqId(String reqId);

    /**
     * 요청 ID로 리스크 평가 결과 삭제 (TX-2 재시도 지원).
     *
     * @param reqId 요청 ID
     */
    void deleteByReqId(String reqId);
}
