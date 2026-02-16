package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefCorpTaxRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * REF_CORP_TAX_RATE_HISTORY 테이블 리포지토리.
 * <p>
 * 법인세 세율 변경 이력 조회를 담당한다.
 * </p>
 */
public interface RefCorpTaxRateHistoryRepository extends JpaRepository<RefCorpTaxRateHistory, Integer> {

    /**
     * 특정 연도에 적용된 세율 이력 조회.
     *
     * @param year 귀속 연도
     * @return 해당 연도의 세율 이력 목록
     */
    @Query("SELECT r FROM RefCorpTaxRateHistory r WHERE r.yearFrom <= :year AND (r.yearTo >= :year OR r.yearTo IS NULL) ORDER BY r.bracketMin ASC")
    List<RefCorpTaxRateHistory> findByYear(@Param("year") String year);

    /**
     * 전체 세율 이력 연도순 조회.
     *
     * @return 세율 이력 목록 (연도순)
     */
    @Query("SELECT r FROM RefCorpTaxRateHistory r ORDER BY r.yearFrom ASC, r.bracketMin ASC")
    List<RefCorpTaxRateHistory> findAllOrderByYearAndBracket();
}
