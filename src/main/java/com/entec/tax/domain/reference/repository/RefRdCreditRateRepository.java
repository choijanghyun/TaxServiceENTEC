package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefRdCreditRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * REF_RD_CREDIT_RATE 테이블 리포지토리.
 * <p>
 * 연구·인력개발비 세액공제율 정보 조회를 담당한다.
 * </p>
 */
public interface RefRdCreditRateRepository extends JpaRepository<RefRdCreditRate, Integer> {

    /**
     * R&D 유형별 공제율 조회.
     *
     * @param rdType R&D 유형
     * @return 해당 R&D 유형의 공제율 목록
     */
    List<RefRdCreditRate> findByRdType(String rdType);

    /**
     * R&D 유형·기업 규모별 공제율 조회.
     *
     * @param rdType   R&D 유형
     * @param corpSize 기업 규모
     * @return 해당 조건의 공제율 목록
     */
    List<RefRdCreditRate> findByRdTypeAndCorpSize(String rdType, String corpSize);

    /**
     * R&D 유형·산출 방식·기업 규모별 공제율 조회.
     *
     * @param rdType   R&D 유형
     * @param method   산출 방식
     * @param corpSize 기업 규모
     * @return 해당 조건의 공제율 목록
     */
    List<RefRdCreditRate> findByRdTypeAndMethodAndCorpSize(String rdType, String method, String corpSize);
}
