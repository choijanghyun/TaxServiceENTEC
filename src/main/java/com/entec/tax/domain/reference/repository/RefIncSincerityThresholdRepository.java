package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefIncSincerityThreshold;
import com.entec.tax.domain.reference.entity.RefIncSincerityThresholdId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * REF_INC_SINCERITY_THRESHOLD 테이블 리포지토리.
 * <p>
 * 개인(소득세) 성실신고확인 대상 수입금액 기준 조회를 담당한다.
 * </p>
 */
public interface RefIncSincerityThresholdRepository extends JpaRepository<RefIncSincerityThreshold, RefIncSincerityThresholdId> {

    /**
     * 업종군별 수입금액 기준 조회.
     *
     * @param industryGroup 업종군
     * @return 해당 업종군의 수입금액 기준 목록
     */
    List<RefIncSincerityThreshold> findByIndustryGroup(String industryGroup);

    /**
     * 적용 시작 연도별 수입금액 기준 조회.
     *
     * @param effectiveFrom 적용 시작 연도
     * @return 해당 연도의 수입금액 기준 목록
     */
    List<RefIncSincerityThreshold> findByEffectiveFrom(String effectiveFrom);

    /**
     * 특정 연도에 유효한 최신 수입금액 기준 조회.
     *
     * @param year 귀속 연도
     * @return 해당 연도에 유효한 수입금액 기준 목록
     */
    @Query("SELECT r FROM RefIncSincerityThreshold r WHERE r.effectiveFrom = (SELECT MAX(r2.effectiveFrom) FROM RefIncSincerityThreshold r2 WHERE r2.effectiveFrom <= :year AND r2.industryGroup = r.industryGroup)")
    List<RefIncSincerityThreshold> findByYear(@Param("year") String year);
}
