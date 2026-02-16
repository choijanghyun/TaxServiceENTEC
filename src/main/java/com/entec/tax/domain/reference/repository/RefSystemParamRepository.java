package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefSystemParam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * REF_SYSTEM_PARAM 테이블 리포지토리.
 * <p>
 * 시스템 파라미터 정보 조회를 담당한다.
 * </p>
 */
public interface RefSystemParamRepository extends JpaRepository<RefSystemParam, String> {

    /**
     * 파라미터 유형별 조회.
     *
     * @param paramType 파라미터 유형
     * @return 해당 유형의 시스템 파라미터 목록
     */
    List<RefSystemParam> findByParamType(String paramType);

    /**
     * 수정 가능한 파라미터만 조회.
     *
     * @param modifiable 수정 가능 여부
     * @return 수정 가능한 시스템 파라미터 목록
     */
    List<RefSystemParam> findByModifiable(Boolean modifiable);
}
