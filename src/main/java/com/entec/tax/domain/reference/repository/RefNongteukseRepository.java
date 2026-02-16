package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefNongteukse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * REF_NONGTEUKSE 테이블 리포지토리.
 * <p>
 * 농어촌특별세 과세·면제 기준 정보 조회를 담당한다.
 * </p>
 */
public interface RefNongteukseRepository extends JpaRepository<RefNongteukse, String> {

    /**
     * 면제 여부별 조항 목록 조회.
     *
     * @param isExempt 면제 여부
     * @return 해당 면제 여부의 조항 목록
     */
    List<RefNongteukse> findByIsExempt(Boolean isExempt);
}
