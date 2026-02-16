package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefDepopulationArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * REF_DEPOPULATION_AREA 테이블 리포지토리.
 * <p>
 * 인구감소지역 지정 정보 조회를 담당한다.
 * </p>
 */
public interface RefDepopulationAreaRepository extends JpaRepository<RefDepopulationArea, Integer> {

    /**
     * 시도·시군구로 인구감소지역 조회.
     *
     * @param sido    시도
     * @param sigungu 시군구
     * @return 해당 인구감소지역 정보
     */
    Optional<RefDepopulationArea> findBySidoAndSigungu(String sido, String sigungu);

    /**
     * 활성 상태인 인구감소지역 전체 조회.
     *
     * @param isActive 활성 여부
     * @return 활성 인구감소지역 목록
     */
    List<RefDepopulationArea> findByIsActive(Boolean isActive);

    /**
     * 시도별 인구감소지역 조회.
     *
     * @param sido 시도
     * @return 해당 시도의 인구감소지역 목록
     */
    List<RefDepopulationArea> findBySido(String sido);
}
