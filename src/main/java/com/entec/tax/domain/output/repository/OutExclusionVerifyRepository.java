package com.entec.tax.domain.output.repository;

import com.entec.tax.domain.output.entity.OutExclusionVerify;
import com.entec.tax.domain.output.entity.OutExclusionVerifyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * OUT_EXCLUSION_VERIFY 테이블 리포지토리.
 * <p>
 * 상호배제 검증 결과 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface OutExclusionVerifyRepository extends JpaRepository<OutExclusionVerify, OutExclusionVerifyId> {

    /**
     * 요청 ID 및 조합 ID로 상호배제 검증 결과 조회.
     *
     * @param reqId   요청 ID
     * @param comboId 조합 ID (OUT_COMBINATION FK)
     * @return 해당 조건의 상호배제 검증 목록
     */
    List<OutExclusionVerify> findByReqIdAndComboId(String reqId, Integer comboId);

    /**
     * 요청 ID로 상호배제 검증 결과 삭제 (TX-2 재시도 지원).
     *
     * @param reqId 요청 ID
     */
    void deleteByReqId(String reqId);
}
