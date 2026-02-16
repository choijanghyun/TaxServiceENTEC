package com.entec.tax.domain.input.repository;

import com.entec.tax.domain.input.entity.InpRawData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * INP_RAW_DATA 테이블 리포지토리.
 * <p>
 * 요청 건에 대한 원본(Raw) JSON 데이터 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface InpRawDataRepository extends JpaRepository<InpRawData, Long> {

    /**
     * 요청 ID로 원본 데이터 전체 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 원본 데이터 목록
     */
    List<InpRawData> findByReqRequestReqId(String reqId);

    /**
     * 요청 ID 및 카테고리로 원본 데이터 조회.
     *
     * @param reqId    요청 ID
     * @param category 데이터 카테고리 (BASIC, EMPLOYEE, DEDUCTION 등)
     * @return 해당 조건의 원본 데이터 목록
     */
    List<InpRawData> findByReqRequestReqIdAndCategory(String reqId, String category);
}
