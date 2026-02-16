package com.entec.tax.domain.input.repository;

import com.entec.tax.domain.input.entity.InpBasic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * INP_BASIC 테이블 리포지토리.
 * <p>
 * 요청 건의 기본 정보(신청자, 사업자, 세무 기본사항) CRUD 및 조회를 담당한다.
 * </p>
 */
public interface InpBasicRepository extends JpaRepository<InpBasic, String> {

    /**
     * 요청 ID로 기본 정보 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 기본 정보 (없으면 Optional.empty)
     */
    Optional<InpBasic> findByReqId(String reqId);
}
