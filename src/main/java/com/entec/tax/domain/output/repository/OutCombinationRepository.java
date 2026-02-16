package com.entec.tax.domain.output.repository;

import com.entec.tax.domain.output.entity.OutCombination;
import com.entec.tax.domain.output.entity.OutCombinationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * OUT_COMBINATION 테이블 리포지토리.
 * <p>
 * 조합 비교 및 최적 선택 결과 CRUD 및 조회를 담당한다.
 * </p>
 */
public interface OutCombinationRepository extends JpaRepository<OutCombination, OutCombinationId> {

    /**
     * 요청 ID로 조합 결과 조회 (순위 오름차순).
     *
     * @param reqId 요청 ID
     * @return 해당 요청의 조합 결과 목록 (순위순)
     */
    List<OutCombination> findByReqIdOrderByComboRankAsc(String reqId);

    /**
     * 요청 ID 및 조합 순위로 조합 결과 조회.
     *
     * @param reqId     요청 ID
     * @param comboRank 조합 순위 (1 = 최적)
     * @return 해당 조건의 조합 결과 (없으면 Optional.empty)
     */
    Optional<OutCombination> findByReqIdAndComboRank(String reqId, Integer comboRank);

    /**
     * 요청 ID로 조합 결과 삭제 (TX-2 재시도 지원).
     *
     * @param reqId 요청 ID
     */
    void deleteByReqId(String reqId);
}
