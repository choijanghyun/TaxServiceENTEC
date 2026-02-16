package com.entec.tax.api.controller;

import com.entec.tax.domain.common.dto.ApiResponse;
import com.entec.tax.engine.orchestrator.AnalysisOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 분석 실행 REST 컨트롤러.
 * <p>
 * API-02 (점검 실행) 엔드포인트를 제공한다.
 * M3(사전점검) → M4(개별공제산출) → M5(최적조합탐색) → M6(환급액산출) 파이프라인을
 * 순차적으로 실행한다.
 * </p>
 *
 * <ul>
 *   <li>API-02: POST /api/v1/requests/{reqId}/analyze — 점검 실행 (TX-2 트랜잭션, 300초 타임아웃)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    /** 분석 오케스트레이터 서비스 */
    private final AnalysisOrchestratorService analysisOrchestratorService;

    /**
     * API-02: 점검 실행 (M3~M6 파이프라인).
     * <p>
     * 요청 건에 대해 전체 분석 파이프라인을 실행한다.
     * TX-2 트랜잭션 범위(300초 타임아웃) 내에서 M3→M4→M5→M6 단계를
     * 순차적으로 실행하며, 각 단계의 결과를 다음 단계에 전달한다.
     * </p>
     *
     * <pre>
     * 처리 순서:
     *   1. 요청 상태 검증 (parsed 상태여야 실행 가능)
     *   2. M3 사전점검 (STEP 0)
     *   3. M4 개별 공제·감면 산출 (STEP 1-2)
     *   4. M5 최적 조합 탐색 (STEP 3)
     *   5. M6 최종 환급액 산출 (STEP 4-5)
     *   6. 상태 업데이트 (completed / failed)
     * </pre>
     *
     * @param reqId 요청 ID (예: C-1234567890-20260216-001)
     * @return 실행 결과 (req_id, status, trace_id, duration_ms)
     */
    @PostMapping("/{reqId}/analyze")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeRequest(
            @PathVariable String reqId) {

        log.info("API-02 점검 실행 시작 — reqId={}", reqId);

        Map<String, Object> result = analysisOrchestratorService.executeAnalysis(reqId);

        log.info("API-02 점검 실행 완료 — reqId={}, status={}, duration_ms={}",
                reqId, result.get("status"), result.get("duration_ms"));

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
