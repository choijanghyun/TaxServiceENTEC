package com.entec.tax.domain.input.repository;

import com.entec.tax.domain.input.entity.InpEmployee;
import com.entec.tax.domain.input.entity.InpEmployeeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * INP_EMPLOYEE 테이블 리포지토리.
 * <p>
 * 요청 건의 고용 인원 및 급여 정보 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface InpEmployeeRepository extends JpaRepository<InpEmployee, InpEmployeeId> {

    /**
     * 요청 ID로 전체 연도 구분의 고용 정보 조회.
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 고용 정보 목록
     */
    List<InpEmployee> findByReqId(String reqId);

    /**
     * 요청 ID 및 연도 구분으로 고용 정보 조회.
     *
     * @param reqId    요청 ID
     * @param yearType 연도 구분 (PRIOR, CURRENT)
     * @return 해당 조건의 고용 정보 (없으면 Optional.empty)
     */
    Optional<InpEmployee> findByReqIdAndYearType(String reqId, String yearType);
}
