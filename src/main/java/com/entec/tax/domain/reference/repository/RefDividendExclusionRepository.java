package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefDividendExclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * REF_DIVIDEND_EXCLUSION 테이블 리포지토리.
 * <p>
 * 수입배당금 익금불산입률 정보 조회를 담당한다.
 * </p>
 */
public interface RefDividendExclusionRepository extends JpaRepository<RefDividendExclusion, Integer> {

    /**
     * 법인 유형별 익금불산입률 조회.
     *
     * @param corpType 법인 유형
     * @return 해당 법인 유형의 익금불산입률 목록
     */
    List<RefDividendExclusion> findByCorpType(String corpType);

    /**
     * 특정 연도에 유효한 익금불산입률 조회.
     *
     * @param year 귀속 연도
     * @return 해당 연도에 유효한 익금불산입률 목록
     */
    @Query("SELECT r FROM RefDividendExclusion r WHERE r.yearFrom <= :year AND (r.yearTo >= :year OR r.yearTo IS NULL)")
    List<RefDividendExclusion> findByYear(@Param("year") String year);

    /**
     * 특정 연도·법인 유형·지분율에 해당하는 익금불산입률 조회.
     *
     * @param year       귀속 연도
     * @param corpType   법인 유형
     * @param shareRatio 지분율
     * @return 해당 조건의 익금불산입률 목록
     */
    @Query("SELECT r FROM RefDividendExclusion r WHERE r.yearFrom <= :year AND (r.yearTo >= :year OR r.yearTo IS NULL) AND r.corpType = :corpType AND r.shareRatioMin <= :shareRatio AND (r.shareRatioMax > :shareRatio OR r.shareRatioMax IS NULL)")
    List<RefDividendExclusion> findByYearAndCorpTypeAndShareRatio(
            @Param("year") String year, @Param("corpType") String corpType, @Param("shareRatio") BigDecimal shareRatio);
}
