package com.entec.tax.api.controller;

import com.entec.tax.domain.common.dto.ApiResponse;
import com.entec.tax.domain.reference.entity.RefMutualExclusion;
import com.entec.tax.domain.reference.repository.RefMutualExclusionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 기준정보 조회 REST 컨트롤러.
 * <p>
 * API-07 (상호배제 기준정보 조회) 엔드포인트를 제공한다.
 * 세액공제·감면 항목 간의 중복 적용 배제 규칙을 조회한다.
 * </p>
 *
 * <ul>
 *   <li>API-07: GET /api/v1/reference/exclusion-matrix — 상호배제 기준정보 조회</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/reference")
@RequiredArgsConstructor
@Slf4j
public class ReferenceController {

    /** 상호배제 기준정보 리포지토리 */
    private final RefMutualExclusionRepository refMutualExclusionRepository;

    /**
     * API-07: 상호배제 기준정보 조회.
     * <p>
     * REF_MUTUAL_EXCLUSION 테이블의 전체 상호배제 규칙을 조회하여 반환한다.
     * 조항 A와 조항 B의 동시 적용 가능 여부, 조건 설명, 법적 근거 등을 포함한다.
     * 프론트엔드에서 상호배제 매트릭스를 시각화하거나,
     * 분석 결과의 조합 탐색 근거를 확인하는 데 활용된다.
     * </p>
     *
     * @return 상호배제 규칙 전체 목록
     */
    @GetMapping("/exclusion-matrix")
    public ResponseEntity<ApiResponse<List<RefMutualExclusion>>> getExclusionMatrix() {

        log.info("API-07 상호배제 기준정보 조회 시작");

        List<RefMutualExclusion> matrix = refMutualExclusionRepository.findAll();

        log.info("API-07 상호배제 기준정보 조회 완료 — 규칙 수={}", matrix.size());

        return ResponseEntity.ok(ApiResponse.ok(matrix));
    }
}
