package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefIncDeductionLimit;
import com.entec.tax.domain.reference.entity.RefIncDeductionLimitId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * REF_INC_DEDUCTION_LIMIT 테이블 리포지토리.
 * <p>
 * 개인(소득세) 소득공제 한도 정보 조회를 담당한다.
 * </p>
 */
public interface RefIncDeductionLimitRepository extends JpaRepository<RefIncDeductionLimit, RefIncDeductionLimitId> {

    /**
     * 공제 유형별 한도 조회.
     *
     * @param deductionType 공제 유형
     * @return 해당 공제 유형의 한도 목록
     */
    List<RefIncDeductionLimit> findByDeductionType(String deductionType);

    /**
     * 소득 구간별 한도 조회.
     *
     * @param incomeBracket 소득 구간
     * @return 해당 소득 구간의 한도 목록
     */
    List<RefIncDeductionLimit> findByIncomeBracket(String incomeBracket);
}
