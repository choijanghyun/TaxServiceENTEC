package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefExchangeRate;
import com.entec.tax.domain.reference.entity.RefExchangeRateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

/**
 * REF_EXCHANGE_RATE 테이블 리포지토리.
 * <p>
 * 환율 정보 조회를 담당한다.
 * </p>
 */
public interface RefExchangeRateRepository extends JpaRepository<RefExchangeRate, RefExchangeRateId> {

    /**
     * 특정 기준일의 전체 통화 환율 조회.
     *
     * @param rateDate 환율 기준일
     * @return 해당 기준일의 환율 목록
     */
    List<RefExchangeRate> findByRateDate(Date rateDate);

    /**
     * 특정 통화의 환율 이력 조회.
     *
     * @param currency 통화 코드
     * @return 해당 통화의 환율 이력 목록
     */
    List<RefExchangeRate> findByCurrency(String currency);

    /**
     * 특정 기간의 특정 통화 환율 조회.
     *
     * @param currency  통화 코드
     * @param startDate 시작일
     * @param endDate   종료일
     * @return 해당 기간의 환율 목록
     */
    @Query("SELECT r FROM RefExchangeRate r WHERE r.currency = :currency AND r.rateDate BETWEEN :startDate AND :endDate ORDER BY r.rateDate ASC")
    List<RefExchangeRate> findByCurrencyAndDateRange(
            @Param("currency") String currency, @Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
