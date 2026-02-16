package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefIndustryEligibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

/**
 * REF_INDUSTRY_ELIGIBILITY 테이블 리포지토리.
 * <p>
 * 업종별 세제 혜택 적격 여부 조회를 담당한다.
 * </p>
 */
public interface RefIndustryEligibilityRepository extends JpaRepository<RefIndustryEligibility, String> {

    /**
     * 창업감면 적격 업종 조회.
     *
     * @param startupEligible 창업감면 적격 여부
     * @return 창업감면 적격 업종 목록
     */
    List<RefIndustryEligibility> findByStartupEligible(Boolean startupEligible);

    /**
     * 중소기업 특별세액감면 적격 업종 조회.
     *
     * @param smeSpecialEligible 중소기업 특별세액감면 적격 여부
     * @return 해당 적격 업종 목록
     */
    List<RefIndustryEligibility> findBySmeSpecialEligible(Boolean smeSpecialEligible);

    /**
     * 중소기업 적격 업종 조회.
     *
     * @param isSmeEligible 중소기업 적격 여부
     * @return 해당 적격 업종 목록
     */
    List<RefIndustryEligibility> findByIsSmeEligible(Boolean isSmeEligible);

    /**
     * 특정 기준일에 유효한 업종 적격 정보 조회.
     *
     * @param targetDate 기준일
     * @return 해당 기준일에 유효한 업종 적격 목록
     */
    @Query("SELECT r FROM RefIndustryEligibility r WHERE (r.effectiveFrom IS NULL OR r.effectiveFrom <= :targetDate) AND (r.effectiveTo IS NULL OR r.effectiveTo >= :targetDate)")
    List<RefIndustryEligibility> findByEffectiveDate(@Param("targetDate") Date targetDate);
}
