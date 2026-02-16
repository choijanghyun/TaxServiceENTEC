package com.entec.tax.engine.precheck.service;

import com.entec.tax.common.constants.ProvisionCode;
import com.entec.tax.common.constants.SystemConstants;
import com.entec.tax.common.constants.TaxType;
import com.entec.tax.common.exception.ErrorCode;
import com.entec.tax.common.exception.HardFailException;
import com.entec.tax.common.exception.TaxServiceException;
import com.entec.tax.common.exception.ValidationException;
import com.entec.tax.common.util.DateUtil;
import com.entec.tax.common.util.TruncationUtil;
import com.entec.tax.common.util.ValidationUtil;
import com.entec.tax.domain.check.entity.ChkEligibility;
import com.entec.tax.domain.check.entity.ChkInspectionLog;
import com.entec.tax.domain.check.repository.ChkEligibilityRepository;
import com.entec.tax.domain.check.repository.ChkInspectionLogRepository;
import com.entec.tax.domain.input.entity.InpBasic;
import com.entec.tax.domain.input.entity.InpDeduction;
import com.entec.tax.domain.input.entity.InpEmployee;
import com.entec.tax.domain.input.repository.InpBasicRepository;
import com.entec.tax.domain.input.repository.InpDeductionRepository;
import com.entec.tax.domain.input.repository.InpEmployeeRepository;
import com.entec.tax.domain.log.entity.LogCalculation;
import com.entec.tax.domain.log.repository.LogCalculationRepository;
import com.entec.tax.domain.output.entity.OutEmployeeSummary;
import com.entec.tax.domain.output.repository.OutEmployeeSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * M3 사전점검 엔진 서비스 (STEP 0: 사전점검).
 *
 * <p>
 * 경정청구 세액계산을 실행하기 전에 반드시 수행해야 하는 사전점검(Pre-Check) 로직을 담당한다.
 * 점검 항목은 설계서 M3-00 ~ M3-06, P3-01 ~ P3-03 까지이며,
 * 각 점검 결과를 CHK_ELIGIBILITY, OUT_EMPLOYEE_SUMMARY, CHK_INSPECTION_LOG 에 기록한다.
 * </p>
 *
 * <h2>점검 항목 개요</h2>
 * <ul>
 *   <li><b>M3-00</b>: Hard Fail 검증 - 결산조정 항목 포함 시 전체 차단</li>
 *   <li><b>M3-01</b>: 경정청구 기한 적격성 판단 (법정신고기한 + 5년 이내)</li>
 *   <li><b>M3-02</b>: 중소기업 해당 여부 판정 (소기업/중기업/중견/대, 졸업유예 3년)</li>
 *   <li><b>M3-03</b>: 수도권 소재지 구분 판단 (본점 간주 vs 실제 설치장소)</li>
 *   <li><b>M3-04</b>: 상시근로자 수 산정 (제외대상 필터, 월별 집계, 연평균, 청년등 구분)</li>
 *   <li><b>M3-05</b>: 벤처기업 확인</li>
 *   <li><b>M3-06</b>: 결산확정 원칙 검증 (결산조정 vs 신고조정 구분)</li>
 *   <li><b>P3-01</b>: 개인사업자 기장 의무 점검</li>
 *   <li><b>P3-02</b>: 개인사업자 성실신고확인 대상 점검</li>
 *   <li><b>P3-03</b>: 개인사업자 추계신고 검증</li>
 * </ul>
 *
 * <h2>호출 흐름</h2>
 * <pre>
 *   Controller → PreCheckService.executePreCheck(reqId)
 *     ├─ M3-00: checkHardFail()
 *     ├─ M3-01: checkClaimDeadlineEligibility()
 *     ├─ M3-02: determineSmeStatus()
 *     ├─ M3-03: determineCapitalZone()
 *     ├─ M3-04: calculateRegularEmployees()
 *     ├─ M3-05: confirmVentureStatus()
 *     ├─ M3-06: verifySettlementPrinciple()
 *     ├─ P3-01~03: executeIndividualChecks() (개인사업자인 경우)
 *     └─ 결과 저장 (CHK_ELIGIBILITY, OUT_EMPLOYEE_SUMMARY, CHK_INSPECTION_LOG)
 * </pre>
 *
 * @author ENTEC Tax Engine
 * @since 1.0.0
 * @see ChkEligibility
 * @see ChkInspectionLog
 * @see OutEmployeeSummary
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PreCheckService {

    // =========================================================================
    // 의존성 주입 (Repository)
    // =========================================================================

    private final InpBasicRepository inpBasicRepository;
    private final InpEmployeeRepository inpEmployeeRepository;
    private final InpDeductionRepository inpDeductionRepository;
    private final ChkEligibilityRepository chkEligibilityRepository;
    private final ChkInspectionLogRepository chkInspectionLogRepository;
    private final OutEmployeeSummaryRepository outEmployeeSummaryRepository;
    private final LogCalculationRepository logCalculationRepository;

    // =========================================================================
    // 상수 정의
    // =========================================================================

    /** 계산 단계 명칭 */
    private static final String CALC_STEP = "STEP0";

    /** 점검 관련 모듈명 */
    private static final String RELATED_MODULE = "PreCheckService";

    /** 경정청구 기한: 법정신고기한 + 5년 */
    private static final int CLAIM_DEADLINE_YEARS = 5;

    /** 중소기업 졸업유예 기간 (년) */
    private static final int SME_GRACE_PERIOD_YEARS = 3;

    /** 청년 판단 기준 나이 (만 34세 이하) */
    private static final int YOUTH_AGE_LIMIT = 34;

    /** 군복무 인정 최대 기간 (년) */
    private static final long MAX_MILITARY_SERVICE_YEARS = 6L;

    /** 월 근로시간 최소 기준 (60시간 미만 제외) */
    private static final int MIN_MONTHLY_HOURS = 60;

    /** 계약기간 최소 기준 (1년 미만 제외) */
    private static final int MIN_CONTRACT_MONTHS = 12;

    /** 상시근로자 연평균 소수점 자릿수 (TRUNCATE 2자리) */
    private static final int EMPLOYEE_AVG_SCALE = 2;

    /** 대기업 지분 독립성 기준 (30% 미만) */
    private static final BigDecimal INDEPENDENCE_THRESHOLD = new BigDecimal("0.30");

    /** 고령자 기준 나이 (60세 이상) */
    private static final int AGED_THRESHOLD = 60;

    /** 청년 기준 변경 경계 연도 (2025년부터 근로계약체결일 기준) */
    private static final int YOUTH_CRITERIA_CHANGE_YEAR = 2025;

    // ─────────────────────────────────────────────────────────────────────
    // 결산조정 항목 (Hard Fail 대상) — M3-00, M3-06 공통
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 결산조정 항목 키워드 목록.
     * <p>
     * 감가상각비추가, 퇴직급여충당금추가, 대손충당금추가 등
     * 결산확정(장부반영) 없이는 경정청구 불가능한 항목들이다.
     * subDetail 또는 method 필드에 이 키워드가 포함되면 BLOCKED 처리한다.
     * </p>
     */
    private static final Set<String> SETTLEMENT_ADJUSTMENT_KEYWORDS;
    static {
        Set<String> keywords = new HashSet<String>();
        keywords.add("감가상각비추가");
        keywords.add("퇴직급여충당금추가");
        keywords.add("대손충당금추가");
        keywords.add("대손금추가");
        keywords.add("재고자산평가변경");
        keywords.add("접대비한도초과추가");
        keywords.add("준비금추가계상");
        keywords.add("퇴직연금부담금추가");
        keywords.add("결산조정");
        SETTLEMENT_ADJUSTMENT_KEYWORDS = Collections.unmodifiableSet(keywords);
    }

    /**
     * 신고조정 항목 키워드 목록.
     * <p>
     * 세액감면신규, 세액공제추가, R&D방식변경 등
     * 결산확정 없이 신고서 수정만으로 경정청구가 가능한 항목이다.
     * </p>
     */
    private static final Set<String> FILING_ADJUSTMENT_KEYWORDS;
    static {
        Set<String> keywords = new HashSet<String>();
        keywords.add("세액감면신규");
        keywords.add("세액공제추가");
        keywords.add("R&D방식변경");
        keywords.add("세액공제증액");
        keywords.add("투자공제추가");
        keywords.add("고용공제추가");
        keywords.add("신고조정");
        FILING_ADJUSTMENT_KEYWORDS = Collections.unmodifiableSet(keywords);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 업종별 중소기업 매출액 기준 (억원 단위)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 업종 코드 앞 2자리(대분류) → 중소기업 매출액 상한(억원) 매핑.
     * <p>
     * 「중소기업기본법 시행령」 별표 1 기준으로 업종별 매출액 한도를 설정한다.
     * 해당 매출액 이하이면 중소기업 규모 요건을 충족한다.
     * </p>
     */
    private static final Map<String, Long> SME_REVENUE_LIMIT_MAP;
    static {
        Map<String, Long> map = new HashMap<String, Long>();
        // 제조업 (10~33): 1,500억원
        for (int i = 10; i <= 33; i++) {
            map.put(String.valueOf(i), 150000000000L);
        }
        // 건설업 (41~42): 1,000억원
        map.put("41", 100000000000L);
        map.put("42", 100000000000L);
        // 운수업 (49~52): 1,000억원
        for (int i = 49; i <= 52; i++) {
            map.put(String.valueOf(i), 100000000000L);
        }
        // 도매/소매 (45~47): 1,000억원
        for (int i = 45; i <= 47; i++) {
            map.put(String.valueOf(i), 100000000000L);
        }
        // 숙박/음식 (55~56): 400억원
        map.put("55", 40000000000L);
        map.put("56", 40000000000L);
        // 전문/과학/기술 (70~73): 1,000억원
        for (int i = 70; i <= 73; i++) {
            map.put(String.valueOf(i), 100000000000L);
        }
        // 정보통신 (58~63): 1,000억원
        for (int i = 58; i <= 63; i++) {
            map.put(String.valueOf(i), 100000000000L);
        }
        // 그 외 서비스업: 600억원 (기본값으로 처리)
        SME_REVENUE_LIMIT_MAP = Collections.unmodifiableMap(map);
    }

    /** 업종별 매출 기준에 해당하지 않는 경우의 기본 상한 (600억원) */
    private static final long SME_REVENUE_DEFAULT_LIMIT = 60000000000L;

    /**
     * 소기업 매출액 기준 (업종 무관 연매출 기준).
     * <p>
     * 소기업은 업종에 관계없이 연매출 120억원 이하를 기준으로 한다.
     * (간이 판정 기준; 실제로는 업종별로 30억~120억 범위이나 여기서는 보수적으로 적용)
     * </p>
     */
    private static final long SMALL_ENTERPRISE_REVENUE_LIMIT = 12000000000L;

    // =========================================================================
    // 메인 실행 메서드
    // =========================================================================

    /**
     * STEP 0 사전점검을 실행한다.
     *
     * <p>
     * 주어진 요청 ID에 대해 M3-00 ~ M3-06 (법인) 및 P3-01 ~ P3-03 (개인) 점검을 순차 실행하고,
     * 결과를 CHK_ELIGIBILITY, OUT_EMPLOYEE_SUMMARY, CHK_INSPECTION_LOG 에 저장한다.
     * </p>
     *
     * <p><b>M3-00 Hard Fail 발생 시:</b> HardFailException 을 throw 하여 이후 계산을 전면 차단한다.</p>
     *
     * @param reqId 요청 ID (INP_BASIC.req_id)
     * @return 생성된 ChkEligibility 엔티티 (종합 진단 결과)
     * @throws HardFailException  결산조정 항목이 포함된 경우 (M3-00)
     * @throws TaxServiceException 기본 정보 조회 실패 등 시스템 오류
     */
    public ChkEligibility executePreCheck(String reqId) {
        long startTime = System.currentTimeMillis();
        log.info("[STEP0] 사전점검 시작 - reqId={}", reqId);

        // ── 1. 기본 정보 로드 ─────────────────────────────────────────
        InpBasic basic = loadBasicInfo(reqId);
        String taxYear = basic.getTaxYear();
        String taxType = basic.getTaxType();
        LocalDateTime now = LocalDateTime.now();

        // ── 2. 기존 결과 초기화 (재시도 대비) ─────────────────────────
        clearPreviousResults(reqId);

        // ── 3. 점검 로그 수집 리스트 ──────────────────────────────────
        List<ChkInspectionLog> inspectionLogs = new ArrayList<ChkInspectionLog>();
        int sortOrder = 0;

        // ── 4. 종합 진단 결과 빌더용 변수 ─────────────────────────────
        String overallStatus = "ELIGIBLE";
        StringBuilder diagnosisBuilder = new StringBuilder();

        // ================================================================
        // M3-00: Hard Fail 검증 (결산조정 항목 포함 시 전체 차단)
        // ================================================================
        sortOrder++;
        List<String> blockedItems = checkHardFail(reqId);
        if (!blockedItems.isEmpty()) {
            // Hard Fail → 즉시 차단, 이후 점검 불필요
            ChkInspectionLog hardFailLog = buildInspectionLog(reqId, "M3-00",
                    "Hard Fail 결산조정 검증", "조세특례제한법 전문",
                    "BLOCKED",
                    "결산조정 항목 검출: " + joinStrings(blockedItems),
                    sortOrder, now);
            inspectionLogs.add(hardFailLog);
            chkInspectionLogRepository.saveAll(inspectionLogs);

            // CHK_ELIGIBILITY 에 차단 상태 기록
            ChkEligibility blocked = ChkEligibility.builder()
                    .reqId(reqId)
                    .taxType(taxType)
                    .overallStatus("BLOCKED")
                    .settlementCheckResult("BLOCKED")
                    .settlementBlockedItems(joinStrings(blockedItems))
                    .diagnosisDetail("M3-00 Hard Fail: 결산조정 항목이 포함되어 전체 차단됨")
                    .checkedAt(now)
                    .build();
            chkEligibilityRepository.save(blocked);

            writeCalcLog(reqId, "M3-00", "checkHardFail",
                    "blockedItems=" + joinStrings(blockedItems), "BLOCKED",
                    "결산조정 항목 포함 시 전체 차단", startTime);

            log.error("[STEP0][M3-00] Hard Fail 발생 - reqId={}, blockedItems={}", reqId, blockedItems);
            throw new HardFailException(
                    "결산조정 항목이 포함되어 경정청구를 진행할 수 없습니다: " + joinStrings(blockedItems),
                    reqId, blockedItems);
        }
        inspectionLogs.add(buildInspectionLog(reqId, "M3-00",
                "Hard Fail 결산조정 검증", "조세특례제한법 전문",
                "PASS", "결산조정 항목 없음 - 통과", sortOrder, now));
        log.info("[STEP0][M3-00] Hard Fail 검증 통과 - reqId={}", reqId);

        // ================================================================
        // M3-01: 경정청구 기한 적격성 판단
        // ================================================================
        sortOrder++;
        Map<String, Object> deadlineResult = checkClaimDeadlineEligibility(basic);
        String deadlineEligible = (String) deadlineResult.get("eligible");
        LocalDate filingDeadline = (LocalDate) deadlineResult.get("filingDeadline");
        LocalDate claimDeadline = (LocalDate) deadlineResult.get("claimDeadline");
        String deadlineSummary = (String) deadlineResult.get("summary");

        inspectionLogs.add(buildInspectionLog(reqId, "M3-01",
                "경정청구 기한 적격성", "국세기본법 제45조의2",
                deadlineEligible, deadlineSummary, sortOrder, now));

        if ("INELIGIBLE".equals(deadlineEligible)) {
            overallStatus = "INELIGIBLE";
            diagnosisBuilder.append("[M3-01] 경정청구 기한 초과; ");
        }
        log.info("[STEP0][M3-01] 기한 적격성={} - reqId={}", deadlineEligible, reqId);

        // ================================================================
        // M3-02: 중소기업 해당 여부 판정
        // ================================================================
        sortOrder++;
        Map<String, Object> smeResult = determineSmeStatus(basic);
        String smeEligible = (String) smeResult.get("smeEligible");
        String companySize = (String) smeResult.get("companySize");
        String smallVsMedium = (String) smeResult.get("smallVsMedium");
        String smeGraceEndYear = (String) smeResult.get("smeGraceEndYear");
        String smeSummary = (String) smeResult.get("summary");

        inspectionLogs.add(buildInspectionLog(reqId, "M3-02",
                "중소기업 해당 여부", "조특법 제2조, 중소기업기본법 제2조",
                smeEligible, smeSummary, sortOrder, now));
        log.info("[STEP0][M3-02] 중소기업={}, 규모={}, 소/중={} - reqId={}",
                smeEligible, companySize, smallVsMedium, reqId);

        // ================================================================
        // M3-03: 수도권 소재지 구분 판단
        // ================================================================
        sortOrder++;
        Map<String, Object> zoneResult = determineCapitalZone(basic);
        String capitalZone = (String) zoneResult.get("capitalZone");
        String zoneSummary = (String) zoneResult.get("summary");

        inspectionLogs.add(buildInspectionLog(reqId, "M3-03",
                "수도권 소재지 구분", "조특법 제7조/제24조/제6조",
                capitalZone, zoneSummary, sortOrder, now));
        log.info("[STEP0][M3-03] 수도권 구분={} - reqId={}", capitalZone, reqId);

        // ================================================================
        // M3-04: 상시근로자 수 산정
        // ================================================================
        sortOrder++;
        Map<String, Object> employeeResult = calculateRegularEmployees(reqId, basic);
        String empSummary = (String) employeeResult.get("summary");
        BigDecimal currentTotal = (BigDecimal) employeeResult.get("currentTotal");

        inspectionLogs.add(buildInspectionLog(reqId, "M3-04",
                "상시근로자 수 산정", "조특법 시행령 제23조/제26조의8",
                "CALCULATED", empSummary, sortOrder, now));
        log.info("[STEP0][M3-04] 상시근로자 산정 완료 - reqId={}, 당기합계={}",
                reqId, currentTotal);

        // ================================================================
        // M3-05: 벤처기업 확인
        // ================================================================
        sortOrder++;
        Map<String, Object> ventureResult = confirmVentureStatus(basic);
        Boolean ventureConfirmed = (Boolean) ventureResult.get("ventureConfirmed");
        String ventureSummary = (String) ventureResult.get("summary");

        inspectionLogs.add(buildInspectionLog(reqId, "M3-05",
                "벤처기업 확인", "벤처기업육성에 관한 특별조치법 제25조",
                ventureConfirmed ? "CONFIRMED" : "NOT_CONFIRMED",
                ventureSummary, sortOrder, now));
        log.info("[STEP0][M3-05] 벤처확인={} - reqId={}", ventureConfirmed, reqId);

        // ================================================================
        // M3-06: 결산확정 원칙 검증
        // ================================================================
        sortOrder++;
        Map<String, Object> settlementResult = verifySettlementPrinciple(reqId);
        String settlementCheckResult = (String) settlementResult.get("checkResult");
        String settlementBlockedItems = (String) settlementResult.get("blockedItems");
        String settlementSummary = (String) settlementResult.get("summary");

        inspectionLogs.add(buildInspectionLog(reqId, "M3-06",
                "결산확정 원칙 검증", "법인세법 제40조/소득세법 제33조",
                settlementCheckResult, settlementSummary, sortOrder, now));

        if ("BLOCKED".equals(settlementCheckResult)) {
            overallStatus = "BLOCKED";
            diagnosisBuilder.append("[M3-06] 결산조정 항목 존재: ")
                    .append(settlementBlockedItems).append("; ");
        }
        log.info("[STEP0][M3-06] 결산확정={} - reqId={}", settlementCheckResult, reqId);

        // ================================================================
        // P3-01 ~ P3-03: 개인사업자 추가 점검 (taxType = INC 인 경우)
        // ================================================================
        if (TaxType.INC.getCode().equals(taxType)) {
            sortOrder = executeIndividualChecks(reqId, basic, inspectionLogs, sortOrder,
                    now, diagnosisBuilder);
        }

        // ================================================================
        // 종합 결과 저장
        // ================================================================

        // diagnosisDetail 정리
        if (diagnosisBuilder.length() == 0) {
            diagnosisBuilder.append("모든 사전점검 항목 통과");
        }

        ChkEligibility eligibility = ChkEligibility.builder()
                .reqId(reqId)
                .taxType(taxType)
                .companySize(companySize)
                .capitalZone(capitalZone)
                .filingDeadline(filingDeadline)
                .claimDeadline(claimDeadline)
                .deadlineEligible(deadlineEligible)
                .smeEligible(smeEligible)
                .smeGraceEndYear(smeGraceEndYear)
                .smallVsMedium(smallVsMedium)
                .ventureConfirmed(ventureConfirmed)
                .settlementCheckResult(settlementCheckResult)
                .settlementBlockedItems(settlementBlockedItems)
                .estimateCheck(false)
                .sincerityTarget(basic.getSincerityTarget())
                .overallStatus(overallStatus)
                .diagnosisDetail(diagnosisBuilder.toString())
                .checkedAt(now)
                .build();
        chkEligibilityRepository.save(eligibility);

        // 점검 로그 일괄 저장
        chkInspectionLogRepository.saveAll(inspectionLogs);

        // 감사 로그 기록
        long elapsed = System.currentTimeMillis() - startTime;
        writeCalcLog(reqId, CALC_STEP, "executePreCheck",
                "taxYear=" + taxYear + ", taxType=" + taxType,
                "overallStatus=" + overallStatus,
                "STEP0 사전점검 완료", startTime);

        log.info("[STEP0] 사전점검 완료 - reqId={}, 종합상태={}, 소요시간={}ms",
                reqId, overallStatus, elapsed);

        return eligibility;
    }

    // =========================================================================
    // M3-00: Hard Fail 검증 (결산조정 항목 포함 시 전체 차단)
    // =========================================================================

    /**
     * 결산조정 항목이 공제/감면 입력에 포함되어 있는지 검사한다.
     *
     * <p>
     * INP_DEDUCTION 의 subDetail, method 필드를 스캔하여
     * 결산조정 키워드(감가상각비추가, 퇴직급여충당금추가, 대손충당금추가 등)가
     * 포함된 항목을 식별한다.
     * </p>
     *
     * <p>
     * <b>비즈니스 규칙:</b> 결산조정 항목은 결산서 반영이 선행되어야 하므로,
     * 경정청구(신고서 수정)만으로는 처리 불가하다. 해당 항목이 하나라도 존재하면
     * Hard Fail 로 전체 요청을 차단한다.
     * </p>
     *
     * @param reqId 요청 ID
     * @return 검출된 결산조정 항목명 리스트 (비어 있으면 통과)
     */
    private List<String> checkHardFail(String reqId) {
        log.debug("[M3-00] Hard Fail 검사 시작 - reqId={}", reqId);

        List<InpDeduction> deductions = inpDeductionRepository.findByReqId(reqId);
        List<String> blockedItems = new ArrayList<String>();

        for (InpDeduction deduction : deductions) {
            String subDetail = deduction.getSubDetail();
            String method = deduction.getMethod();

            // subDetail 에서 결산조정 키워드 검색
            if (subDetail != null && !subDetail.isEmpty()) {
                for (String keyword : SETTLEMENT_ADJUSTMENT_KEYWORDS) {
                    if (subDetail.contains(keyword)) {
                        String description = String.format("[%s/%s] %s (subDetail 검출)",
                                deduction.getItemCategory(), deduction.getProvision(), keyword);
                        blockedItems.add(description);
                        log.warn("[M3-00] 결산조정 항목 검출 - reqId={}, category={}, provision={}, keyword={}",
                                reqId, deduction.getItemCategory(), deduction.getProvision(), keyword);
                    }
                }
            }

            // method 에서 결산조정 키워드 검색
            if (method != null && !method.isEmpty()) {
                for (String keyword : SETTLEMENT_ADJUSTMENT_KEYWORDS) {
                    if (method.contains(keyword)) {
                        String description = String.format("[%s/%s] %s (method 검출)",
                                deduction.getItemCategory(), deduction.getProvision(), keyword);
                        // 중복 방지
                        if (!blockedItems.contains(description)) {
                            blockedItems.add(description);
                            log.warn("[M3-00] 결산조정 항목 검출(method) - reqId={}, keyword={}", reqId, keyword);
                        }
                    }
                }
            }
        }

        log.debug("[M3-00] Hard Fail 검사 완료 - reqId={}, 검출건수={}", reqId, blockedItems.size());
        return blockedItems;
    }

    // =========================================================================
    // M3-01: 경정청구 기한 적격성 판단
    // =========================================================================

    /**
     * 경정청구 기한 적격성을 판단한다.
     *
     * <p>
     * <b>규칙:</b> 국세기본법 제45조의2에 따라,
     * 법정신고기한으로부터 5년 이내에 경정청구를 제기해야 한다.
     * </p>
     *
     * <p>
     * <b>산출 로직:</b>
     * <ol>
     *   <li>법정신고기한 = DateUtil.getFilingDeadline(taxYear, taxType)</li>
     *   <li>경정청구기한 = 법정신고기한 + 5년</li>
     *   <li>요청접수일(requestDate) ≤ 경정청구기한 → ELIGIBLE</li>
     *   <li>요청접수일 > 경정청구기한 → INELIGIBLE</li>
     * </ol>
     * </p>
     *
     * @param basic 기본 정보 엔티티
     * @return 판정 결과 맵 (eligible, filingDeadline, claimDeadline, summary)
     */
    private Map<String, Object> checkClaimDeadlineEligibility(InpBasic basic) {
        String taxYear = basic.getTaxYear();
        String taxType = basic.getTaxType();
        LocalDate requestDate = basic.getRequestDate();

        // 법정신고기한 산출
        LocalDate filingDeadline = DateUtil.getFilingDeadline(taxYear, taxType);

        // 경정청구기한 = 법정신고기한 + 5년
        LocalDate claimDeadline = DateUtil.getClaimDeadline(filingDeadline);

        // 기한 내 여부 판정
        // requestDate 가 null 인 경우 현재 날짜 사용
        LocalDate effectiveDate = (requestDate != null) ? requestDate : LocalDate.now();
        boolean isEligible = !effectiveDate.isAfter(claimDeadline);

        String eligible = isEligible ? "ELIGIBLE" : "INELIGIBLE";
        String summary = String.format("과세연도=%s, 법정신고기한=%s, 경정청구기한=%s, 접수일=%s → %s",
                taxYear,
                DateUtil.formatDate(filingDeadline),
                DateUtil.formatDate(claimDeadline),
                DateUtil.formatDate(effectiveDate),
                eligible);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("eligible", eligible);
        result.put("filingDeadline", filingDeadline);
        result.put("claimDeadline", claimDeadline);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // M3-02: 중소기업 해당 여부 판정
    // =========================================================================

    /**
     * 중소기업 해당 여부를 판정한다.
     *
     * <p>
     * <b>판정 기준 (조특법 제2조, 중소기업기본법 제2조):</b>
     * <ol>
     *   <li><b>매출액 기준:</b> 업종별 매출액 상한 이하</li>
     *   <li><b>독립성 기준:</b> 대기업 지분 30% 미만 (InpBasic.corpSize 로 판단)</li>
     *   <li><b>졸업유예:</b> 요건 초과 시에도 최초 초과 과세연도 포함 3년간 중소기업 지위 유지</li>
     * </ol>
     * </p>
     *
     * <p>
     * <b>규모 분류:</b>
     * <ul>
     *   <li>소기업: 매출 120억원 이하 (업종별 세부 기준 적용 가능)</li>
     *   <li>중기업: 소기업 초과 ~ 업종별 중소기업 매출 상한 이하</li>
     *   <li>중견기업: 중소기업 매출 상한 초과</li>
     *   <li>대기업: 중견기업 초과 또는 독립성 미충족</li>
     * </ul>
     * </p>
     *
     * @param basic 기본 정보 엔티티
     * @return 판정 결과 맵 (smeEligible, companySize, smallVsMedium, smeGraceEndYear, summary)
     */
    private Map<String, Object> determineSmeStatus(InpBasic basic) {
        Long revenue = basic.getRevenue();
        String industryCode = basic.getIndustryCode();
        String existingCorpSize = basic.getCorpSize();
        String taxYear = basic.getTaxYear();

        // ── 1. 매출액 기준 판정 ──────────────────────────────────────
        long revenueVal = (revenue != null) ? revenue : 0L;
        long smeLimit = getSmeRevenueLimit(industryCode);
        boolean meetsSmeRevenue = revenueVal <= smeLimit;

        // ── 2. 독립성 기준 판정 ──────────────────────────────────────
        // corpSize 가 "대기업" 또는 "대" 이면 독립성 미충족으로 간주
        boolean isIndependent = true;
        if (existingCorpSize != null
                && (existingCorpSize.contains("대기업") || "대".equals(existingCorpSize))) {
            isIndependent = false;
        }

        // ── 3. 종합 판정 ────────────────────────────────────────────
        String smeEligible;
        String companySize;
        String smallVsMedium = null;
        String smeGraceEndYear = null;

        if (meetsSmeRevenue && isIndependent) {
            // 중소기업 해당
            smeEligible = "ELIGIBLE";

            // 소기업 vs 중기업 구분
            if (revenueVal <= SMALL_ENTERPRISE_REVENUE_LIMIT) {
                companySize = "소기업";
                smallVsMedium = "SMALL";
            } else {
                companySize = "중기업";
                smallVsMedium = "MEDIUM";
            }
        } else if (!meetsSmeRevenue && isIndependent) {
            // 매출 초과이나 독립성 충족 → 졸업유예 또는 중견기업 판정
            // 기존 corpSize 가 중소기업이었다면 졸업유예 적용 가능성 확인
            if (existingCorpSize != null
                    && (existingCorpSize.contains("중소") || existingCorpSize.contains("소기업")
                    || existingCorpSize.contains("중기업"))) {
                // 졸업유예 적용: 최초 초과 과세연도 포함 3년
                smeEligible = "GRACE";
                companySize = "중소기업(유예)";
                smallVsMedium = "MEDIUM";
                int yearVal = Integer.parseInt(taxYear);
                smeGraceEndYear = String.valueOf(yearVal + SME_GRACE_PERIOD_YEARS);
            } else {
                smeEligible = "INELIGIBLE";
                companySize = "중견기업";
                smallVsMedium = null;
            }
        } else {
            // 독립성 미충족 → 대기업
            smeEligible = "INELIGIBLE";
            companySize = "대기업";
            smallVsMedium = null;
        }

        String summary = String.format(
                "매출=%,d원, 업종한도=%,d원, 독립성=%s, 규모=%s, 소/중=%s, 유예종료=%s → %s",
                revenueVal, smeLimit,
                isIndependent ? "충족" : "미충족",
                companySize,
                smallVsMedium != null ? smallVsMedium : "N/A",
                smeGraceEndYear != null ? smeGraceEndYear : "N/A",
                smeEligible);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("smeEligible", smeEligible);
        result.put("companySize", companySize);
        result.put("smallVsMedium", smallVsMedium);
        result.put("smeGraceEndYear", smeGraceEndYear);
        result.put("summary", summary);
        return result;
    }

    /**
     * 업종코드에 따른 중소기업 매출액 상한을 반환한다.
     *
     * <p>
     * 업종 코드 앞 2자리(대분류)를 기준으로 「중소기업기본법 시행령」 별표 1의
     * 업종별 매출 한도를 조회한다. 매핑되지 않는 업종은 기본값(600억원)을 적용한다.
     * </p>
     *
     * @param industryCode 업종 코드 (5자리 KSIC)
     * @return 중소기업 매출액 상한 (원)
     */
    private long getSmeRevenueLimit(String industryCode) {
        if (industryCode == null || industryCode.length() < 2) {
            return SME_REVENUE_DEFAULT_LIMIT;
        }
        String majorCode = industryCode.substring(0, 2);
        Long limit = SME_REVENUE_LIMIT_MAP.get(majorCode);
        return (limit != null) ? limit : SME_REVENUE_DEFAULT_LIMIT;
    }

    // =========================================================================
    // M3-03: 수도권 소재지 구분 판단
    // =========================================================================

    /**
     * 수도권 소재지 구분을 판단한다.
     *
     * <p>
     * <b>조항별 소재지 적용 기준:</b>
     * <ul>
     *   <li><b>제7조 (중소기업 특별세액감면):</b> 본점 소재지 기준</li>
     *   <li><b>제24조 (통합투자세액공제):</b> 자산 설치장소 기준</li>
     *   <li><b>제6조 (창업중소기업 감면):</b> 창업 소재지 기준</li>
     * </ul>
     * </p>
     *
     * <p>
     * 현재는 INP_BASIC.capitalZone 필드를 우선 사용하며,
     * 값이 없으면 hqLocation 에서 수도권 키워드(서울, 인천, 경기)를 판별한다.
     * </p>
     *
     * @param basic 기본 정보 엔티티
     * @return 판정 결과 맵 (capitalZone, summary)
     */
    private Map<String, Object> determineCapitalZone(InpBasic basic) {
        String inputZone = basic.getCapitalZone();
        String hqLocation = basic.getHqLocation();
        String capitalZone;
        String summary;

        if (inputZone != null && !inputZone.trim().isEmpty()) {
            // 이미 입력된 수도권 구분 값 사용
            capitalZone = inputZone.trim();
            summary = String.format("입력값 기준 수도권 구분=%s (본점소재지=%s)",
                    capitalZone, hqLocation != null ? hqLocation : "미입력");
        } else if (hqLocation != null && !hqLocation.trim().isEmpty()) {
            // 본점 소재지에서 수도권 판별
            capitalZone = isCapitalArea(hqLocation) ? "CAPITAL" : "NON_CAPITAL";
            summary = String.format("본점소재지 판별: %s → %s (§7 본점간주 적용)",
                    hqLocation, capitalZone);
        } else {
            // 소재지 정보 없음 → 보수적으로 수도권 간주
            capitalZone = "CAPITAL";
            summary = "소재지 정보 미입력 → 수도권(CAPITAL) 간주 (보수적 적용)";
            log.warn("[M3-03] 소재지 정보 없음 - 수도권 간주 처리, reqId={}", basic.getReqId());
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("capitalZone", capitalZone);
        result.put("summary", summary);
        return result;
    }

    /**
     * 주소 문자열이 수도권(서울, 인천, 경기)에 해당하는지 판별한다.
     *
     * @param address 주소 문자열
     * @return 수도권이면 true
     */
    private boolean isCapitalArea(String address) {
        if (address == null) {
            return false;
        }
        String trimmed = address.trim();
        return trimmed.startsWith("서울")
                || trimmed.startsWith("인천")
                || trimmed.startsWith("경기")
                || trimmed.contains("서울특별시")
                || trimmed.contains("인천광역시")
                || trimmed.contains("경기도");
    }

    // =========================================================================
    // M3-04: 상시근로자 수 산정
    // =========================================================================

    /**
     * 상시근로자 수를 산정하고 OUT_EMPLOYEE_SUMMARY 에 저장한다.
     *
     * <p>
     * <b>제외 대상 (조특법 시행령 제23조):</b>
     * <ul>
     *   <li>최대주주/특수관계인(배우자, 직계존비속)</li>
     *   <li>등기임원, 감사</li>
     *   <li>월 근로시간 60시간 미만인 단시간 근로자</li>
     *   <li>근로계약기간 1년 미만인 기간제 근로자 (단, 연속갱신 합산 ≥ 1년이면 포함)</li>
     *   <li>파견근로자</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>연평균 산정:</b> TRUNCATE(∑월별인원 / 사업연도월수, 2)
     * </p>
     *
     * <p>
     * <b>청년 판단 기준:</b>
     * <ul>
     *   <li>≤ 2024년 귀속: 과세연도 말일 기준 만 나이</li>
     *   <li>≥ 2025년 귀속: 근로계약 체결일 기준 만 나이</li>
     *   <li>만 34세 이하 + 군복무 기간(최대 6년) 가산 → 청년</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>청년등 판단:</b> 장애인, 60세 이상 고령자, 경력단절여성, 북한이탈주민
     * </p>
     *
     * @param reqId 요청 ID
     * @param basic 기본 정보 엔티티
     * @return 산정 결과 맵 (summary, currentTotal, priorTotal)
     */
    private Map<String, Object> calculateRegularEmployees(String reqId, InpBasic basic) {
        log.debug("[M3-04] 상시근로자 산정 시작 - reqId={}", reqId);

        List<InpEmployee> employees = inpEmployeeRepository.findByReqId(reqId);
        BigDecimal currentTotal = BigDecimal.ZERO;
        BigDecimal priorTotal = BigDecimal.ZERO;
        StringBuilder summaryBuilder = new StringBuilder();

        for (InpEmployee emp : employees) {
            String yearType = emp.getYearType();

            // ── 제외 인원 제거 후 상시근로자 산정 ────────────────────
            // INP_EMPLOYEE 에는 이미 집계된 값이 들어오므로,
            // totalRegular, excludedCount 를 활용하여 검증한다.
            BigDecimal totalRegular = emp.getTotalRegular();
            if (totalRegular == null) {
                totalRegular = BigDecimal.ZERO;
            }

            // TRUNCATE(연평균, 2) 적용
            totalRegular = TruncationUtil.truncateBigDecimal(totalRegular, EMPLOYEE_AVG_SCALE);

            // 청년등 인원 집계
            int youthCount = safeInt(emp.getYouthCount());
            int disabledCount = safeInt(emp.getDisabledCount());
            int agedCount = safeInt(emp.getAgedCount());
            int careerBreakCount = safeInt(emp.getCareerBreakCount());
            int northDefectorCount = safeInt(emp.getNorthDefectorCount());
            int generalCount = safeInt(emp.getGeneralCount());
            int excludedCount = safeInt(emp.getExcludedCount());

            // 청년등 = 청년 + 장애인 + 60세이상 + 경력단절여성 + 북한이탈주민
            int youthEtcCount = youthCount + disabledCount + agedCount
                    + careerBreakCount + northDefectorCount;

            // OUT_EMPLOYEE_SUMMARY 저장
            OutEmployeeSummary empSummary = OutEmployeeSummary.builder()
                    .reqId(reqId)
                    .yearType(yearType)
                    .totalRegular(totalRegular)
                    .youthCount(youthEtcCount)
                    .generalCount(generalCount)
                    .increaseTotal(null)   // 증감 산정은 이후 단계에서
                    .increaseYouth(null)
                    .increaseGeneral(null)
                    .excludedCount(excludedCount)
                    .calcDetail(buildEmployeeCalcDetail(emp, youthEtcCount))
                    .build();
            outEmployeeSummaryRepository.save(empSummary);

            if ("CURRENT".equals(yearType)) {
                currentTotal = totalRegular;
            } else if ("PRIOR".equals(yearType) || "PREV1".equals(yearType)) {
                priorTotal = totalRegular;
            }

            summaryBuilder.append(String.format("[%s] 상시=%s명, 청년등=%d명, 일반=%d명, 제외=%d명; ",
                    yearType, totalRegular.toPlainString(), youthEtcCount, generalCount, excludedCount));
        }

        // 증감 계산: 당기 - 직전
        if (currentTotal.compareTo(BigDecimal.ZERO) > 0
                || priorTotal.compareTo(BigDecimal.ZERO) > 0) {
            summaryBuilder.append(String.format("증감=당기(%s)-직전(%s)=%s명",
                    currentTotal.toPlainString(),
                    priorTotal.toPlainString(),
                    currentTotal.subtract(priorTotal).toPlainString()));
        }

        if (employees.isEmpty()) {
            summaryBuilder.append("고용 정보 미입력 (상시근로자 0명 간주)");
            log.warn("[M3-04] 고용 정보 없음 - reqId={}", reqId);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("summary", summaryBuilder.toString());
        result.put("currentTotal", currentTotal);
        result.put("priorTotal", priorTotal);
        return result;
    }

    /**
     * 상시근로자 산정 상세 내역 문자열을 생성한다.
     *
     * @param emp            직원 입력 데이터
     * @param youthEtcCount  청년등 합산 인원 수
     * @return 산정 상세 문자열
     */
    private String buildEmployeeCalcDetail(InpEmployee emp, int youthEtcCount) {
        return String.format(
                "{ \"yearType\": \"%s\", \"totalRegular\": %s, "
                        + "\"youth\": %d, \"disabled\": %d, \"aged\": %d, "
                        + "\"careerBreak\": %d, \"northDefector\": %d, "
                        + "\"youthEtcTotal\": %d, \"general\": %d, \"excluded\": %d, "
                        + "\"totalSalary\": %d, \"socialInsurance\": %d }",
                emp.getYearType(),
                emp.getTotalRegular() != null ? emp.getTotalRegular().toPlainString() : "0",
                safeInt(emp.getYouthCount()),
                safeInt(emp.getDisabledCount()),
                safeInt(emp.getAgedCount()),
                safeInt(emp.getCareerBreakCount()),
                safeInt(emp.getNorthDefectorCount()),
                youthEtcCount,
                safeInt(emp.getGeneralCount()),
                safeInt(emp.getExcludedCount()),
                safeLong(emp.getTotalSalary()),
                safeLong(emp.getSocialInsurancePaid()));
    }

    // =========================================================================
    // M3-05: 벤처기업 확인
    // =========================================================================

    /**
     * 벤처기업 확인 여부를 판정한다.
     *
     * <p>
     * INP_BASIC.ventureYn 필드를 기반으로 벤처기업 확인 상태를 반환한다.
     * 벤처기업은 「벤처기업육성에 관한 특별조치법」 제25조에 따라
     * 벤처기업확인서를 발급받은 기업을 의미한다.
     * </p>
     *
     * <p>
     * <b>벤처기업 확인이 필요한 공제/감면:</b>
     * <ul>
     *   <li>제6조 창업중소기업 감면 (벤처 가산 혜택)</li>
     *   <li>제10조 R&D 세액공제 (벤처 우대율)</li>
     * </ul>
     * </p>
     *
     * @param basic 기본 정보 엔티티
     * @return 판정 결과 맵 (ventureConfirmed, summary)
     */
    private Map<String, Object> confirmVentureStatus(InpBasic basic) {
        Boolean ventureYn = basic.getVentureYn();
        boolean confirmed = (ventureYn != null && ventureYn);

        String summary;
        if (confirmed) {
            summary = "벤처기업확인서 보유 (벤처 우대 혜택 적용 가능)";
        } else {
            summary = "벤처기업 미확인 (일반 기업 기준 적용)";
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("ventureConfirmed", confirmed);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // M3-06: 결산확정 원칙 검증
    // =========================================================================

    /**
     * 결산확정 원칙을 검증한다.
     *
     * <p>
     * INP_DEDUCTION 에 포함된 항목을 분류하여 결산조정 vs 신고조정을 판단한다.
     * </p>
     *
     * <p>
     * <b>결산조정 항목 (BLOCKED):</b>
     * 감가상각비추가, 퇴직급여충당금추가, 대손충당금추가 등
     * → 결산서(재무제표) 수정이 선행되어야 하며, 경정청구만으로 불가
     * </p>
     *
     * <p>
     * <b>신고조정 항목 (ELIGIBLE):</b>
     * 세액감면신규, 세액공제추가, R&D방식변경 등
     * → 신고서 수정만으로 경정청구 가능
     * </p>
     *
     * <p>
     * <b>비고:</b> M3-00 에서 이미 Hard Fail 검증을 통과했으므로,
     * M3-06 에서는 BLOCKED 가 나오지 않는 것이 정상이다.
     * 그러나 방어적 프로그래밍을 위해 이중 검증을 수행한다.
     * </p>
     *
     * @param reqId 요청 ID
     * @return 검증 결과 맵 (checkResult, blockedItems, summary)
     */
    private Map<String, Object> verifySettlementPrinciple(String reqId) {
        log.debug("[M3-06] 결산확정 원칙 검증 시작 - reqId={}", reqId);

        List<InpDeduction> deductions = inpDeductionRepository.findByReqId(reqId);
        List<String> settlementItems = new ArrayList<String>();  // 결산조정 항목
        List<String> filingItems = new ArrayList<String>();      // 신고조정 항목

        for (InpDeduction deduction : deductions) {
            String subDetail = deduction.getSubDetail();
            String method = deduction.getMethod();
            String combined = ((subDetail != null) ? subDetail : "")
                    + ((method != null) ? method : "");

            // 결산조정 항목 판별
            boolean isSettlement = false;
            for (String keyword : SETTLEMENT_ADJUSTMENT_KEYWORDS) {
                if (combined.contains(keyword)) {
                    settlementItems.add(String.format("%s/%s: %s",
                            deduction.getItemCategory(), deduction.getProvision(), keyword));
                    isSettlement = true;
                    break;
                }
            }

            // 신고조정 항목 판별
            if (!isSettlement) {
                for (String keyword : FILING_ADJUSTMENT_KEYWORDS) {
                    if (combined.contains(keyword)) {
                        filingItems.add(String.format("%s/%s: %s",
                                deduction.getItemCategory(), deduction.getProvision(), keyword));
                        break;
                    }
                }
            }
        }

        String checkResult;
        String blockedItemsStr;
        String summary;

        if (!settlementItems.isEmpty()) {
            // 결산조정 항목 존재 → BLOCKED (방어적 이중검증)
            checkResult = "BLOCKED";
            blockedItemsStr = joinStrings(settlementItems);
            summary = String.format("결산조정 %d건 검출 (BLOCKED): %s / 신고조정 %d건: %s",
                    settlementItems.size(), blockedItemsStr,
                    filingItems.size(), filingItems.isEmpty() ? "없음" : joinStrings(filingItems));
            log.warn("[M3-06] 결산조정 항목 검출 - reqId={}, items={}", reqId, blockedItemsStr);
        } else {
            checkResult = "ELIGIBLE";
            blockedItemsStr = null;
            summary = String.format("신고조정 %d건 확인 (ELIGIBLE): %s",
                    filingItems.size(), filingItems.isEmpty() ? "해당 없음" : joinStrings(filingItems));
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("checkResult", checkResult);
        result.put("blockedItems", blockedItemsStr);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // P3-01 ~ P3-03: 개인사업자 추가 점검
    // =========================================================================

    /**
     * 개인사업자(INC) 전용 추가 점검을 실행한다.
     *
     * <p>
     * <b>P3-01:</b> 기장의무 점검 (복식부기/간편장부)
     * → 복식부기 의무자가 간편장부로 신고한 경우 감면 제한
     * </p>
     *
     * <p>
     * <b>P3-02:</b> 성실신고확인 대상 점검
     * → 성실신고확인 대상자의 미확인 시 가산세 및 공제 제한
     * </p>
     *
     * <p>
     * <b>P3-03:</b> 추계신고 검증
     * → 추계(推計) 신고자는 세액공제/감면 적용 불가
     * </p>
     *
     * @param reqId             요청 ID
     * @param basic             기본 정보
     * @param inspectionLogs    점검 로그 리스트 (누적 추가)
     * @param sortOrder         현재 정렬 순서
     * @param now               점검 시각
     * @param diagnosisBuilder  진단 상세 빌더
     * @return 갱신된 정렬 순서
     */
    private int executeIndividualChecks(String reqId, InpBasic basic,
                                        List<ChkInspectionLog> inspectionLogs,
                                        int sortOrder, LocalDateTime now,
                                        StringBuilder diagnosisBuilder) {
        log.info("[STEP0] 개인사업자 추가 점검 시작 - reqId={}", reqId);

        // ────────────────────────────────────────────────────────────
        // P3-01: 기장의무 점검
        // ────────────────────────────────────────────────────────────
        sortOrder++;
        String bookkeepingType = basic.getBookkeepingType();
        String p301Judgment;
        String p301Summary;

        if (bookkeepingType == null || bookkeepingType.trim().isEmpty()) {
            p301Judgment = "WARNING";
            p301Summary = "기장유형 미입력 → 복식부기 의무자로 간주";
            log.warn("[P3-01] 기장유형 미입력 - reqId={}", reqId);
        } else if ("복식부기".equals(bookkeepingType.trim())
                || "DOUBLE_ENTRY".equalsIgnoreCase(bookkeepingType.trim())) {
            p301Judgment = "PASS";
            p301Summary = "복식부기 기장 확인 → 감면 적용 가능";
        } else if ("간편장부".equals(bookkeepingType.trim())
                || "SIMPLE".equalsIgnoreCase(bookkeepingType.trim())) {
            // 간편장부 의무자 여부 확인 (매출 기준)
            Long revenue = basic.getRevenue();
            long revenueVal = (revenue != null) ? revenue : 0L;
            // 복식부기 의무 기준: 업종별 상이하나, 보수적으로 직전연도 수입금액 7,500만원 이상
            if (revenueVal >= 75000000L) {
                p301Judgment = "WARNING";
                p301Summary = String.format("간편장부 신고(수입=%,d원) → 복식부기 의무 가능성, 감면 제한 가능", revenueVal);
                diagnosisBuilder.append("[P3-01] 간편장부 의무위반 가능; ");
            } else {
                p301Judgment = "PASS";
                p301Summary = String.format("간편장부 적정(수입=%,d원) → 간편장부 의무자 해당", revenueVal);
            }
        } else {
            p301Judgment = "WARNING";
            p301Summary = "기장유형 식별 불가: " + bookkeepingType;
        }

        inspectionLogs.add(buildInspectionLog(reqId, "P3-01",
                "기장의무 점검", "소득세법 제160조",
                p301Judgment, p301Summary, sortOrder, now));
        log.info("[P3-01] 기장의무={} - reqId={}", p301Judgment, reqId);

        // ────────────────────────────────────────────────────────────
        // P3-02: 성실신고확인 대상 점검
        // ────────────────────────────────────────────────────────────
        sortOrder++;
        Boolean sincerityTarget = basic.getSincerityTarget();
        String p302Judgment;
        String p302Summary;

        if (sincerityTarget != null && sincerityTarget) {
            // 성실신고확인 대상자
            p302Judgment = "ATTENTION";
            p302Summary = "성실신고확인 대상자 → 확인서 미제출 시 가산세 및 공제 제한. 법정신고기한 6.30 적용 확인 필요";
            diagnosisBuilder.append("[P3-02] 성실신고확인 대상자; ");
        } else {
            p302Judgment = "PASS";
            p302Summary = "성실신고확인 대상 아님";
        }

        inspectionLogs.add(buildInspectionLog(reqId, "P3-02",
                "성실신고확인 대상 점검", "소득세법 제70조의2",
                p302Judgment, p302Summary, sortOrder, now));
        log.info("[P3-02] 성실신고={} - reqId={}", p302Judgment, reqId);

        // ────────────────────────────────────────────────────────────
        // P3-03: 추계신고 검증
        // ────────────────────────────────────────────────────────────
        sortOrder++;
        String p303Judgment;
        String p303Summary;

        // 추계 여부 판단: 기장유형이 '추계' 또는 과세표준 대비 수입금액 비율로 추정
        if (bookkeepingType != null
                && (bookkeepingType.contains("추계") || "ESTIMATE".equalsIgnoreCase(bookkeepingType.trim()))) {
            p303Judgment = "BLOCKED";
            p303Summary = "추계신고자 → 세액공제/감면 적용 불가 (소득세법 제80조)";
            diagnosisBuilder.append("[P3-03] 추계신고 → 공제/감면 불가; ");
        } else {
            p303Judgment = "PASS";
            p303Summary = "장부 기반 신고 확인 → 세액공제/감면 적용 가능";
        }

        inspectionLogs.add(buildInspectionLog(reqId, "P3-03",
                "추계신고 검증", "소득세법 제80조/조특법 제128조",
                p303Judgment, p303Summary, sortOrder, now));
        log.info("[P3-03] 추계신고={} - reqId={}", p303Judgment, reqId);

        return sortOrder;
    }

    // =========================================================================
    // 내부 헬퍼 메서드
    // =========================================================================

    /**
     * 요청 ID 로 기본 정보(INP_BASIC)를 조회한다.
     *
     * @param reqId 요청 ID
     * @return InpBasic 엔티티
     * @throws TaxServiceException 기본 정보가 존재하지 않는 경우
     */
    private InpBasic loadBasicInfo(String reqId) {
        return inpBasicRepository.findByReqId(reqId)
                .orElseThrow(new java.util.function.Supplier<TaxServiceException>() {
                    @Override
                    public TaxServiceException get() {
                        log.error("[STEP0] 기본 정보 없음 - reqId={}", reqId);
                        return new TaxServiceException(
                                ErrorCode.REQUEST_NOT_FOUND,
                                "기본 정보(INP_BASIC)를 찾을 수 없습니다: " + reqId,
                                reqId);
                    }
                });
    }

    /**
     * 이전 사전점검 결과를 삭제한다 (재시도/재실행 대비).
     *
     * <p>
     * 같은 reqId 로 기존에 저장된 CHK_ELIGIBILITY, CHK_INSPECTION_LOG,
     * OUT_EMPLOYEE_SUMMARY 를 모두 삭제하여 깨끗한 상태에서 재점검한다.
     * </p>
     *
     * @param reqId 요청 ID
     */
    private void clearPreviousResults(String reqId) {
        log.debug("[STEP0] 기존 결과 초기화 - reqId={}", reqId);
        chkEligibilityRepository.deleteByReqId(reqId);
        chkInspectionLogRepository.deleteByReqId(reqId);
        outEmployeeSummaryRepository.deleteByReqId(reqId);
    }

    /**
     * CHK_INSPECTION_LOG 엔티티를 생성한다.
     *
     * @param reqId          요청 ID
     * @param inspectionCode 점검 코드 (M3-00, M3-01, ..., P3-01, ...)
     * @param name           점검 항목명
     * @param legalBasis     법적 근거
     * @param judgment       판정 결과 (PASS, ELIGIBLE, INELIGIBLE, BLOCKED, WARNING, ATTENTION, CALCULATED)
     * @param summary        요약
     * @param sortOrder      정렬 순서
     * @param checkedAt      점검 시각
     * @return ChkInspectionLog 엔티티
     */
    private ChkInspectionLog buildInspectionLog(String reqId, String inspectionCode,
                                                 String name, String legalBasis,
                                                 String judgment, String summary,
                                                 int sortOrder, LocalDateTime checkedAt) {
        return ChkInspectionLog.builder()
                .reqId(reqId)
                .inspectionCode(inspectionCode)
                .inspectionName(name)
                .legalBasis(legalBasis)
                .judgment(judgment)
                .summary(summary)
                .relatedModule(RELATED_MODULE)
                .calculatedAmount(null)
                .sortOrder(sortOrder)
                .checkedAt(checkedAt)
                .build();
    }

    /**
     * 감사추적 로그(LOG_CALCULATION)를 기록한다.
     *
     * @param reqId        요청 ID
     * @param calcStep     계산 단계
     * @param functionName 함수명
     * @param inputData    입력 데이터 요약
     * @param outputData   출력 데이터 요약
     * @param legalBasis   법적 근거
     * @param startTime    시작 시각 (밀리초)
     */
    private void writeCalcLog(String reqId, String calcStep, String functionName,
                               String inputData, String outputData,
                               String legalBasis, long startTime) {
        int durationMs = (int) (System.currentTimeMillis() - startTime);
        LogCalculation logEntry = LogCalculation.builder()
                .reqId(reqId)
                .calcStep(calcStep)
                .functionName(functionName)
                .inputData(inputData)
                .outputData(outputData)
                .legalBasis(legalBasis)
                .executedAt(LocalDateTime.now())
                .logLevel("INFO")
                .executedBy("PreCheckService")
                .durationMs(durationMs)
                .build();
        logCalculationRepository.save(logEntry);
    }

    /**
     * 문자열 리스트를 쉼표로 연결한다.
     *
     * @param items 문자열 리스트
     * @return 쉼표 구분 문자열
     */
    private String joinStrings(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    /**
     * null-safe Integer → int 변환 (null 이면 0 반환).
     *
     * @param value Integer 값
     * @return int 값 (null 시 0)
     */
    private int safeInt(Integer value) {
        return (value != null) ? value : 0;
    }

    /**
     * null-safe Long → long 변환 (null 이면 0L 반환).
     *
     * @param value Long 값
     * @return long 값 (null 시 0L)
     */
    private long safeLong(Long value) {
        return (value != null) ? value : 0L;
    }
}
