package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefDeemedInterestRate;
import com.entec.tax.domain.reference.entity.RefDeemedInterestRateId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * REF_DEEMED_INTEREST_RATE 테이블 리포지토리.
 * <p>
 * 인정이자율 정보 조회를 담당한다.
 * </p>
 */
public interface RefDeemedInterestRateRepository extends JpaRepository<RefDeemedInterestRate, RefDeemedInterestRateId> {

    /**
     * 연도별 인정이자율 조회.
     *
     * @param year 연도
     * @return 해당 연도의 인정이자율 목록
     */
    List<RefDeemedInterestRate> findByYear(String year);

    /**
     * 이자율 유형별 인정이자율 조회.
     *
     * @param rateType 이자율 유형
     * @return 해당 유형의 인정이자율 목록
     */
    List<RefDeemedInterestRate> findByRateType(String rateType);
}
