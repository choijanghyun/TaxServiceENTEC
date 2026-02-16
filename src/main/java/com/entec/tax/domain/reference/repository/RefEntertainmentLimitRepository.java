package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefEntertainmentLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * REF_ENTERTAINMENT_LIMIT 테이블 리포지토리.
 * <p>
 * 접대비 한도액 산정 기준 정보 조회를 담당한다.
 * </p>
 */
public interface RefEntertainmentLimitRepository extends JpaRepository<RefEntertainmentLimit, Integer> {

    /**
     * 기업 규모별 접대비 한도 기준 조회.
     *
     * @param corpSize 기업 규모
     * @return 해당 기업 규모의 접대비 한도 기준 목록
     */
    List<RefEntertainmentLimit> findByCorpSize(String corpSize);

    /**
     * 특정 연도에 유효한 접대비 한도 기준 조회.
     *
     * @param year 귀속 연도
     * @return 해당 연도에 유효한 접대비 한도 기준 목록
     */
    @Query("SELECT r FROM RefEntertainmentLimit r WHERE r.yearFrom <= :year AND (r.yearTo >= :year OR r.yearTo IS NULL)")
    List<RefEntertainmentLimit> findByYear(@Param("year") String year);

    /**
     * 특정 연도·기업 규모·매출액에 해당하는 접대비 한도 기준 조회.
     *
     * @param year     귀속 연도
     * @param corpSize 기업 규모
     * @param revenue  매출액
     * @return 해당 조건의 접대비 한도 기준 목록
     */
    @Query("SELECT r FROM RefEntertainmentLimit r WHERE r.yearFrom <= :year AND (r.yearTo >= :year OR r.yearTo IS NULL) AND r.corpSize = :corpSize AND r.revenueBracketMin <= :revenue AND (r.revenueBracketMax > :revenue OR r.revenueBracketMax IS NULL)")
    List<RefEntertainmentLimit> findByYearAndCorpSizeAndRevenue(
            @Param("year") String year, @Param("corpSize") String corpSize, @Param("revenue") Long revenue);
}
