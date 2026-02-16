package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefEmploymentCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * REF_EMPLOYMENT_CREDIT 테이블 리포지토리.
 * <p>
 * 고용 관련 세액공제 기준 정보 조회를 담당한다.
 * </p>
 */
public interface RefEmploymentCreditRepository extends JpaRepository<RefEmploymentCredit, Integer> {

    /**
     * 귀속 연도별 고용 세액공제 기준 조회.
     *
     * @param taxYear 귀속 연도
     * @return 해당 연도의 고용 세액공제 기준 목록
     */
    List<RefEmploymentCredit> findByTaxYear(String taxYear);

    /**
     * 귀속 연도·기업 규모별 고용 세액공제 기준 조회.
     *
     * @param taxYear  귀속 연도
     * @param corpSize 기업 규모
     * @return 해당 조건의 고용 세액공제 기준 목록
     */
    List<RefEmploymentCredit> findByTaxYearAndCorpSize(String taxYear, String corpSize);

    /**
     * 귀속 연도·기업 규모·지역·근로자 유형별 공제 기준 조회.
     *
     * @param taxYear    귀속 연도
     * @param corpSize   기업 규모
     * @param region     지역
     * @param workerType 근로자 유형
     * @return 해당 조건의 고용 세액공제 기준 목록
     */
    List<RefEmploymentCredit> findByTaxYearAndCorpSizeAndRegionAndWorkerType(
            String taxYear, String corpSize, String region, String workerType);
}
