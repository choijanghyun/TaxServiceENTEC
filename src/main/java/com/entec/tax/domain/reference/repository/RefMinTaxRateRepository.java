package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefMinTaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * REF_MIN_TAX_RATE 테이블 리포지토리.
 * <p>
 * 최저한세율 조회를 담당한다.
 * </p>
 */
public interface RefMinTaxRateRepository extends JpaRepository<RefMinTaxRate, Integer> {

    /**
     * 기업 규모별 최저한세율 구간 조회.
     *
     * @param corpSize 기업 규모
     * @return 해당 기업 규모의 최저한세율 구간 목록
     */
    List<RefMinTaxRate> findByCorpSize(String corpSize);

    /**
     * 기업 규모 및 과세표준에 해당하는 최저한세율 조회.
     *
     * @param corpSize      기업 규모
     * @param taxableIncome 과세표준 금액
     * @return 해당 최저한세율 구간
     */
    @Query("SELECT r FROM RefMinTaxRate r WHERE r.corpSize = :corpSize AND r.bracketMin <= :taxableIncome AND (r.bracketMax > :taxableIncome OR r.bracketMax IS NULL)")
    List<RefMinTaxRate> findByCorpSizeAndTaxableIncome(@Param("corpSize") String corpSize, @Param("taxableIncome") Long taxableIncome);
}
