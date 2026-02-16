package com.entec.tax.engine.orchestrator;

import com.entec.tax.common.exception.ErrorCode;
import com.entec.tax.common.exception.HardFailException;
import com.entec.tax.common.exception.RequestNotFoundException;
import com.entec.tax.common.exception.TaxServiceException;
import com.entec.tax.domain.log.entity.LogCalculation;
import com.entec.tax.domain.log.repository.LogCalculationRepository;
import com.entec.tax.domain.request.entity.ReqRequest;
import com.entec.tax.domain.request.repository.ReqRequestRepository;
import com.entec.tax.engine.combination.service.CombinationSearchService;
import com.entec.tax.engine.credit.service.CreditCalculationService;
import com.entec.tax.engine.precheck.service.PreCheckService;
import com.entec.tax.engine.refund.service.RefundCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 분석 오케스트레이터 서비스.
 * <p>
 * M3(사전점검) → M4(개별공제산출) → M5(최적조합탐색) → M6(환급액산출) 파이프라인을
 * 순차적으로 실행한다.
 * TX-2 트랜잭션 범위(300초) 내에서 전체 파이프라인을 실행하며,
 * 각 단계의 결과를 다음 단계에 전달한다.
 * </p>
 *
 * <p>
 * API-02 (POST /api/v1/requests/{reqId}/analyze)에서 호출된다.
 * </p>
 *
 * <pre>
 * 처리 순서:
 *   1. 요청 상태 검증 (parsed 상태여야 실행 가능)
 *   2. M3 사전점검 (STEP 0) — 자격 진단, 상시근로자 산정, 결산확정 검증
 *   3. M4 개별 공제·감면 산출 (STEP 1-2) — 각 항목별 공제/감면액 산출
 *   4. M5 최적 조합 탐색 (STEP 3) — 상호배제, 최저한세 반영 조합 탐색
 *   5. M6 최종 환급액 산출 (STEP 4-5) — 환급가산금, 지방세, 보고서 생성
 *   6. 상태 업데이트 (completed / failed)
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisOrchestratorService {

    /** 요청 리포지토리 */
    private final ReqRequestRepository reqRequestRepository;

    /** M3 사전점검 서비스 */
    private final PreCheckService preCheckService;

    /** M4 개별 공제·감면 산출 서비스 */
    private final CreditCalculationService creditCalculationService;

    /** M5 최적 조합 탐색 서비스 */
    private final CombinationSearchService combinationSearchService;

    /** M6 최종 환급액 산출 서비스 */
    private final RefundCalculationService refundCalculationService;

    /** 감사추적 로그 리포지토리 */
    private final LogCalculationRepository logCalculationRepository;

    /**
     * 분석 파이프라인을 실행한다.
     * <p>
     * 요청 상태를 검증한 후 M3→M4→M5→M6 단계를 순차적으로 실행한다.
     * 각 단계 시작/종료 시점에 감사추적 로그를 기록하며,
     * 실패 시 요청 상태를 failed 또는 hard_fail 로 갱신한다.
     * </p>
     *
     * <p>TX-2 트랜잭션 범위: 300초 타임아웃</p>
     *
     * @param reqId 요청번호 (예: C-1234567890-20260216-001)
     * @return 실행 결과 맵 (req_id, status, trace_id, duration_ms)
     * @throws RequestNotFoundException 요청이 존재하지 않을 경우
     * @throws TaxServiceException 분석 중 오류 발생 시
     * @throws HardFailException 결산조정 차단 항목이 발견된 경우
     */
    @Transactional(timeout = 300)
    public Map<String, Object> executeAnalysis(String reqId) {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();

        log.info("분석 파이프라인 실행 시작 — reqId={}, traceId={}", reqId, traceId);

        // ──────────────────────────────────────────────────────────────
        // 1. 요청 상태 검증: parsed 상태여야 실행 가능
        // ──────────────────────────────────────────────────────────────
        ReqRequest request = reqRequestRepository.findById(reqId)
                .orElseThrow(() -> new RequestNotFoundException(
                        "요청을 찾을 수 없습니다: " + reqId, reqId));

        String currentStatus = request.getRequestStatus();
        if (!"parsed".equalsIgnoreCase(currentStatus)
                && !"PARSED".equals(currentStatus)) {
            throw new TaxServiceException(
                    ErrorCode.CONCURRENT_CONFLICT,
                    "현재 상태(" + currentStatus + ")에서는 점검을 실행할 수 없습니다. "
                            + "'parsed' 상태에서만 분석을 시작할 수 있습니다.",
                    reqId);
        }

        // 상태를 checking 으로 갱신
        LocalDateTime now = LocalDateTime.now();
        reqRequestRepository.updateStatus(reqId, "checking", now);

        try {
            // ──────────────────────────────────────────────────────────
            // 2. STEP 0: M3 사전점검
            //    - 자격 진단 (M3-01)
            //    - 상시근로자 산정 (M3-02)
            //    - 결산확정 검증 (M3-03)
            // ──────────────────────────────────────────────────────────
            logStep(reqId, "M3-START", traceId, startTime);
            log.info("M3 사전점검 시작 — reqId={}", reqId);

            preCheckService.executePreCheck(reqId);

            logStep(reqId, "M3-END", traceId, startTime);
            log.info("M3 사전점검 완료 — reqId={}, elapsed={}ms",
                    reqId, System.currentTimeMillis() - startTime);

            // ──────────────────────────────────────────────────────────
            // 3. STEP 1-2: M4 개별 공제·감면 산출
            //    - 각 항목별 공제/감면액 산출
            //    - 고용증대, 연구개발비, 투자 등 개별 공제 항목 처리
            // ──────────────────────────────────────────────────────────
            logStep(reqId, "M4-START", traceId, startTime);
            log.info("M4 개별 공제·감면 산출 시작 — reqId={}", reqId);

            reqRequestRepository.updateStatus(reqId, "calculating", LocalDateTime.now());
            creditCalculationService.calculateCredits(reqId);

            logStep(reqId, "M4-END", traceId, startTime);
            log.info("M4 개별 공제·감면 산출 완료 — reqId={}, elapsed={}ms",
                    reqId, System.currentTimeMillis() - startTime);

            // ──────────────────────────────────────────────────────────
            // 4. STEP 3: M5 최적 조합 탐색
            //    - 상호배제 규칙 적용
            //    - 최저한세 제약 반영
            //    - 환급액 최대화 조합 탐색
            // ──────────────────────────────────────────────────────────
            logStep(reqId, "M5-START", traceId, startTime);
            log.info("M5 최적 조합 탐색 시작 — reqId={}", reqId);

            reqRequestRepository.updateStatus(reqId, "optimizing", LocalDateTime.now());
            combinationSearchService.findOptimalCombination(reqId);

            logStep(reqId, "M5-END", traceId, startTime);
            log.info("M5 최적 조합 탐색 완료 — reqId={}, elapsed={}ms",
                    reqId, System.currentTimeMillis() - startTime);

            // ──────────────────────────────────────────────────────────
            // 5. STEP 4-5: M6 최종 환급액 산출 및 보고서 생성
            //    - 환급가산금 계산 (M6-01)
            //    - 지방세 환급액 계산 (M6-02)
            //    - 보고서 JSON 생성 (M6-03)
            // ──────────────────────────────────────────────────────────
            logStep(reqId, "M6-START", traceId, startTime);
            log.info("M6 최종 환급액 산출 시작 — reqId={}", reqId);

            reqRequestRepository.updateStatus(reqId, "reporting", LocalDateTime.now());
            refundCalculationService.calculateFinalRefund(reqId);

            logStep(reqId, "M6-END", traceId, startTime);
            log.info("M6 최종 환급액 산출 완료 — reqId={}, elapsed={}ms",
                    reqId, System.currentTimeMillis() - startTime);

            // ──────────────────────────────────────────────────────────
            // 6. 완료 상태 갱신
            // ──────────────────────────────────────────────────────────
            reqRequestRepository.updateStatus(reqId, "completed", LocalDateTime.now());

            long durationMs = System.currentTimeMillis() - startTime;
            log.info("분석 파이프라인 실행 완료 — reqId={}, traceId={}, duration={}ms",
                    reqId, traceId, durationMs);

            // 결과 맵 생성 (키 순서 보장을 위해 LinkedHashMap 사용)
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("req_id", reqId);
            result.put("status", "completed");
            result.put("trace_id", traceId);
            result.put("duration_ms", durationMs);
            return result;

        } catch (HardFailException e) {
            // 결산조정 차단(Hard-Fail): 복구 불가능한 오류
            log.error("분석 파이프라인 Hard-Fail 발생 — reqId={}, blockedItems={}, message={}",
                    reqId, e.getBlockedItems(), e.getMessage(), e);

            reqRequestRepository.updateStatusWithError(
                    reqId, "hard_fail", e.getMessage(), LocalDateTime.now());
            logError(reqId, "HARD_FAIL", e, traceId);
            throw e;

        } catch (TaxServiceException e) {
            // 서비스 레벨 오류
            log.error("분석 파이프라인 서비스 오류 — reqId={}, errorCode={}, message={}",
                    reqId, e.getErrorCode(), e.getMessage(), e);

            reqRequestRepository.updateStatusWithError(
                    reqId, "failed", e.getMessage(), LocalDateTime.now());
            logError(reqId, "PIPELINE_ERROR", e, traceId);
            throw e;

        } catch (Exception e) {
            // 예상치 못한 시스템 오류
            log.error("분석 파이프라인 시스템 오류 — reqId={}, message={}",
                    reqId, e.getMessage(), e);

            reqRequestRepository.updateStatusWithError(
                    reqId, "failed", e.getMessage(), LocalDateTime.now());
            logError(reqId, "PIPELINE_ERROR", e, traceId);

            throw new TaxServiceException(
                    ErrorCode.CALCULATION_FAILED,
                    "분석 파이프라인 실행 중 오류: " + e.getMessage(),
                    reqId,
                    e);
        }
    }

    /**
     * 파이프라인 단계별 감사추적 로그를 기록한다.
     *
     * @param reqId     요청 ID
     * @param step      단계 식별자 (예: M3-START, M4-END)
     * @param traceId   추적 ID
     * @param startTime 파이프라인 시작 시간 (밀리초)
     */
    private void logStep(String reqId, String step, String traceId, long startTime) {
        LogCalculation logEntry = LogCalculation.builder()
                .reqId(reqId)
                .calcStep(step)
                .functionName("AnalysisOrchestratorService.executeAnalysis")
                .logLevel("INFO")
                .traceId(traceId)
                .durationMs((int) (System.currentTimeMillis() - startTime))
                .executedAt(LocalDateTime.now())
                .build();
        logCalculationRepository.save(logEntry);
    }

    /**
     * 파이프라인 오류 감사추적 로그를 기록한다.
     *
     * @param reqId   요청 ID
     * @param step    오류 발생 단계 식별자 (예: HARD_FAIL, PIPELINE_ERROR)
     * @param e       발생한 예외
     * @param traceId 추적 ID
     */
    private void logError(String reqId, String step, Exception e, String traceId) {
        // 오류 메시지에 포함된 큰따옴표를 이스케이프하여 JSON 형식 유지
        String errorMessage = e.getMessage() != null
                ? e.getMessage().replace("\"", "\\\"")
                : "Unknown error";

        LogCalculation logEntry = LogCalculation.builder()
                .reqId(reqId)
                .calcStep(step)
                .functionName("AnalysisOrchestratorService.executeAnalysis")
                .logLevel("ERROR")
                .outputData("{\"error\": \"" + errorMessage + "\"}")
                .traceId(traceId)
                .executedAt(LocalDateTime.now())
                .build();
        logCalculationRepository.save(logEntry);
    }
}
