package com.entec.tax.domain.request.service;

import com.entec.tax.domain.request.dto.RequestCreateDto;
import com.entec.tax.domain.request.dto.RequestResponseDto;
import com.entec.tax.domain.request.dto.RequestStatusDto;
import com.entec.tax.domain.request.dto.RequestSummaryDto;

/**
 * 요청 관리 서비스 인터페이스.
 * <p>
 * API-01 (요청 접수), API-03 (상태 조회), API-06 (원시 데이터 조회),
 * API-08 (경량 요약 조회) 엔드포인트에서 호출되는 비즈니스 로직을 정의한다.
 * </p>
 */
public interface RequestManagementService {

    /**
     * API-01: 요청 접수 + 원시 JSON 수신.
     * <p>
     * 입력 JSON 데이터셋을 수신하여 요청을 생성하고,
     * 원시 데이터를 INP_RAW_DATA 테이블에 저장한다.
     * Idempotency-Key 를 통한 중복 요청 방지를 지원한다 (v3.2).
     * </p>
     *
     * @param requestCreateDto 입력 JSON 데이터셋
     * @param idempotencyKey   멱등성 키 (nullable)
     * @return 요청 접수 결과 (req_id, status, datasets_received, created_at)
     */
    RequestResponseDto createRequest(RequestCreateDto requestCreateDto, String idempotencyKey);

    /**
     * API-03: 요청 상태 조회.
     * <p>
     * 요청 건의 현재 처리 상태와 진행 단계를 조회한다.
     * </p>
     *
     * @param reqId 요청 ID
     * @return 요청 상태 정보
     */
    RequestStatusDto getRequestStatus(String reqId);

    /**
     * API-06: 원시 입력 JSON 조회.
     * <p>
     * 요청 건의 원시 입력 데이터를 카테고리별로 조회한다.
     * category 파라미터가 null 이면 전체 데이터를 반환한다.
     * </p>
     *
     * @param reqId    요청 ID
     * @param category 데이터 카테고리 (nullable)
     * @return 원시 입력 JSON 데이터
     */
    Object getRawData(String reqId, String category);

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
    RequestSummaryDto getRequestSummary(String reqId);
}
