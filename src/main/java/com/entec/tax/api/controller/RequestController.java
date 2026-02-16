package com.entec.tax.api.controller;

import com.entec.tax.domain.common.dto.ApiResponse;
import com.entec.tax.domain.request.dto.RequestCreateDto;
import com.entec.tax.domain.request.dto.RequestResponseDto;
import com.entec.tax.domain.request.dto.RequestStatusDto;
import com.entec.tax.domain.request.dto.RequestSummaryDto;
import com.entec.tax.domain.request.service.RequestManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 요청 관리 REST 컨트롤러.
 * <p>
 * API-01 (요청 접수), API-03 (상태 조회), API-06 (원시 데이터 조회),
 * API-08 (경량 요약 조회) 엔드포인트를 제공한다.
 * </p>
 *
 * <ul>
 *   <li>API-01: POST /api/v1/requests — 요청 접수 + 원시 JSON 수신</li>
 *   <li>API-03: GET  /api/v1/requests/{reqId}/status — 요청 상태 조회</li>
 *   <li>API-06: GET  /api/v1/requests/{reqId}/raw-data — 원시 입력 JSON 조회</li>
 *   <li>API-08: GET  /api/v1/requests/{reqId}/summary — 경량 요약 조회 (v3.2 신규)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
@Slf4j
public class RequestController {

    /** 요청 관리 서비스 */
    private final RequestManagementService requestManagementService;

    /**
     * API-01: 요청 접수 + 원시 JSON 수신.
     * <p>
     * 클라이언트로부터 세액공제 환급 분석 요청을 접수하고 원시 JSON 데이터를 수신한다.
     * Idempotency-Key 헤더를 통한 중복 요청 방지를 지원한다 (v3.2).
     * </p>
     *
     * @param apiKey          API 인증 키 (X-API-Key 헤더, 필수)
     * @param idempotencyKey  멱등성 키 (Idempotency-Key 헤더, 선택)
     * @param requestCreateDto 입력 JSON 데이터셋
     * @return req_id, status, datasets_received, created_at 를 포함하는 응답
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RequestResponseDto>> createRequest(
            @RequestHeader(value = "X-API-Key", required = true) String apiKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RequestCreateDto requestCreateDto) {

        log.info("API-01 요청 접수 시작 — applicantType={}, applicantId={}, idempotencyKey={}",
                requestCreateDto.getApplicantType(),
                requestCreateDto.getApplicantId(),
                idempotencyKey);

        RequestResponseDto result = requestManagementService.createRequest(requestCreateDto, idempotencyKey);

        log.info("API-01 요청 접수 완료 — reqId={}, status={}, datasetsReceived={}",
                result.getReqId(), result.getStatus(), result.getDatasetsReceived());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(result));
    }

    /**
     * API-03: 요청 상태 조회.
     * <p>
     * 요청 건의 현재 처리 상태와 진행 단계를 조회하여 반환한다.
     * </p>
     *
     * @param reqId 요청 ID (예: C-1234567890-20260216-001)
     * @return 요청 상태 정보 (req_id, status, progress)
     */
    @GetMapping("/{reqId}/status")
    public ResponseEntity<ApiResponse<RequestStatusDto>> getRequestStatus(
            @PathVariable String reqId) {

        log.info("API-03 요청 상태 조회 — reqId={}", reqId);

        RequestStatusDto status = requestManagementService.getRequestStatus(reqId);

        log.info("API-03 요청 상태 조회 완료 — reqId={}, status={}", reqId, status.getStatus());

        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    /**
     * API-06: 원시 입력 JSON 조회.
     * <p>
     * 요청 건의 원시 입력 데이터를 조회한다.
     * category 파라미터로 특정 카테고리의 데이터만 필터링할 수 있다.
     * </p>
     *
     * @param reqId    요청 ID
     * @param category 데이터 카테고리 (선택, 예: BASIC, EMPLOYEE, DEDUCTION)
     * @return 원시 입력 JSON 데이터
     */
    @GetMapping("/{reqId}/raw-data")
    public ResponseEntity<ApiResponse<Object>> getRawData(
            @PathVariable String reqId,
            @RequestParam(required = false) String category) {

        log.info("API-06 원시 데이터 조회 — reqId={}, category={}", reqId, category);

        Object rawData = requestManagementService.getRawData(reqId, category);

        log.info("API-06 원시 데이터 조회 완료 — reqId={}", reqId);

        return ResponseEntity.ok(ApiResponse.ok(rawData));
    }

    /**
     * API-08: 경량 요약 조회 (v3.2 신규).
     * <p>
     * 요청 건의 분석 결과를 경량 요약 형태로 반환한다.
     * 환급 예상 금액, 환급 이자, 지방세 환급, 최적 조합 정보 등을 포함한다.
     * </p>
     *
     * @param reqId 요청 ID
     * @return 경량 요약 정보
     */
    @GetMapping("/{reqId}/summary")
    public ResponseEntity<ApiResponse<RequestSummaryDto>> getRequestSummary(
            @PathVariable String reqId) {

        log.info("API-08 경량 요약 조회 — reqId={}", reqId);

        RequestSummaryDto summary = requestManagementService.getRequestSummary(reqId);

        log.info("API-08 경량 요약 조회 완료 — reqId={}, status={}", reqId, summary.getStatus());

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
