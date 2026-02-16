package com.entec.tax.domain.report.service;

import com.entec.tax.common.exception.ErrorCode;
import com.entec.tax.common.exception.RequestNotFoundException;
import com.entec.tax.common.exception.TaxServiceException;
import com.entec.tax.domain.report.dto.ReportResponseDto;
import com.entec.tax.domain.report.entity.OutReportJson;
import com.entec.tax.domain.report.repository.OutReportJsonRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 보고서 조회 서비스.
 * <p>
 * OUT_REPORT_JSON 테이블에서 분석 보고서를 조회하여 반환한다.
 * API-04 (전체 보고서 JSON 조회)와 API-05 (섹션별 JSON 조회)를 처리한다.
 * </p>
 *
 * <pre>
 * 보고서 섹션 구조:
 *   Section A — 요약 정보 (환급 예상 금액, 공제 항목 수 등)
 *   Section B — 개별공제 상세 (항목별 산출 근거, 금액)
 *   Section C — 최적 조합 정보 (조합별 비교, 최적 조합 선정 근거)
 *   Section D — 환급 정보 (환급가산금, 지방세 환급 등)
 *   Section E — 위험 요소 분석 (세무 리스크, 감사 주의 사항)
 *   Section F — 추가 점검 사항 (보완 필요 항목, 추가 확인 사항)
 *   Section G — 메타 정보 (버전, 생성 일시, 체크섬 등)
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    /** 보고서 JSON 리포지토리 */
    private final OutReportJsonRepository outReportJsonRepository;

    /** JSON 직렬화/역직렬화 유틸 */
    private final ObjectMapper objectMapper;

    /**
     * 전체 보고서를 조회한다 (API-04).
     * <p>
     * 요청 ID에 해당하는 보고서 전체를 조회하여 {@link ReportResponseDto}로 반환한다.
     * 보고서 JSON 문자열을 Map 구조로 역직렬화하여 응답에 포함시킨다.
     * </p>
     *
     * @param reqId 요청 ID (예: C-1234567890-20260216-001)
     * @return 전체 보고서 DTO
     * @throws RequestNotFoundException 보고서가 존재하지 않을 경우
     */
    public ReportResponseDto getFullReport(String reqId) {
        log.info("전체 보고서 조회 — reqId={}", reqId);

        OutReportJson reportEntity = outReportJsonRepository.findById(reqId)
                .orElseThrow(() -> new RequestNotFoundException(
                        "보고서를 찾을 수 없습니다: " + reqId, reqId));

        // 보고서 JSON 문자열을 Map 으로 변환
        Map<String, Object> reportMap = parseReportJson(reportEntity, reqId);

        // 보고서 메타 정보를 Map 에 추가
        reportMap.put("req_id", reportEntity.getReqId());
        reportMap.put("report_version", reportEntity.getReportVersion());
        reportMap.put("report_status", reportEntity.getReportStatus());
        reportMap.put("result_code", reportEntity.getResultCode());
        reportMap.put("generated_at", reportEntity.getGeneratedAt() != null
                ? reportEntity.getGeneratedAt().toString() : null);

        ReportResponseDto responseDto = new ReportResponseDto();
        responseDto.setReport(reportMap);

        log.info("전체 보고서 조회 완료 — reqId={}, reportVersion={}, reportStatus={}",
                reqId, reportEntity.getReportVersion(), reportEntity.getReportStatus());

        return responseDto;
    }

    /**
     * 섹션별 보고서를 조회한다 (API-05).
     * <p>
     * 요청 ID에 해당하는 보고서에서 지정된 섹션(A~G)의 데이터만 추출하여 반환한다.
     * 대용량 보고서에서 필요한 부분만 선택적으로 조회할 수 있어 네트워크 효율이 높다.
     * </p>
     *
     * @param reqId   요청 ID
     * @param section 섹션 코드 (A, B, C, D, E, F, G)
     * @return 해당 섹션의 JSON 데이터 (문자열 또는 Map)
     * @throws RequestNotFoundException 보고서가 존재하지 않을 경우
     * @throws TaxServiceException 유효하지 않은 섹션 코드일 경우
     */
    public Object getReportSection(String reqId, String section) {
        log.info("섹션별 보고서 조회 — reqId={}, section={}", reqId, section);

        OutReportJson reportEntity = outReportJsonRepository.findById(reqId)
                .orElseThrow(() -> new RequestNotFoundException(
                        "보고서를 찾을 수 없습니다: " + reqId, reqId));

        String sectionUpper = section.toUpperCase();
        String sectionJson;

        switch (sectionUpper) {
            case "A":
                sectionJson = reportEntity.getSectionAJson();
                break;
            case "B":
                sectionJson = reportEntity.getSectionBJson();
                break;
            case "C":
                sectionJson = reportEntity.getSectionCJson();
                break;
            case "D":
                sectionJson = reportEntity.getSectionDJson();
                break;
            case "E":
                sectionJson = reportEntity.getSectionEJson();
                break;
            case "F":
                sectionJson = reportEntity.getSectionFJson();
                break;
            case "G":
                sectionJson = reportEntity.getSectionGMeta();
                break;
            default:
                throw new TaxServiceException(
                        ErrorCode.VALIDATION_FAILED,
                        "유효하지 않은 섹션: " + section + " (A~G만 허용)",
                        reqId);
        }

        log.info("섹션별 보고서 조회 완료 — reqId={}, section={}, hasData={}",
                reqId, sectionUpper, sectionJson != null);

        // JSON 문자열이면 Map 으로 변환하여 반환, null 이면 빈 Map 반환
        if (sectionJson == null || sectionJson.trim().isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }

        return parseSectionJson(sectionJson, reqId, sectionUpper);
    }

    /**
     * 보고서 전체 JSON 문자열을 Map 으로 역직렬화한다.
     *
     * @param reportEntity 보고서 엔티티
     * @param reqId        요청 ID (오류 추적용)
     * @return 역직렬화된 보고서 Map
     */
    private Map<String, Object> parseReportJson(OutReportJson reportEntity, String reqId) {
        String reportJsonStr = reportEntity.getReportJson();
        if (reportJsonStr == null || reportJsonStr.trim().isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }

        try {
            return objectMapper.readValue(reportJsonStr,
                    new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            log.warn("보고서 JSON 파싱 실패 — reqId={}, error={}", reqId, e.getMessage());
            // 파싱 실패 시 원본 문자열을 report_json 키에 담아 반환
            Map<String, Object> fallbackMap = new LinkedHashMap<String, Object>();
            fallbackMap.put("report_json_raw", reportJsonStr);
            return fallbackMap;
        }
    }

    /**
     * 섹션별 JSON 문자열을 Object 로 역직렬화한다.
     *
     * @param sectionJson 섹션 JSON 문자열
     * @param reqId       요청 ID (오류 추적용)
     * @param section     섹션 코드 (오류 추적용)
     * @return 역직렬화된 섹션 데이터 (Map 또는 List)
     */
    private Object parseSectionJson(String sectionJson, String reqId, String section) {
        try {
            return objectMapper.readValue(sectionJson, Object.class);
        } catch (Exception e) {
            log.warn("섹션 JSON 파싱 실패 — reqId={}, section={}, error={}",
                    reqId, section, e.getMessage());
            // 파싱 실패 시 원본 문자열 그대로 반환
            return sectionJson;
        }
    }
}
