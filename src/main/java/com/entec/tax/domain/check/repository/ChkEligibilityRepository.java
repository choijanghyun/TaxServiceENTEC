package com.entec.tax.domain.check.repository;

import com.entec.tax.domain.check.entity.ChkEligibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * CHK_ELIGIBILITY 테이블 리포지토리.
 * <p>
 * 세액공제 환급 적격 여부 검증 결과 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface ChkEligibilityRepository extends JpaRepository<ChkEligibility, String> {

    /**
     * 요청 ID로 자격 진단 결과 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 자격 진단 결과 (없으면 Optional.empty)
     */
    Optional<ChkEligibility> findByReqId(String reqId);

    /**
     * 요청 ID로 자격 진단 결과 삭제 (TX-2 재시도 지원).
     *
     * @param reqId 요청 ID
     */
    void deleteByReqId(String reqId);
}
