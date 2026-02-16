package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefRdMinTaxExempt;
import com.entec.tax.domain.reference.entity.RefRdMinTaxExemptId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * REF_RD_MIN_TAX_EXEMPT 테이블 리포지토리.
 * <p>
 * R&D 세액공제의 최저한세 초과 적용 특례율 조회를 담당한다.
 * </p>
 */
public interface RefRdMinTaxExemptRepository extends JpaRepository<RefRdMinTaxExempt, RefRdMinTaxExemptId> {

    /**
     * R&D 유형별 면제율 조회.
     *
     * @param rdType R&D 유형
     * @return 해당 R&D 유형의 면제율 목록
     */
    List<RefRdMinTaxExempt> findByRdType(String rdType);

    /**
     * 기업 규모별 면제율 조회.
     *
     * @param corpSize 기업 규모
     * @return 해당 기업 규모의 면제율 목록
     */
    List<RefRdMinTaxExempt> findByCorpSize(String corpSize);
}
