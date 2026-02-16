package com.entec.tax.engine.refund.service;

import com.entec.tax.common.constants.LogLevel;
import com.entec.tax.common.constants.ProvisionCode;
import com.entec.tax.common.constants.RiskLevel;
import com.entec.tax.common.constants.SystemConstants;
import com.entec.tax.common.exception.CalculationException;
import com.entec.tax.common.exception.ErrorCode;
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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * M6 최종 환급액 산출 서비스 구현체.
 *
 * <p>
 * STEP 4-5 단계에서 최종 환급액을 산출하고 보고서를 생성한다.
 * M6-01 ~ M6-05 서브 메서드로 구성되며, 각 메서드는 환급액 산출,
 * 환급가산금 계산, 지방세 환급액 산출, 사후관리 리스크 평가,
 * 최종 보고서 JSON 직렬화를 수행한다.
 * </p>
 *
 * <p><b>절사 원칙:</b> 모든 금액은 {@link TruncationUtil}을 사용하여
 * 절사(TRUNCATE)한다. 반올림(ROUND)은 절대 사용하지 않는다.</p>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 * @see RefundCalculationService
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class RefundCalculationServiceImpl implements RefundCalculationService {

    private static final String CALC_STEP = "M6";
    private static final String REPORT_VERSION = "1.0";

    // ──────────────────────────────────────────────
    // 입력 리포지토리
    // ──────────────────────────────────────────────
    private final InpBasicRepository inpBasicRepository;
    private final InpFinancialRepository inpFinancialRepository;

    // ──────────────────────────────────────────────
    // 출력 리포지토리
    // ──────────────────────────────────────────────
    private final OutRefundRepository outRefundRepository;
    private final OutCombinationRepository outCombinationRepository;
    private final OutCreditDetailRepository outCreditDetailRepository;
    private final OutRiskRepository outRiskRepository;
    private final OutAdditionalCheckRepository outAdditionalCheckRepository;

    // ──────────────────────────────────────────────
    // 보고서 리포지토리
    // ──────────────────────────────────────────────
    private final OutReportJsonRepository outReportJsonRepository;

    // ──────────────────────────────────────────────
    // 검증 리포지토리
    // ──────────────────────────────────────────────
    private final ChkEligibilityRepository chkEligibilityRepository;
    private final ChkInspectionLogRepository chkInspectionLogRepository;

    // ──────────────────────────────────────────────
    // 로그 리포지토리
    // ──────────────────────────────────────────────
    private final LogCalculationRepository logCalculationRepository;

    // ──────────────────────────────────────────────
    // 기준정보 리포지토리
    // ──────────────────────────────────────────────
    private final RefRefundInterestRateRepository refRefundInterestRateRepository;

    // ──────────────────────────────────────────────
    // JSON 직렬화
    // ──────────────────────────────────────────────
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     *
     * <p>
     * 최종 환급액을 산출하고 보고서를 생성한다.
     * M6-01 ~ M6-05까지의 서브 메서드를 순차적으로 호출한다.
     * </p>
     *
     * @param reqId 요청 ID
     * @throws CalculationException 계산 중 오류가 발생한 경우
     */
    @Override
    public void calculateFinalRefund(String reqId) {
        log.info("[{}] M6 최종 환급액 산출 시작", reqId);
        long startTime = System.currentTimeMillis();

        try {
            // 기존 산출 결과 초기화 (TX-2 재시도 지원)
            outRefundRepository.deleteByReqId(reqId);
            outRiskRepository.deleteByReqId(reqId);
            outReportJsonRepository.deleteByReqId(reqId);

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

            // 최적 조합 조회 (M5 결과)
            Optional<OutCombination> optimalComboOpt =
                    outCombinationRepository.findByReqIdAndComboRank(reqId, 1);

            // M6-01: 최종 환급액 산출
            OutRefund refund = calculateRefundAmount(reqId, basic, financial, optimalComboOpt);

            // M6-02: 환급가산금 산출
            refund = calculateRefundInterest(reqId, basic, refund);

            // M6-03: 지방소득세 환급 산출
            refund = calculateLocalTaxRefund(reqId, refund);

            // M6-04: 사후관리 리스크 평가
            evaluatePostManagementRisk(reqId, basic, refund);

            // M6-05: 보고서 JSON 직렬화
            serializeReportJson(reqId, basic, financial, refund);

            long duration = System.currentTimeMillis() - startTime;
            saveCalculationLog(reqId, CALC_STEP, "calculateFinalRefund",
                    "reqId=" + reqId,
                    String.format("환급액=%d, 총기대액=%d",
                            refund.getRefundAmount(), refund.getTotalExpected()),
                    LogLevel.INFO.getCode(), (int) duration);

            log.info("[{}] M6 최종 환급액 산출 완료 ({}ms): 환급액={}, 총기대액={}",
                    reqId, duration, refund.getRefundAmount(), refund.getTotalExpected());

        } catch (CalculationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}] M6 최종 환급액 산출 중 오류 발생", reqId, e);
            throw new CalculationException(
                    ErrorCode.CALCULATION_STEP_FAILED,
                    "M6 최종 환급액 산출 중 오류가 발생했습니다: " + e.getMessage(),
                    reqId, CALC_STEP, e);
        }
    }

    // ================================================================
    // M6-01: 최종 환급액 산출
    // ================================================================

    /**
     * M6-01: 최종 환급액을 산출한다.
     *
     * <p>
     * 최종 환급액 = 기존납부세액 - 경정후결정세액.
     * 경정후결정세액은 최적 조합의 순환급액을 기반으로 산출한다.
     * </p>
     *
     * <ul>
     *   <li>기존납부세액: INP_BASIC.paid_tax</li>
     *   <li>경정후결정세액: 산출세액 - 최적 조합 공제/감면 합계 + 최저한세 조정</li>
     *   <li>환급액 = 기존납부세액 - 경정후결정세액 (음수면 0)</li>
     * </ul>
     *
     * @param reqId           요청 ID
     * @param basic           기본 정보
     * @param financial       재무 정보
     * @param optimalComboOpt 최적 조합 (없을 수 있음)
     * @return 산출된 환급 결과
     */
    private OutRefund calculateRefundAmount(String reqId, InpBasic basic,
                                             InpFinancial financial,
                                             Optional<OutCombination> optimalComboOpt) {
        log.debug("[{}] M6-01 최종 환급액 산출 시작", reqId);

        long existingComputedTax = safeLong(basic.getComputedTax());
        long existingPaidTax = safeLong(basic.getPaidTax());
        long existingDeterminedTax = safeLong(financial.getDeterminedTax());

        // 기존 공제액 (기존에 적용받은 공제/감면)
        long existingDeductions = existingComputedTax - existingDeterminedTax;
        if (existingDeductions < 0L) {
            existingDeductions = 0L;
        }

        // 최적 조합에서 신규 공제/감면 합계 및 최저한세 조정액 산출
        long newExemptionTotal = 0L;
        long newCreditTotal = 0L;
        long newMinTaxAdj = 0L;
        long nongteukTotal = 0L;
        String optimalComboId = null;

        if (optimalComboOpt.isPresent()) {
            OutCombination optimalCombo = optimalComboOpt.get();
            newExemptionTotal = safeLong(optimalCombo.getExemptionTotal());
            newCreditTotal = safeLong(optimalCombo.getCreditTotal());
            newMinTaxAdj = safeLong(optimalCombo.getMinTaxAdj());
            nongteukTotal = safeLong(optimalCombo.getNongteukTotal());
            optimalComboId = optimalCombo.getComboId();
        } else {
            // 최적 조합이 없는 경우 개별 공제 합산
            List<OutCreditDetail> credits = outCreditDetailRepository.findByReqIdAndItemStatus(
                    reqId, "applicable");
            for (OutCreditDetail credit : credits) {
                long netAmt = safeLong(credit.getNetAmount());
                nongteukTotal += safeLong(credit.getNongteukAmount());
                if ("감면".equals(credit.getCreditType())) {
                    newExemptionTotal += netAmt;
                } else {
                    newCreditTotal += netAmt;
                }
            }
        }

        long newDeductions = TruncationUtil.truncateAmount(newExemptionTotal + newCreditTotal);
        long newComputedTax = existingComputedTax;

        // 경정후결정세액 = 산출세액 - 신규 감면/공제 합계 + 최저한세 조정액
        long newDeterminedTax = TruncationUtil.truncateAmount(
                newComputedTax - newDeductions + newMinTaxAdj);
        if (newDeterminedTax < 0L) {
            newDeterminedTax = 0L;
        }

        // 최종 환급액 = 기존납부세액 - 경정후결정세액
        long refundAmount = TruncationUtil.truncateAmount(
                existingPaidTax - newDeterminedTax);
        if (refundAmount < 0L) {
            refundAmount = 0L;
        }

        // 이월공제 합계
        long carryforwardCredits = 0L;
        List<OutCreditDetail> allCredits = outCreditDetailRepository.findByReqId(reqId);
        StringBuilder carryforwardDetail = new StringBuilder();
        for (OutCreditDetail credit : allCredits) {
            if (Boolean.TRUE.equals(credit.getIsCarryforward())
                    && credit.getCarryforwardAmount() != null
                    && credit.getCarryforwardAmount() > 0L) {
                carryforwardCredits += credit.getCarryforwardAmount();
                if (carryforwardDetail.length() > 0) {
                    carryforwardDetail.append("; ");
                }
                carryforwardDetail.append(String.format("%s(%s): %d원",
                        credit.getItemName(), credit.getProvision(),
                        credit.getCarryforwardAmount()));
            }
        }

        OutRefund refund = OutRefund.builder()
                .reqId(reqId)
                .existingComputedTax(existingComputedTax)
                .existingDeductions(existingDeductions)
                .existingDeterminedTax(existingDeterminedTax)
                .existingPaidTax(existingPaidTax)
                .newComputedTax(newComputedTax)
                .newDeductions(newDeductions)
                .newMinTaxAdj(newMinTaxAdj)
                .newDeterminedTax(newDeterminedTax)
                .nongteukTotal(TruncationUtil.truncateAmount(nongteukTotal))
                .refundAmount(refundAmount)
                .optimalComboId(optimalComboId)
                .carryforwardCredits(TruncationUtil.truncateAmount(carryforwardCredits))
                .carryforwardDetail(carryforwardDetail.length() > 0
                        ? carryforwardDetail.toString() : null)
                .build();

        outRefundRepository.save(refund);

        saveCalculationLog(reqId, "M6-01", "calculateRefundAmount",
                String.format("기존납부세액=%d, 기존결정세액=%d", existingPaidTax, existingDeterminedTax),
                String.format("신규결정세액=%d, 환급액=%d", newDeterminedTax, refundAmount),
                LogLevel.INFO.getCode(), null);

        log.debug("[{}] M6-01 최종 환급액 산출 완료: 환급액={}", reqId, refundAmount);
        return refund;
    }

    // ================================================================
    // M6-02: 환급가산금 산출
    // ================================================================

    /**
     * M6-02: 환급가산금을 산출한다.
     *
     * <p>
     * 환급가산금 = TRUNCATE(환급액 x 이율 x 일수 / 365, 0).
     * 기산일은 법정신고기한 다음 날부터 환급결정일까지의 일수를 계산한다.
     * 이율은 REF_REFUND_INTEREST_RATE 테이블에서 해당 기간의 이율을 조회한다.
     * </p>
     *
     * @param reqId  요청 ID
     * @param basic  기본 정보
     * @param refund 환급 결과 (M6-01 산출)
     * @return 환급가산금이 반영된 환급 결과
     */
    private OutRefund calculateRefundInterest(String reqId, InpBasic basic,
                                               OutRefund refund) {
        log.debug("[{}] M6-02 환급가산금 산출 시작", reqId);

        long refundAmount = safeLong(refund.getRefundAmount());
        if (refundAmount <= 0L) {
            log.debug("[{}] M6-02 환급액 0 이하, 환급가산금 미산출", reqId);
            return refund;
        }

        // 환급가산금 기산일 = 법정신고기한 다음 날
        LocalDate fiscalEnd = basic.getFiscalEnd();
        LocalDate interestStart;

        if (fiscalEnd != null) {
            // 법인세: 사업연도 종료일 + 3개월 후 다음 날
            // 소득세: 다음 해 5월 31일 다음 날
            if ("CORP".equals(basic.getTaxType())) {
                interestStart = fiscalEnd.plusMonths(3).plusDays(1);
            } else {
                interestStart = LocalDate.of(fiscalEnd.getYear() + 1, 6, 1);
            }
        } else {
            log.warn("[{}] M6-02 사업연도 종료일 없음, 환급가산금 산출 불가", reqId);
            return refund;
        }

        // 환급결정일 = 현재 날짜 (실제로는 환급 결정 시점)
        LocalDate interestEnd = LocalDate.now();

        // 일수 산출
        long days = ChronoUnit.DAYS.between(interestStart, interestEnd);
        if (days <= 0L) {
            log.debug("[{}] M6-02 환급가산금 기간 0일 이하, 미산출", reqId);
            return refund;
        }

        // 이율 조회
        Date targetDate = Date.from(interestEnd.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<RefRefundInterestRate> rates = refRefundInterestRateRepository
                .findByEffectiveDate(targetDate);

        BigDecimal annualRate;
        if (!rates.isEmpty()) {
            annualRate = rates.get(0).getAnnualRate();
        } else {
            log.warn("[{}] M6-02 환급가산금 이율을 찾을 수 없습니다. 기본 이율 적용", reqId);
            annualRate = new BigDecimal("0.02200");
        }

        // 환급가산금 = TRUNCATE(환급액 × 이율 × 일수 / 365, 0)
        long refundInterestAmount = TruncationUtil.truncateInterest(
                new BigDecimal(refundAmount)
                        .multiply(annualRate)
                        .multiply(BigDecimal.valueOf(days))
                        .divide(BigDecimal.valueOf(365), 0, RoundingMode.DOWN)
                        .longValue());

        // 기존 OutRefund 삭제 후 업데이트된 값으로 재저장
        outRefundRepository.deleteByReqId(reqId);

        OutRefund updatedRefund = OutRefund.builder()
                .reqId(reqId)
                .existingComputedTax(refund.getExistingComputedTax())
                .existingDeductions(refund.getExistingDeductions())
                .existingDeterminedTax(refund.getExistingDeterminedTax())
                .existingPaidTax(refund.getExistingPaidTax())
                .newComputedTax(refund.getNewComputedTax())
                .newDeductions(refund.getNewDeductions())
                .newMinTaxAdj(refund.getNewMinTaxAdj())
                .newDeterminedTax(refund.getNewDeterminedTax())
                .nongteukTotal(refund.getNongteukTotal())
                .refundAmount(refundAmount)
                .refundInterestStart(interestStart)
                .refundInterestEnd(interestEnd.toString())
                .refundInterestRate(TruncationUtil.truncateRate(annualRate, 5))
                .refundInterestAmount(refundInterestAmount)
                .localTaxRefund(refund.getLocalTaxRefund())
                .totalExpected(refund.getTotalExpected())
                .optimalComboId(refund.getOptimalComboId())
                .carryforwardCredits(refund.getCarryforwardCredits())
                .carryforwardDetail(refund.getCarryforwardDetail())
                .penaltyTaxChange(refund.getPenaltyTaxChange())
                .build();

        outRefundRepository.save(updatedRefund);

        saveCalculationLog(reqId, "M6-02", "calculateRefundInterest",
                String.format("환급액=%d, 이율=%s, 일수=%d",
                        refundAmount, annualRate.toPlainString(), days),
                String.format("환급가산금=%d", refundInterestAmount),
                LogLevel.INFO.getCode(), null);

        log.debug("[{}] M6-02 환급가산금 산출 완료: 가산금={}", reqId, refundInterestAmount);
        return updatedRefund;
    }

    // ================================================================
    // M6-03: 지방소득세 환급 산출
    // ================================================================

    /**
     * M6-03: 지방소득세 환급액을 산출한다.
     *
     * <p>
     * 지방소득세 환급 = 국세 환급액 x 10%.
     * 총 기대 금액 = 국세 환급액 + 환급가산금 + 지방소득세 환급.
     * </p>
     *
     * @param reqId  요청 ID
     * @param refund 환급 결과 (M6-02 산출)
     * @return 지방세 환급이 반영된 환급 결과
     */
    private OutRefund calculateLocalTaxRefund(String reqId, OutRefund refund) {
        log.debug("[{}] M6-03 지방소득세 환급 산출 시작", reqId);

        long refundAmount = safeLong(refund.getRefundAmount());

        // 지방소득세 환급 = TRUNCATE(국세 환급액 × 10%)
        long localTaxRefund = TruncationUtil.truncateAmount(
                new BigDecimal(refundAmount)
                        .multiply(SystemConstants.LOCAL_TAX_RATE)
                        .setScale(0, RoundingMode.DOWN)
                        .longValue());

        // 총 기대 금액 = 국세 환급액 + 환급가산금 + 지방소득세 환급
        long refundInterestAmount = safeLong(refund.getRefundInterestAmount());
        long totalExpected = TruncationUtil.truncateAmount(
                refundAmount + refundInterestAmount + localTaxRefund);

        // 기존 OutRefund 삭제 후 업데이트된 값으로 재저장
        outRefundRepository.deleteByReqId(reqId);

        OutRefund updatedRefund = OutRefund.builder()
                .reqId(reqId)
                .existingComputedTax(refund.getExistingComputedTax())
                .existingDeductions(refund.getExistingDeductions())
                .existingDeterminedTax(refund.getExistingDeterminedTax())
                .existingPaidTax(refund.getExistingPaidTax())
                .newComputedTax(refund.getNewComputedTax())
                .newDeductions(refund.getNewDeductions())
                .newMinTaxAdj(refund.getNewMinTaxAdj())
                .newDeterminedTax(refund.getNewDeterminedTax())
                .nongteukTotal(refund.getNongteukTotal())
                .refundAmount(refundAmount)
                .refundInterestStart(refund.getRefundInterestStart())
                .refundInterestEnd(refund.getRefundInterestEnd())
                .refundInterestRate(refund.getRefundInterestRate())
                .refundInterestAmount(refundInterestAmount)
                .localTaxRefund(localTaxRefund)
                .totalExpected(totalExpected)
                .optimalComboId(refund.getOptimalComboId())
                .carryforwardCredits(refund.getCarryforwardCredits())
                .carryforwardDetail(refund.getCarryforwardDetail())
                .penaltyTaxChange(refund.getPenaltyTaxChange())
                .build();

        outRefundRepository.save(updatedRefund);

        saveCalculationLog(reqId, "M6-03", "calculateLocalTaxRefund",
                String.format("국세환급=%d", refundAmount),
                String.format("지방세환급=%d, 총기대액=%d", localTaxRefund, totalExpected),
                LogLevel.INFO.getCode(), null);

        log.debug("[{}] M6-03 지방소득세 환급 산출 완료: 지방세환급={}, 총기대액={}",
                reqId, localTaxRefund, totalExpected);
        return updatedRefund;
    }

    // ================================================================
    // M6-04: 사후관리 리스크 평가
    // ================================================================

    /**
     * M6-04: 사후관리 리스크를 평가한다.
     *
     * <p>
     * 세액공제 적용 후 사후관리 의무사항에 대한 리스크를 평가하여
     * {@code OUT_RISK} 테이블에 저장한다.
     * </p>
     *
     * <ul>
     *   <li>고용유지 2년: §29의8 통합고용세액공제 적용 시 2년간 고용 유지 의무</li>
     *   <li>자산처분 2년: §24 통합투자세액공제 적용 시 2년간 자산 보유 의무</li>
     * </ul>
     *
     * @param reqId  요청 ID
     * @param basic  기본 정보
     * @param refund 환급 결과
     */
    private void evaluatePostManagementRisk(String reqId, InpBasic basic,
                                             OutRefund refund) {
        log.debug("[{}] M6-04 사후관리 리스크 평가 시작", reqId);

        List<OutCreditDetail> applicableCredits = outCreditDetailRepository
                .findByReqIdAndItemStatus(reqId, "applicable");

        int riskSeq = 1;

        for (OutCreditDetail credit : applicableCredits) {
            String provision = credit.getProvision();

            // §29의8 통합고용세액공제 - 고용유지 2년 의무
            if (ProvisionCode.ART_29_8.equals(provision)) {
                LocalDate periodStart = basic.getFiscalEnd() != null
                        ? basic.getFiscalEnd().plusDays(1) : LocalDate.now();
                LocalDate periodEnd = periodStart.plusYears(2);

                long potentialClawback = safeLong(credit.getNetAmount());
                long interestSurcharge = TruncationUtil.truncateAmount(
                        new BigDecimal(potentialClawback)
                                .multiply(new BigDecimal("0.022"))
                                .multiply(BigDecimal.valueOf(730))
                                .divide(BigDecimal.valueOf(365), 0, RoundingMode.DOWN)
                                .longValue());

                OutRisk risk = OutRisk.builder()
                        .reqId(reqId)
                        .riskId("RISK-" + String.format("%03d", riskSeq++))
                        .provision(ProvisionCode.ART_29_8)
                        .riskType("고용유지의무")
                        .obligation("상시근로자 수 2년간 유지")
                        .periodStart(periodStart)
                        .periodEnd(periodEnd.toString())
                        .violationAction("공제세액 추징 + 이자상당가산액")
                        .potentialClawback(potentialClawback)
                        .interestSurcharge(interestSurcharge)
                        .riskLevel(potentialClawback > 10_000_000L
                                ? RiskLevel.HIGH.getCode() : RiskLevel.MEDIUM.getCode())
                        .description(String.format(
                                "통합고용세액공제(§29의8) 적용에 따라 %s부터 %s까지 "
                                        + "상시근로자 수를 유지해야 합니다. "
                                        + "미유지 시 공제세액 %d원 추징 및 이자상당가산액 %d원이 부과됩니다.",
                                periodStart, periodEnd, potentialClawback, interestSurcharge))
                        .build();

                outRiskRepository.save(risk);
            }

            // §24 통합투자세액공제 - 자산처분 2년 의무
            if (ProvisionCode.ART_24.equals(provision)) {
                LocalDate periodStart = basic.getFiscalEnd() != null
                        ? basic.getFiscalEnd().plusDays(1) : LocalDate.now();
                LocalDate periodEnd = periodStart.plusYears(2);

                long potentialClawback = safeLong(credit.getNetAmount());
                long interestSurcharge = TruncationUtil.truncateAmount(
                        new BigDecimal(potentialClawback)
                                .multiply(new BigDecimal("0.022"))
                                .multiply(BigDecimal.valueOf(730))
                                .divide(BigDecimal.valueOf(365), 0, RoundingMode.DOWN)
                                .longValue());

                OutRisk risk = OutRisk.builder()
                        .reqId(reqId)
                        .riskId("RISK-" + String.format("%03d", riskSeq++))
                        .provision(ProvisionCode.ART_24)
                        .riskType("자산처분제한")
                        .obligation("투자자산 2년간 보유 의무")
                        .periodStart(periodStart)
                        .periodEnd(periodEnd.toString())
                        .violationAction("공제세액 추징 + 이자상당가산액")
                        .potentialClawback(potentialClawback)
                        .interestSurcharge(interestSurcharge)
                        .riskLevel(potentialClawback > 10_000_000L
                                ? RiskLevel.HIGH.getCode() : RiskLevel.MEDIUM.getCode())
                        .description(String.format(
                                "통합투자세액공제(§24) 적용에 따라 %s부터 %s까지 "
                                        + "투자자산을 처분할 수 없습니다. "
                                        + "처분 시 공제세액 %d원 추징 및 이자상당가산액 %d원이 부과됩니다.",
                                periodStart, periodEnd, potentialClawback, interestSurcharge))
                        .build();

                outRiskRepository.save(risk);
            }

            // §6 창업중소기업감면 - 업종 유지 의무
            if (ProvisionCode.ART_6.equals(provision)) {
                LocalDate periodStart = basic.getFiscalEnd() != null
                        ? basic.getFiscalEnd().plusDays(1) : LocalDate.now();
                LocalDate periodEnd = periodStart.plusYears(2);

                long potentialClawback = safeLong(credit.getNetAmount());

                OutRisk risk = OutRisk.builder()
                        .reqId(reqId)
                        .riskId("RISK-" + String.format("%03d", riskSeq++))
                        .provision(ProvisionCode.ART_6)
                        .riskType("창업감면유지의무")
                        .obligation("창업업종 유지 및 휴·폐업 금지")
                        .periodStart(periodStart)
                        .periodEnd(periodEnd.toString())
                        .violationAction("감면세액 추징")
                        .potentialClawback(potentialClawback)
                        .interestSurcharge(0L)
                        .riskLevel(RiskLevel.MEDIUM.getCode())
                        .description(String.format(
                                "창업중소기업감면(§6) 적용에 따라 창업업종을 유지해야 합니다. "
                                        + "폐업·전업 시 감면세액 %d원이 추징됩니다.",
                                potentialClawback))
                        .build();

                outRiskRepository.save(risk);
            }
        }

        saveCalculationLog(reqId, "M6-04", "evaluatePostManagementRisk",
                String.format("적용항목수=%d", applicableCredits.size()),
                String.format("리스크항목수=%d", riskSeq - 1),
                LogLevel.INFO.getCode(), null);

        log.debug("[{}] M6-04 사후관리 리스크 평가 완료: 리스크항목수={}", reqId, riskSeq - 1);
    }

    // ================================================================
    // M6-05: 보고서 JSON 직렬화
    // ================================================================

    /**
     * M6-05: 최종 보고서 JSON을 직렬화하여 OUT_REPORT_JSON에 저장한다.
     *
     * <p>
     * 7개 섹션(A ~ G)으로 구성된 보고서 JSON을 생성한다.
     * </p>
     *
     * <ul>
     *   <li>Section A: 요청 기본 정보 (신청자, 사업자, 세무 기본사항)</li>
     *   <li>Section B: 적격 진단 결과 (중소기업, 벤처, 결산 확인 등)</li>
     *   <li>Section C: 개별 공제/감면 산출 결과</li>
     *   <li>Section D: 최적 조합 및 환급액 산출 결과</li>
     *   <li>Section E: 사후관리 리스크</li>
     *   <li>Section F: 추가 확인 사항</li>
     *   <li>Section G: 메타 정보 (버전, 생성일시, 법적 고지 등)</li>
     * </ul>
     *
     * @param reqId     요청 ID
     * @param basic     기본 정보
     * @param financial 재무 정보
     * @param refund    환급 결과
     */
    private void serializeReportJson(String reqId, InpBasic basic,
                                      InpFinancial financial, OutRefund refund) {
        log.debug("[{}] M6-05 보고서 JSON 직렬화 시작", reqId);

        try {
            // ── Section A: 요청 기본 정보 ──
            ObjectNode sectionA = objectMapper.createObjectNode();
            sectionA.put("reqId", reqId);
            sectionA.put("taxType", basic.getTaxType());
            sectionA.put("applicantName", basic.getApplicantName());
            sectionA.put("bizRegNo", basic.getBizRegNo());
            sectionA.put("corpSize", basic.getCorpSize());
            sectionA.put("industryCode", basic.getIndustryCode());
            sectionA.put("taxYear", basic.getTaxYear());
            sectionA.put("capitalZone", basic.getCapitalZone());
            sectionA.put("revenue", safeLong(basic.getRevenue()));
            sectionA.put("taxableIncome", safeLong(basic.getTaxableIncome()));
            sectionA.put("computedTax", safeLong(basic.getComputedTax()));
            sectionA.put("paidTax", safeLong(basic.getPaidTax()));
            if (basic.getFiscalStart() != null) {
                sectionA.put("fiscalStart", basic.getFiscalStart().toString());
            }
            if (basic.getFiscalEnd() != null) {
                sectionA.put("fiscalEnd", basic.getFiscalEnd().toString());
            }

            // ── Section B: 적격 진단 결과 ──
            ObjectNode sectionB = objectMapper.createObjectNode();
            Optional<ChkEligibility> eligibilityOpt = chkEligibilityRepository.findByReqId(reqId);
            if (eligibilityOpt.isPresent()) {
                ChkEligibility elig = eligibilityOpt.get();
                sectionB.put("overallStatus", elig.getOverallStatus());
                sectionB.put("smeEligible", elig.getSmeEligible());
                sectionB.put("smallVsMedium", elig.getSmallVsMedium());
                sectionB.put("deadlineEligible", elig.getDeadlineEligible());
                sectionB.put("ventureConfirmed", Boolean.TRUE.equals(elig.getVentureConfirmed()));
                sectionB.put("settlementCheckResult", elig.getSettlementCheckResult());
            }

            List<ChkInspectionLog> inspections = chkInspectionLogRepository
                    .findByReqIdOrderBySortOrder(reqId);
            ArrayNode inspectionArray = objectMapper.createArrayNode();
            for (ChkInspectionLog inspection : inspections) {
                ObjectNode inspNode = objectMapper.createObjectNode();
                inspNode.put("inspectionCode", inspection.getInspectionCode());
                inspNode.put("inspectionName", inspection.getInspectionName());
                inspNode.put("judgment", inspection.getJudgment());
                inspNode.put("summary", inspection.getSummary());
                inspectionArray.add(inspNode);
            }
            sectionB.set("inspections", inspectionArray);

            // ── Section C: 개별 공제/감면 산출 결과 ──
            ObjectNode sectionC = objectMapper.createObjectNode();
            List<OutCreditDetail> credits = outCreditDetailRepository.findByReqId(reqId);
            ArrayNode creditArray = objectMapper.createArrayNode();
            for (OutCreditDetail credit : credits) {
                ObjectNode creditNode = objectMapper.createObjectNode();
                creditNode.put("itemId", credit.getItemId());
                creditNode.put("itemName", credit.getItemName());
                creditNode.put("provision", credit.getProvision());
                creditNode.put("creditType", credit.getCreditType());
                creditNode.put("itemStatus", credit.getItemStatus());
                creditNode.put("grossAmount", safeLong(credit.getGrossAmount()));
                creditNode.put("nongteukExempt", Boolean.TRUE.equals(credit.getNongteukExempt()));
                creditNode.put("nongteukAmount", safeLong(credit.getNongteukAmount()));
                creditNode.put("netAmount", safeLong(credit.getNetAmount()));
                creditNode.put("minTaxSubject", Boolean.TRUE.equals(credit.getMinTaxSubject()));
                creditNode.put("isCarryforward", Boolean.TRUE.equals(credit.getIsCarryforward()));
                if (credit.getDeductionRate() != null) {
                    creditNode.put("deductionRate", credit.getDeductionRate());
                }
                if (credit.getCalcDetail() != null) {
                    creditNode.put("calcDetail", credit.getCalcDetail());
                }
                if (credit.getLegalBasis() != null) {
                    creditNode.put("legalBasis", credit.getLegalBasis());
                }
                creditArray.add(creditNode);
            }
            sectionC.set("credits", creditArray);
            sectionC.put("totalCount", credits.size());

            // ── Section D: 환급액 산출 결과 ──
            ObjectNode sectionD = objectMapper.createObjectNode();
            sectionD.put("existingComputedTax", safeLong(refund.getExistingComputedTax()));
            sectionD.put("existingDeductions", safeLong(refund.getExistingDeductions()));
            sectionD.put("existingDeterminedTax", safeLong(refund.getExistingDeterminedTax()));
            sectionD.put("existingPaidTax", safeLong(refund.getExistingPaidTax()));
            sectionD.put("newComputedTax", safeLong(refund.getNewComputedTax()));
            sectionD.put("newDeductions", safeLong(refund.getNewDeductions()));
            sectionD.put("newMinTaxAdj", safeLong(refund.getNewMinTaxAdj()));
            sectionD.put("newDeterminedTax", safeLong(refund.getNewDeterminedTax()));
            sectionD.put("nongteukTotal", safeLong(refund.getNongteukTotal()));
            sectionD.put("refundAmount", safeLong(refund.getRefundAmount()));
            sectionD.put("refundInterestAmount", safeLong(refund.getRefundInterestAmount()));
            sectionD.put("localTaxRefund", safeLong(refund.getLocalTaxRefund()));
            sectionD.put("totalExpected", safeLong(refund.getTotalExpected()));
            if (refund.getOptimalComboId() != null) {
                sectionD.put("optimalComboId", refund.getOptimalComboId());
            }
            sectionD.put("carryforwardCredits", safeLong(refund.getCarryforwardCredits()));

            // 조합 정보
            List<OutCombination> combos = outCombinationRepository
                    .findByReqIdOrderByComboRankAsc(reqId);
            ArrayNode comboArray = objectMapper.createArrayNode();
            for (OutCombination combo : combos) {
                ObjectNode comboNode = objectMapper.createObjectNode();
                comboNode.put("comboId", combo.getComboId());
                comboNode.put("comboRank", combo.getComboRank() != null ? combo.getComboRank() : 0);
                comboNode.put("comboName", combo.getComboName());
                comboNode.put("netRefund", safeLong(combo.getNetRefund()));
                comboNode.put("isValid", Boolean.TRUE.equals(combo.getIsValid()));
                comboArray.add(comboNode);
            }
            sectionD.set("combinations", comboArray);

            // ── Section E: 사후관리 리스크 ──
            ObjectNode sectionE = objectMapper.createObjectNode();
            List<OutRisk> risks = outRiskRepository.findByReqId(reqId);
            ArrayNode riskArray = objectMapper.createArrayNode();
            for (OutRisk risk : risks) {
                ObjectNode riskNode = objectMapper.createObjectNode();
                riskNode.put("riskId", risk.getRiskId());
                riskNode.put("provision", risk.getProvision());
                riskNode.put("riskType", risk.getRiskType());
                riskNode.put("obligation", risk.getObligation());
                if (risk.getPeriodStart() != null) {
                    riskNode.put("periodStart", risk.getPeriodStart().toString());
                }
                riskNode.put("periodEnd", risk.getPeriodEnd());
                riskNode.put("violationAction", risk.getViolationAction());
                riskNode.put("potentialClawback", safeLong(risk.getPotentialClawback()));
                riskNode.put("interestSurcharge", safeLong(risk.getInterestSurcharge()));
                riskNode.put("riskLevel", risk.getRiskLevel());
                riskNode.put("description", risk.getDescription());
                riskArray.add(riskNode);
            }
            sectionE.set("risks", riskArray);
            sectionE.put("totalRiskCount", risks.size());

            // ── Section F: 추가 확인 사항 ──
            ObjectNode sectionF = objectMapper.createObjectNode();
            List<OutAdditionalCheck> checks = outAdditionalCheckRepository.findByReqId(reqId);
            ArrayNode checkArray = objectMapper.createArrayNode();
            for (OutAdditionalCheck check : checks) {
                ObjectNode checkNode = objectMapper.createObjectNode();
                checkNode.put("checkId", check.getCheckId());
                checkNode.put("description", check.getDescription());
                checkNode.put("reason", check.getReason());
                checkNode.put("priority", check.getPriority());
                checkNode.put("status", check.getStatus());
                checkArray.add(checkNode);
            }
            sectionF.set("additionalChecks", checkArray);

            // ── Section G: 메타 정보 ──
            ObjectNode sectionG = objectMapper.createObjectNode();
            sectionG.put("reportVersion", REPORT_VERSION);
            sectionG.put("generatedAt", LocalDateTime.now().toString());
            sectionG.put("serviceName", "ENTEC Tax Refund Service");
            sectionG.put("taxYear", basic.getTaxYear());
            sectionG.put("disclaimer",
                    "본 보고서는 입력된 데이터를 기반으로 자동 산출된 결과이며, "
                            + "최종적인 세무 판단은 관할 세무서 및 세무전문가의 확인이 필요합니다.");
            sectionG.put("legalNotice",
                    "본 분석은 조세특례제한법, 법인세법, 소득세법 등 관련 법령에 근거하여 수행되었습니다.");

            // 전체 보고서 조립
            ObjectNode fullReport = objectMapper.createObjectNode();
            fullReport.set("sectionA", sectionA);
            fullReport.set("sectionB", sectionB);
            fullReport.set("sectionC", sectionC);
            fullReport.set("sectionD", sectionD);
            fullReport.set("sectionE", sectionE);
            fullReport.set("sectionF", sectionF);
            fullReport.set("sectionG", sectionG);

            String fullReportJson = objectMapper.writeValueAsString(fullReport);
            int jsonByteSize = fullReportJson.getBytes(StandardCharsets.UTF_8).length;

            // 체크섬 생성 (SHA-256 간이 구현)
            String checksum = generateSimpleChecksum(fullReportJson);

            // 성공 코드 판정
            String resultCode = safeLong(refund.getRefundAmount()) > 0L ? "REFUND" : "NO_REFUND";

            OutReportJson reportJson = OutReportJson.builder()
                    .reqId(reqId)
                    .reportVersion(REPORT_VERSION)
                    .reportStatus("COMPLETED")
                    .reportJson(fullReportJson)
                    .sectionAJson(objectMapper.writeValueAsString(sectionA))
                    .sectionBJson(objectMapper.writeValueAsString(sectionB))
                    .sectionCJson(objectMapper.writeValueAsString(sectionC))
                    .sectionDJson(objectMapper.writeValueAsString(sectionD))
                    .sectionEJson(objectMapper.writeValueAsString(sectionE))
                    .sectionFJson(objectMapper.writeValueAsString(sectionF))
                    .sectionGMeta(objectMapper.writeValueAsString(sectionG))
                    .jsonByteSize(jsonByteSize)
                    .resultCode(resultCode)
                    .checksum(checksum)
                    .generatedAt(LocalDateTime.now())
                    .build();

            outReportJsonRepository.save(reportJson);

            saveCalculationLog(reqId, "M6-05", "serializeReportJson",
                    String.format("섹션수=7"),
                    String.format("jsonByteSize=%d, resultCode=%s", jsonByteSize, resultCode),
                    LogLevel.INFO.getCode(), null);

            log.debug("[{}] M6-05 보고서 JSON 직렬화 완료: byteSize={}", reqId, jsonByteSize);

        } catch (Exception e) {
            log.error("[{}] M6-05 보고서 JSON 직렬화 중 오류 발생", reqId, e);
            throw new CalculationException(
                    ErrorCode.CALCULATION_STEP_FAILED,
                    "보고서 JSON 직렬화 중 오류가 발생했습니다: " + e.getMessage(),
                    reqId, "M6-05", e);
        }
    }

    // ================================================================
    // 내부 유틸리티 메서드
    // ================================================================

    /**
     * Long null 안전 변환.
     *
     * @param value Long 값
     * @return null이면 0L, 아니면 원래 값
     */
    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    /**
     * 간이 체크섬을 생성한다.
     *
     * <p>
     * SHA-256을 사용할 수 없는 경우를 대비한 간이 체크섬이다.
     * 실제 운영 환경에서는 {@code java.security.MessageDigest}를 사용한다.
     * </p>
     *
     * @param content 체크섬 대상 문자열
     * @return 16진수 체크섬 문자열
     */
    private String generateSimpleChecksum(String content) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest
                    .getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            log.warn("SHA-256 알고리즘을 사용할 수 없습니다. 대체 체크섬 생성", e);
            return String.valueOf(content.hashCode());
        }
    }

    /**
     * 계산 감사추적 로그를 저장한다.
     *
     * @param reqId        요청 ID
     * @param calcStep     계산 단계
     * @param functionName 함수명
     * @param inputData    입력 데이터
     * @param outputData   출력 데이터
     * @param logLevel     로그 레벨
     * @param durationMs   실행 시간 (밀리초)
     */
    private void saveCalculationLog(String reqId, String calcStep, String functionName,
                                     String inputData, String outputData,
                                     String logLevel, Integer durationMs) {
        LogCalculation logEntry = LogCalculation.builder()
                .reqId(reqId)
                .calcStep(calcStep)
                .functionName(functionName)
                .inputData(inputData)
                .outputData(outputData)
                .legalBasis(calcStep + " 산출")
                .executedAt(LocalDateTime.now())
                .logLevel(logLevel)
                .executedBy("SYSTEM")
                .durationMs(durationMs)
                .build();

        logCalculationRepository.save(logEntry);
    }
}
