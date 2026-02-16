package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefRefundInterestRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

/**
 * REF_REFUND_INTEREST_RATE 테이블 리포지토리.
 * <p>
 * 환급 가산금 이자율 정보 조회를 담당한다.
 * </p>
 */
public interface RefRefundInterestRateRepository extends JpaRepository<RefRefundInterestRate, Integer> {

    /**
     * 특정 기준일에 유효한 이자율 조회.
     *
     * @param targetDate 기준일
     * @return 해당 기준일에 유효한 이자율 목록
     */
    @Query("SELECT r FROM RefRefundInterestRate r WHERE r.effectiveFrom <= :targetDate AND (r.effectiveTo >= :targetDate OR r.effectiveTo IS NULL) ORDER BY r.effectiveFrom DESC")
    List<RefRefundInterestRate> findByEffectiveDate(@Param("targetDate") Date targetDate);
}
