package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefIncTaxRate;
import com.entec.tax.domain.reference.entity.RefIncTaxRateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * REF_INC_TAX_RATE 테이블 리포지토리.
 * <p>
 * 개인(소득세) 기본 세율표 조회를 담당한다.
 * </p>
 */
public interface RefIncTaxRateRepository extends JpaRepository<RefIncTaxRate, RefIncTaxRateId> {

    /**
     * 적용 시작 연도별 전체 세율 구간 조회.
     *
     * @param effectiveFrom 적용 시작 연도
     * @return 해당 연도의 세율 구간 목록
     */
    List<RefIncTaxRate> findByEffectiveFromOrderByBracketNoAsc(String effectiveFrom);

    /**
     * 특정 연도에 유효한 최신 세율 구간 조회.
     *
     * @param year 귀속 연도
     * @return 해당 연도에 유효한 세율 구간 목록
     */
    @Query("SELECT r FROM RefIncTaxRate r WHERE r.effectiveFrom = (SELECT MAX(r2.effectiveFrom) FROM RefIncTaxRate r2 WHERE r2.effectiveFrom <= :year) ORDER BY r.bracketNo ASC")
    List<RefIncTaxRate> findByYear(@Param("year") String year);

    /**
     * 특정 연도·과세표준에 해당하는 세율 구간 조회.
     *
     * @param year          귀속 연도
     * @param taxableIncome 과세표준 금액
     * @return 해당 세율 구간
     */
    @Query("SELECT r FROM RefIncTaxRate r WHERE r.effectiveFrom = (SELECT MAX(r2.effectiveFrom) FROM RefIncTaxRate r2 WHERE r2.effectiveFrom <= :year) AND r.lowerLimit <= :taxableIncome AND (r.upperLimit > :taxableIncome OR r.upperLimit IS NULL)")
    List<RefIncTaxRate> findByYearAndTaxableIncome(@Param("year") String year, @Param("taxableIncome") Long taxableIncome);
}
