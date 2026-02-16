package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefSmeDeductionRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * REF_SME_DEDUCTION_RATE 테이블 리포지토리.
 * <p>
 * 중소기업 특별세액감면율 정보 조회를 담당한다.
 * </p>
 */
public interface RefSmeDeductionRateRepository extends JpaRepository<RefSmeDeductionRate, Integer> {

    /**
     * 기업 규모 세부 분류별 감면율 조회.
     *
     * @param corpSizeDetail 기업 규모 세부 분류
     * @return 해당 기업 규모의 감면율 목록
     */
    List<RefSmeDeductionRate> findByCorpSizeDetail(String corpSizeDetail);

    /**
     * 기업 규모 세부 분류·업종·권역별 감면율 조회.
     *
     * @param corpSizeDetail 기업 규모 세부 분류
     * @param industryClass  업종 분류
     * @param zoneType       권역 유형
     * @return 해당 조건의 감면율 목록
     */
    List<RefSmeDeductionRate> findByCorpSizeDetailAndIndustryClassAndZoneType(
            String corpSizeDetail, String industryClass, String zoneType);
}
