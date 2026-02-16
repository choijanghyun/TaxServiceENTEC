package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefMutualExclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * REF_MUTUAL_EXCLUSION 테이블 리포지토리.
 * <p>
 * 세액공제·감면 항목 간 중복 적용 배제 규칙 조회를 담당한다.
 * </p>
 */
public interface RefMutualExclusionRepository extends JpaRepository<RefMutualExclusion, Integer> {

    /**
     * 특정 조항이 포함된 배제 규칙 조회 (조항 A 또는 B에 해당).
     *
     * @param provision 조항 코드
     * @return 해당 조항이 포함된 배제 규칙 목록
     */
    @Query("SELECT r FROM RefMutualExclusion r WHERE r.provisionA = :provision OR r.provisionB = :provision")
    List<RefMutualExclusion> findByProvision(@Param("provision") String provision);

    /**
     * 두 조항 간 배제 규칙 조회.
     *
     * @param provisionA 조항 A
     * @param provisionB 조항 B
     * @return 해당 두 조항 간 배제 규칙 목록
     */
    @Query("SELECT r FROM RefMutualExclusion r WHERE (r.provisionA = :provisionA AND r.provisionB = :provisionB) OR (r.provisionA = :provisionB AND r.provisionB = :provisionA)")
    List<RefMutualExclusion> findByProvisionPair(@Param("provisionA") String provisionA, @Param("provisionB") String provisionB);

    /**
     * 특정 연도에 유효한 배제 규칙 조회.
     *
     * @param year 귀속 연도
     * @return 해당 연도에 유효한 배제 규칙 목록
     */
    @Query("SELECT r FROM RefMutualExclusion r WHERE r.yearFrom <= :year AND (r.yearTo >= :year OR r.yearTo IS NULL)")
    List<RefMutualExclusion> findByYear(@Param("year") String year);
}
