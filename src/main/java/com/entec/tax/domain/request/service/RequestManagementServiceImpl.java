package com.entec.tax.domain.request.service;

import com.entec.tax.common.constants.LogLevel;
import com.entec.tax.common.constants.RequestStatus;
import com.entec.tax.common.constants.SystemConstants;
import com.entec.tax.common.constants.TaxType;
import com.entec.tax.common.dto.RequestContext;
import com.entec.tax.common.exception.ConcurrentConflictException;
import com.entec.tax.common.exception.RequestNotFoundException;
import com.entec.tax.common.exception.TaxServiceException;
import com.entec.tax.common.exception.ValidationException;
import com.entec.tax.common.exception.ErrorCode;
import com.entec.tax.common.util.CryptoUtil;
import com.entec.tax.common.util.DateUtil;
import com.entec.tax.common.util.JsonUtil;
import com.entec.tax.common.util.ValidationUtil;
import com.entec.tax.domain.input.entity.InpBasic;
import com.entec.tax.domain.input.entity.InpDeduction;
import com.entec.tax.domain.input.entity.InpEmployee;
import com.entec.tax.domain.input.entity.InpFinancial;
import com.entec.tax.domain.input.entity.InpRawData;
import com.entec.tax.domain.input.repository.InpBasicRepository;
import com.entec.tax.domain.input.repository.InpDeductionRepository;
import com.entec.tax.domain.input.repository.InpEmployeeRepository;
import com.entec.tax.domain.input.repository.InpFinancialRepository;
import com.entec.tax.domain.input.repository.InpRawDataRepository;
import com.entec.tax.domain.log.entity.LogCalculation;
import com.entec.tax.domain.log.repository.LogCalculationRepository;
import com.entec.tax.domain.output.entity.OutRefund;
import com.entec.tax.domain.output.repository.OutRefundRepository;
import com.entec.tax.domain.request.dto.DatasetDto;
import com.entec.tax.domain.request.dto.RequestCreateDto;
import com.entec.tax.domain.request.dto.RequestResponseDto;
import com.entec.tax.domain.request.dto.RequestStatusDto;
import com.entec.tax.domain.request.dto.RequestSummaryDto;
import com.entec.tax.domain.request.entity.ReqRequest;
import com.entec.tax.domain.request.repository.ReqRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * M1 입력 관리 서비스 구현체.
 *
 * <p>세액공제 환급 요청의 접수, 원시 데이터 저장, 요약 테이블 생성 등
 * 입력 단계(M1)의 핵심 비즈니스 로직을 수행한다.</p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li><b>M1-01</b>: 요청 접수 및 req_id 발급 (create_request)</li>
 *   <li><b>M1-02</b>: 원시 JSON 수신 및 보관 (store_raw_data)</li>
 *   <li><b>M1-03</b>: 요약 테이블 생성 (generate_summaries)</li>
 *   <li>상태 조회, 원시 데이터 조회, 경량 요약 조회</li>
 * </ul>
 *
 * <h3>트랜잭션 정책</h3>
 * <ul>
 *   <li>createRequest: PESSIMISTIC_WRITE 잠금 + 데드락 재시도(최대 3회, 100ms x 2^n 백오프)</li>
 *   <li>조회 메서드: readOnly 트랜잭션</li>
 * </ul>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 * @see RequestManagementService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RequestManagementServiceImpl implements RequestManagementService {

    // ──────────────────────────────────────────────────────────────────
    // 상수 정의
    // ──────────────────────────────────────────────────────────────────

    /** 데드락 재시도 최대 횟수 */
    private static final int MAX_DEADLOCK_RETRY = 3;

    /** 데드락 재시도 기본 대기 시간 (밀리초) */
    private static final long DEADLOCK_BASE_BACKOFF_MS = 100L;

    /** 요청 ID 날짜 포맷 (YYYYMMDD) */
    private static final DateTimeFormatter REQ_ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** CORP 세금 유형에서 필수 카테고리 */
    private static final String REQUIRED_CATEGORY_CORP = "corp_basic";

    /** INC 세금 유형에서 필수 카테고리 */
    private static final String REQUIRED_CATEGORY_INC = "inc_basic";

    /** 계산 단계명: M1-01 (요청 접수) */
    private static final String CALC_STEP_M1_01 = "M1-01";

    /** 계산 단계명: M1-02 (원시 데이터 저장) */
    private static final String CALC_STEP_M1_02 = "M1-02";

    /** 계산 단계명: M1-03 (요약 테이블 생성) */
    private static final String CALC_STEP_M1_03 = "M1-03";

    // ──────────────────────────────────────────────────────────────────
    // 의존성 주입 (생성자 주입 via @RequiredArgsConstructor)
    // ──────────────────────────────────────────────────────────────────

    private final ReqRequestRepository reqRequestRepository;
    private final InpRawDataRepository inpRawDataRepository;
    private final InpBasicRepository inpBasicRepository;
    private final InpEmployeeRepository inpEmployeeRepository;
    private final InpDeductionRepository inpDeductionRepository;
    private final InpFinancialRepository inpFinancialRepository;
    private final OutRefundRepository outRefundRepository;
    private final LogCalculationRepository logCalculationRepository;

    // ══════════════════════════════════════════════════════════════════
    // M1-01: 요청 접수 및 req_id 발급
    // ══════════════════════════════════════════════════════════════════

    /**
     * 요청 접수 및 req_id 발급 (M1-01) + 원시 JSON 수신 (M1-02) + 요약 생성 (M1-03).
     *
     * <p>전체 흐름:</p>
     * <ol>
     *   <li>입력 값 유효성 검증 (applicantType, applicantId, taxType, taxYear, datasets)</li>
     *   <li>Idempotency-Key 중복 확인: 동일 키가 존재하면 기존 req_id 반환</li>
     *   <li>seq_no 발급: SELECT MAX(seq_no) FROM REQ_REQUEST WHERE applicant_id=? AND request_date=CURRENT_DATE FOR UPDATE</li>
     *   <li>req_id 생성: {applicant_type}-{applicant_id(하이픈제거)}-{YYYYMMDD}-{seq_no:03d}</li>
     *   <li>REQ_REQUEST INSERT (status='RECEIVED', version=1)</li>
     *   <li>원시 데이터 저장 (M1-02): datasets 유효성 검증 후 INP_RAW_DATA INSERT</li>
     *   <li>요약 테이블 생성 (M1-03): INP_BASIC, INP_EMPLOYEE, INP_DEDUCTION, INP_FINANCIAL 생성</li>
     *   <li>데드락 발생 시: ROLLBACK → RETRY (최대 3회, 100ms × 2^n 백오프)</li>
     * </ol>
     *
     * @param requestCreateDto 요청 접수 DTO (applicantType, applicantId, taxType, taxYear, datasets)
     * @param idempotencyKey   멱등성 키 (nullable). null이 아닌 경우 (key, applicantId, taxYear) 조합으로 중복 검사
     * @return RequestResponseDto 요청 접수 결과 (reqId, status, datasetsReceived, createdAt)
     * @throws ValidationException          입력 값 검증 실패 시
     * @throws ConcurrentConflictException  데드락 재시도 초과 시
     * @throws TaxServiceException          기타 처리 오류 시
     */
    @Override
    public RequestResponseDto createRequest(RequestCreateDto requestCreateDto, String idempotencyKey) {
        long startTime = System.currentTimeMillis();

        // 1. 입력 값 유효성 검증
        validateCreateRequest(requestCreateDto);

        // 2. Idempotency-Key 중복 확인
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            RequestResponseDto existingResponse = checkIdempotency(
                    idempotencyKey, requestCreateDto.getApplicantId(), requestCreateDto.getTaxYear());
            if (existingResponse != null) {
                log.info("[M1-01] 멱등성 키 중복 감지. 기존 요청 반환. idempotencyKey={}, reqId={}",
                        idempotencyKey, existingResponse.getReqId());
                return existingResponse;
            }
        }

        // 3. 데드락 재시도 루프를 포함한 요청 생성
        PessimisticLockingFailureException lastException = null;
        for (int attempt = 0; attempt < MAX_DEADLOCK_RETRY; attempt++) {
            try {
                RequestResponseDto result = executeCreateRequest(requestCreateDto, idempotencyKey, startTime);
                return result;
            } catch (PessimisticLockingFailureException e) {
                lastException = e;
                long backoffMs = DEADLOCK_BASE_BACKOFF_MS * (1L << attempt);
                log.warn("[M1-01] 데드락 발생. 재시도 {}/{}. 대기시간={}ms, applicantId={}",
                        attempt + 1, MAX_DEADLOCK_RETRY, backoffMs, requestCreateDto.getApplicantId());
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new TaxServiceException(
                            ErrorCode.INTERNAL_ERROR,
                            "데드락 재시도 대기 중 인터럽트 발생",
                            null, ie);
                }
            }
        }

        // 재시도 소진
        log.error("[M1-01] 데드락 재시도 횟수 초과. applicantId={}", requestCreateDto.getApplicantId());
        throw new ConcurrentConflictException(
                "데드락 재시도 횟수 초과 (max=" + MAX_DEADLOCK_RETRY + ")",
                null, lastException);
    }

    /**
     * 실제 요청 생성 트랜잭션을 수행한다.
     *
     * <p>비관적 잠금(PESSIMISTIC_WRITE)을 사용하여 seq_no를 안전하게 발급하고,
     * REQ_REQUEST, INP_RAW_DATA, 요약 테이블(INP_BASIC 등)을 생성한다.</p>
     *
     * @param dto            요청 접수 DTO
     * @param idempotencyKey 멱등성 키 (nullable)
     * @param startTime      처리 시작 시각 (밀리초, 로깅용)
     * @return RequestResponseDto 요청 접수 결과
     */
    @Transactional
    protected RequestResponseDto executeCreateRequest(RequestCreateDto dto,
                                                      String idempotencyKey,
                                                      long startTime) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        String applicantId = dto.getApplicantId();

        // ── seq_no 발급 (비관적 잠금) ──
        Optional<Integer> maxSeqOpt = reqRequestRepository.findMaxSeqNoForUpdate(applicantId, today);
        int seqNo = maxSeqOpt.orElse(0) + 1;

        // ── req_id 생성: {applicant_type}-{applicant_id(하이픈제거)}-{YYYYMMDD}-{seq_no:03d} ──
        String sanitizedApplicantId = applicantId.replaceAll("-", "");
        String dateStr = today.format(REQ_ID_DATE_FORMAT);
        String reqId = String.format(SystemConstants.REQ_ID_FORMAT,
                dto.getApplicantType(), sanitizedApplicantId, dateStr, seqNo);

        log.info("[M1-01] 요청 ID 생성 완료. reqId={}, seqNo={}, applicantId={}",
                reqId, seqNo, applicantId);

        // ── REQ_REQUEST INSERT ──
        ReqRequest request = ReqRequest.builder()
                .reqId(reqId)
                .applicantType(dto.getApplicantType())
                .applicantId(applicantId)
                .applicantName(sanitizedApplicantId) // applicantName은 DTO에 없으므로 ID 기반 기본값
                .taxType(dto.getTaxType())
                .taxYear(dto.getTaxYear())
                .requestDate(today)
                .seqNo(seqNo)
                .requestStatus(RequestStatus.RECEIVED.getCode())
                .createdAt(now)
                .requestSource(resolveRequestSource())
                .requestedBy(resolveRequestedBy())
                .clientIp(resolveClientIp())
                .version(1)
                .build();

        reqRequestRepository.save(request);

        // ── 감사추적 로그 기록 (M1-01) ──
        saveLog(reqId, CALC_STEP_M1_01, "createRequest",
                "applicantId=" + applicantId + ", taxType=" + dto.getTaxType() + ", taxYear=" + dto.getTaxYear(),
                "reqId=" + reqId + ", seqNo=" + seqNo,
                LogLevel.INFO.getCode(), startTime);

        // ── M1-02: 원시 JSON 수신 및 보관 ──
        int datasetsReceived = storeRawData(reqId, request, dto.getDatasets(), dto.getTaxType());

        // ── M1-03: 요약 테이블 생성 ──
        generateSummaries(reqId, request);

        // ── 응답 DTO 구성 ──
        RequestResponseDto responseDto = new RequestResponseDto();
        responseDto.setReqId(reqId);
        responseDto.setStatus(RequestStatus.PARSED.getCode());
        responseDto.setDatasetsReceived(datasetsReceived);
        responseDto.setCreatedAt(now.toString());

        log.info("[M1-01] 요청 접수 완료. reqId={}, datasetsReceived={}, 소요시간={}ms",
                reqId, datasetsReceived, System.currentTimeMillis() - startTime);

        return responseDto;
    }

    // ══════════════════════════════════════════════════════════════════
    // M1-02: 원시 JSON 수신 및 보관
    // ══════════════════════════════════════════════════════════════════

    /**
     * 원시 JSON 데이터를 검증하고 INP_RAW_DATA 테이블에 저장한다 (M1-02).
     *
     * <p>검증 규칙:</p>
     * <ul>
     *   <li>datasets.length &le; 40 (SystemConstants.MAX_CATEGORIES_PER_REQUEST)</li>
     *   <li>각 JSON &le; 10MB (SystemConstants.MAX_JSON_SIZE_PER_CATEGORY)</li>
     *   <li>총합 &le; 50MB (SystemConstants.MAX_PAYLOAD_SIZE)</li>
     *   <li>CORP 세금 유형 → corp_basic 카테고리 필수</li>
     *   <li>INC 세금 유형 → inc_basic 카테고리 필수</li>
     * </ul>
     *
     * <p>각 데이터셋에 대해:</p>
     * <ul>
     *   <li>checksum = SHA256(rawJson)</li>
     *   <li>byte_size = UTF-8 바이트 크기</li>
     *   <li>record_count = JSON 배열이면 배열 크기, 단건이면 1</li>
     * </ul>
     *
     * @param reqId     요청 ID
     * @param request   ReqRequest 엔티티 (FK 참조용)
     * @param datasets  데이터셋 목록
     * @param taxType   세금 유형 코드 (CORP/INC)
     * @return 저장된 데이터셋 수
     * @throws ValidationException 데이터 검증 실패 시
     */
    private int storeRawData(String reqId, ReqRequest request,
                             List<DatasetDto> datasets, String taxType) {
        long startTime = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        // ── 데이터셋 크기 검증 ──
        if (datasets == null || datasets.isEmpty()) {
            throw new ValidationException(
                    "데이터셋은 1건 이상 필수입니다.",
                    reqId,
                    createFieldErrors("datasets", "1건 이상 필수", "1~40", "0"));
        }

        if (datasets.size() > SystemConstants.MAX_CATEGORIES_PER_REQUEST) {
            throw new ValidationException(
                    "데이터셋 수가 최대 허용 수를 초과합니다. (max=" + SystemConstants.MAX_CATEGORIES_PER_REQUEST + ")",
                    reqId,
                    createFieldErrors("datasets.length", "최대 수 초과",
                            String.valueOf(SystemConstants.MAX_CATEGORIES_PER_REQUEST),
                            String.valueOf(datasets.size())));
        }

        // ── 필수 카테고리 검증 ──
        validateRequiredCategories(reqId, datasets, taxType);

        // ── 각 데이터셋 저장 ──
        long totalByteSize = 0L;
        int savedCount = 0;

        for (DatasetDto dataset : datasets) {
            // 데이터를 JSON 문자열로 변환
            String rawJson;
            if (dataset.getData() instanceof String) {
                rawJson = (String) dataset.getData();
            } else {
                rawJson = JsonUtil.toJson(dataset.getData());
            }

            // 개별 JSON 크기 검증 (10MB)
            long byteSize = rawJson.getBytes(StandardCharsets.UTF_8).length;
            if (byteSize > SystemConstants.MAX_JSON_SIZE_PER_CATEGORY) {
                throw new ValidationException(
                        "카테고리별 JSON 크기가 최대 허용 크기를 초과합니다. category=" + dataset.getCategory(),
                        reqId,
                        createFieldErrors("dataset.data", "카테고리별 크기 초과",
                                SystemConstants.MAX_JSON_SIZE_PER_CATEGORY + " bytes",
                                byteSize + " bytes"));
            }

            totalByteSize += byteSize;

            // 총합 크기 검증 (50MB)
            if (totalByteSize > SystemConstants.MAX_PAYLOAD_SIZE) {
                throw new ValidationException(
                        "전체 페이로드 크기가 최대 허용 크기를 초과합니다.",
                        reqId,
                        createFieldErrors("datasets.totalSize", "전체 크기 초과",
                                SystemConstants.MAX_PAYLOAD_SIZE + " bytes",
                                totalByteSize + " bytes"));
            }

            // 체크섬, 레코드 수 계산
            String checksum = CryptoUtil.generateChecksum(rawJson);
            int recordCount = calculateRecordCount(dataset.getData());

            // INP_RAW_DATA INSERT
            InpRawData rawData = InpRawData.builder()
                    .reqRequest(request)
                    .category(dataset.getCategory())
                    .subCategory(dataset.getSubCategory())
                    .rawJson(rawJson)
                    .recordCount(recordCount)
                    .byteSize(byteSize)
                    .checksum(checksum)
                    .receivedAt(now)
                    .build();

            inpRawDataRepository.save(rawData);
            savedCount++;

            log.debug("[M1-02] 원시 데이터 저장 완료. reqId={}, category={}, byteSize={}, recordCount={}, checksum={}",
                    reqId, dataset.getCategory(), byteSize, recordCount, checksum);
        }

        // ── 상태 갱신: RECEIVED → PARSING ──
        reqRequestRepository.updateStatus(reqId, RequestStatus.PARSING.getCode(), LocalDateTime.now());

        // ── 감사추적 로그 기록 (M1-02) ──
        saveLog(reqId, CALC_STEP_M1_02, "storeRawData",
                "datasetsCount=" + datasets.size() + ", totalByteSize=" + totalByteSize,
                "savedCount=" + savedCount,
                LogLevel.INFO.getCode(), startTime);

        log.info("[M1-02] 원시 데이터 저장 완료. reqId={}, savedCount={}, totalByteSize={}",
                reqId, savedCount, totalByteSize);

        return savedCount;
    }

    // ══════════════════════════════════════════════════════════════════
    // M1-03: 요약 테이블 생성
    // ══════════════════════════════════════════════════════════════════

    /**
     * INP_RAW_DATA에서 데이터를 추출하여 요약 테이블(INP_BASIC, INP_EMPLOYEE, INP_DEDUCTION, INP_FINANCIAL)을 생성한다 (M1-03).
     *
     * <p>처리 흐름:</p>
     * <ol>
     *   <li>INP_RAW_DATA에서 해당 reqId의 원시 데이터 전체를 조회한다.</li>
     *   <li>카테고리별로 분류하여 각 요약 테이블에 데이터를 삽입한다.</li>
     *   <li>재처리(version &gt; 1) 시: 기존 요약 데이터를 삭제하고, prev_data_hash를 감사추적 로그에 기록한다.</li>
     *   <li>상태를 PARSING → PARSED로 갱신한다.</li>
     * </ol>
     *
     * @param reqId   요청 ID
     * @param request ReqRequest 엔티티 (version 확인용)
     */
    private void generateSummaries(String reqId, ReqRequest request) {
        long startTime = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        // ── 재처리 여부 확인 (version > 1) ──
        boolean isReprocessing = request.getVersion() != null && request.getVersion() > 1;
        String prevDataHash = null;

        if (isReprocessing) {
            log.info("[M1-03] 재처리 감지. 기존 요약 데이터 삭제 시작. reqId={}, version={}",
                    reqId, request.getVersion());

            // 기존 요약 데이터의 해시 기록
            prevDataHash = calculatePreviousSummaryHash(reqId);

            // 기존 요약 데이터 삭제
            deleteExistingSummaries(reqId);

            log.info("[M1-03] 기존 요약 데이터 삭제 완료. reqId={}, prevDataHash={}", reqId, prevDataHash);
        }

        // ── INP_RAW_DATA에서 원시 데이터 조회 ──
        List<InpRawData> rawDataList = inpRawDataRepository.findByReqRequestReqId(reqId);

        if (rawDataList.isEmpty()) {
            log.warn("[M1-03] 원시 데이터가 없습니다. reqId={}", reqId);
            reqRequestRepository.updateStatus(reqId, RequestStatus.PARSED.getCode(), now);
            return;
        }

        // ── 카테고리별 요약 데이터 추출 및 저장 ──
        for (InpRawData rawData : rawDataList) {
            String category = rawData.getCategory();
            String rawJson = rawData.getRawJson();

            try {
                switch (category.toLowerCase()) {
                    case "basic":
                    case "corp_basic":
                    case "inc_basic":
                        extractAndSaveBasic(reqId, rawJson, request);
                        break;
                    case "employee":
                        extractAndSaveEmployee(reqId, rawJson);
                        break;
                    case "deduction":
                        extractAndSaveDeduction(reqId, rawJson, request.getTaxYear());
                        break;
                    case "financial":
                        extractAndSaveFinancial(reqId, rawJson);
                        break;
                    default:
                        log.debug("[M1-03] 요약 미대상 카테고리. reqId={}, category={}", reqId, category);
                        break;
                }
            } catch (Exception e) {
                log.error("[M1-03] 요약 데이터 추출 실패. reqId={}, category={}, error={}",
                        reqId, category, e.getMessage(), e);
                // 개별 카테고리 실패 시에도 나머지 카테고리 처리 계속
            }
        }

        // ── 상태 갱신: PARSING → PARSED ──
        reqRequestRepository.updateStatus(reqId, RequestStatus.PARSED.getCode(), now);

        // ── 감사추적 로그 기록 (M1-03) ──
        LogCalculation logEntry = LogCalculation.builder()
                .reqId(reqId)
                .calcStep(CALC_STEP_M1_03)
                .functionName("generateSummaries")
                .inputData("rawDataCount=" + rawDataList.size() + ", isReprocessing=" + isReprocessing)
                .outputData("status=PARSED")
                .logLevel(LogLevel.INFO.getCode())
                .executedAt(now)
                .executedBy(resolveRequestedBy())
                .durationMs((int) (System.currentTimeMillis() - startTime))
                .traceId(resolveTraceId())
                .prevDataHash(prevDataHash)
                .build();
        logCalculationRepository.save(logEntry);

        log.info("[M1-03] 요약 테이블 생성 완료. reqId={}, rawDataCount={}, 소요시간={}ms",
                reqId, rawDataList.size(), System.currentTimeMillis() - startTime);
    }

    // ══════════════════════════════════════════════════════════════════
    // 상태 조회 (API-03)
    // ══════════════════════════════════════════════════════════════════

    /**
     * 요청 상태를 조회한다.
     *
     * <p>REQ_REQUEST 테이블에서 요청 ID에 해당하는 요청을 조회하고,
     * 현재 상태와 진행 단계 설명을 반환한다.</p>
     *
     * @param reqId 요청 ID
     * @return RequestStatusDto 요청 상태 정보 (reqId, status, progress)
     * @throws RequestNotFoundException 해당 요청 ID가 존재하지 않을 때
     */
    @Override
    @Transactional(readOnly = true)
    public RequestStatusDto getRequestStatus(String reqId) {
        ReqRequest request = findRequestOrThrow(reqId);

        RequestStatusDto statusDto = new RequestStatusDto();
        statusDto.setReqId(reqId);
        statusDto.setStatus(request.getRequestStatus());
        statusDto.setProgress(resolveProgressDescription(request.getRequestStatus()));

        log.debug("[API-03] 요청 상태 조회. reqId={}, status={}", reqId, request.getRequestStatus());
        return statusDto;
    }

    // ══════════════════════════════════════════════════════════════════
    // 원시 데이터 조회 (API-06)
    // ══════════════════════════════════════════════════════════════════

    /**
     * 원시 입력 JSON 데이터를 조회한다.
     *
     * <p>category가 null이면 해당 요청의 전체 카테고리 데이터를 Map 형태로 반환하고,
     * category가 지정되면 해당 카테고리의 데이터만 List 형태로 반환한다.</p>
     *
     * @param reqId    요청 ID
     * @param category 데이터 카테고리 (nullable, 예: "BASIC", "EMPLOYEE", "DEDUCTION")
     * @return 원시 입력 JSON 데이터. category null → Map&lt;String, List&lt;Object&gt;&gt;, category 지정 → List&lt;Object&gt;
     * @throws RequestNotFoundException 해당 요청 ID가 존재하지 않을 때
     */
    @Override
    @Transactional(readOnly = true)
    public Object getRawData(String reqId, String category) {
        // 요청 존재 여부 확인
        findRequestOrThrow(reqId);

        if (category != null && !category.trim().isEmpty()) {
            // 특정 카테고리 조회
            List<InpRawData> rawDataList = inpRawDataRepository.findByReqRequestReqIdAndCategory(reqId, category);
            List<Object> result = new ArrayList<Object>();
            for (InpRawData rawData : rawDataList) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("rawId", rawData.getRawId());
                item.put("category", rawData.getCategory());
                item.put("subCategory", rawData.getSubCategory());
                item.put("rawJson", rawData.getRawJson());
                item.put("recordCount", rawData.getRecordCount());
                item.put("byteSize", rawData.getByteSize());
                item.put("checksum", rawData.getChecksum());
                item.put("receivedAt", rawData.getReceivedAt() != null ? rawData.getReceivedAt().toString() : null);
                result.add(item);
            }

            log.debug("[API-06] 원시 데이터 조회. reqId={}, category={}, count={}", reqId, category, result.size());
            return result;
        } else {
            // 전체 카테고리 조회
            List<InpRawData> rawDataList = inpRawDataRepository.findByReqRequestReqId(reqId);
            Map<String, List<Object>> resultMap = new HashMap<String, List<Object>>();

            for (InpRawData rawData : rawDataList) {
                String cat = rawData.getCategory();
                if (!resultMap.containsKey(cat)) {
                    resultMap.put(cat, new ArrayList<Object>());
                }
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("rawId", rawData.getRawId());
                item.put("category", cat);
                item.put("subCategory", rawData.getSubCategory());
                item.put("rawJson", rawData.getRawJson());
                item.put("recordCount", rawData.getRecordCount());
                item.put("byteSize", rawData.getByteSize());
                item.put("checksum", rawData.getChecksum());
                item.put("receivedAt", rawData.getReceivedAt() != null ? rawData.getReceivedAt().toString() : null);
                resultMap.get(cat).add(item);
            }

            log.debug("[API-06] 원시 데이터 전체 조회. reqId={}, categoryCount={}", reqId, resultMap.size());
            return resultMap;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // 경량 요약 조회 (API-08)
    // ══════════════════════════════════════════════════════════════════

    /**
     * 경량 요약 정보를 조회한다.
     *
     * <p>OUT_REFUND 테이블에서 환급 산출 결과가 존재하면 요약 정보를 반환하고,
     * 아직 계산이 완료되지 않았으면 현재 상태 정보만 반환한다.</p>
     *
     * @param reqId 요청 ID
     * @return RequestSummaryDto 경량 요약 정보
     *         (reqId, status, totalExpected, refundAmount, refundInterestAmount, localTaxRefund, optimalComboId, analysisCompletedAt)
     * @throws RequestNotFoundException 해당 요청 ID가 존재하지 않을 때
     */
    @Override
    @Transactional(readOnly = true)
    public RequestSummaryDto getRequestSummary(String reqId) {
        ReqRequest request = findRequestOrThrow(reqId);

        RequestSummaryDto summaryDto = new RequestSummaryDto();
        summaryDto.setReqId(reqId);
        summaryDto.setStatus(request.getRequestStatus());

        // OutRefund가 존재하면 환급 요약 정보 포함
        Optional<OutRefund> refundOpt = outRefundRepository.findByReqId(reqId);
        if (refundOpt.isPresent()) {
            OutRefund refund = refundOpt.get();
            summaryDto.setTotalExpected(refund.getTotalExpected());
            summaryDto.setRefundAmount(refund.getRefundAmount());
            summaryDto.setRefundInterestAmount(refund.getRefundInterestAmount());
            summaryDto.setLocalTaxRefund(refund.getLocalTaxRefund());

            // optimalComboId: OutRefund의 optimalComboId는 String이므로 Integer 변환 시도
            if (refund.getOptimalComboId() != null && !refund.getOptimalComboId().isEmpty()) {
                try {
                    summaryDto.setOptimalComboId(Integer.parseInt(refund.getOptimalComboId()));
                } catch (NumberFormatException e) {
                    log.debug("[API-08] optimalComboId를 Integer로 변환 불가. reqId={}, value={}",
                            reqId, refund.getOptimalComboId());
                }
            }

            // 분석 완료 일시
            if (request.getCompletedAt() != null) {
                summaryDto.setAnalysisCompletedAt(request.getCompletedAt().toString());
            }
        }

        log.debug("[API-08] 경량 요약 조회. reqId={}, status={}, hasRefund={}",
                reqId, request.getRequestStatus(), refundOpt.isPresent());
        return summaryDto;
    }

    // ══════════════════════════════════════════════════════════════════
    // Private 헬퍼 메서드
    // ══════════════════════════════════════════════════════════════════

    /**
     * 요청 접수 DTO의 필수 값 유효성을 검증한다.
     *
     * <p>검증 항목:</p>
     * <ul>
     *   <li>applicantType: C 또는 P</li>
     *   <li>applicantId: 비어있지 않은 문자열</li>
     *   <li>taxType: CORP 또는 INC</li>
     *   <li>taxYear: 4자리 연도 문자열</li>
     *   <li>datasets: 1건 이상</li>
     * </ul>
     *
     * @param dto 요청 접수 DTO
     * @throws ValidationException 유효성 검증 실패 시
     */
    private void validateCreateRequest(RequestCreateDto dto) {
        List<ValidationException.FieldError> errors = new ArrayList<ValidationException.FieldError>();

        // applicantType 검증 (C 또는 P)
        if (!ValidationUtil.isNotEmpty(dto.getApplicantType())) {
            errors.add(new ValidationException.FieldError(
                    "applicantType", "필수 항목 누락", "C 또는 P", "null/empty"));
        } else if (!"C".equals(dto.getApplicantType()) && !"P".equals(dto.getApplicantType())) {
            errors.add(new ValidationException.FieldError(
                    "applicantType", "유효하지 않은 값", "C 또는 P", dto.getApplicantType()));
        }

        // applicantId 검증
        if (!ValidationUtil.isNotEmpty(dto.getApplicantId())) {
            errors.add(new ValidationException.FieldError(
                    "applicantId", "필수 항목 누락", "사업자등록번호/주민등록번호", "null/empty"));
        }

        // taxType 검증 (CORP 또는 INC)
        if (!ValidationUtil.isNotEmpty(dto.getTaxType())) {
            errors.add(new ValidationException.FieldError(
                    "taxType", "필수 항목 누락", "CORP 또는 INC", "null/empty"));
        } else {
            try {
                TaxType.fromCode(dto.getTaxType());
            } catch (IllegalArgumentException e) {
                errors.add(new ValidationException.FieldError(
                        "taxType", "유효하지 않은 세금 유형", "CORP 또는 INC", dto.getTaxType()));
            }
        }

        // taxYear 검증 (4자리 연도)
        if (!ValidationUtil.isValidTaxYear(dto.getTaxYear())) {
            errors.add(new ValidationException.FieldError(
                    "taxYear", "유효하지 않은 과세연도", "YYYY (4자리 숫자)", dto.getTaxYear()));
        }

        // datasets 검증
        if (dto.getDatasets() == null || dto.getDatasets().isEmpty()) {
            errors.add(new ValidationException.FieldError(
                    "datasets", "데이터셋 누락", "1건 이상", "0"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(
                    "요청 접수 입력 값 검증에 실패했습니다.",
                    null, errors);
        }
    }

    /**
     * 세금 유형에 따른 필수 카테고리 존재 여부를 검증한다.
     *
     * <p>CORP → corp_basic 카테고리 필수, INC → inc_basic 카테고리 필수</p>
     *
     * @param reqId    요청 ID (오류 메시지용)
     * @param datasets 데이터셋 목록
     * @param taxType  세금 유형 코드
     * @throws ValidationException 필수 카테고리가 누락된 경우
     */
    private void validateRequiredCategories(String reqId, List<DatasetDto> datasets, String taxType) {
        String requiredCategory = null;

        if (TaxType.CORP.getCode().equals(taxType)) {
            requiredCategory = REQUIRED_CATEGORY_CORP;
        } else if (TaxType.INC.getCode().equals(taxType)) {
            requiredCategory = REQUIRED_CATEGORY_INC;
        }

        if (requiredCategory != null) {
            boolean found = false;
            for (DatasetDto dataset : datasets) {
                if (requiredCategory.equals(dataset.getCategory())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ValidationException(
                        "세금 유형 " + taxType + "에 필수 카테고리 '" + requiredCategory + "'가 누락되었습니다.",
                        reqId,
                        createFieldErrors("datasets.category", "필수 카테고리 누락",
                                requiredCategory, "미포함"));
            }
        }
    }

    /**
     * Idempotency-Key 중복을 확인한다.
     *
     * <p>(idempotencyKey, applicantId, taxYear) 조합으로 기존 요청을 조회하고,
     * 존재하면 기존 요청의 응답 DTO를 반환한다.</p>
     *
     * @param idempotencyKey 멱등성 키
     * @param applicantId    신청자 식별번호
     * @param taxYear        귀속 연도
     * @return 기존 요청 응답 DTO (중복이면 반환, 중복 아니면 null)
     */
    private RequestResponseDto checkIdempotency(String idempotencyKey, String applicantId, String taxYear) {
        // requestSource에 idempotencyKey를 기록하는 방식으로 검색
        // (idempotencyKey, applicantId, taxYear) 조합으로 기존 요청 조회
        List<ReqRequest> existingRequests = reqRequestRepository.findByApplicantIdOrderByCreatedAtDesc(applicantId);

        for (ReqRequest existing : existingRequests) {
            // requestSource에 idempotencyKey가 기록되어 있고, taxYear가 일치하면 중복으로 판단
            if (idempotencyKey.equals(existing.getRequestSource())
                    && taxYear.equals(existing.getTaxYear())) {

                RequestResponseDto responseDto = new RequestResponseDto();
                responseDto.setReqId(existing.getReqId());
                responseDto.setStatus(existing.getRequestStatus());
                responseDto.setCreatedAt(existing.getCreatedAt() != null
                        ? existing.getCreatedAt().toString() : null);

                // 기존 저장된 원시 데이터 수
                List<InpRawData> rawDataList = inpRawDataRepository.findByReqRequestReqId(existing.getReqId());
                responseDto.setDatasetsReceived(rawDataList.size());

                return responseDto;
            }
        }

        return null;
    }

    /**
     * 요청 ID로 ReqRequest를 조회하고, 존재하지 않으면 RequestNotFoundException을 발생시킨다.
     *
     * @param reqId 요청 ID
     * @return ReqRequest 엔티티
     * @throws RequestNotFoundException 해당 요청이 존재하지 않을 때
     */
    private ReqRequest findRequestOrThrow(String reqId) {
        return reqRequestRepository.findById(reqId)
                .orElseThrow(new java.util.function.Supplier<RequestNotFoundException>() {
                    @Override
                    public RequestNotFoundException get() {
                        return new RequestNotFoundException(
                                "요청을 찾을 수 없습니다. reqId=" + reqId, reqId);
                    }
                });
    }

    /**
     * 데이터의 레코드 수를 계산한다.
     *
     * <p>List(배열)이면 배열 크기를 반환하고, 단건 객체이면 1을 반환한다.</p>
     *
     * @param data 데이터 객체 (List 또는 Map 등)
     * @return 레코드 수
     */
    private int calculateRecordCount(Object data) {
        if (data == null) {
            return 0;
        }
        if (data instanceof List) {
            return ((List<?>) data).size();
        }
        return 1;
    }

    /**
     * 이전 요약 데이터의 해시를 계산한다 (재처리 시 감사추적 기록용).
     *
     * <p>INP_BASIC, INP_EMPLOYEE, INP_DEDUCTION, INP_FINANCIAL 데이터를
     * 연결하여 SHA-256 해시를 생성한다.</p>
     *
     * @param reqId 요청 ID
     * @return 이전 요약 데이터의 SHA-256 해시. 데이터가 없으면 null
     */
    private String calculatePreviousSummaryHash(String reqId) {
        StringBuilder sb = new StringBuilder();

        Optional<InpBasic> basicOpt = inpBasicRepository.findByReqId(reqId);
        if (basicOpt.isPresent()) {
            sb.append(JsonUtil.toJson(basicOpt.get()));
        }

        List<InpEmployee> employees = inpEmployeeRepository.findByReqId(reqId);
        if (!employees.isEmpty()) {
            sb.append(JsonUtil.toJson(employees));
        }

        List<InpDeduction> deductions = inpDeductionRepository.findByReqId(reqId);
        if (!deductions.isEmpty()) {
            sb.append(JsonUtil.toJson(deductions));
        }

        Optional<InpFinancial> financialOpt = inpFinancialRepository.findByReqId(reqId);
        if (financialOpt.isPresent()) {
            sb.append(JsonUtil.toJson(financialOpt.get()));
        }

        if (sb.length() == 0) {
            return null;
        }

        return CryptoUtil.sha256(sb.toString());
    }

    /**
     * 기존 요약 테이블 데이터를 삭제한다 (재처리 시 사용).
     *
     * <p>INP_BASIC, INP_EMPLOYEE, INP_DEDUCTION, INP_FINANCIAL 테이블에서
     * 해당 reqId의 데이터를 모두 삭제한다.</p>
     *
     * @param reqId 요청 ID
     */
    private void deleteExistingSummaries(String reqId) {
        // INP_BASIC 삭제
        Optional<InpBasic> basicOpt = inpBasicRepository.findByReqId(reqId);
        if (basicOpt.isPresent()) {
            inpBasicRepository.delete(basicOpt.get());
        }

        // INP_EMPLOYEE 삭제
        List<InpEmployee> employees = inpEmployeeRepository.findByReqId(reqId);
        if (!employees.isEmpty()) {
            inpEmployeeRepository.deleteAll(employees);
        }

        // INP_DEDUCTION 삭제
        List<InpDeduction> deductions = inpDeductionRepository.findByReqId(reqId);
        if (!deductions.isEmpty()) {
            inpDeductionRepository.deleteAll(deductions);
        }

        // INP_FINANCIAL 삭제
        Optional<InpFinancial> financialOpt = inpFinancialRepository.findByReqId(reqId);
        if (financialOpt.isPresent()) {
            inpFinancialRepository.delete(financialOpt.get());
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // 요약 추출 메서드 (M1-03 내부)
    // ──────────────────────────────────────────────────────────────────

    /**
     * 원시 JSON에서 기본 정보를 추출하여 INP_BASIC에 저장한다.
     *
     * <p>이미 동일 reqId의 INP_BASIC이 존재하면 건너뛴다 (중복 카테고리 방어).</p>
     *
     * @param reqId   요청 ID
     * @param rawJson 원시 JSON 문자열
     * @param request ReqRequest 엔티티 (메타정보 참조용)
     */
    @SuppressWarnings("unchecked")
    private void extractAndSaveBasic(String reqId, String rawJson, ReqRequest request) {
        // 이미 존재하면 건너뜀 (중복 카테고리 방어)
        if (inpBasicRepository.findByReqId(reqId).isPresent()) {
            log.debug("[M1-03] INP_BASIC 이미 존재. 건너뜀. reqId={}", reqId);
            return;
        }

        Map<String, Object> data = JsonUtil.fromJson(rawJson, Map.class);

        InpBasic basic = InpBasic.builder()
                .reqId(reqId)
                .requestDate(request.getRequestDate())
                .taxType(request.getTaxType())
                .applicantName(getStringValue(data, "applicant_name"))
                .bizRegNo(getStringValue(data, "biz_reg_no"))
                .corpSize(getStringValue(data, "corp_size"))
                .industryCode(getStringValue(data, "industry_code"))
                .hqLocation(getStringValue(data, "hq_location"))
                .capitalZone(getStringValue(data, "capital_zone"))
                .depopulationArea(getBooleanValue(data, "depopulation_area"))
                .taxYear(request.getTaxYear())
                .fiscalStart(getLocalDateValue(data, "fiscal_start"))
                .fiscalEnd(getLocalDateValue(data, "fiscal_end"))
                .revenue(getLongValue(data, "revenue"))
                .taxableIncome(getLongValue(data, "taxable_income"))
                .computedTax(getLongValue(data, "computed_tax"))
                .paidTax(getLongValue(data, "paid_tax"))
                .foundingDate(getLocalDateValue(data, "founding_date"))
                .ventureYn(getBooleanValue(data, "venture_yn"))
                .rdDeptYn(getBooleanValue(data, "rd_dept_yn"))
                .claimReason(getStringValue(data, "claim_reason"))
                .sincerityTarget(getBooleanValue(data, "sincerity_target"))
                .bookkeepingType(getStringValue(data, "bookkeeping_type"))
                .consolidatedTax(getBooleanValue(data, "consolidated_tax"))
                .summaryGeneratedAt(LocalDateTime.now())
                .build();

        inpBasicRepository.save(basic);
        log.debug("[M1-03] INP_BASIC 저장 완료. reqId={}", reqId);
    }

    /**
     * 원시 JSON에서 고용 정보를 추출하여 INP_EMPLOYEE에 저장한다.
     *
     * <p>JSON이 배열(List)이면 각 요소를 연도 구분별로 저장하고,
     * 단건(Map)이면 yearType을 "CURRENT"로 저장한다.</p>
     *
     * @param reqId   요청 ID
     * @param rawJson 원시 JSON 문자열
     */
    @SuppressWarnings("unchecked")
    private void extractAndSaveEmployee(String reqId, String rawJson) {
        Object parsed;
        try {
            parsed = JsonUtil.fromJson(rawJson, List.class);
        } catch (Exception e) {
            // 단건 Map인 경우
            parsed = JsonUtil.fromJson(rawJson, Map.class);
        }

        if (parsed instanceof List) {
            List<Map<String, Object>> employeeList = (List<Map<String, Object>>) parsed;
            for (Map<String, Object> empData : employeeList) {
                saveEmployeeRecord(reqId, empData);
            }
        } else if (parsed instanceof Map) {
            Map<String, Object> empData = (Map<String, Object>) parsed;
            saveEmployeeRecord(reqId, empData);
        }

        log.debug("[M1-03] INP_EMPLOYEE 저장 완료. reqId={}", reqId);
    }

    /**
     * 단일 고용 정보 레코드를 INP_EMPLOYEE에 저장한다.
     *
     * @param reqId   요청 ID
     * @param empData 고용 정보 데이터 Map
     */
    private void saveEmployeeRecord(String reqId, Map<String, Object> empData) {
        String yearType = getStringValue(empData, "year_type");
        if (yearType == null || yearType.isEmpty()) {
            yearType = "CURRENT";
        }

        // 이미 존재하면 건너뜀
        if (inpEmployeeRepository.findByReqIdAndYearType(reqId, yearType).isPresent()) {
            log.debug("[M1-03] INP_EMPLOYEE 이미 존재. 건너뜀. reqId={}, yearType={}", reqId, yearType);
            return;
        }

        InpEmployee employee = InpEmployee.builder()
                .reqId(reqId)
                .yearType(yearType)
                .totalRegular(getBigDecimalValue(empData, "total_regular"))
                .youthCount(getIntegerValue(empData, "youth_count"))
                .disabledCount(getIntegerValue(empData, "disabled_count"))
                .agedCount(getIntegerValue(empData, "aged_count"))
                .careerBreakCount(getIntegerValue(empData, "career_break_count"))
                .northDefectorCount(getIntegerValue(empData, "north_defector_count"))
                .generalCount(getIntegerValue(empData, "general_count"))
                .excludedCount(getIntegerValue(empData, "excluded_count"))
                .totalSalary(getLongValue(empData, "total_salary"))
                .socialInsurancePaid(getLongValue(empData, "social_insurance_paid"))
                .build();

        inpEmployeeRepository.save(employee);
    }

    /**
     * 원시 JSON에서 공제/감면 정보를 추출하여 INP_DEDUCTION에 저장한다.
     *
     * <p>JSON 배열의 각 요소를 공제 항목으로 저장한다.
     * item_seq가 없으면 순번을 자동 부여한다.</p>
     *
     * @param reqId   요청 ID
     * @param rawJson 원시 JSON 문자열
     * @param taxYear 귀속 연도
     */
    @SuppressWarnings("unchecked")
    private void extractAndSaveDeduction(String reqId, String rawJson, String taxYear) {
        Object parsed;
        try {
            parsed = JsonUtil.fromJson(rawJson, List.class);
        } catch (Exception e) {
            parsed = JsonUtil.fromJson(rawJson, Map.class);
        }

        List<Map<String, Object>> deductionList;
        if (parsed instanceof List) {
            deductionList = (List<Map<String, Object>>) parsed;
        } else {
            deductionList = new ArrayList<Map<String, Object>>();
            deductionList.add((Map<String, Object>) parsed);
        }

        int autoSeq = 1;
        for (Map<String, Object> dedData : deductionList) {
            String itemCategory = getStringValue(dedData, "item_category");
            String provision = getStringValue(dedData, "provision");
            String dedTaxYear = getStringValue(dedData, "tax_year");
            if (dedTaxYear == null || dedTaxYear.isEmpty()) {
                dedTaxYear = taxYear;
            }
            Integer itemSeq = getIntegerValue(dedData, "item_seq");
            if (itemSeq == null) {
                itemSeq = autoSeq++;
            } else {
                autoSeq = itemSeq + 1;
            }

            InpDeduction deduction = InpDeduction.builder()
                    .reqId(reqId)
                    .itemCategory(itemCategory != null ? itemCategory : "UNKNOWN")
                    .provision(provision != null ? provision : "N/A")
                    .taxYear(dedTaxYear)
                    .itemSeq(itemSeq)
                    .baseAmount(getLongValue(dedData, "base_amount"))
                    .zoneType(getStringValue(dedData, "zone_type"))
                    .assetType(getStringValue(dedData, "asset_type"))
                    .rdType(getStringValue(dedData, "rd_type"))
                    .method(getStringValue(dedData, "method"))
                    .subDetail(getStringValue(dedData, "sub_detail"))
                    .existingApplied(getBooleanValue(dedData, "existing_applied"))
                    .existingAmount(getLongValue(dedData, "existing_amount"))
                    .carryforwardBalance(getLongValue(dedData, "carryforward_balance"))
                    .build();

            inpDeductionRepository.save(deduction);
        }

        log.debug("[M1-03] INP_DEDUCTION 저장 완료. reqId={}, count={}", reqId, deductionList.size());
    }

    /**
     * 원시 JSON에서 재무/세무 정보를 추출하여 INP_FINANCIAL에 저장한다.
     *
     * <p>이미 동일 reqId의 INP_FINANCIAL이 존재하면 건너뛴다.</p>
     *
     * @param reqId   요청 ID
     * @param rawJson 원시 JSON 문자열
     */
    @SuppressWarnings("unchecked")
    private void extractAndSaveFinancial(String reqId, String rawJson) {
        // 이미 존재하면 건너뜀
        if (inpFinancialRepository.findByReqId(reqId).isPresent()) {
            log.debug("[M1-03] INP_FINANCIAL 이미 존재. 건너뜀. reqId={}", reqId);
            return;
        }

        Map<String, Object> data = JsonUtil.fromJson(rawJson, Map.class);

        InpFinancial financial = InpFinancial.builder()
                .reqId(reqId)
                .bizIncome(getLongValue(data, "biz_income"))
                .nonTaxableIncome(getLongValue(data, "non_taxable_income"))
                .lossCarryforwardTotal(getLongValue(data, "loss_carryforward_total"))
                .lossCarryforwardDetail(getStringValue(data, "loss_carryforward_detail"))
                .interimPrepaidTax(getLongValue(data, "interim_prepaid_tax"))
                .withholdingTax(getLongValue(data, "withholding_tax"))
                .determinedTax(getLongValue(data, "determined_tax"))
                .dividendIncomeTotal(getLongValue(data, "dividend_income_total"))
                .dividendExclusionDetail(getStringValue(data, "dividend_exclusion_detail"))
                .foreignTaxTotal(getLongValue(data, "foreign_tax_total"))
                .foreignIncomeTotal(getLongValue(data, "foreign_income_total"))
                .taxAdjustmentDetail(getStringValue(data, "tax_adjustment_detail"))
                .incDeductionTotal(getLongValue(data, "inc_deduction_total"))
                .incDeductionDetail(getStringValue(data, "inc_deduction_detail"))
                .incComprehensiveIncome(getLongValue(data, "inc_comprehensive_income"))
                .currentYearLoss(getLongValue(data, "current_year_loss"))
                .priorYearTaxPaid(getLongValue(data, "prior_year_tax_paid"))
                .amendmentHistory(getStringValue(data, "amendment_history"))
                .vehicleExpenseDetail(getStringValue(data, "vehicle_expense_detail"))
                .build();

        inpFinancialRepository.save(financial);
        log.debug("[M1-03] INP_FINANCIAL 저장 완료. reqId={}", reqId);
    }

    // ──────────────────────────────────────────────────────────────────
    // 감사추적 로그 기록
    // ──────────────────────────────────────────────────────────────────

    /**
     * 감사추적 로그를 LOG_CALCULATION 테이블에 기록한다.
     *
     * @param reqId        요청 ID
     * @param calcStep     계산 단계 (M1-01, M1-02, M1-03 등)
     * @param functionName 함수명
     * @param inputData    입력 데이터 요약 문자열
     * @param outputData   출력 데이터 요약 문자열
     * @param logLevel     로그 레벨 (INFO, WARN, ERROR)
     * @param startTime    처리 시작 시각 (밀리초)
     */
    private void saveLog(String reqId, String calcStep, String functionName,
                         String inputData, String outputData,
                         String logLevel, long startTime) {
        int durationMs = (int) (System.currentTimeMillis() - startTime);
        LocalDateTime now = LocalDateTime.now();

        LogCalculation logEntry = LogCalculation.builder()
                .reqId(reqId)
                .calcStep(calcStep)
                .functionName(functionName)
                .inputData(inputData)
                .outputData(outputData)
                .logLevel(logLevel)
                .executedAt(now)
                .executedBy(resolveRequestedBy())
                .durationMs(durationMs)
                .traceId(resolveTraceId())
                .build();

        logCalculationRepository.save(logEntry);
    }

    // ──────────────────────────────────────────────────────────────────
    // 컨텍스트 해석 유틸리티
    // ──────────────────────────────────────────────────────────────────

    /**
     * 현재 요청의 출처(source)를 반환한다.
     *
     * @return 요청 출처 문자열 (기본값: "API")
     */
    private String resolveRequestSource() {
        return "API";
    }

    /**
     * 현재 요청자 정보를 RequestContext에서 추출한다.
     *
     * @return 요청자 식별자 (기본값: "SYSTEM")
     */
    private String resolveRequestedBy() {
        RequestContext ctx = RequestContext.get();
        if (ctx != null && ctx.getRequestedBy() != null) {
            return ctx.getRequestedBy();
        }
        return "SYSTEM";
    }

    /**
     * 현재 클라이언트 IP를 RequestContext에서 추출한다.
     *
     * @return 클라이언트 IP 주소 (기본값: null)
     */
    private String resolveClientIp() {
        RequestContext ctx = RequestContext.get();
        if (ctx != null && ctx.getClientIp() != null) {
            return ctx.getClientIp();
        }
        return null;
    }

    /**
     * 현재 트레이스 ID를 RequestContext에서 추출한다.
     *
     * @return 트레이스 ID (기본값: null)
     */
    private String resolveTraceId() {
        RequestContext ctx = RequestContext.get();
        if (ctx != null && ctx.getTraceId() != null) {
            return ctx.getTraceId();
        }
        return null;
    }

    /**
     * 요청 상태 코드에 따른 진행 단계 설명을 반환한다.
     *
     * @param statusCode 요청 상태 코드 (예: "RECEIVED", "PARSING")
     * @return 진행 단계 설명 한글 문자열
     */
    private String resolveProgressDescription(String statusCode) {
        try {
            RequestStatus status = RequestStatus.fromCode(statusCode);
            return status.getDescription();
        } catch (IllegalArgumentException e) {
            return "알 수 없는 상태: " + statusCode;
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // 데이터 추출 유틸리티 (Map에서 안전하게 값 추출)
    // ──────────────────────────────────────────────────────────────────

    /**
     * Map에서 문자열 값을 안전하게 추출한다.
     *
     * @param data Map 데이터
     * @param key  키
     * @return 문자열 값 (없으면 null)
     */
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        // JSON 객체/배열인 경우 JSON 문자열로 변환
        if (value instanceof Map || value instanceof List) {
            return JsonUtil.toJson(value);
        }
        return String.valueOf(value);
    }

    /**
     * Map에서 Long 값을 안전하게 추출한다.
     *
     * @param data Map 데이터
     * @param key  키
     * @return Long 값 (없거나 변환 불가하면 null)
     */
    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.debug("Long 변환 실패. key={}, value={}", key, value);
            return null;
        }
    }

    /**
     * Map에서 Integer 값을 안전하게 추출한다.
     *
     * @param data Map 데이터
     * @param key  키
     * @return Integer 값 (없거나 변환 불가하면 null)
     */
    private Integer getIntegerValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.debug("Integer 변환 실패. key={}, value={}", key, value);
            return null;
        }
    }

    /**
     * Map에서 Boolean 값을 안전하게 추출한다.
     *
     * @param data Map 데이터
     * @param key  키
     * @return Boolean 값 (없으면 null)
     */
    private Boolean getBooleanValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * Map에서 BigDecimal 값을 안전하게 추출한다.
     *
     * @param data Map 데이터
     * @param key  키
     * @return BigDecimal 값 (없거나 변환 불가하면 null)
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(String.valueOf(value));
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.debug("BigDecimal 변환 실패. key={}, value={}", key, value);
            return null;
        }
    }

    /**
     * Map에서 LocalDate 값을 안전하게 추출한다.
     *
     * <p>ISO 8601(YYYY-MM-DD) 또는 YYYYMMDD 형식을 지원한다.</p>
     *
     * @param data Map 데이터
     * @param key  키
     * @return LocalDate 값 (없거나 파싱 불가하면 null)
     */
    private LocalDate getLocalDateValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        String dateStr = String.valueOf(value);
        if (dateStr.isEmpty()) {
            return null;
        }
        try {
            return DateUtil.parseDate(dateStr);
        } catch (Exception e) {
            log.debug("LocalDate 변환 실패. key={}, value={}", key, value);
            return null;
        }
    }

    /**
     * ValidationException용 단일 FieldError 목록을 생성하는 헬퍼 메서드.
     *
     * @param field    오류 필드명
     * @param issue    오류 설명
     * @param expected 기대 값
     * @param received 실제 수신 값
     * @return FieldError 단일 항목 목록
     */
    private List<ValidationException.FieldError> createFieldErrors(
            String field, String issue, String expected, String received) {
        List<ValidationException.FieldError> errors = new ArrayList<ValidationException.FieldError>();
        errors.add(new ValidationException.FieldError(field, issue, expected, received));
        return errors;
    }
}
