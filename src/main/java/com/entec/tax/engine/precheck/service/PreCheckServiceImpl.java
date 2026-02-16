package com.entec.tax.engine.precheck.service;

import com.entec.tax.common.exception.ErrorCode;
import com.entec.tax.common.exception.HardFailException;
import com.entec.tax.common.exception.RequestNotFoundException;
import com.entec.tax.common.util.DateUtil;
import com.entec.tax.common.util.TruncationUtil;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * M3 사전점검 서비스 구현체.
 * <p>
 * 세액공제 환급 분석 전 사전점검(Pre-Check)을 수행한다.
 * 결산조정 차단(Hard-Fail), 기한 적격, 중소기업 판정, 수도권 구분,
 * 상시근로자 산정, 결산확정 검증 등을 순차적으로 실행하고
 * 결과를 CHK_ELIGIBILITY / CHK_INSPECTION_LOG 테이블에 저장한다.
 * </p>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PreCheckServiceImpl implements PreCheckService {

    // ── 결산조정 차단(Hard-Fail) 대상 항목 카테고리 ──────────────────────
    /** 감가상각비추가 */
    private static final String SETTLEMENT_DEPRECIATION = "감가상각비추가";
    /** 퇴직급여충당금추가 */
    private static final String SETTLEMENT_RETIREMENT = "퇴직급여충당금추가";
    /** 대손충당금추가 */
    private static final String SETTLEMENT_BAD_DEBT = "대손충당금추가";

    /** 결산조정 차단 대상 항목 목록 */
    private static final List<String> HARD_FAIL_ITEMS = Arrays.asList(
            SETTLEMENT_DEPRECIATION,
            SETTLEMENT_RETIREMENT,
            SETTLEMENT_BAD_DEBT
    );

    // ── 점검 코드 상수 ─────────────────────────────────────────────────
    private static final String INSP_M3_00 = "M3-00";
    private static final String INSP_M3_01 = "M3-01";
    private static final String INSP_M3_02 = "M3-02";
    private static final String INSP_M3_03 = "M3-03";
    private static final String INSP_M3_04 = "M3-04";
    private static final String INSP_M3_06 = "M3-06";

    // ── 판정 결과 상수 ─────────────────────────────────────────────────
    private static final String JUDGMENT_PASS = "PASS";
    private static final String JUDGMENT_FAIL = "FAIL";
    private static final String JUDGMENT_WARN = "WARN";
    private static final String JUDGMENT_INFO = "INFO";

    // ── 연도 구분 상수 ─────────────────────────────────────────────────
    private static final String YEAR_TYPE_CURRENT = "CURRENT";
    private static final String YEAR_TYPE_PRIOR = "PREV1";

    // ── 기업 규모 상수 ─────────────────────────────────────────────────
    private static final String SIZE_SME = "중소";
    private static final String SIZE_SMALL = "소기업";
    private static final String SIZE_MEDIUM = "중기업";
    private static final String SIZE_MID = "중견";
    private static final String SIZE_LARGE = "대";

    // ── 수도권 구분 상수 ───────────────────────────────────────────────
    private static final String ZONE_CAPITAL = "수도권";
    private static final String ZONE_NON_CAPITAL = "비수도권";
    private static final String ZONE_OVERPOPULATION = "과밀억제권역";

    // ── 종합 상태 상수 ─────────────────────────────────────────────────
    private static final String STATUS_ELIGIBLE = "ELIGIBLE";
    private static final String STATUS_INELIGIBLE = "INELIGIBLE";
    private static final String STATUS_CONDITIONAL = "CONDITIONAL";

    // ── 관련 모듈 상수 ─────────────────────────────────────────────────
    private static final String MODULE_PRECHECK = "M3_PRECHECK";

    // ── 리포지토리 의존성 ──────────────────────────────────────────────
    private final InpBasicRepository inpBasicRepository;
    private final InpEmployeeRepository inpEmployeeRepository;
    private final InpDeductionRepository inpDeductionRepository;
    private final ChkEligibilityRepository chkEligibilityRepository;
    private final ChkInspectionLogRepository chkInspectionLogRepository;
    private final OutEmployeeSummaryRepository outEmployeeSummaryRepository;
    private final LogCalculationRepository logCalculationRepository;

    /**
     * {@inheritDoc}
     *
     * <p>M3 사전점검을 순차적으로 실행한다.</p>
     * <ol>
     *   <li>M3-00: 결산조정 차단(Hard-Fail) 검사</li>
     *   <li>M3-01: 경정청구기한 적격 검사</li>
     *   <li>M3-02: 중소기업 적격 판정</li>
     *   <li>M3-03: 수도권 구분 판정</li>
     *   <li>M3-04: 상시근로자 산정</li>
     *   <li>M3-06: 결산확정 검증</li>
     * </ol>
     */
    @Override
    public void executePreCheck(String reqId) {
        log.info("[M3 사전점검] 시작 - reqId: {}", reqId);
        long startTime = System.currentTimeMillis();

        // ── 기본 정보 조회 ─────────────────────────────────────────────
        InpBasic basic = inpBasicRepository.findByReqId(reqId)
                .orElseThrow(() -> new RequestNotFoundException(
                        "요청 기본정보를 찾을 수 없습니다. reqId=" + reqId, reqId));

        // ── 공제/감면 항목 조회 ────────────────────────────────────────
        List<InpDeduction> deductions = inpDeductionRepository.findByReqId(reqId);

        // ── 점검 로그 수집 리스트 ──────────────────────────────────────
        List<ChkInspectionLog> inspectionLogs = new ArrayList<ChkInspectionLog>();
        int sortOrder = 0;
        LocalDateTime now = LocalDateTime.now();

        // ================================================================
        // M3-00: 결산조정 차단(Hard-Fail) 검사
        // ================================================================
        sortOrder++;
        List<String> blockedItems = checkHardFail(deductions);
        if (!blockedItems.isEmpty()) {
            // 차단 항목 발견 시 점검 로그 저장 후 예외 발생
            ChkInspectionLog hardFailLog = ChkInspectionLog.builder()
                    .reqId(reqId)
                    .inspectionCode(INSP_M3_00)
                    .inspectionName("결산조정 차단(Hard-Fail) 검사")
                    .legalBasis("법인세법 시행령")
                    .judgment(JUDGMENT_FAIL)
                    .summary("결산조정 차단 항목 발견: " + joinStrings(blockedItems))
                    .relatedModule(MODULE_PRECHECK)
                    .sortOrder(sortOrder)
                    .checkedAt(now)
                    .build();
            chkInspectionLogRepository.save(hardFailLog);

            // 계산 로그 기록
            saveCalculationLog(reqId, INSP_M3_00, "checkHardFail",
                    "deductions.size=" + deductions.size(),
                    "HARD_FAIL: " + joinStrings(blockedItems),
                    "법인세법 시행령", startTime);

            log.error("[M3-00] 결산조정 차단 항목 발견 - reqId: {}, blockedItems: {}", reqId, blockedItems);
            throw new HardFailException(
                    "결산조정 차단 항목이 존재하여 분석을 진행할 수 없습니다: " + joinStrings(blockedItems),
                    reqId,
                    blockedItems);
        }

        // M3-00 통과 로그
        inspectionLogs.add(ChkInspectionLog.builder()
                .reqId(reqId)
                .inspectionCode(INSP_M3_00)
                .inspectionName("결산조정 차단(Hard-Fail) 검사")
                .legalBasis("법인세법 시행령")
                .judgment(JUDGMENT_PASS)
                .summary("결산조정 차단 항목 없음")
                .relatedModule(MODULE_PRECHECK)
                .sortOrder(sortOrder)
                .checkedAt(now)
                .build());
        log.info("[M3-00] 결산조정 차단 검사 통과 - reqId: {}", reqId);

        // ================================================================
        // M3-01: 경정청구기한 적격 검사
        // filing_deadline + 5년 >= 오늘
        // ================================================================
        sortOrder++;
        LocalDate filingDeadline = DateUtil.getFilingDeadline(basic.getTaxYear(), basic.getTaxType());
        LocalDate claimDeadline = DateUtil.getClaimDeadline(filingDeadline);
        LocalDate today = LocalDate.now();
        boolean deadlineEligible = !today.isAfter(claimDeadline);
        String deadlineJudgment = deadlineEligible ? JUDGMENT_PASS : JUDGMENT_FAIL;

        inspectionLogs.add(ChkInspectionLog.builder()
                .reqId(reqId)
                .inspectionCode(INSP_M3_01)
                .inspectionName("경정청구기한 적격 검사")
                .legalBasis("국세기본법 제45조의2")
                .judgment(deadlineJudgment)
                .summary("법정신고기한: " + DateUtil.formatDate(filingDeadline)
                        + ", 청구기한: " + DateUtil.formatDate(claimDeadline)
                        + ", 오늘: " + DateUtil.formatDate(today)
                        + ", 적격: " + (deadlineEligible ? "Y" : "N"))
                .relatedModule(MODULE_PRECHECK)
                .sortOrder(sortOrder)
                .checkedAt(now)
                .build());
        log.info("[M3-01] 경정청구기한 검사 - reqId: {}, filingDeadline: {}, claimDeadline: {}, eligible: {}",
                reqId, filingDeadline, claimDeadline, deadlineEligible);

        // ================================================================
        // M3-02: 중소기업 적격 판정
        // INP_BASIC.corp_size 기반으로 기업 규모 판정
        // ================================================================
        sortOrder++;
        String corpSize = basic.getCorpSize();
        String companySize = determineCompanySize(corpSize);
        boolean smeEligible = isSmeEligible(companySize);
        String smallVsMedium = determineSmallVsMedium(corpSize);
        String smeJudgment = smeEligible ? JUDGMENT_PASS : JUDGMENT_INFO;

        inspectionLogs.add(ChkInspectionLog.builder()
                .reqId(reqId)
                .inspectionCode(INSP_M3_02)
                .inspectionName("중소기업 적격 판정")
                .legalBasis("조세특례제한법 제2조, 중소기업기본법 제2조")
                .judgment(smeJudgment)
                .summary("법인 규모: " + corpSize
                        + ", 기업 규모 판정: " + companySize
                        + ", 중소기업 적격: " + (smeEligible ? "Y" : "N")
                        + ", 소/중 구분: " + smallVsMedium)
                .relatedModule(MODULE_PRECHECK)
                .sortOrder(sortOrder)
                .checkedAt(now)
                .build());
        log.info("[M3-02] 중소기업 판정 - reqId: {}, corpSize: {}, companySize: {}, smeEligible: {}",
                reqId, corpSize, companySize, smeEligible);

        // ================================================================
        // M3-03: 수도권 구분 판정
        // INP_BASIC 정보 기반으로 수도권/비수도권/과밀억제권역 판정
        // ================================================================
        sortOrder++;
        String capitalZone = determineCapitalZone(basic);

        inspectionLogs.add(ChkInspectionLog.builder()
                .reqId(reqId)
                .inspectionCode(INSP_M3_03)
                .inspectionName("수도권 구분 판정")
                .legalBasis("수도권정비계획법 제2조, 조세특례제한법 시행령 제21조")
                .judgment(JUDGMENT_INFO)
                .summary("본점 소재지: " + basic.getHqLocation()
                        + ", 수도권 구분: " + capitalZone)
                .relatedModule(MODULE_PRECHECK)
                .sortOrder(sortOrder)
                .checkedAt(now)
                .build());
        log.info("[M3-03] 수도권 구분 - reqId: {}, capitalZone: {}", reqId, capitalZone);

        // ================================================================
        // M3-04: 상시근로자 산정
        // INP_EMPLOYEE의 CURRENT/PREV1 데이터를 기반으로 산정
        // ================================================================
        sortOrder++;
        List<InpEmployee> employees = inpEmployeeRepository.findByReqId(reqId);
        processEmployeeSummary(reqId, employees, inspectionLogs, sortOrder, now);
        log.info("[M3-04] 상시근로자 산정 완료 - reqId: {}", reqId);

        // ================================================================
        // M3-06: 결산확정 검증
        // INP_DEDUCTION 항목 중 결산확정 필요 항목 검사
        // ================================================================
        sortOrder++;
        String settlementResult = checkSettlement(deductions);
        String settlementBlockedItems = findSettlementBlockedItems(deductions);
        boolean hasSettlementIssue = !"CLEAR".equals(settlementResult);
        String settlementJudgment = hasSettlementIssue ? JUDGMENT_WARN : JUDGMENT_PASS;

        inspectionLogs.add(ChkInspectionLog.builder()
                .reqId(reqId)
                .inspectionCode(INSP_M3_06)
                .inspectionName("결산확정 검증")
                .legalBasis("법인세법 제40조, 법인세법 시행령")
                .judgment(settlementJudgment)
                .summary("결산확정 결과: " + settlementResult
                        + (hasSettlementIssue ? ", 주의항목: " + settlementBlockedItems : ""))
                .relatedModule(MODULE_PRECHECK)
                .sortOrder(sortOrder)
                .checkedAt(now)
                .build());
        log.info("[M3-06] 결산확정 검증 - reqId: {}, result: {}", reqId, settlementResult);

        // ================================================================
        // 종합 상태 판정 및 CHK_ELIGIBILITY 저장
        // ================================================================
        String overallStatus = determineOverallStatus(deadlineEligible, smeEligible, hasSettlementIssue);

        ChkEligibility eligibility = ChkEligibility.builder()
                .reqId(reqId)
                .taxType(basic.getTaxType())
                .companySize(companySize)
                .capitalZone(capitalZone)
                .filingDeadline(filingDeadline)
                .claimDeadline(claimDeadline)
                .deadlineEligible(deadlineEligible ? "Y" : "N")
                .smeEligible(smeEligible ? "Y" : "N")
                .smallVsMedium(smallVsMedium)
                .ventureConfirmed(basic.getVentureYn())
                .settlementCheckResult(settlementResult)
                .settlementBlockedItems(hasSettlementIssue ? settlementBlockedItems : null)
                .sincerityTarget(basic.getSincerityTarget())
                .overallStatus(overallStatus)
                .diagnosisDetail(buildDiagnosisDetail(deadlineEligible, smeEligible,
                        hasSettlementIssue, settlementResult))
                .checkedAt(now)
                .build();

        chkEligibilityRepository.save(eligibility);
        log.info("[M3 사전점검] CHK_ELIGIBILITY 저장 완료 - reqId: {}, overallStatus: {}", reqId, overallStatus);

        // ================================================================
        // CHK_INSPECTION_LOG 일괄 저장
        // ================================================================
        for (ChkInspectionLog inspLog : inspectionLogs) {
            chkInspectionLogRepository.save(inspLog);
        }
        log.info("[M3 사전점검] CHK_INSPECTION_LOG {}건 저장 완료 - reqId: {}", inspectionLogs.size(), reqId);

        // ── 계산 로그 기록 ─────────────────────────────────────────────
        saveCalculationLog(reqId, "M3_PRECHECK", "executePreCheck",
                "taxYear=" + basic.getTaxYear() + ", taxType=" + basic.getTaxType(),
                "overallStatus=" + overallStatus + ", deadlineEligible=" + deadlineEligible
                        + ", smeEligible=" + smeEligible,
                "조세특례제한법, 국세기본법", startTime);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[M3 사전점검] 완료 - reqId: {}, 소요시간: {}ms", reqId, elapsed);
    }

    // ====================================================================
    // M3-00: 결산조정 차단(Hard-Fail) 검사 내부 로직
    // ====================================================================

    /**
     * 결산조정 차단 대상 항목이 INP_DEDUCTION에 존재하는지 검사한다.
     * <p>
     * 감가상각비추가, 퇴직급여충당금추가, 대손충당금추가 항목이
     * itemCategory, provision, subDetail 필드에 포함되어 있으면 차단 대상이다.
     * </p>
     *
     * @param deductions 공제/감면 항목 목록
     * @return 차단 대상 항목명 목록 (비어있으면 차단 없음)
     */
    private List<String> checkHardFail(List<InpDeduction> deductions) {
        List<String> blocked = new ArrayList<String>();

        for (InpDeduction deduction : deductions) {
            String category = deduction.getItemCategory();
            String provision = deduction.getProvision();
            String subDetail = deduction.getSubDetail();

            for (String hardFailItem : HARD_FAIL_ITEMS) {
                // 항목 카테고리, 조항, 세부내역에 결산조정 항목이 포함되어 있는지 확인
                boolean found = (category != null && category.contains(hardFailItem))
                        || (provision != null && provision.contains(hardFailItem))
                        || (subDetail != null && subDetail.contains(hardFailItem));
                if (found && !blocked.contains(hardFailItem)) {
                    blocked.add(hardFailItem);
                }
            }
        }

        return blocked;
    }

    // ====================================================================
    // M3-02: 중소기업 적격 판정 내부 로직
    // ====================================================================

    /**
     * INP_BASIC.corp_size 값으로 기업 규모를 판정한다.
     * <p>
     * 중소, 소기업, 중기업 → 중소기업 (SME)<br>
     * 중견 → 중견기업<br>
     * 대 → 대기업
     * </p>
     *
     * @param corpSize INP_BASIC.corp_size 값
     * @return 기업 규모 문자열
     */
    private String determineCompanySize(String corpSize) {
        if (corpSize == null) {
            log.warn("[M3-02] corp_size가 null입니다. 기본값 '중소'로 판정합니다.");
            return SIZE_SME;
        }

        if (corpSize.contains(SIZE_SMALL) || corpSize.contains(SIZE_MEDIUM) || corpSize.contains(SIZE_SME)) {
            return SIZE_SME;
        } else if (corpSize.contains(SIZE_MID)) {
            return SIZE_MID;
        } else if (corpSize.contains(SIZE_LARGE)) {
            return SIZE_LARGE;
        }

        log.warn("[M3-02] 알 수 없는 corp_size: {}. 기본값 '중소'로 판정합니다.", corpSize);
        return SIZE_SME;
    }

    /**
     * 중소기업 적격 여부를 판정한다.
     *
     * @param companySize 기업 규모 (determineCompanySize 결과)
     * @return 중소기업이면 true
     */
    private boolean isSmeEligible(String companySize) {
        return SIZE_SME.equals(companySize);
    }

    /**
     * 소기업/중기업 구분을 판정한다.
     *
     * @param corpSize INP_BASIC.corp_size 값
     * @return 소기업/중기업 구분 문자열
     */
    private String determineSmallVsMedium(String corpSize) {
        if (corpSize == null) {
            return SIZE_SMALL;
        }

        if (corpSize.contains(SIZE_SMALL)) {
            return SIZE_SMALL;
        } else if (corpSize.contains(SIZE_MEDIUM)) {
            return SIZE_MEDIUM;
        } else if (corpSize.contains(SIZE_SME)) {
            // "중소"만 있는 경우 기본적으로 소기업으로 분류
            return SIZE_SMALL;
        }

        // 중견/대기업은 소/중 구분 해당 없음
        return "N/A";
    }

    // ====================================================================
    // M3-03: 수도권 구분 판정 내부 로직
    // ====================================================================

    /**
     * INP_BASIC에서 수도권 구분을 판정한다.
     * <p>
     * INP_BASIC.capital_zone 값이 이미 설정되어 있으면 그대로 사용하고,
     * 없으면 hq_location 기반으로 수도권 여부를 판단한다.
     * </p>
     *
     * @param basic INP_BASIC 엔티티
     * @return 수도권 구분 (수도권/비수도권/과밀억제권역)
     */
    private String determineCapitalZone(InpBasic basic) {
        // capital_zone 값이 이미 설정되어 있으면 사용
        if (basic.getCapitalZone() != null && !basic.getCapitalZone().trim().isEmpty()) {
            return basic.getCapitalZone();
        }

        // hq_location 기반 수도권 판정
        String location = basic.getHqLocation();
        if (location == null || location.trim().isEmpty()) {
            log.warn("[M3-03] hq_location이 null입니다. 기본값 '수도권'으로 판정합니다. reqId: {}",
                    basic.getReqId());
            return ZONE_CAPITAL;
        }

        // 서울 → 과밀억제권역, 인천/경기 → 수도권, 그 외 → 비수도권
        if (location.contains("서울")) {
            return ZONE_OVERPOPULATION;
        } else if (location.contains("인천") || location.contains("경기")) {
            return ZONE_CAPITAL;
        }

        return ZONE_NON_CAPITAL;
    }

    // ====================================================================
    // M3-04: 상시근로자 산정 내부 로직
    // ====================================================================

    /**
     * INP_EMPLOYEE 데이터를 기반으로 상시근로자를 산정하고 OUT_EMPLOYEE_SUMMARY에 저장한다.
     * <p>
     * CURRENT(당기)와 PREV1(직전)의 고용 데이터를 조회하여
     * 총 인원, 청년등 인원, 일반 인원을 산출하고 증감을 계산한다.
     * </p>
     *
     * @param reqId          요청 ID
     * @param employees      INP_EMPLOYEE 목록
     * @param inspectionLogs 점검 로그 수집 리스트
     * @param sortOrder      정렬 순서
     * @param now            검사 일시
     */
    private void processEmployeeSummary(String reqId, List<InpEmployee> employees,
                                         List<ChkInspectionLog> inspectionLogs,
                                         int sortOrder, LocalDateTime now) {
        // 당기(CURRENT) 고용 데이터 조회
        InpEmployee currentEmp = findEmployeeByYearType(employees, YEAR_TYPE_CURRENT);
        // 직전(PREV1) 고용 데이터 조회
        InpEmployee priorEmp = findEmployeeByYearType(employees, YEAR_TYPE_PRIOR);

        // 당기 인원 산정
        BigDecimal currentTotal = currentEmp != null
                ? safeDecimal(currentEmp.getTotalRegular()) : BigDecimal.ZERO;
        int currentYouth = currentEmp != null ? safeInt(currentEmp.getYouthCount()) : 0;
        int currentGeneral = currentEmp != null ? safeInt(currentEmp.getGeneralCount()) : 0;
        int currentExcluded = currentEmp != null ? safeInt(currentEmp.getExcludedCount()) : 0;

        // 직전 인원 산정
        BigDecimal priorTotal = priorEmp != null
                ? safeDecimal(priorEmp.getTotalRegular()) : BigDecimal.ZERO;
        int priorYouth = priorEmp != null ? safeInt(priorEmp.getYouthCount()) : 0;
        int priorGeneral = priorEmp != null ? safeInt(priorEmp.getGeneralCount()) : 0;
        int priorExcluded = priorEmp != null ? safeInt(priorEmp.getExcludedCount()) : 0;

        // 증가 인원 계산 (당기 - 직전)
        int increaseYouth = currentYouth - priorYouth;
        int increaseGeneral = currentGeneral - priorGeneral;
        int increaseTotal = increaseYouth + increaseGeneral;

        // ── 당기 OUT_EMPLOYEE_SUMMARY 저장 ────────────────────────────
        OutEmployeeSummary currentSummary = OutEmployeeSummary.builder()
                .reqId(reqId)
                .yearType(YEAR_TYPE_CURRENT)
                .totalRegular(currentTotal)
                .youthCount(currentYouth)
                .generalCount(currentGeneral)
                .increaseTotal(increaseTotal)
                .increaseYouth(increaseYouth)
                .increaseGeneral(increaseGeneral)
                .excludedCount(currentExcluded)
                .calcDetail("당기 상시근로자: 총 " + currentTotal
                        + "명, 청년등 " + currentYouth
                        + "명, 일반 " + currentGeneral
                        + "명, 제외 " + currentExcluded + "명")
                .build();
        outEmployeeSummaryRepository.save(currentSummary);

        // ── 직전 OUT_EMPLOYEE_SUMMARY 저장 ────────────────────────────
        OutEmployeeSummary priorSummary = OutEmployeeSummary.builder()
                .reqId(reqId)
                .yearType(YEAR_TYPE_PRIOR)
                .totalRegular(priorTotal)
                .youthCount(priorYouth)
                .generalCount(priorGeneral)
                .increaseTotal(0)
                .increaseYouth(0)
                .increaseGeneral(0)
                .excludedCount(priorExcluded)
                .calcDetail("직전 상시근로자: 총 " + priorTotal
                        + "명, 청년등 " + priorYouth
                        + "명, 일반 " + priorGeneral
                        + "명, 제외 " + priorExcluded + "명")
                .build();
        outEmployeeSummaryRepository.save(priorSummary);

        // ── 점검 로그 추가 ────────────────────────────────────────────
        String empJudgment = increaseTotal >= 0 ? JUDGMENT_PASS : JUDGMENT_WARN;
        long calculatedAmount = TruncationUtil.truncateAmount(
                currentEmp != null && currentEmp.getTotalSalary() != null
                        ? currentEmp.getTotalSalary() : 0L);

        inspectionLogs.add(ChkInspectionLog.builder()
                .reqId(reqId)
                .inspectionCode(INSP_M3_04)
                .inspectionName("상시근로자 산정")
                .legalBasis("조세특례제한법 시행령 제23조, 제26조의8")
                .judgment(empJudgment)
                .summary("당기 총 " + currentTotal + "명 (청년등 " + currentYouth
                        + ", 일반 " + currentGeneral + ")"
                        + " / 직전 총 " + priorTotal + "명 (청년등 " + priorYouth
                        + ", 일반 " + priorGeneral + ")"
                        + " / 증감: 총 " + increaseTotal + "명 (청년등 " + increaseYouth
                        + ", 일반 " + increaseGeneral + ")")
                .relatedModule(MODULE_PRECHECK)
                .calculatedAmount(calculatedAmount)
                .sortOrder(sortOrder)
                .checkedAt(now)
                .build());

        log.info("[M3-04] 상시근로자 산정 - reqId: {}, 당기: {}, 직전: {}, 증가: {}",
                reqId, currentTotal, priorTotal, increaseTotal);
    }

    /**
     * 고용 데이터 목록에서 연도 구분에 해당하는 데이터를 찾는다.
     *
     * @param employees 고용 데이터 목록
     * @param yearType  연도 구분
     * @return 해당 연도 구분의 고용 데이터 (없으면 null)
     */
    private InpEmployee findEmployeeByYearType(List<InpEmployee> employees, String yearType) {
        for (InpEmployee emp : employees) {
            if (yearType.equals(emp.getYearType())) {
                return emp;
            }
        }
        return null;
    }

    // ====================================================================
    // M3-06: 결산확정 검증 내부 로직
    // ====================================================================

    /**
     * INP_DEDUCTION 항목 중 결산확정이 필요한 항목이 있는지 검사한다.
     * <p>
     * 결산확정이 필요한 항목(결산조정 대상)이 존재하면 "SETTLEMENT_REQUIRED",
     * 없으면 "CLEAR"를 반환한다.
     * </p>
     *
     * @param deductions 공제/감면 항목 목록
     * @return 결산확정 결과 ("CLEAR" 또는 "SETTLEMENT_REQUIRED")
     */
    private String checkSettlement(List<InpDeduction> deductions) {
        for (InpDeduction deduction : deductions) {
            if (isSettlementRequiredItem(deduction)) {
                return "SETTLEMENT_REQUIRED";
            }
        }
        return "CLEAR";
    }

    /**
     * 결산확정이 필요한 차단 항목명을 수집한다.
     *
     * @param deductions 공제/감면 항목 목록
     * @return 결산확정 필요 항목 목록 (쉼표 구분 문자열)
     */
    private String findSettlementBlockedItems(List<InpDeduction> deductions) {
        List<String> items = new ArrayList<String>();
        for (InpDeduction deduction : deductions) {
            if (isSettlementRequiredItem(deduction)) {
                String itemDesc = deduction.getItemCategory() + "(" + deduction.getProvision() + ")";
                if (!items.contains(itemDesc)) {
                    items.add(itemDesc);
                }
            }
        }
        return items.isEmpty() ? "" : joinStrings(items);
    }

    /**
     * 해당 공제/감면 항목이 결산확정이 필요한 항목인지 판정한다.
     * <p>
     * existingApplied가 false(미적용)이고 subDetail에 "결산조정" 문구가 포함되어 있거나,
     * method가 "결산조정"인 경우 결산확정 필요 항목으로 판정한다.
     * </p>
     *
     * @param deduction 공제/감면 항목
     * @return 결산확정 필요 항목이면 true
     */
    private boolean isSettlementRequiredItem(InpDeduction deduction) {
        // 기적용된 항목은 결산확정 불필요
        if (Boolean.TRUE.equals(deduction.getExistingApplied())) {
            return false;
        }

        String method = deduction.getMethod();
        String subDetail = deduction.getSubDetail();

        // method가 "결산조정"이면 결산확정 필요
        if (method != null && method.contains("결산조정")) {
            return true;
        }

        // subDetail에 "결산조정" 문구 포함 시 결산확정 필요
        if (subDetail != null && subDetail.contains("결산조정")) {
            return true;
        }

        return false;
    }

    // ====================================================================
    // 종합 상태 판정 로직
    // ====================================================================

    /**
     * 각 점검 결과를 종합하여 최종 상태를 판정한다.
     *
     * @param deadlineEligible    기한 적격 여부
     * @param smeEligible         중소기업 적격 여부
     * @param hasSettlementIssue  결산확정 이슈 여부
     * @return 종합 상태 (ELIGIBLE / INELIGIBLE / CONDITIONAL)
     */
    private String determineOverallStatus(boolean deadlineEligible, boolean smeEligible,
                                           boolean hasSettlementIssue) {
        // 기한 초과 시 부적격
        if (!deadlineEligible) {
            return STATUS_INELIGIBLE;
        }

        // 결산확정 이슈가 있으면 조건부 적격
        if (hasSettlementIssue) {
            return STATUS_CONDITIONAL;
        }

        // 모든 조건 충족 시 적격
        return STATUS_ELIGIBLE;
    }

    /**
     * 진단 상세 내용을 구성한다.
     *
     * @param deadlineEligible    기한 적격 여부
     * @param smeEligible         중소기업 적격 여부
     * @param hasSettlementIssue  결산확정 이슈 여부
     * @param settlementResult    결산확정 결과
     * @return 진단 상세 문자열
     */
    private String buildDiagnosisDetail(boolean deadlineEligible, boolean smeEligible,
                                         boolean hasSettlementIssue, String settlementResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("[경정청구기한] ").append(deadlineEligible ? "적격" : "부적격 - 청구기한 경과");
        sb.append(" | [중소기업] ").append(smeEligible ? "적격" : "비해당 (중견/대기업)");
        sb.append(" | [결산확정] ").append(settlementResult);
        return sb.toString();
    }

    // ====================================================================
    // 계산 로그 저장
    // ====================================================================

    /**
     * LOG_CALCULATION에 계산 단계 로그를 저장한다.
     *
     * @param reqId        요청 ID
     * @param calcStep     계산 단계
     * @param functionName 함수명
     * @param inputData    입력 데이터
     * @param outputData   출력 데이터
     * @param legalBasis   법적 근거
     * @param startTime    시작 시각 (밀리초)
     */
    private void saveCalculationLog(String reqId, String calcStep, String functionName,
                                     String inputData, String outputData,
                                     String legalBasis, long startTime) {
        int durationMs = (int) (System.currentTimeMillis() - startTime);

        LogCalculation calcLog = LogCalculation.builder()
                .reqId(reqId)
                .calcStep(calcStep)
                .functionName(functionName)
                .inputData(inputData)
                .outputData(outputData)
                .legalBasis(legalBasis)
                .executedAt(LocalDateTime.now())
                .logLevel("INFO")
                .executedBy("SYSTEM")
                .durationMs(durationMs)
                .build();

        logCalculationRepository.save(calcLog);
    }

    // ====================================================================
    // 유틸리티 메서드
    // ====================================================================

    /**
     * null-safe BigDecimal 변환.
     *
     * @param value BigDecimal 값 (nullable)
     * @return null이면 BigDecimal.ZERO, 아니면 원래 값
     */
    private BigDecimal safeDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * null-safe Integer 변환.
     *
     * @param value Integer 값 (nullable)
     * @return null이면 0, 아니면 원래 값
     */
    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * 문자열 리스트를 쉼표로 연결한다 (Java 1.8 호환).
     *
     * @param items 문자열 리스트
     * @return 쉼표 구분 문자열
     */
    private String joinStrings(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}
