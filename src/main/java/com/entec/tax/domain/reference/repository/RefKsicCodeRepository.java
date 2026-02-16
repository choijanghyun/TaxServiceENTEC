package com.entec.tax.domain.reference.repository;

import com.entec.tax.domain.reference.entity.RefKsicCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * REF_KSIC_CODE 테이블 리포지토리.
 * <p>
 * 한국표준산업분류(KSIC) 코드 정보 조회를 담당한다.
 * </p>
 */
public interface RefKsicCodeRepository extends JpaRepository<RefKsicCode, String> {

    /**
     * 대분류(섹션)별 업종 코드 조회.
     *
     * @param section 대분류 코드
     * @return 해당 대분류의 업종 코드 목록
     */
    List<RefKsicCode> findBySection(String section);

    /**
     * 중분류(디비전)별 업종 코드 조회.
     *
     * @param division 중분류 코드
     * @return 해당 중분류의 업종 코드 목록
     */
    List<RefKsicCode> findByDivision(String division);

    /**
     * 업종명 키워드 검색.
     *
     * @param keyword 검색 키워드
     * @return 업종명에 키워드가 포함된 업종 코드 목록
     */
    @Query("SELECT r FROM RefKsicCode r WHERE r.industryName LIKE %:keyword%")
    List<RefKsicCode> findByIndustryNameContaining(@Param("keyword") String keyword);

    /**
     * 개정 버전별 업종 코드 조회.
     *
     * @param revision 개정 버전
     * @return 해당 개정 버전의 업종 코드 목록
     */
    List<RefKsicCode> findByRevision(String revision);
}
