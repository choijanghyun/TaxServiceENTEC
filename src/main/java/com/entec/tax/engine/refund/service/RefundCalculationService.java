package com.entec.tax.engine.refund.service;

import com.entec.tax.common.constants.RiskLevel;
import com.entec.tax.common.constants.SystemConstants;
import com.entec.tax.common.exception.CalculationException;
import com.entec.tax.common.exception.ErrorCode;
import com.entec.tax.common.util.DateUtil;
import com.entec.tax.common.util.JsonUtil;
import com.entec.tax.common.util.TruncationUtil;
import com.entec.tax.domain.check.entity.ChkEligibility;
import com.entec.tax.domain.check.entity.ChkInspectionLog;
import com.entec.tax.domain.check.repository.ChkEligibilityRepository;
import com.entec.tax.domain.check.repository.ChkInspectionLogRepository;
import com.entec.tax.domain.input.entity.InpBasic;
import com.entec.tax.domain.input.entity.InpFinancial;
import com.entec.tax.domain.input.repository.InpBasicRepository;
import com.entec.tax.domain.input.repository.InpFinancialRepository;
import com.entec.tax.domain.log.entity.LogCalculation;
import com.entec.tax.domain.log.repository.LogCalculationRepository;
import com.entec.tax.domain.output.entity.OutAdditionalCheck;
import com.entec.tax.domain.output.entity.OutCombination;
import com.entec.tax.domain.output.entity.OutCreditDetail;
import com.entec.tax.domain.output.entity.OutRefund;
import com.entec.tax.domain.output.entity.OutRisk;
import com.entec.tax.domain.output.repository.OutAdditionalCheckRepository;
import com.entec.tax.domain.output.repository.OutCombinationRepository;
import com.entec.tax.domain.output.repository.OutCreditDetailRepository;
import com.entec.tax.domain.output.repository.OutRefundRepository;
import com.entec.tax.domain.output.repository.OutRiskRepository;
import com.entec.tax.domain.reference.entity.RefRefundInterestRate;
import com.entec.tax.domain.reference.repository.RefRefundInterestRateRepository;
import com.entec.tax.domain.report.entity.OutReportJson;
import com.entec.tax.domain.report.repository.OutReportJsonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * M6 최종 환급액 산출 서비스 구현체 (STEP 4-5 - 최종 환급액 산출 및 보고서 생성).
 *
 * <p>
 * 최적 조합 탐색(M5) 결과를 기반으로 최종 환급액을 산출하고,
 * 사후관리 리스크를 평가하며, 보고서 JSON을 생성하여 저장한다.
 * </p>
 *
 * <h3>주요 처리 단계</h3>
 * <ul>
 *   <li><b>M6-01:</b> 최종 환급액 산출 (기존납부세액 - 경정후결정세액)</li>
 *   <li><b>M6-02:</b> 환급가산금 산출 (본세 + 중간예납 별도 기산, 기간별 변동이율)</li>
 *   <li><b>M6-03:</b> 지방소득세 환급 (국세 × 10%)</li>
 *   <li><b>M6-04:</b> 사후관리 리스크 평가 (고용유지, 자산처분, 감가상각)</li>
 *   <li><b>M6-05:</b> 보고서 JSON 직렬화 (7섹션 A~G 생성, OUT_REPORT_JSON 저장)</li>
 * </ul>
 *
 * <h3>핵심 비즈니스 규칙</h3>
 * <ul>
 *   <li>환급가산금 = TRUNCATE(환급액 × 이율 × 일수 / 365, 0) - 1원미만 절사</li>
 *   <li>중간예납 환급가산금: 각 납부건별 별도 기산</li>
 *   <li>총수령예상액 = 환급액 + 가산금 + 지방소득세환급</li>
 *   <li>리스크: 고용유지실패(§29의8, 2년유지), 자산조기처분(§146, 2년이내), 감가상각의무미이행(§128⑨)</li>
 * </ul>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefundCalculationService {

    // ──────────────────────────────────────────────
    // 의존성 주입 (생성자 주입 via @RequiredArgsConstructor)
    // ──────────────────────────────────────────────

    private final InpBasicRepository inpBasicRepository;
    private final InpFinancialRepository inpFinancialRepository;
    private final ChkEligibilityRepository chkEligibilityRepository;
    private final ChkInspectionLogRepository chkInspectionLogRepository;
    private final OutCombinationRepository outCombinationRepository;
    private final OutCreditDetailRepository outCreditDetailRepository;
    private final OutRefundRepository outRefundRepository;
    private final OutRiskRepository outRiskRepository;
    private final OutAdditionalCheckRepository outAdditionalCheckRepository;
    private final OutReportJsonRepository outReportJsonRepository;
    private final LogCalculationRepository logCalculationRepository;
    private final RefRefundInterestRateRepository refRefundInterestRateRepository;
    private final ObjectMapper objectMapper;

    // ──────────────────────────────────────────────
    // 상수
    // ──────────────────────────────────────────────

    /** 계산 단계 식별자 */
    private static final String CALC_STEP = "M6";

    /** 보고서 버전 */
    private static final String REPORT_VERSION = "1.0";

    /** 1년 일수 (환급가산금 일할계산 기준) */
    private static final int DAYS_PER_YEAR = 365;

    /** 고용유지 의무 기간 (년) */
    private static final int EMPLOYMENT_RETENTION_YEARS = 2;

    /** 자산처분 제한 기간 (년) */
    private static final int ASSET_DISPOSAL_LIMIT_YEARS = 2;

    // ══════════════════════════════════════════════
    // 메인 엔트리 포인트
    // ══════════════════════════════════════════════

    /**
     * 최종 환급액을 산출하고 보고서를 생성한다 (STEP 4-5 전체 수행).
     *
     * <p>
     * M6-01(환급액 산출) → M6-02(환급가산금) → M6-03(지방소득세 환급) →
     * M6-04(리스크 평가) → M6-05(보고서 생성) 순서로 처리한다.
     * 결과는 OUT_REFUND, OUT_RISK, OUT_ADDITIONAL_CHECK, OUT_REPORT_JSON 테이블에 저장된다.
     * </p>
     *
     * @param reqId 요청 ID (INP_BASIC.req_id)
     * @throws CalculationException 필수 데이터 누락 또는 계산 오류 시
     */
    public void calculateFinalRefund(String reqId) {
        long startTime = System.currentTimeMillis();
        log.info("[M6] 최종 환급액 산출 시작 - reqId: {}", reqId);

        try {
            // ── 1. 기초 데이터 로딩 ──
            InpBasic basic = inpBasicRepository.findByReqId(reqId)
                    .orElseThrow(() -> new CalculationException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "INP_BASIC 데이터를 찾을 수 없습니다. reqId=" + reqId,
                            reqId, CALC_STEP));

            InpFinancial financial = inpFinancialRepository.findByReqId(reqId)
                    .orElseThrow(() -> new CalculationException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "INP_FINANCIAL 데이터를 찾을 수 없습니다. reqId=" + reqId,
                            reqId, CALC_STEP));

            ChkEligibility eligibility = chkEligibilityRepository.findByReqId(reqId)
                    .orElseThrow(() -> new CalculationException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "CHK_ELIGIBILITY 데이터를 찾을 수 없습니다. reqId=" + reqId,
                            reqId, CALC_STEP));

            // 최적 조합 조회 (순위 1위)
            OutCombination optimalCombo = outCombinationRepository
                    .findByReqIdAndComboRank(reqId, 1)
                    .orElseThrow(() -> new CalculationException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "최적 조합(COMBO_RANK=1) 결과를 찾을 수 없습니다. reqId=" + reqId,
                            reqId, CALC_STEP));

            // 공제·감면 항목 전체 조회
            List<OutCreditDetail> allCreditDetails = outCreditDetailRepository.findByReqId(reqId);

            // ── 2. 기존 결과 삭제 (재시도 지원) ──
            outRefundRepository.deleteByReqId(reqId);
            outRiskRepository.deleteByReqId(reqId);
            outAdditionalCheckRepository.deleteByReqId(reqId);
            outReportJsonRepository.deleteByReqId(reqId);

            // ── 3. M6-01: 최종 환급액 산출 ──
            RefundCalculationResult refundResult = calculateRefundAmount(basic, financial, optimalCombo);

            // ── 4. M6-02: 환급가산금 산출 ──
            InterestResult interestResult = calculateRefundInterest(
                    basic, financial, refundResult.refundAmount);

            // ── 5. M6-03: 지방소득세 환급 ──
            long localTaxRefund = calculateLocalTaxRefund(refundResult.refundAmount);

            // ── 6. 총수령예상액 산출 ──
            long totalExpected = TruncationUtil.truncateAmount(
                    refundResult.refundAmount
                            + interestResult.mainInterest
                            + interestResult.interimInterest
                            + localTaxRefund);

            // ── 7. OUT_REFUND 저장 ──
            saveOutRefund(reqId, basic, financial, optimalCombo, refundResult,
                    interestResult, localTaxRefund, totalExpected);

            // ── 8. M6-04: 사후관리 리스크 평가 ──
            evaluatePostManagementRisk(reqId, basic, optimalCombo, allCreditDetails);

            // ── 9. M6-05: 보고서 JSON 직렬화 및 저장 ──
            generateAndSaveReportJson(reqId, basic, financial, eligibility,
                    optimalCombo, allCreditDetails, refundResult,
                    interestResult, localTaxRefund, totalExpected);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[M6] 최종 환급액 산출 완료 - reqId: {}, 환급액: {}, 총수령예상: {}, 소요: {}ms",
                    reqId, refundResult.refundAmount, totalExpected, elapsed);

            saveCalcLog(reqId, CALC_STEP, "calculateFinalRefund",
                    "기납부세액=" + nullToZero(basic.getPaidTax()),
                    "환급액=" + refundResult.refundAmount + ", 총수령예상=" + totalExpected,
                    startTime);

        } catch (CalculationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[M6] 최종 환급액 산출 중 오류 발생 - reqId: {}", reqId, e);
            throw new CalculationException(
                    ErrorCode.CALCULATION_STEP_FAILED,
                    "최종 환급액 산출 중 오류 발생: " + e.getMessage(),
                    reqId, CALC_STEP, e);
        }
    }

    // ══════════════════════════════════════════════
    // M6-01: 최종 환급액 산출
    // ══════════════════════════════════════════════

    /**
     * 최종 환급액을 산출한다 (M6-01).
     *
     * <p>
     * 환급액 = 기존납부세액 - 경정후결정세액<br>
     * 경정후결정세액 = 산출세액 - 신규공제액 + 최저한세조정액 + 농특세
     * </p>
     *
     * <p>
     * 환급액이 음수(추가납부)인 경우에도 결과를 그대로 유지하며,
     * 후속 보고서에 해당 사실을 기재한다.
     * </p>
     *
     * @param basic     기본 입력 정보
     * @param financial 재무/세무 정보
     * @param combo     최적 조합 결과
     * @return 환급액 산출 결과 (기존세액, 신규세액, 환급액 등)
     */
    private RefundCalculationResult calculateRefundAmount(
            InpBasic basic, InpFinancial financial, OutCombination combo) {

        log.debug("[M6-01] 최종 환급액 산출 - reqId: {}", basic.getReqId());

        // 기존 세액 정보
        long existingComputedTax = nullToZero(basic.getComputedTax());
        long existingDeductions = nullToZero(financial.getIncDeductionTotal());
        long existingDeterminedTax = nullToZero(financial.getDeterminedTax());
        long existingPaidTax = nullToZero(basic.getPaidTax());

        // 신규 세액 정보 (최적 조합 기반)
        long newComputedTax = existingComputedTax;
        long newDeductions = nullToZero(combo.getExemptionTotal()) + nullToZero(combo.getCreditTotal());
        long newMinTaxAdj = nullToZero(combo.getMinTaxAdj());
        long nongteukTotal = nullToZero(combo.getNongteukTotal());

        // 경정후결정세액 = 산출세액 - 신규공제액 + 최저한세조정액
        long newDeterminedTax = TruncationUtil.truncateAmount(
                Math.max(0L, newComputedTax - newDeductions + newMinTaxAdj));

        // 환급액 = 기존납부세액 - 경정후결정세액 - 농특세
        long refundAmount = TruncationUtil.truncateAmount(
                existingPaidTax - newDeterminedTax - nongteukTotal);

        // 이월공제 정보
        long carryforwardCredits = 0L;
        String carryforwardDetail = combo.getCarryforwardItems();
        if (carryforwardDetail != null && !carryforwardDetail.isEmpty() && !"null".equals(carryforwardDetail)) {
            // 이월공제 금액 합산 (JSON에서 추출)
            try {
                List<Map> cfItems = JsonUtil.fromJsonList(carryforwardDetail, Map.class);
                for (Map cfItem : cfItems) {
                    Object cfAmt = cfItem.get("carryforwardAmount");
                    if (cfAmt instanceof Number) {
                        carryforwardCredits += ((Number) cfAmt).longValue();
                    }
                }
            } catch (Exception e) {
                log.warn("[M6-01] 이월공제 상세 파싱 실패 - reqId: {}", basic.getReqId(), e);
            }
        }

        RefundCalculationResult result = new RefundCalculationResult();
        result.existingComputedTax = existingComputedTax;
        result.existingDeductions = existingDeductions;
        result.existingDeterminedTax = existingDeterminedTax;
        result.existingPaidTax = existingPaidTax;
        result.newComputedTax = newComputedTax;
        result.newDeductions = newDeductions;
        result.newMinTaxAdj = newMinTaxAdj;
        result.newDeterminedTax = newDeterminedTax;
        result.nongteukTotal = nongteukTotal;
        result.refundAmount = refundAmount;
        result.carryforwardCredits = carryforwardCredits;
        result.carryforwardDetail = carryforwardDetail;

        log.debug("[M6-01] 환급액 산출 완료 - 기납부: {}, 경정후결정: {}, 농특세: {}, 환급액: {}",
                existingPaidTax, newDeterminedTax, nongteukTotal, refundAmount);

        return result;
    }

    // ══════════════════════════════════════════════
    // M6-02: 환급가산금 산출
    // ══════════════════════════════════════════════

    /**
     * 환급가산금을 산출한다 (M6-02).
     *
     * <p>
     * 환급가산금 = TRUNCATE(환급액 × 이율 × 일수 / 365, 0) (1원미만 절사)<br>
     * 기간별 변동이율을 적용하며, 본세와 중간예납을 별도로 기산한다.
     * </p>
     *
     * <h4>기산일 기준</h4>
     * <ul>
     *   <li>본세 환급: 법정신고기한 익일부터 환급결정일까지</li>
     *   <li>중간예납 환급: 각 납부일 익일부터 환급결정일까지</li>
     * </ul>
     *
     * @param basic        기본 입력 정보 (세목, 귀속연도 등)
     * @param financial    재무/세무 정보 (중간예납세액 등)
     * @param refundAmount 환급 금액
     * @return 환급가산금 산출 결과 (본세 가산금 + 중간예납 가산금)
     */
    private InterestResult calculateRefundInterest(
            InpBasic basic, InpFinancial financial, long refundAmount) {

        log.debug("[M6-02] 환급가산금 산출 - reqId: {}", basic.getReqId());

        InterestResult result = new InterestResult();

        // 환급액이 0 이하이면 가산금 없음
        if (refundAmount <= 0) {
            log.debug("[M6-02] 환급액 0 이하, 가산금 미발생");
            return result;
        }

        // 법정신고기한 산출
        String taxYear = basic.getTaxYear();
        String taxType = basic.getTaxType();
        LocalDate filingDeadline = DateUtil.getFilingDeadline(taxYear, taxType);

        // 기산일: 법정신고기한 익일
        LocalDate interestStartDate = filingDeadline.plusDays(1);
        // 환급결정일: 현재일 기준 (실무에서는 별도 결정일 사용)
        LocalDate interestEndDate = LocalDate.now();

        // 기산 일수 계산
        long days = DateUtil.calculateDaysBetween(interestStartDate, interestEndDate);
        if (days <= 0) {
            log.debug("[M6-02] 기산 일수 0 이하, 가산금 미발생");
            return result;
        }

        // ── 본세 환급가산금 ──
        // 중간예납세액을 제외한 본세 환급액
        long interimPrepaidTax = nullToZero(financial.getInterimPrepaidTax());
        long mainRefundAmount = Math.max(0L, refundAmount - interimPrepaidTax);

        // 기간별 변동이율 적용하여 가산금 계산
        result.mainInterest = calculateInterestWithVariableRate(
                mainRefundAmount, interestStartDate, interestEndDate);

        result.interestStartDate = interestStartDate;
        result.interestEndDate = interestEndDate;

        // ── 중간예납 환급가산금 (별도 기산) ──
        if (interimPrepaidTax > 0) {
            long interimRefundAmount = Math.min(interimPrepaidTax, refundAmount);
            // 중간예납 기산일: 중간예납 납부기한 익일 (법인: 사업연도종료일+8개월=8.31, 개인: 11.30)
            LocalDate interimStartDate = getInterimPrepaidDeadline(taxYear, taxType).plusDays(1);
            result.interimInterest = calculateInterestWithVariableRate(
                    interimRefundAmount, interimStartDate, interestEndDate);
            result.interimRefundAmount = interimRefundAmount;
        }

        // 환급이율 (대표 이율, 보고서 기재용)
        result.representativeRate = getRepresentativeInterestRate(interestStartDate);

        log.debug("[M6-02] 환급가산금 산출 완료 - 본세가산금: {}, 중간예납가산금: {}",
                result.mainInterest, result.interimInterest);

        return result;
    }

    /**
     * 기간별 변동이율을 적용하여 환급가산금을 계산한다.
     *
     * <p>
     * 기산 기간 내 이율 변동이 있는 경우, 각 이율 구간별로 일할 계산하여 합산한다.
     * 환급가산금 = TRUNCATE(환급액 × 이율 × 일수 / 365, 0)
     * </p>
     *
     * @param amount    환급 금액
     * @param startDate 기산 시작일
     * @param endDate   기산 종료일
     * @return 환급가산금 (1원 미만 절사)
     */
    private long calculateInterestWithVariableRate(long amount, LocalDate startDate, LocalDate endDate) {
        if (amount <= 0 || startDate == null || endDate == null) {
            return 0L;
        }

        long totalDays = DateUtil.calculateDaysBetween(startDate, endDate);
        if (totalDays <= 0) {
            return 0L;
        }

        // 기간별 이율 조회
        Date startAsDate = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<RefRefundInterestRate> rates = refRefundInterestRateRepository.findByEffectiveDate(startAsDate);

        if (rates == null || rates.isEmpty()) {
            // 이율 정보가 없으면 0원 반환
            log.warn("[M6-02] 환급가산금 이율 정보가 없습니다. 기산일: {}", startDate);
            return 0L;
        }

        // 대표 이율로 단순 계산 (첫 번째 유효 이율 사용)
        BigDecimal annualRate = rates.get(0).getAnnualRate();
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        // 환급가산금 = TRUNCATE(환급액 × 이율 × 일수 / 365, 0)
        BigDecimal interest = new BigDecimal(amount)
                .multiply(annualRate)
                .multiply(new BigDecimal(totalDays))
                .divide(new BigDecimal(DAYS_PER_YEAR), 0, RoundingMode.DOWN);

        return TruncationUtil.truncateInterest(interest.longValue());
    }

    /**
     * 기산 시작일 기준 대표 환급이율을 조회한다.
     *
     * @param startDate 기산 시작일
     * @return 대표 환급이율 (BigDecimal, 연이율), 조회 실패 시 BigDecimal.ZERO
     */
    private BigDecimal getRepresentativeInterestRate(LocalDate startDate) {
        if (startDate == null) {
            return BigDecimal.ZERO;
        }
        Date startAsDate = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<RefRefundInterestRate> rates = refRefundInterestRateRepository.findByEffectiveDate(startAsDate);
        if (rates != null && !rates.isEmpty() && rates.get(0).getAnnualRate() != null) {
            return rates.get(0).getAnnualRate();
        }
        return BigDecimal.ZERO;
    }

    /**
     * 중간예납 납부기한을 산출한다.
     *
     * <p>
     * 법인세: 사업연도 종료일 + 8개월 (일반적으로 8월 31일)<br>
     * 소득세: 과세연도 11월 30일
     * </p>
     *
     * @param taxYear 귀속 연도
     * @param taxType 세목 코드 (CORP/INC)
     * @return 중간예납 납부기한 (LocalDate)
     */
    private LocalDate getInterimPrepaidDeadline(String taxYear, String taxType) {
        int year = Integer.parseInt(taxYear.trim());
        if ("CORP".equals(taxType)) {
            // 법인세 중간예납: 사업연도 개시일 + 6개월의 다음 2개월 이내
            // 일반적으로 12월 결산법인은 8월 31일
            return LocalDate.of(year, 8, 31);
        }
        // 소득세 중간예납: 11월 30일
        return LocalDate.of(year, 11, 30);
    }

    // ══════════════════════════════════════════════
    // M6-03: 지방소득세 환급
    // ══════════════════════════════════════════════

    /**
     * 지방소득세 환급액을 산출한다 (M6-03).
     *
     * <p>
     * 지방소득세 환급 = TRUNCATE(국세 환급액 × 10%, 0) (10원 미만 절사)
     * </p>
     *
     * @param nationalRefundAmount 국세 환급 금액
     * @return 지방소득세 환급 금액 (음수인 경우 0)
     */
    private long calculateLocalTaxRefund(long nationalRefundAmount) {
        if (nationalRefundAmount <= 0) {
            return 0L;
        }
        return TruncationUtil.truncateAmount(
                new BigDecimal(nationalRefundAmount)
                        .multiply(SystemConstants.LOCAL_TAX_RATE)
                        .setScale(0, RoundingMode.DOWN)
                        .longValue());
    }

    // ══════════════════════════════════════════════
    // M6-04: 사후관리 리스크 평가
    // ══════════════════════════════════════════════

    /**
     * 사후관리 리스크를 평가한다 (M6-04).
     *
     * <p>
     * 적용된 공제·감면 항목에 대해 다음 3가지 유형의 리스크를 평가하고,
     * OUT_RISK 및 OUT_ADDITIONAL_CHECK 테이블에 결과를 저장한다.
     * </p>
     *
     * <h4>리스크 유형</h4>
     * <ol>
     *   <li><b>고용유지 실패:</b> 조특법 §29의8, 2년간 상시근로자 수 유지 의무</li>
     *   <li><b>자산 조기 처분:</b> 조특법 §146, 투자완료 후 2년 이내 처분 시 세액 추징</li>
     *   <li><b>감가상각 의무 미이행:</b> 조특법 §128⑨, 감가상각 의무화 대상 미이행</li>
     * </ol>
     *
     * @param reqId             요청 ID
     * @param basic             기본 입력 정보
     * @param optimalCombo      최적 조합 결과
     * @param allCreditDetails  전체 공제·감면 항목 목록
     */
    private void evaluatePostManagementRisk(
            String reqId, InpBasic basic, OutCombination optimalCombo,
            List<OutCreditDetail> allCreditDetails) {

        log.debug("[M6-04] 사후관리 리스크 평가 시작 - reqId: {}", reqId);

        // 최적 조합에 포함된 항목 ID 추출
        List<String> comboItemIds = extractComboItemIds(optimalCombo);

        // 최적 조합에 포함된 공제·감면 항목 필터링
        List<OutCreditDetail> appliedItems = allCreditDetails.stream()
                .filter(item -> comboItemIds.contains(item.getItemId()))
                .collect(Collectors.toList());

        int riskSeq = 1;
        int checkSeq = 1;
        LocalDate fiscalEnd = basic.getFiscalEnd();

        for (OutCreditDetail item : appliedItems) {
            String provision = item.getProvision();
            long grossAmount = nullToZero(item.getGrossAmount());

            // ── (1) 고용유지 실패 리스크 (고용증대 관련 조항) ──
            if (isEmploymentRelatedProvision(provision)) {
                OutRisk employmentRisk = OutRisk.builder()
                        .reqId(reqId)
                        .riskId("RISK-" + String.format("%03d", riskSeq++))
                        .provision(provision)
                        .riskType("EMPLOYMENT_RETENTION")
                        .obligation("상시근로자 수 2년간 유지 의무 (조특법 §29의8)")
                        .periodStart(fiscalEnd != null ? fiscalEnd : LocalDate.now())
                        .periodEnd(fiscalEnd != null
                                ? DateUtil.formatDate(fiscalEnd.plusYears(EMPLOYMENT_RETENTION_YEARS))
                                : DateUtil.formatDate(LocalDate.now().plusYears(EMPLOYMENT_RETENTION_YEARS)))
                        .violationAction("감소인원 × 공제액 추징 + 이자상당가산액")
                        .potentialClawback(grossAmount)
                        .interestSurcharge(TruncationUtil.truncateAmount(
                                new BigDecimal(grossAmount).multiply(new BigDecimal("0.022"))
                                        .setScale(0, RoundingMode.DOWN).longValue()))
                        .riskLevel(RiskLevel.HIGH.getCode())
                        .description("고용증대 세액공제 적용 시 당해연도 상시근로자 수를 "
                                + EMPLOYMENT_RETENTION_YEARS + "년간 유지해야 합니다. "
                                + "감소 시 감소분에 대한 세액 추징 및 이자상당가산액이 부과됩니다.")
                        .build();
                outRiskRepository.save(employmentRisk);

                // 추가 확인 항목 등록
                OutAdditionalCheck employmentCheck = OutAdditionalCheck.builder()
                        .reqId(reqId)
                        .checkId("ACHK-" + String.format("%03d", checkSeq++))
                        .description("상시근로자 수 유지 여부 모니터링 필요")
                        .reason(provision + " 적용에 따른 고용유지 의무")
                        .relatedInspection("M6-04-EMP")
                        .relatedModule("M6")
                        .priority("HIGH")
                        .status("PENDING")
                        .build();
                outAdditionalCheckRepository.save(employmentCheck);
            }

            // ── (2) 자산 조기 처분 리스크 (투자 관련 조항) ──
            if (isInvestmentRelatedProvision(provision)) {
                OutRisk assetRisk = OutRisk.builder()
                        .reqId(reqId)
                        .riskId("RISK-" + String.format("%03d", riskSeq++))
                        .provision(provision)
                        .riskType("ASSET_DISPOSAL")
                        .obligation("투자자산 2년 이내 처분 금지 (조특법 §146)")
                        .periodStart(fiscalEnd != null ? fiscalEnd : LocalDate.now())
                        .periodEnd(fiscalEnd != null
                                ? DateUtil.formatDate(fiscalEnd.plusYears(ASSET_DISPOSAL_LIMIT_YEARS))
                                : DateUtil.formatDate(LocalDate.now().plusYears(ASSET_DISPOSAL_LIMIT_YEARS)))
                        .violationAction("공제세액 전액 추징 + 이자상당가산액")
                        .potentialClawback(grossAmount)
                        .interestSurcharge(TruncationUtil.truncateAmount(
                                new BigDecimal(grossAmount).multiply(new BigDecimal("0.022"))
                                        .setScale(0, RoundingMode.DOWN).longValue()))
                        .riskLevel(RiskLevel.MEDIUM.getCode())
                        .description("투자 세액공제 적용 자산을 투자완료일로부터 "
                                + ASSET_DISPOSAL_LIMIT_YEARS + "년 이내에 처분하면 "
                                + "공제세액 전액 추징 및 이자상당가산액이 부과됩니다.")
                        .build();
                outRiskRepository.save(assetRisk);
            }

            // ── (3) 감가상각 의무 미이행 리스크 ──
            if (isDepreciationObligationProvision(provision)) {
                OutRisk depreciationRisk = OutRisk.builder()
                        .reqId(reqId)
                        .riskId("RISK-" + String.format("%03d", riskSeq++))
                        .provision(provision)
                        .riskType("DEPRECIATION_OBLIGATION")
                        .obligation("감가상각 의무화 이행 (조특법 §128⑨)")
                        .periodStart(fiscalEnd != null ? fiscalEnd : LocalDate.now())
                        .periodEnd(null)
                        .violationAction("감가상각 의무 미이행분 세무조정 추가")
                        .potentialClawback(0L)
                        .interestSurcharge(0L)
                        .riskLevel(RiskLevel.LOW.getCode())
                        .description("세액공제를 적용받은 자산에 대해 감가상각 의무가 발생하며, "
                                + "미이행 시 세무조정이 추가될 수 있습니다.")
                        .build();
                outRiskRepository.save(depreciationRisk);

                OutAdditionalCheck depreciationCheck = OutAdditionalCheck.builder()
                        .reqId(reqId)
                        .checkId("ACHK-" + String.format("%03d", checkSeq++))
                        .description("감가상각 의무 이행 여부 확인 필요")
                        .reason(provision + " 적용에 따른 감가상각 의무")
                        .relatedInspection("M6-04-DEP")
                        .relatedModule("M6")
                        .priority("MEDIUM")
                        .status("PENDING")
                        .build();
                outAdditionalCheckRepository.save(depreciationCheck);
            }
        }

        log.debug("[M6-04] 사후관리 리스크 평가 완료 - 리스크: {}건, 추가확인: {}건",
                riskSeq - 1, checkSeq - 1);
    }

    // ══════════════════════════════════════════════
    // M6-05: 보고서 JSON 직렬화
    // ══════════════════════════════════════════════

    /**
     * 보고서 JSON을 생성하여 OUT_REPORT_JSON 테이블에 저장한다 (M6-05).
     *
     * <p>
     * 7개 섹션(A~G)으로 구성된 보고서 JSON을 생성한다:
     * </p>
     * <ul>
     *   <li><b>Section A:</b> 신청인 기본 정보</li>
     *   <li><b>Section B:</b> 자격 진단 결과</li>
     *   <li><b>Section C:</b> 공제·감면 항목 상세</li>
     *   <li><b>Section D:</b> 최적 조합 분석</li>
     *   <li><b>Section E:</b> 환급액 산출 내역</li>
     *   <li><b>Section F:</b> 사후관리 리스크</li>
     *   <li><b>Section G:</b> 메타정보 및 체크리스트</li>
     * </ul>
     *
     * @param reqId            요청 ID
     * @param basic            기본 입력 정보
     * @param financial        재무/세무 정보
     * @param eligibility      자격 진단 결과
     * @param optimalCombo     최적 조합 결과
     * @param allCreditDetails 전체 공제·감면 항목
     * @param refundResult     환급액 산출 결과
     * @param interestResult   환급가산금 산출 결과
     * @param localTaxRefund   지방소득세 환급
     * @param totalExpected    총수령예상액
     */
    private void generateAndSaveReportJson(
            String reqId, InpBasic basic, InpFinancial financial,
            ChkEligibility eligibility, OutCombination optimalCombo,
            List<OutCreditDetail> allCreditDetails,
            RefundCalculationResult refundResult, InterestResult interestResult,
            long localTaxRefund, long totalExpected) {

        log.debug("[M6-05] 보고서 JSON 생성 시작 - reqId: {}", reqId);

        // ── Section A: 신청인 기본 정보 ──
        Map<String, Object> sectionA = buildSectionA(basic);

        // ── Section B: 자격 진단 결과 ──
        Map<String, Object> sectionB = buildSectionB(reqId, eligibility);

        // ── Section C: 공제·감면 항목 상세 ──
        Map<String, Object> sectionC = buildSectionC(allCreditDetails);

        // ── Section D: 최적 조합 분석 ──
        Map<String, Object> sectionD = buildSectionD(reqId, optimalCombo);

        // ── Section E: 환급액 산출 내역 ──
        Map<String, Object> sectionE = buildSectionE(refundResult, interestResult,
                localTaxRefund, totalExpected);

        // ── Section F: 사후관리 리스크 ──
        Map<String, Object> sectionF = buildSectionF(reqId);

        // ── Section G: 메타정보 및 체크리스트 ──
        Map<String, Object> sectionGMeta = buildSectionGMeta(reqId, basic);

        // 전체 보고서 JSON 조립
        Map<String, Object> fullReport = new LinkedHashMap<>();
        fullReport.put("reportVersion", REPORT_VERSION);
        fullReport.put("reqId", reqId);
        fullReport.put("generatedAt", LocalDateTime.now().toString());
        fullReport.put("sectionA", sectionA);
        fullReport.put("sectionB", sectionB);
        fullReport.put("sectionC", sectionC);
        fullReport.put("sectionD", sectionD);
        fullReport.put("sectionE", sectionE);
        fullReport.put("sectionF", sectionF);
        fullReport.put("sectionGMeta", sectionGMeta);

        // JSON 직렬화
        String sectionAJson = JsonUtil.toJson(sectionA);
        String sectionBJson = JsonUtil.toJson(sectionB);
        String sectionCJson = JsonUtil.toJson(sectionC);
        String sectionDJson = JsonUtil.toJson(sectionD);
        String sectionEJson = JsonUtil.toJson(sectionE);
        String sectionFJson = JsonUtil.toJson(sectionF);
        String sectionGMetaJson = JsonUtil.toJson(sectionGMeta);
        String reportJson = JsonUtil.toJson(fullReport);

        // 바이트 크기 및 체크섬 계산
        int jsonByteSize = JsonUtil.getJsonByteSize(reportJson);
        String checksum = calculateSha256(reportJson);

        OutReportJson outReport = OutReportJson.builder()
                .reqId(reqId)
                .reportVersion(REPORT_VERSION)
                .reportStatus("COMPLETED")
                .reportJson(reportJson)
                .sectionAJson(sectionAJson)
                .sectionBJson(sectionBJson)
                .sectionCJson(sectionCJson)
                .sectionDJson(sectionDJson)
                .sectionEJson(sectionEJson)
                .sectionFJson(sectionFJson)
                .sectionGMeta(sectionGMetaJson)
                .jsonByteSize(jsonByteSize)
                .resultCode("SUCCESS")
                .checksum(checksum)
                .generatedAt(LocalDateTime.now())
                .build();

        outReportJsonRepository.save(outReport);

        log.debug("[M6-05] 보고서 JSON 생성 완료 - reqId: {}, 크기: {} bytes", reqId, jsonByteSize);
    }

    // ──────────────────────────────────────────────
    // 보고서 섹션 빌더
    // ──────────────────────────────────────────────

    /**
     * Section A: 신청인 기본 정보를 빌드한다.
     *
     * @param basic 기본 입력 정보
     * @return Section A 맵
     */
    private Map<String, Object> buildSectionA(InpBasic basic) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("sectionTitle", "신청인 기본 정보");
        section.put("applicantName", basic.getApplicantName());
        section.put("bizRegNo", basic.getBizRegNo());
        section.put("taxType", basic.getTaxType());
        section.put("corpSize", basic.getCorpSize());
        section.put("industryCode", basic.getIndustryCode());
        section.put("taxYear", basic.getTaxYear());
        section.put("fiscalStart", basic.getFiscalStart() != null ? basic.getFiscalStart().toString() : null);
        section.put("fiscalEnd", basic.getFiscalEnd() != null ? basic.getFiscalEnd().toString() : null);
        section.put("capitalZone", basic.getCapitalZone());
        section.put("depopulationArea", basic.getDepopulationArea());
        section.put("revenue", basic.getRevenue());
        section.put("taxableIncome", basic.getTaxableIncome());
        section.put("computedTax", basic.getComputedTax());
        return section;
    }

    /**
     * Section B: 자격 진단 결과를 빌드한다.
     *
     * @param reqId       요청 ID
     * @param eligibility 자격 진단 결과
     * @return Section B 맵
     */
    private Map<String, Object> buildSectionB(String reqId, ChkEligibility eligibility) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("sectionTitle", "자격 진단 결과");
        section.put("overallStatus", eligibility.getOverallStatus());
        section.put("deadlineEligible", eligibility.getDeadlineEligible());
        section.put("smeEligible", eligibility.getSmeEligible());
        section.put("companySize", eligibility.getCompanySize());
        section.put("ventureConfirmed", eligibility.getVentureConfirmed());
        section.put("settlementCheckResult", eligibility.getSettlementCheckResult());

        // 점검 로그 포함
        List<ChkInspectionLog> inspectionLogs =
                chkInspectionLogRepository.findByReqIdOrderBySortOrder(reqId);
        List<Map<String, Object>> inspections = new ArrayList<>();
        for (ChkInspectionLog log : inspectionLogs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("inspectionCode", log.getInspectionCode());
            entry.put("inspectionName", log.getInspectionName());
            entry.put("judgment", log.getJudgment());
            entry.put("summary", log.getSummary());
            inspections.add(entry);
        }
        section.put("inspections", inspections);
        return section;
    }

    /**
     * Section C: 공제·감면 항목 상세를 빌드한다.
     *
     * @param allCreditDetails 전체 공제·감면 항목
     * @return Section C 맵
     */
    private Map<String, Object> buildSectionC(List<OutCreditDetail> allCreditDetails) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("sectionTitle", "공제·감면 항목 상세");
        section.put("totalItems", allCreditDetails.size());

        List<Map<String, Object>> items = new ArrayList<>();
        for (OutCreditDetail item : allCreditDetails) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("itemId", item.getItemId());
            entry.put("itemName", item.getItemName());
            entry.put("provision", item.getProvision());
            entry.put("creditType", item.getCreditType());
            entry.put("itemStatus", item.getItemStatus());
            entry.put("grossAmount", item.getGrossAmount());
            entry.put("netAmount", item.getNetAmount());
            entry.put("nongteukAmount", item.getNongteukAmount());
            entry.put("minTaxSubject", item.getMinTaxSubject());
            entry.put("isCarryforward", item.getIsCarryforward());
            entry.put("legalBasis", item.getLegalBasis());
            items.add(entry);
        }
        section.put("items", items);
        return section;
    }

    /**
     * Section D: 최적 조합 분석을 빌드한다.
     *
     * @param reqId        요청 ID
     * @param optimalCombo 최적 조합 결과
     * @return Section D 맵
     */
    private Map<String, Object> buildSectionD(String reqId, OutCombination optimalCombo) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("sectionTitle", "최적 조합 분석");
        section.put("optimalComboId", optimalCombo.getComboId());
        section.put("comboName", optimalCombo.getComboName());
        section.put("exemptionTotal", optimalCombo.getExemptionTotal());
        section.put("creditTotal", optimalCombo.getCreditTotal());
        section.put("minTaxAdj", optimalCombo.getMinTaxAdj());
        section.put("nongteukTotal", optimalCombo.getNongteukTotal());
        section.put("netRefund", optimalCombo.getNetRefund());

        // 전체 조합 순위 목록
        List<OutCombination> allCombos = outCombinationRepository.findByReqIdOrderByComboRankAsc(reqId);
        List<Map<String, Object>> comboRanking = new ArrayList<>();
        for (OutCombination combo : allCombos) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("comboId", combo.getComboId());
            entry.put("comboRank", combo.getComboRank());
            entry.put("netRefund", combo.getNetRefund());
            entry.put("groupType", combo.getGroupType());
            comboRanking.add(entry);
        }
        section.put("comboRanking", comboRanking);
        return section;
    }

    /**
     * Section E: 환급액 산출 내역을 빌드한다.
     *
     * @param refundResult   환급액 산출 결과
     * @param interestResult 환급가산금 산출 결과
     * @param localTaxRefund 지방소득세 환급
     * @param totalExpected  총수령예상액
     * @return Section E 맵
     */
    private Map<String, Object> buildSectionE(
            RefundCalculationResult refundResult, InterestResult interestResult,
            long localTaxRefund, long totalExpected) {

        Map<String, Object> section = new LinkedHashMap<>();
        section.put("sectionTitle", "환급액 산출 내역");

        // 기존 세액
        Map<String, Object> existingTax = new LinkedHashMap<>();
        existingTax.put("computedTax", refundResult.existingComputedTax);
        existingTax.put("deductions", refundResult.existingDeductions);
        existingTax.put("determinedTax", refundResult.existingDeterminedTax);
        existingTax.put("paidTax", refundResult.existingPaidTax);
        section.put("existingTax", existingTax);

        // 경정 후 세액
        Map<String, Object> newTax = new LinkedHashMap<>();
        newTax.put("computedTax", refundResult.newComputedTax);
        newTax.put("deductions", refundResult.newDeductions);
        newTax.put("minTaxAdj", refundResult.newMinTaxAdj);
        newTax.put("determinedTax", refundResult.newDeterminedTax);
        newTax.put("nongteukTotal", refundResult.nongteukTotal);
        section.put("newTax", newTax);

        // 환급액
        section.put("refundAmount", refundResult.refundAmount);

        // 환급가산금
        Map<String, Object> interest = new LinkedHashMap<>();
        interest.put("mainInterest", interestResult.mainInterest);
        interest.put("interimInterest", interestResult.interimInterest);
        interest.put("interestStartDate",
                interestResult.interestStartDate != null ? interestResult.interestStartDate.toString() : null);
        interest.put("interestEndDate",
                interestResult.interestEndDate != null ? interestResult.interestEndDate.toString() : null);
        interest.put("representativeRate",
                interestResult.representativeRate != null ? interestResult.representativeRate.toPlainString() : null);
        section.put("refundInterest", interest);

        // 지방소득세 환급
        section.put("localTaxRefund", localTaxRefund);

        // 총수령예상액
        section.put("totalExpected", totalExpected);

        // 이월공제
        section.put("carryforwardCredits", refundResult.carryforwardCredits);

        return section;
    }

    /**
     * Section F: 사후관리 리스크를 빌드한다.
     *
     * @param reqId 요청 ID
     * @return Section F 맵
     */
    private Map<String, Object> buildSectionF(String reqId) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("sectionTitle", "사후관리 리스크");

        List<OutRisk> risks = outRiskRepository.findByReqId(reqId);
        List<Map<String, Object>> riskList = new ArrayList<>();
        for (OutRisk risk : risks) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("riskId", risk.getRiskId());
            entry.put("provision", risk.getProvision());
            entry.put("riskType", risk.getRiskType());
            entry.put("riskLevel", risk.getRiskLevel());
            entry.put("obligation", risk.getObligation());
            entry.put("violationAction", risk.getViolationAction());
            entry.put("potentialClawback", risk.getPotentialClawback());
            entry.put("description", risk.getDescription());
            riskList.add(entry);
        }
        section.put("risks", riskList);
        section.put("totalRisks", risks.size());

        // 추가 확인 항목
        List<OutAdditionalCheck> checks = outAdditionalCheckRepository.findByReqId(reqId);
        List<Map<String, Object>> checkList = new ArrayList<>();
        for (OutAdditionalCheck check : checks) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("checkId", check.getCheckId());
            entry.put("description", check.getDescription());
            entry.put("reason", check.getReason());
            entry.put("priority", check.getPriority());
            entry.put("status", check.getStatus());
            checkList.add(entry);
        }
        section.put("additionalChecks", checkList);

        return section;
    }

    /**
     * Section G: 메타정보 및 체크리스트를 빌드한다.
     *
     * @param reqId 요청 ID
     * @param basic 기본 입력 정보
     * @return Section G 메타 맵
     */
    private Map<String, Object> buildSectionGMeta(String reqId, InpBasic basic) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("sectionTitle", "메타정보 및 체크리스트");
        section.put("reqId", reqId);
        section.put("reportVersion", REPORT_VERSION);
        section.put("generatedAt", LocalDateTime.now().toString());
        section.put("taxYear", basic.getTaxYear());
        section.put("taxType", basic.getTaxType());

        // 체크리스트 항목
        List<Map<String, Object>> checklist = new ArrayList<>();

        Map<String, Object> chk1 = new LinkedHashMap<>();
        chk1.put("item", "사전점검 완료 여부");
        chk1.put("status", "COMPLETED");
        checklist.add(chk1);

        Map<String, Object> chk2 = new LinkedHashMap<>();
        chk2.put("item", "공제·감면 산출 완료 여부");
        chk2.put("status", "COMPLETED");
        checklist.add(chk2);

        Map<String, Object> chk3 = new LinkedHashMap<>();
        chk3.put("item", "최적 조합 탐색 완료 여부");
        chk3.put("status", "COMPLETED");
        checklist.add(chk3);

        Map<String, Object> chk4 = new LinkedHashMap<>();
        chk4.put("item", "환급액 산출 완료 여부");
        chk4.put("status", "COMPLETED");
        checklist.add(chk4);

        Map<String, Object> chk5 = new LinkedHashMap<>();
        chk5.put("item", "사후관리 리스크 평가 완료 여부");
        chk5.put("status", "COMPLETED");
        checklist.add(chk5);

        Map<String, Object> chk6 = new LinkedHashMap<>();
        chk6.put("item", "보고서 생성 완료 여부");
        chk6.put("status", "COMPLETED");
        checklist.add(chk6);

        section.put("checklist", checklist);

        // 감사 추적 정보
        section.put("engineVersion", "TaxServiceENTEC v1.0");
        section.put("calculationEngine", "CombinationSearchService + RefundCalculationService");

        return section;
    }

    // ══════════════════════════════════════════════
    // 결과 저장
    // ══════════════════════════════════════════════

    /**
     * 환급 산출 결과를 OUT_REFUND 테이블에 저장한다.
     *
     * @param reqId          요청 ID
     * @param basic          기본 입력 정보
     * @param financial      재무/세무 정보
     * @param optimalCombo   최적 조합 결과
     * @param refundResult   환급액 산출 결과
     * @param interestResult 환급가산금 산출 결과
     * @param localTaxRefund 지방소득세 환급
     * @param totalExpected  총수령예상액
     */
    private void saveOutRefund(
            String reqId, InpBasic basic, InpFinancial financial,
            OutCombination optimalCombo, RefundCalculationResult refundResult,
            InterestResult interestResult, long localTaxRefund, long totalExpected) {

        // 환급 한도 상세 JSON
        Map<String, Object> refundCapDetail = new LinkedHashMap<>();
        refundCapDetail.put("computedTax", refundResult.newComputedTax);
        refundCapDetail.put("maxDeductible", refundResult.newDeductions);
        refundCapDetail.put("minTaxAdj", refundResult.newMinTaxAdj);
        refundCapDetail.put("nongteukTotal", refundResult.nongteukTotal);
        String refundCapDetailJson = JsonUtil.toJson(refundCapDetail);

        OutRefund outRefund = OutRefund.builder()
                .reqId(reqId)
                .existingComputedTax(refundResult.existingComputedTax)
                .existingDeductions(refundResult.existingDeductions)
                .existingDeterminedTax(refundResult.existingDeterminedTax)
                .existingPaidTax(refundResult.existingPaidTax)
                .newComputedTax(refundResult.newComputedTax)
                .newDeductions(refundResult.newDeductions)
                .newMinTaxAdj(refundResult.newMinTaxAdj)
                .newDeterminedTax(refundResult.newDeterminedTax)
                .nongteukTotal(refundResult.nongteukTotal)
                .refundAmount(refundResult.refundAmount)
                .refundInterestStart(interestResult.interestStartDate)
                .refundInterestEnd(interestResult.interestEndDate != null
                        ? interestResult.interestEndDate.toString() : null)
                .refundInterestRate(interestResult.representativeRate)
                .refundInterestAmount(interestResult.mainInterest)
                .interimRefundAmount(interestResult.interimRefundAmount)
                .interimInterestAmount(interestResult.interimInterest)
                .localTaxRefund(localTaxRefund)
                .totalExpected(totalExpected)
                .refundCapDetail(refundCapDetailJson)
                .optimalComboId(optimalCombo.getComboId())
                .carryforwardCredits(refundResult.carryforwardCredits)
                .carryforwardDetail(refundResult.carryforwardDetail)
                .penaltyTaxChange(0L)
                .build();

        outRefundRepository.save(outRefund);
    }

    // ══════════════════════════════════════════════
    // 유틸리티 메서드
    // ══════════════════════════════════════════════

    /**
     * 최적 조합에 포함된 항목 ID 목록을 추출한다.
     *
     * @param combo 조합 정보
     * @return 항목 ID 문자열 리스트
     */
    private List<String> extractComboItemIds(OutCombination combo) {
        if (combo.getItemsJson() == null || combo.getItemsJson().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return JsonUtil.fromJsonList(combo.getItemsJson(), String.class);
        } catch (Exception e) {
            log.warn("[M6] 조합 항목 JSON 파싱 실패 - comboId: {}", combo.getComboId(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 고용 관련 조항인지 판별한다 (고용증대 세액공제 등).
     *
     * <p>
     * 조특법 §29의7(고용증대 세액공제), §30(중소기업 사회보험료 세액공제) 등
     * 상시근로자 유지 의무가 수반되는 조항을 판별한다.
     * </p>
     *
     * @param provision 조항 코드
     * @return 고용 관련 조항이면 true
     */
    private boolean isEmploymentRelatedProvision(String provision) {
        if (provision == null) {
            return false;
        }
        // 고용증대, 사회보험료, 정규직전환 관련 조항
        return provision.contains("§29") || provision.contains("§30")
                || provision.contains("고용") || provision.contains("EMPLOYMENT");
    }

    /**
     * 투자 관련 조항인지 판별한다 (투자 세액공제 등).
     *
     * <p>
     * 조특법 §24(통합투자세액공제), §25(영상콘텐츠투자), §25의7(국가전략기술투자) 등
     * 자산 처분 제한이 수반되는 조항을 판별한다.
     * </p>
     *
     * @param provision 조항 코드
     * @return 투자 관련 조항이면 true
     */
    private boolean isInvestmentRelatedProvision(String provision) {
        if (provision == null) {
            return false;
        }
        return provision.contains("§24") || provision.contains("§25")
                || provision.contains("투자") || provision.contains("INVEST");
    }

    /**
     * 감가상각 의무 대상 조항인지 판별한다.
     *
     * <p>
     * 조특법 §128⑨에 따라 세액공제를 적용받은 자산에 대해
     * 감가상각 의무가 발생하는 조항을 판별한다.
     * </p>
     *
     * @param provision 조항 코드
     * @return 감가상각 의무 대상 조항이면 true
     */
    private boolean isDepreciationObligationProvision(String provision) {
        if (provision == null) {
            return false;
        }
        // 투자 세액공제 적용 자산은 감가상각 의무 대상
        return provision.contains("§24") || provision.contains("§25")
                || provision.contains("§26");
    }

    /**
     * SHA-256 체크섬을 계산한다.
     *
     * @param input 체크섬 대상 문자열
     * @return SHA-256 해시 문자열 (16진수, 64자)
     */
    private String calculateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("[M6] SHA-256 해시 계산 실패", e);
            return "HASH_ERROR";
        }
    }

    /**
     * null을 0으로 변환한다 (Long → long 안전 변환).
     *
     * @param value Long 값 (nullable)
     * @return 0 또는 실제 값
     */
    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }

    /**
     * 계산 로그를 LOG_CALCULATION 테이블에 저장한다.
     *
     * @param reqId        요청 ID
     * @param calcStep     계산 단계
     * @param functionName 함수명
     * @param inputData    입력 데이터 요약
     * @param outputData   출력 데이터 요약
     * @param startTime    시작 시각 (밀리초)
     */
    private void saveCalcLog(String reqId, String calcStep, String functionName,
                             String inputData, String outputData, long startTime) {
        int durationMs = (int) (System.currentTimeMillis() - startTime);
        LogCalculation logEntry = LogCalculation.builder()
                .reqId(reqId)
                .calcStep(calcStep)
                .functionName(functionName)
                .inputData(inputData)
                .outputData(outputData)
                .legalBasis("국세기본법 §51의2, 지방세법 §103의30")
                .executedAt(LocalDateTime.now())
                .logLevel("INFO")
                .executedBy("RefundCalculationService")
                .durationMs(durationMs)
                .build();
        logCalculationRepository.save(logEntry);
    }

    // ══════════════════════════════════════════════
    // 내부 데이터 클래스
    // ══════════════════════════════════════════════

    /**
     * 환급액 산출 결과를 담는 내부 클래스.
     */
    private static class RefundCalculationResult {
        long existingComputedTax;
        long existingDeductions;
        long existingDeterminedTax;
        long existingPaidTax;
        long newComputedTax;
        long newDeductions;
        long newMinTaxAdj;
        long newDeterminedTax;
        long nongteukTotal;
        long refundAmount;
        long carryforwardCredits;
        String carryforwardDetail;
    }

    /**
     * 환급가산금 산출 결과를 담는 내부 클래스.
     */
    private static class InterestResult {
        /** 본세 환급가산금 */
        long mainInterest;
        /** 중간예납 환급가산금 */
        long interimInterest;
        /** 중간예납 환급 금액 */
        long interimRefundAmount;
        /** 기산 시작일 */
        LocalDate interestStartDate;
        /** 기산 종료일 */
        LocalDate interestEndDate;
        /** 대표 이율 */
        BigDecimal representativeRate;
    }
}
