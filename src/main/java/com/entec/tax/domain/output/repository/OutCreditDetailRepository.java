package com.entec.tax.domain.output.repository;

import com.entec.tax.domain.output.entity.OutCreditDetail;
import com.entec.tax.domain.output.entity.OutCreditDetailId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * OUT_CREDIT_DETAIL 테이블 리포지토리.
 * <p>
 * 개별 공제/감면 산출 결과 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface OutCreditDetailRepository extends JpaRepository<OutCreditDetail, OutCreditDetailId> {

    /**
     * 요청 ID로 전체 공제/감면 산출 결과 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 공제/감면 산출 목록
     */
    List<OutCreditDetail> findByReqId(String reqId);

    /**
     * 요청 ID 및 판정 상태로 공제/감면 산출 결과 조회.
     *
     * @param reqId      요청 ID
     * @param itemStatus 판정 상태 (applicable, not_applicable, needs_review)
     * @return 해당 조건의 공제/감면 산출 목록
     */
    List<OutCreditDetail> findByReqIdAndItemStatus(String reqId, String itemStatus);

    /**
     * 요청 ID 및 적용 조항으로 공제/감면 산출 결과 조회.
     *
     * @param reqId     요청 ID
     * @param provision 적용 조항 (예: §6, §7, §10, §24)
     * @return 해당 조건의 공제/감면 산출 목록
     */
    List<OutCreditDetail> findByReqIdAndProvision(String reqId, String provision);

    /**
     * 요청 ID로 공제/감면 산출 결과 삭제 (TX-2 재시도 지원).
     *
     * @param reqId 요청 ID
     */
    void deleteByReqId(String reqId);
}
