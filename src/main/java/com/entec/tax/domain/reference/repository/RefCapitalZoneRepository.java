package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefCapitalZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * REF_CAPITAL_ZONE 테이블 리포지토리.
 * <p>
 * 수도권 과밀억제권역 등 지역 구분 정보 조회를 담당한다.
 * </p>
 */
public interface RefCapitalZoneRepository extends JpaRepository<RefCapitalZone, Integer> {

    /**
     * 시도·시군구로 지역 정보 조회.
     *
     * @param sido    시도
     * @param sigungu 시군구
     * @return 해당 지역 정보
     */
    Optional<RefCapitalZone> findBySidoAndSigungu(String sido, String sigungu);

    /**
     * 시도별 전체 지역 조회.
     *
     * @param sido 시도
     * @return 해당 시도의 지역 목록
     */
    List<RefCapitalZone> findBySido(String sido);

    /**
     * 권역 유형별 지역 조회.
     *
     * @param zoneType 권역 유형
     * @return 해당 권역의 지역 목록
     */
    List<RefCapitalZone> findByZoneType(String zoneType);

    /**
     * 수도권 지역만 조회.
     *
     * @param isCapital 수도권 여부
     * @return 수도권 지역 목록
     */
    List<RefCapitalZone> findByIsCapital(Boolean isCapital);

    /**
     * 인구감소지역만 조회.
     *
     * @param isDepopulation 인구감소지역 여부
     * @return 인구감소지역 목록
     */
    List<RefCapitalZone> findByIsDepopulation(Boolean isDepopulation);
}
