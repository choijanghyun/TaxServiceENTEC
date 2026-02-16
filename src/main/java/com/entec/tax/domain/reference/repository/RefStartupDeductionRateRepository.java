package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefStartupDeductionRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * REF_STARTUP_DEDUCTION_RATE 테이블 리포지토리.
 * <p>
 * 창업 중소기업 세액감면율 정보 조회를 담당한다.
 * </p>
 */
public interface RefStartupDeductionRateRepository extends JpaRepository<RefStartupDeductionRate, Integer> {

    /**
     * 창업자 유형별 감면율 조회.
     *
     * @param founderType 창업자 유형
     * @return 해당 유형의 감면율 목록
     */
    List<RefStartupDeductionRate> findByFounderType(String founderType);

    /**
     * 창업자 유형·소재지 유형별 감면율 조회.
     *
     * @param founderType  창업자 유형
     * @param locationType 소재지 유형
     * @return 해당 조건의 감면율 목록
     */
    List<RefStartupDeductionRate> findByFounderTypeAndLocationType(String founderType, String locationType);

    /**
     * 특정 연도에 유효한 감면율 조회.
     *
     * @param year 귀속 연도
     * @return 해당 연도에 유효한 감면율 목록
     */
    @Query("SELECT r FROM RefStartupDeductionRate r WHERE r.yearFrom <= :year AND (r.yearTo >= :year OR r.yearTo IS NULL)")
    List<RefStartupDeductionRate> findByYear(@Param("year") String year);
}
