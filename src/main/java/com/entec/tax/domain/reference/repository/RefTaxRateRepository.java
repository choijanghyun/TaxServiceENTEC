package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefTaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * REF_TAX_RATE 테이블 리포지토리.
 * <p>
 * 법인세 기본 세율표 조회를 담당한다.
 * </p>
 */
public interface RefTaxRateRepository extends JpaRepository<RefTaxRate, Integer> {

    /**
     * 특정 연도에 적용되는 세율 구간 전체 조회.
     *
     * @param year 귀속 연도
     * @return 해당 연도의 세율 구간 목록
     */
    @Query("SELECT r FROM RefTaxRate r WHERE r.yearFrom <= :year AND (r.yearTo >= :year OR r.yearTo IS NULL) ORDER BY r.bracketMin ASC")
    List<RefTaxRate> findByYear(@Param("year") String year);

    /**
     * 특정 연도 및 과세표준에 해당하는 세율 구간 조회.
     *
     * @param year           귀속 연도
     * @param taxableIncome  과세표준 금액
     * @return 해당 세율 구간
     */
    @Query("SELECT r FROM RefTaxRate r WHERE r.yearFrom <= :year AND (r.yearTo >= :year OR r.yearTo IS NULL) AND r.bracketMin <= :taxableIncome AND (r.bracketMax > :taxableIncome OR r.bracketMax IS NULL)")
    List<RefTaxRate> findByYearAndTaxableIncome(@Param("year") String year, @Param("taxableIncome") Long taxableIncome);
}
