package com.entec.tax.domain.input.repository;

import com.entec.tax.domain.input.entity.InpDeduction;
import com.entec.tax.domain.input.entity.InpDeductionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * INP_DEDUCTION 테이블 리포지토리.
 * <p>
 * 공제/감면 기초 요약 데이터 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface InpDeductionRepository extends JpaRepository<InpDeduction, InpDeductionId> {

    /**
     * 요청 ID로 전체 공제/감면 기초 데이터 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 공제/감면 목록
     */
    List<InpDeduction> findByReqId(String reqId);

    /**
     * 요청 ID 및 항목 구분으로 공제/감면 기초 데이터 조회.
     *
     * @param reqId        요청 ID
     * @param itemCategory 항목 구분 (INVEST, RD, STARTUP, SME_SPECIAL, EMPLOYMENT 등)
     * @return 해당 조건의 공제/감면 목록
     */
    List<InpDeduction> findByReqIdAndItemCategory(String reqId, String itemCategory);

    /**
     * 요청 ID, 항목 구분, 적용 조항으로 공제/감면 기초 데이터 조회.
     *
     * @param reqId        요청 ID
     * @param itemCategory 항목 구분
     * @param provision    적용 조항 (예: §24, §10, §6, §7)
     * @return 해당 조건의 공제/감면 목록
     */
    List<InpDeduction> findByReqIdAndItemCategoryAndProvision(String reqId, String itemCategory, String provision);
}
