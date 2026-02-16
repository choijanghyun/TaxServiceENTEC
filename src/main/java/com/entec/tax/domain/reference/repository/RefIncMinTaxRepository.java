package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefIncMinTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * REF_INC_MIN_TAX 테이블 리포지토리.
 * <p>
 * 개인(소득세) 최저한세 기준 정보 조회를 담당한다.
 * </p>
 */
public interface RefIncMinTaxRepository extends JpaRepository<RefIncMinTax, String> {

    /**
     * 특정 연도에 유효한 최신 최저한세 기준 조회.
     *
     * @param year 귀속 연도
     * @return 해당 연도에 유효한 최저한세 기준
     */
    @Query("SELECT r FROM RefIncMinTax r WHERE r.effectiveFrom = (SELECT MAX(r2.effectiveFrom) FROM RefIncMinTax r2 WHERE r2.effectiveFrom <= :year)")
    Optional<RefIncMinTax> findByYear(@Param("year") String year);
}
