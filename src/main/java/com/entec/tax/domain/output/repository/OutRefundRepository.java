package com.entec.tax.domain.output.repository;

import com.entec.tax.domain.output.entity.OutRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * OUT_REFUND 테이블 리포지토리.
 * <p>
 * 최종 환급액 산출 결과 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface OutRefundRepository extends JpaRepository<OutRefund, String> {

    /**
     * 요청 ID로 최종 환급액 산출 결과 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 환급액 산출 결과 (없으면 Optional.empty)
     */
    Optional<OutRefund> findByReqId(String reqId);

    /**
     * 요청 ID로 최종 환급액 산출 결과 삭제 (TX-2 재시도 지원).
     *
     * @param reqId 요청 ID
     */
    void deleteByReqId(String reqId);
}
