package com.entec.tax.api.controller;

import com.entec.tax.domain.common.dto.ApiResponse;
import com.entec.tax.domain.report.dto.ReportResponseDto;
import com.entec.tax.domain.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보고서 조회 REST 컨트롤러.
 * <p>
 * API-04 (전체 보고서 JSON 조회), API-05 (섹션별 JSON 조회) 엔드포인트를 제공한다.
 * OUT_REPORT_JSON 테이블에 저장된 분석 보고서를 조회하여 반환한다.
 * </p>
 *
 * <ul>
 *   <li>API-04: GET /api/v1/requests/{reqId}/report — 전체 보고서 JSON 조회</li>
 *   <li>API-05: GET /api/v1/requests/{reqId}/report/sections/{section} — 섹션별 JSON 조회</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    /** 보고서 조회 서비스 */
    private final ReportService reportService;

    /**
     * API-04: 전체 보고서 JSON 조회.
     * <p>
     * 요청 건의 분석 보고서 전체를 JSON 형태로 반환한다.
     * 보고서에는 요약(Section A), 개별공제 상세(Section B), 최적 조합(Section C),
     * 환급 정보(Section D), 위험 요소(Section E), 추가 점검(Section F),
     * 메타 정보(Section G) 등이 포함된다.
     * </p>
     *
     * @param reqId 요청 ID (예: C-1234567890-20260216-001)
     * @return 전체 보고서 JSON 데이터
     */
    @GetMapping("/{reqId}/report")
    public ResponseEntity<ApiResponse<ReportResponseDto>> getFullReport(
            @PathVariable String reqId) {

        log.info("API-04 전체 보고서 조회 — reqId={}", reqId);

        ReportResponseDto report = reportService.getFullReport(reqId);

        log.info("API-04 전체 보고서 조회 완료 — reqId={}", reqId);

        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    /**
     * API-05: 섹션별 JSON 조회.
     * <p>
     * 요청 건의 분석 보고서 중 특정 섹션(A~G)만 조회하여 반환한다.
     * 대용량 보고서에서 필요한 섹션만 선택적으로 조회할 수 있다.
     * </p>
     *
     * <pre>
     * 섹션 구분:
     *   A — 요약 정보
     *   B — 개별공제 상세
     *   C — 최적 조합 정보
     *   D — 환급 정보
     *   E — 위험 요소 분석
     *   F — 추가 점검 사항
     *   G — 메타 정보
     * </pre>
     *
     * @param reqId   요청 ID
     * @param section 섹션 코드 (A, B, C, D, E, F, G)
     * @return 해당 섹션의 JSON 데이터
     */
    @GetMapping("/{reqId}/report/sections/{section}")
    public ResponseEntity<ApiResponse<Object>> getReportSection(
            @PathVariable String reqId,
            @PathVariable String section) {

        log.info("API-05 섹션별 보고서 조회 — reqId={}, section={}", reqId, section);

        Object sectionData = reportService.getReportSection(reqId, section);

        log.info("API-05 섹션별 보고서 조회 완료 — reqId={}, section={}", reqId, section);

        return ResponseEntity.ok(ApiResponse.ok(sectionData));
    }
}
