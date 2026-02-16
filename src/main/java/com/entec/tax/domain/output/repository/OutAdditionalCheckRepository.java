package com.entec.tax.domain.output.repository;

import com.entec.tax.domain.output.entity.OutAdditionalCheck;
import com.entec.tax.domain.output.entity.OutAdditionalCheckId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * OUT_ADDITIONAL_CHECK 테이블 리포지토리.
 * <p>
 * 추가 확인 필요 항목 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface OutAdditionalCheckRepository extends JpaRepository<OutAdditionalCheck, OutAdditionalCheckId> {

    /**
     * 요청 ID로 추가 확인 필요 항목 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 추가 확인 항목 목록
     */
    List<OutAdditionalCheck> findByReqId(String reqId);

    /**
     * 요청 ID로 추가 확인 필요 항목 삭제 (TX-2 재시도 지원).
     *
     * @param reqId 요청 ID
     */
    void deleteByReqId(String reqId);
}
