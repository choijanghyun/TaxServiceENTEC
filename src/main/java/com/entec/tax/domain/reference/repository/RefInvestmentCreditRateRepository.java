package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefInvestmentCreditRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * REF_INVESTMENT_CREDIT_RATE 테이블 리포지토리.
 * <p>
 * 투자 세액공제율 정보 조회를 담당한다.
 * </p>
 */
public interface RefInvestmentCreditRateRepository extends JpaRepository<RefInvestmentCreditRate, Integer> {

    /**
     * 투자 유형·기업 규모별 공제율 조회.
     *
     * @param investType 투자 유형
     * @param corpSize   기업 규모
     * @return 해당 조건의 투자 세액공제율 목록
     */
    List<RefInvestmentCreditRate> findByInvestTypeAndCorpSize(String investType, String corpSize);

    /**
     * 특정 연도에 적용되는 투자 세액공제율 조회.
     *
     * @param year 귀속 연도
     * @return 해당 연도의 투자 세액공제율 목록
     */
    @Query("SELECT r FROM RefInvestmentCreditRate r WHERE r.taxYearFrom <= :year ORDER BY r.investType, r.corpSize")
    List<RefInvestmentCreditRate> findByTaxYear(@Param("year") String year);

    /**
     * 특정 연도·투자 유형·기업 규모에 해당하는 공제율 조회.
     *
     * @param year       귀속 연도
     * @param investType 투자 유형
     * @param corpSize   기업 규모
     * @return 해당 조건의 투자 세액공제율 목록
     */
    @Query("SELECT r FROM RefInvestmentCreditRate r WHERE r.taxYearFrom <= :year AND r.investType = :investType AND r.corpSize = :corpSize")
    List<RefInvestmentCreditRate> findByYearAndInvestTypeAndCorpSize(
            @Param("year") String year, @Param("investType") String investType, @Param("corpSize") String corpSize);
}
