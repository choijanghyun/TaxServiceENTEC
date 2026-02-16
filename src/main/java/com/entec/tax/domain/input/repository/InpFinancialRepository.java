package com.entec.tax.domain.input.repository;

import com.entec.tax.domain.input.entity.InpFinancial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * INP_FINANCIAL 테이블 리포지토리.
 * <p>
 * 재무/세무 수치 요약 데이터 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface InpFinancialRepository extends JpaRepository<InpFinancial, String> {

    /**
     * 요청 ID로 재무/세무 수치 요약 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 재무/세무 수치 요약 (없으면 Optional.empty)
     */
    Optional<InpFinancial> findByReqId(String reqId);
}
