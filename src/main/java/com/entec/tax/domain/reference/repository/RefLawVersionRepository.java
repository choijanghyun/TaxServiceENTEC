package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefLawVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * REF_LAW_VERSION 테이블 리포지토리.
 * <p>
 * 관련 법령 버전 이력 조회를 담당한다.
 * </p>
 */
public interface RefLawVersionRepository extends JpaRepository<RefLawVersion, Integer> {

    /**
     * 법률명으로 버전 이력 조회.
     *
     * @param lawName 법률명
     * @return 해당 법률의 버전 이력 목록
     */
    List<RefLawVersion> findByLawName(String lawName);

    /**
     * 조항으로 버전 이력 조회.
     *
     * @param provision 조항
     * @return 해당 조항의 버전 이력 목록
     */
    List<RefLawVersion> findByProvision(String provision);

    /**
     * 특정 연도에 유효한 법령 버전 조회.
     *
     * @param year 귀속 연도
     * @return 해당 연도에 유효한 법령 버전 목록
     */
    @Query("SELECT r FROM RefLawVersion r WHERE r.yearFrom <= :year AND (r.yearTo >= :year OR r.yearTo IS NULL)")
    List<RefLawVersion> findByYear(@Param("year") String year);

    /**
     * 특정 조항의 특정 연도에 유효한 법령 버전 조회.
     *
     * @param provision 조항
     * @param year      귀속 연도
     * @return 해당 조건의 법령 버전 목록
     */
    @Query("SELECT r FROM RefLawVersion r WHERE r.provision = :provision AND r.yearFrom <= :year AND (r.yearTo >= :year OR r.yearTo IS NULL)")
    List<RefLawVersion> findByProvisionAndYear(@Param("provision") String provision, @Param("year") String year);
}
