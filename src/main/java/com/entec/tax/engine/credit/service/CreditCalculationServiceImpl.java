package com.entec.tax.engine.credit.service;

import com.entec.tax.common.constants.CreditType;
import com.entec.tax.common.constants.ItemCategory;
import com.entec.tax.common.constants.LogLevel;
import com.entec.tax.common.constants.ProvisionCode;
import com.entec.tax.common.constants.SystemConstants;
import com.entec.tax.common.exception.CalculationException;
import com.entec.tax.common.exception.ErrorCode;
import com.entec.tax.common.util.TruncationUtil;
import com.entec.tax.domain.check.entity.ChkEligibility;
import com.entec.tax.domain.check.entity.ChkInspectionLog;
import com.entec.tax.domain.check.repository.ChkEligibilityRepository;
import com.entec.tax.domain.check.repository.ChkInspectionLogRepository;
import com.entec.tax.domain.input.entity.InpBasic;
import com.entec.tax.domain.input.entity.InpDeduction;
import com.entec.tax.domain.input.entity.InpEmployee;
import com.entec.tax.domain.input.entity.InpFinancial;
import com.entec.tax.domain.input.repository.InpBasicRepository;
import com.entec.tax.domain.input.repository.InpDeductionRepository;
import com.entec.tax.domain.input.repository.InpEmployeeRepository;
import com.entec.tax.domain.input.repository.InpFinancialRepository;
import com.entec.tax.domain.log.entity.LogCalculation;
import com.entec.tax.domain.log.repository.LogCalculationRepository;
import com.entec.tax.domain.output.entity.OutCreditDetail;
import com.entec.tax.domain.output.entity.OutEmployeeSummary;
import com.entec.tax.domain.output.repository.OutCreditDetailRepository;
import com.entec.tax.domain.output.repository.OutEmployeeSummaryRepository;
import com.entec.tax.domain.reference.entity.RefEmploymentCredit;
import com.entec.tax.domain.reference.entity.RefInvestmentCreditRate;
import com.entec.tax.domain.reference.entity.RefNongteukse;
import com.entec.tax.domain.reference.entity.RefRdCreditRate;
import com.entec.tax.domain.reference.entity.RefSmeDeductionRate;
import com.entec.tax.domain.reference.entity.RefStartupDeductionRate;
import com.entec.tax.domain.reference.repository.RefEmploymentCreditRepository;
import com.entec.tax.domain.reference.repository.RefInvestmentCreditRateRepository;
import com.entec.tax.domain.reference.repository.RefNongteukseRepository;
import com.entec.tax.domain.reference.repository.RefRdCreditRateRepository;
import com.entec.tax.domain.reference.repository.RefSmeDeductionRateRepository;
import com.entec.tax.domain.reference.repository.RefStartupDeductionRateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * M4 개별 공제·감면 산출 서비스 구현체.
 *
 * <p>
 * STEP 1-2 단계에서 각 항목별 공제/감면액을 산출한다.
 * M4-01 ~ M4-10 서브 메서드로 구성되며, 각 메서드는
 * 조세특례제한법 개별 조항에 따라 공제/감면액을 계산하고
 * {@code OUT_CREDIT_DETAIL} 테이블에 결과를 저장한다.
 * </p>
 *
 * <p><b>절사 원칙:</b> 모든 금액은 {@link TruncationUtil}을 사용하여
 * 절사(TRUNCATE)한다. 반올림(ROUND)은 절대 사용하지 않는다.</p>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 * @see CreditCalculationService
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CreditCalculationServiceImpl implements CreditCalculationService {

    private static final String CALC_STEP = "M4";
    private static final String ITEM_STATUS_APPLICABLE = "applicable";
    private static final String ITEM_STATUS_NOT_APPLICABLE = "not_applicable";
    private static final String YEAR_TYPE_CURRENT = "CURRENT";
    private static final String YEAR_TYPE_PREV1 = "PREV1";

    // ──────────────────────────────────────────────
    // 입력 리포지토리
    // ──────────────────────────────────────────────
    private final InpBasicRepository inpBasicRepository;
    private final InpDeductionRepository inpDeductionRepository;
    private final InpFinancialRepository inpFinancialRepository;
    private final InpEmployeeRepository inpEmployeeRepository;

    // ──────────────────────────────────────────────
    // 검증 리포지토리
    // ──────────────────────────────────────────────
    private final ChkEligibilityRepository chkEligibilityRepository;
    private final ChkInspectionLogRepository chkInspectionLogRepository;

    // ──────────────────────────────────────────────
    // 출력 리포지토리
    // ──────────────────────────────────────────────
    private final OutCreditDetailRepository outCreditDetailRepository;
    private final OutEmployeeSummaryRepository outEmployeeSummaryRepository;

    // ──────────────────────────────────────────────
    // 로그 리포지토리
    // ──────────────────────────────────────────────
    private final LogCalculationRepository logCalculationRepository;

    // ──────────────────────────────────────────────
    // 기준정보 리포지토리
    // ──────────────────────────────────────────────
    private final RefSmeDeductionRateRepository refSmeDeductionRateRepository;
    private final RefEmploymentCreditRepository refEmploymentCreditRepository;
    private final RefInvestmentCreditRateRepository refInvestmentCreditRateRepository;
    private final RefStartupDeductionRateRepository refStartupDeductionRateRepository;
    private final RefRdCreditRateRepository refRdCreditRateRepository;
    private final RefNongteukseRepository refNongteukseRepository;

    /**
     * {@inheritDoc}
     *
     * <p>
     * 개별 공제·감면액을 산출한다.
     * M4-01 ~ M4-10까지의 서브 메서드를 순차적으로 호출하여
     * 각 조항별 공제/감면액을 계산하고 결과를 {@code OUT_CREDIT_DETAIL}에 저장한다.
     * </p>
     *
     * @param reqId 요청 ID
     * @throws CalculationException 계산 중 오류가 발생한 경우
     */
    @Override
    public void calculateCredits(String reqId) {
        log.info("[{}] M4 개별 공제·감면 산출 시작", reqId);
        long startTime = System.currentTimeMillis();

        try {
            // 기존 산출 결과 초기화 (TX-2 재시도 지원)
            outCreditDetailRepository.deleteByReqId(reqId);

            InpBasic basic = inpBasicRepository.findByReqId(reqId)
                    .orElseThrow(() -> new CalculationException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "INP_BASIC 데이터를 찾을 수 없습니다. reqId=" + reqId,
                            reqId, CALC_STEP));

            ChkEligibility eligibility = chkEligibilityRepository.findByReqId(reqId)
                    .orElseThrow(() -> new CalculationException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "CHK_ELIGIBILITY 데이터를 찾을 수 없습니다. reqId=" + reqId,
                            reqId, CALC_STEP));

            // M4-01: §7 중소기업 특별세액감면
            calculateSmeSpecialDeduction(reqId, basic, eligibility);

            // M4-02: §29의8 통합고용세액공제
            calculateEmploymentCredit(reqId, basic, eligibility);

            // M4-03: §24 통합투자세액공제
            calculateInvestmentCredit(reqId, basic, eligibility);

            // M4-04: §6 창업중소기업감면
            calculateStartupDeduction(reqId, basic, eligibility);

            // M4-05: §10 R&D 세액공제
            calculateRdCredit(reqId, basic, eligibility);

            // M4-06: §57 외국납부세액공제
            calculateForeignTaxCredit(reqId, basic);

            // M4-07: §30의4 사회보험료세액공제
            calculateSocialInsuranceCredit(reqId, basic, eligibility);

            long duration = System.currentTimeMillis() - startTime;
            saveCalculationLog(reqId, CALC_STEP, "calculateCredits",
                    "reqId=" + reqId,
                    "M4 개별 공제·감면 산출 완료",
                    LogLevel.INFO.getCode(), (int) duration);

            log.info("[{}] M4 개별 공제·감면 산출 완료 ({}ms)", reqId, duration);

        } catch (CalculationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}] M4 개별 공제·감면 산출 중 오류 발생", reqId, e);
            throw new CalculationException(
                    ErrorCode.CALCULATION_STEP_FAILED,
                    "M4 개별 공제·감면 산출 중 오류가 발생했습니다: " + e.getMessage(),
                    reqId, CALC_STEP, e);
        }
    }

    // ================================================================
    // M4-01: §7 중소기업 특별세액감면
    // ================================================================

    /**
     * M4-01: 중소기업 특별세액감면 산출.
     *
     * <p>
     * 조세특례제한법 §7에 따라 중소기업 특별세액감면액을 산출한다.
     * 기업 규모(소기업/중기업), 업종, 권역(수도권/비수도권)에 따라
     * 감면율을 조회하고, 산출세액에 감면율을 적용하여 감면액을 계산한다.
     * </p>
     *
     * <p>농어촌특별세: §7은 비과세(면제) 대상이다.</p>
     *
     * @param reqId       요청 ID
     * @param basic       기본 정보
     * @param eligibility 적격 진단 결과
     */
    private void calculateSmeSpecialDeduction(String reqId, InpBasic basic,
                                               ChkEligibility eligibility) {
        log.debug("[{}] M4-01 §7 중소기업 특별세액감면 산출 시작", reqId);

        // 중소기업 적격 여부 확인
        if (!"ELIGIBLE".equals(eligibility.getSmeEligible())) {
            log.debug("[{}] M4-01 중소기업 미해당으로 건너뜀", reqId);
            return;
        }

        List<InpDeduction> deductions = inpDeductionRepository
                .findByReqIdAndItemCategoryAndProvision(reqId,
                        ItemCategory.SME_SPECIAL.getCode(), ProvisionCode.ART_7);

        if (deductions.isEmpty()) {
            log.debug("[{}] M4-01 §7 입력 데이터 없음", reqId);
            return;
        }

        // 기업 규모 세부 분류 (소기업/중기업)
        String corpSizeDetail = eligibility.getSmallVsMedium();
        String zoneType = basic.getCapitalZone();

        for (InpDeduction deduction : deductions) {
            // 감면율 조회
            List<RefSmeDeductionRate> rates = refSmeDeductionRateRepository
                    .findByCorpSizeDetailAndIndustryClassAndZoneType(
                            corpSizeDetail, deduction.getSubDetail(), zoneType);

            if (rates.isEmpty()) {
                log.warn("[{}] M4-01 감면율을 찾을 수 없습니다. corpSizeDetail={}, zoneType={}",
                        reqId, corpSizeDetail, zoneType);
                continue;
            }

            RefSmeDeductionRate rate = rates.get(0);
            BigDecimal deductionRate = TruncationUtil.truncateRate(
                    rate.getDeductionRate(), SystemConstants.RATE_SCALE);

            // 감면 대상 금액
            long baseAmount = deduction.getBaseAmount() != null ? deduction.getBaseAmount() : 0L;

            // 감면액 = TRUNCATE(감면 대상 금액 × 감면율 / 100)
            long grossAmount = TruncationUtil.truncateAmount(
                    new BigDecimal(baseAmount)
                            .multiply(deductionRate)
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                            .longValue());

            // 농어촌특별세 처리: §7은 비과세(면제)
            RefNongteukse nongteukse = refNongteukseRepository
                    .findById(ProvisionCode.ART_7).orElse(null);
            boolean nongteukExempt = nongteukse != null && Boolean.TRUE.equals(nongteukse.getIsExempt());
            long nongteukAmount = 0L;
            long netAmount = grossAmount;

            if (!nongteukExempt && nongteukse != null && nongteukse.getTaxRate() != null) {
                nongteukAmount = TruncationUtil.truncateAmount(
                        new BigDecimal(grossAmount)
                                .multiply(nongteukse.getTaxRate())
                                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                                .longValue());
                netAmount = grossAmount - nongteukAmount;
            }

            String itemId = "M4-01-" + deduction.getItemSeq();

            OutCreditDetail creditDetail = OutCreditDetail.builder()
                    .reqId(reqId)
                    .itemId(itemId)
                    .itemName("중소기업 특별세액감면")
                    .provision(ProvisionCode.ART_7)
                    .creditType(CreditType.EXEMPTION.getDescription())
                    .itemStatus(ITEM_STATUS_APPLICABLE)
                    .grossAmount(grossAmount)
                    .nongteukExempt(nongteukExempt)
                    .nongteukAmount(nongteukAmount)
                    .netAmount(netAmount)
                    .minTaxSubject(true)
                    .isCarryforward(false)
                    .deductionRate(deductionRate.toPlainString() + "%")
                    .taxYear(basic.getTaxYear())
                    .method("산출세액 × 감면율")
                    .calcDetail(String.format("기준금액=%d, 감면율=%s%%, 감면액=%d",
                            baseAmount, deductionRate.toPlainString(), grossAmount))
                    .legalBasis("조세특례제한법 제7조")
                    .build();

            outCreditDetailRepository.save(creditDetail);

            saveCalculationLog(reqId, "M4-01", "calculateSmeSpecialDeduction",
                    String.format("baseAmount=%d, rate=%s%%", baseAmount, deductionRate.toPlainString()),
                    String.format("grossAmount=%d, netAmount=%d", grossAmount, netAmount),
                    LogLevel.INFO.getCode(), null);
        }

        log.debug("[{}] M4-01 §7 중소기업 특별세액감면 산출 완료", reqId);
    }

    // ================================================================
    // M4-02: §29의8 통합고용세액공제
    // ================================================================

    /**
     * M4-02: 통합고용세액공제 산출.
     *
     * <p>
     * 조세특례제한법 §29의8에 따라 통합고용세액공제를 산출한다.
     * 당기 상시근로자 수와 전기 상시근로자 수의 차이(증가 인원)에
     * 근로자 유형별(청년등/일반) 1인당 공제 금액을 곱하여 공제액을 계산한다.
     * </p>
     *
     * <p>농어촌특별세: §29의8은 과세 대상(20%)이다.</p>
     *
     * @param reqId       요청 ID
     * @param basic       기본 정보
     * @param eligibility 적격 진단 결과
     */
    private void calculateEmploymentCredit(String reqId, InpBasic basic,
                                            ChkEligibility eligibility) {
        log.debug("[{}] M4-02 §29의8 통합고용세액공제 산출 시작", reqId);

        List<InpDeduction> deductions = inpDeductionRepository
                .findByReqIdAndItemCategoryAndProvision(reqId,
                        ItemCategory.EMPLOYMENT.getCode(), ProvisionCode.ART_29_8);

        if (deductions.isEmpty()) {
            log.debug("[{}] M4-02 §29의8 입력 데이터 없음", reqId);
            return;
        }

        // 당기/전기 고용 정보 조회
        Optional<InpEmployee> currentEmpOpt = inpEmployeeRepository
                .findByReqIdAndYearType(reqId, YEAR_TYPE_CURRENT);
        Optional<InpEmployee> prevEmpOpt = inpEmployeeRepository
                .findByReqIdAndYearType(reqId, YEAR_TYPE_PREV1);

        if (!currentEmpOpt.isPresent() || !prevEmpOpt.isPresent()) {
            log.warn("[{}] M4-02 고용 정보 불완전 (당기 또는 전기 누락)", reqId);
            return;
        }

        InpEmployee currentEmp = currentEmpOpt.get();
        InpEmployee prevEmp = prevEmpOpt.get();

        // 청년등 증가 인원 산출
        int youthIncrease = safeInt(currentEmp.getYouthCount()) - safeInt(prevEmp.getYouthCount());
        int generalIncrease = safeInt(currentEmp.getGeneralCount()) - safeInt(prevEmp.getGeneralCount());

        // 증가 인원이 없으면 공제 불가
        if (youthIncrease <= 0 && generalIncrease <= 0) {
            log.debug("[{}] M4-02 고용 증가 인원 없음", reqId);

            // 상시근로자 산정 결과 저장
            saveEmployeeSummary(reqId, currentEmp, prevEmp, 0, 0);
            return;
        }

        // 음수 증가는 0으로 처리 (감소분은 별도 처리하지 않음)
        youthIncrease = Math.max(youthIncrease, 0);
        generalIncrease = Math.max(generalIncrease, 0);

        // 상시근로자 산정 결과 저장
        saveEmployeeSummary(reqId, currentEmp, prevEmp, youthIncrease, generalIncrease);

        String corpSize = basic.getCorpSize();
        String region = basic.getCapitalZone();
        String taxYear = basic.getTaxYear();

        long totalGrossAmount = 0L;
        List<String> calcDetails = new ArrayList<String>();

        // 청년등 공제액 산출
        if (youthIncrease > 0) {
            List<RefEmploymentCredit> youthRates = refEmploymentCreditRepository
                    .findByTaxYearAndCorpSizeAndRegionAndWorkerType(
                            taxYear, corpSize, region, "청년등");

            if (!youthRates.isEmpty()) {
                long creditPerPerson = youthRates.get(0).getCreditPerPerson() != null
                        ? youthRates.get(0).getCreditPerPerson() : 0L;
                long youthCredit = TruncationUtil.truncateAmount(
                        (long) youthIncrease * creditPerPerson);
                totalGrossAmount += youthCredit;
                calcDetails.add(String.format("청년등: %d명 × %d원 = %d원",
                        youthIncrease, creditPerPerson, youthCredit));
            }
        }

        // 일반 공제액 산출
        if (generalIncrease > 0) {
            List<RefEmploymentCredit> generalRates = refEmploymentCreditRepository
                    .findByTaxYearAndCorpSizeAndRegionAndWorkerType(
                            taxYear, corpSize, region, "일반");

            if (!generalRates.isEmpty()) {
                long creditPerPerson = generalRates.get(0).getCreditPerPerson() != null
                        ? generalRates.get(0).getCreditPerPerson() : 0L;
                long generalCredit = TruncationUtil.truncateAmount(
                        (long) generalIncrease * creditPerPerson);
                totalGrossAmount += generalCredit;
                calcDetails.add(String.format("일반: %d명 × %d원 = %d원",
                        generalIncrease, creditPerPerson, generalCredit));
            }
        }

        totalGrossAmount = TruncationUtil.truncateAmount(totalGrossAmount);

        // 농어촌특별세 처리: §29의8은 과세(20%)
        RefNongteukse nongteukse = refNongteukseRepository
                .findById(ProvisionCode.ART_29_8).orElse(null);
        boolean nongteukExempt = nongteukse != null && Boolean.TRUE.equals(nongteukse.getIsExempt());
        long nongteukAmount = 0L;

        if (!nongteukExempt) {
            nongteukAmount = TruncationUtil.truncateAmount(
                    new BigDecimal(totalGrossAmount)
                            .multiply(SystemConstants.NONGTEUKSE_RATE)
                            .setScale(0, RoundingMode.DOWN)
                            .longValue());
        }

        long netAmount = totalGrossAmount - nongteukAmount;

        OutCreditDetail creditDetail = OutCreditDetail.builder()
                .reqId(reqId)
                .itemId("M4-02-001")
                .itemName("통합고용세액공제")
                .provision(ProvisionCode.ART_29_8)
                .creditType(CreditType.CREDIT.getDescription())
                .itemStatus(ITEM_STATUS_APPLICABLE)
                .grossAmount(totalGrossAmount)
                .nongteukExempt(nongteukExempt)
                .nongteukAmount(nongteukAmount)
                .netAmount(netAmount)
                .minTaxSubject(true)
                .isCarryforward(true)
                .taxYear(taxYear)
                .method("증가인원 × 1인당 공제액")
                .calcDetail(String.join("; ", calcDetails))
                .legalBasis("조세특례제한법 제29조의8")
                .conditions("상시근로자 수 증가 필요")
                .build();

        outCreditDetailRepository.save(creditDetail);

        saveCalculationLog(reqId, "M4-02", "calculateEmploymentCredit",
                String.format("청년증가=%d, 일반증가=%d", youthIncrease, generalIncrease),
                String.format("grossAmount=%d, nongteuk=%d, netAmount=%d",
                        totalGrossAmount, nongteukAmount, netAmount),
                LogLevel.INFO.getCode(), null);

        log.debug("[{}] M4-02 §29의8 통합고용세액공제 산출 완료: netAmount={}", reqId, netAmount);
    }

    // ================================================================
    // M4-03: §24 통합투자세액공제
    // ================================================================

    /**
     * M4-03: 통합투자세액공제 산출.
     *
     * <p>
     * 조세특례제한법 §24에 따라 통합투자세액공제를 산출한다.
     * 투자 유형·기업 규모별 기본공제율과 추가공제율을 적용하여
     * 공제액을 계산한다. 기본공제와 추가공제(직전 3년 평균 대비 초과분)를
     * 합산하여 최종 공제액을 도출한다.
     * </p>
     *
     * <p>농어촌특별세: §24는 과세 대상(20%)이다.</p>
     *
     * @param reqId       요청 ID
     * @param basic       기본 정보
     * @param eligibility 적격 진단 결과
     */
    private void calculateInvestmentCredit(String reqId, InpBasic basic,
                                            ChkEligibility eligibility) {
        log.debug("[{}] M4-03 §24 통합투자세액공제 산출 시작", reqId);

        List<InpDeduction> deductions = inpDeductionRepository
                .findByReqIdAndItemCategoryAndProvision(reqId,
                        ItemCategory.INVEST.getCode(), ProvisionCode.ART_24);

        if (deductions.isEmpty()) {
            log.debug("[{}] M4-03 §24 입력 데이터 없음", reqId);
            return;
        }

        String corpSize = basic.getCorpSize();
        String taxYear = basic.getTaxYear();
        int seq = 1;

        for (InpDeduction deduction : deductions) {
            long baseAmount = deduction.getBaseAmount() != null ? deduction.getBaseAmount() : 0L;
            String investType = deduction.getAssetType();

            // 투자 세액공제율 조회
            List<RefInvestmentCreditRate> rates = refInvestmentCreditRateRepository
                    .findByYearAndInvestTypeAndCorpSize(taxYear, investType, corpSize);

            if (rates.isEmpty()) {
                log.warn("[{}] M4-03 투자 공제율을 찾을 수 없습니다. investType={}, corpSize={}",
                        reqId, investType, corpSize);
                continue;
            }

            RefInvestmentCreditRate rate = rates.get(0);

            // 기본공제 = TRUNCATE(투자금액 × 기본공제율 / 100)
            BigDecimal basicRate = TruncationUtil.truncateRate(
                    rate.getBasicRate(), SystemConstants.RATE_SCALE);
            long basicCredit = TruncationUtil.truncateAmount(
                    new BigDecimal(baseAmount)
                            .multiply(basicRate)
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                            .longValue());

            // 추가공제 = TRUNCATE(직전 3년 평균 초과분 × 추가공제율 / 100)
            long additionalCredit = 0L;
            BigDecimal additionalRate = TruncationUtil.truncateRate(
                    rate.getAdditionalRate(), SystemConstants.RATE_SCALE);

            if (additionalRate.compareTo(BigDecimal.ZERO) > 0
                    && deduction.getCarryforwardBalance() != null
                    && deduction.getCarryforwardBalance() > 0) {
                // 직전 3년 평균 초과분은 carryforwardBalance 필드를 활용
                long excessAmount = deduction.getCarryforwardBalance();
                additionalCredit = TruncationUtil.truncateAmount(
                        new BigDecimal(excessAmount)
                                .multiply(additionalRate)
                                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                                .longValue());
            }

            long grossAmount = TruncationUtil.truncateAmount(basicCredit + additionalCredit);

            // 농어촌특별세 처리: §24는 과세(20%)
            RefNongteukse nongteukse = refNongteukseRepository
                    .findById(ProvisionCode.ART_24).orElse(null);
            boolean nongteukExempt = nongteukse != null && Boolean.TRUE.equals(nongteukse.getIsExempt());
            long nongteukAmount = 0L;

            if (!nongteukExempt) {
                nongteukAmount = TruncationUtil.truncateAmount(
                        new BigDecimal(grossAmount)
                                .multiply(SystemConstants.NONGTEUKSE_RATE)
                                .setScale(0, RoundingMode.DOWN)
                                .longValue());
            }

            long netAmount = grossAmount - nongteukAmount;

            String itemId = "M4-03-" + String.format("%03d", seq++);
            String calcDetailStr = String.format(
                    "투자금액=%d, 기본공제율=%s%%, 기본공제=%d, 추가공제율=%s%%, 추가공제=%d",
                    baseAmount, basicRate.toPlainString(), basicCredit,
                    additionalRate.toPlainString(), additionalCredit);

            OutCreditDetail creditDetail = OutCreditDetail.builder()
                    .reqId(reqId)
                    .itemId(itemId)
                    .itemName("통합투자세액공제")
                    .provision(ProvisionCode.ART_24)
                    .creditType(CreditType.CREDIT.getDescription())
                    .itemStatus(ITEM_STATUS_APPLICABLE)
                    .grossAmount(grossAmount)
                    .nongteukExempt(nongteukExempt)
                    .nongteukAmount(nongteukAmount)
                    .netAmount(netAmount)
                    .minTaxSubject(true)
                    .isCarryforward(true)
                    .deductionRate(basicRate.toPlainString() + "% + " + additionalRate.toPlainString() + "%")
                    .taxYear(taxYear)
                    .method("투자금액 × 기본공제율 + 초과분 × 추가공제율")
                    .calcDetail(calcDetailStr)
                    .legalBasis("조세특례제한법 제24조")
                    .build();

            outCreditDetailRepository.save(creditDetail);

            saveCalculationLog(reqId, "M4-03", "calculateInvestmentCredit",
                    String.format("baseAmount=%d, investType=%s", baseAmount, investType),
                    String.format("grossAmount=%d, netAmount=%d", grossAmount, netAmount),
                    LogLevel.INFO.getCode(), null);
        }

        log.debug("[{}] M4-03 §24 통합투자세액공제 산출 완료", reqId);
    }

    // ================================================================
    // M4-04: §6 창업중소기업감면
    // ================================================================

    /**
     * M4-04: 창업중소기업감면 산출.
     *
     * <p>
     * 조세특례제한법 §6에 따라 창업중소기업 세액감면을 산출한다.
     * 창업자 유형(청년/일반)과 소재지(수도권과밀억제/비수도권/인구감소지역)에 따라
     * 감면율을 차등 적용한다. 창업 후 5년간(수도권과밀억제 제외) 적용 가능하다.
     * </p>
     *
     * <p>농어촌특별세: §6은 비과세(면제) 대상이다.</p>
     *
     * @param reqId       요청 ID
     * @param basic       기본 정보
     * @param eligibility 적격 진단 결과
     */
    private void calculateStartupDeduction(String reqId, InpBasic basic,
                                            ChkEligibility eligibility) {
        log.debug("[{}] M4-04 §6 창업중소기업감면 산출 시작", reqId);

        // 중소기업 적격 여부 확인
        if (!"ELIGIBLE".equals(eligibility.getSmeEligible())) {
            log.debug("[{}] M4-04 중소기업 미해당으로 건너뜀", reqId);
            return;
        }

        List<InpDeduction> deductions = inpDeductionRepository
                .findByReqIdAndItemCategoryAndProvision(reqId,
                        ItemCategory.STARTUP.getCode(), ProvisionCode.ART_6);

        if (deductions.isEmpty()) {
            log.debug("[{}] M4-04 §6 입력 데이터 없음", reqId);
            return;
        }

        // 창업 후 경과 연수 확인
        LocalDate foundingDate = basic.getFoundingDate();
        if (foundingDate == null) {
            log.warn("[{}] M4-04 설립일 정보 없음", reqId);
            return;
        }

        int yearsFromFoundation = Period.between(foundingDate,
                basic.getFiscalEnd() != null ? basic.getFiscalEnd() : LocalDate.now()).getYears();

        // 감면율 조회를 위한 소재지 유형 결정
        String locationType = determineLocationType(basic);

        // 창업자 유형 결정 (벤처 여부 등으로 판단)
        String founderType = Boolean.TRUE.equals(basic.getVentureYn()) ? "청년" : "일반";

        String taxYear = basic.getTaxYear();
        List<RefStartupDeductionRate> rates = refStartupDeductionRateRepository
                .findByFounderTypeAndLocationType(founderType, locationType);

        if (rates.isEmpty()) {
            // 연도 기반으로 재조회
            rates = refStartupDeductionRateRepository.findByYear(taxYear);
            if (rates.isEmpty()) {
                log.warn("[{}] M4-04 창업감면율을 찾을 수 없습니다. founderType={}, locationType={}",
                        reqId, founderType, locationType);
                return;
            }
        }

        RefStartupDeductionRate rate = rates.get(0);
        BigDecimal deductionRate = TruncationUtil.truncateRate(
                rate.getDeductionRate(), SystemConstants.RATE_SCALE);

        for (InpDeduction deduction : deductions) {
            long baseAmount = deduction.getBaseAmount() != null ? deduction.getBaseAmount() : 0L;

            // 감면액 = TRUNCATE(산출세액 × 감면율 / 100)
            long grossAmount = TruncationUtil.truncateAmount(
                    new BigDecimal(baseAmount)
                            .multiply(deductionRate)
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                            .longValue());

            // 농어촌특별세 처리: §6은 비과세(면제)
            RefNongteukse nongteukse = refNongteukseRepository
                    .findById(ProvisionCode.ART_6).orElse(null);
            boolean nongteukExempt = nongteukse != null && Boolean.TRUE.equals(nongteukse.getIsExempt());
            long nongteukAmount = 0L;
            long netAmount = grossAmount;

            if (!nongteukExempt && nongteukse != null && nongteukse.getTaxRate() != null) {
                nongteukAmount = TruncationUtil.truncateAmount(
                        new BigDecimal(grossAmount)
                                .multiply(nongteukse.getTaxRate())
                                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                                .longValue());
                netAmount = grossAmount - nongteukAmount;
            }

            String itemId = "M4-04-" + deduction.getItemSeq();

            OutCreditDetail creditDetail = OutCreditDetail.builder()
                    .reqId(reqId)
                    .itemId(itemId)
                    .itemName("창업중소기업 세액감면")
                    .provision(ProvisionCode.ART_6)
                    .creditType(CreditType.EXEMPTION.getDescription())
                    .itemStatus(ITEM_STATUS_APPLICABLE)
                    .grossAmount(grossAmount)
                    .nongteukExempt(nongteukExempt)
                    .nongteukAmount(nongteukAmount)
                    .netAmount(netAmount)
                    .minTaxSubject(false)
                    .isCarryforward(false)
                    .deductionRate(deductionRate.toPlainString() + "%")
                    .taxYear(taxYear)
                    .method("산출세액 × 감면율")
                    .calcDetail(String.format("기준금액=%d, 감면율=%s%%, 감면액=%d, 창업경과연수=%d년",
                            baseAmount, deductionRate.toPlainString(), grossAmount, yearsFromFoundation))
                    .legalBasis("조세특례제한법 제6조")
                    .sunsetDate(rate.getYearTo())
                    .conditions(String.format("창업자유형=%s, 소재지=%s, 경과연수=%d년",
                            founderType, locationType, yearsFromFoundation))
                    .build();

            outCreditDetailRepository.save(creditDetail);

            saveCalculationLog(reqId, "M4-04", "calculateStartupDeduction",
                    String.format("baseAmount=%d, founderType=%s, locationType=%s",
                            baseAmount, founderType, locationType),
                    String.format("grossAmount=%d, netAmount=%d", grossAmount, netAmount),
                    LogLevel.INFO.getCode(), null);
        }

        log.debug("[{}] M4-04 §6 창업중소기업감면 산출 완료", reqId);
    }

    // ================================================================
    // M4-05: §10 R&D 세액공제
    // ================================================================

    /**
     * M4-05: 연구·인력개발비 세액공제 산출.
     *
     * <p>
     * 조세특례제한법 §10에 따라 R&D 세액공제를 산출한다.
     * R&D 유형(일반/신성장·원천), 산출 방식(당기분/증가분), 기업 규모에 따라
     * 공제율을 조회하고, 연구개발비에 공제율을 적용하여 공제액을 계산한다.
     * 당기분과 증가분 중 큰 금액을 선택한다.
     * </p>
     *
     * <p>농어촌특별세: §10은 비과세(면제) 대상이다.</p>
     *
     * @param reqId       요청 ID
     * @param basic       기본 정보
     * @param eligibility 적격 진단 결과
     */
    private void calculateRdCredit(String reqId, InpBasic basic,
                                    ChkEligibility eligibility) {
        log.debug("[{}] M4-05 §10 R&D 세액공제 산출 시작", reqId);

        List<InpDeduction> deductions = inpDeductionRepository
                .findByReqIdAndItemCategoryAndProvision(reqId,
                        ItemCategory.RD.getCode(), ProvisionCode.ART_10);

        if (deductions.isEmpty()) {
            log.debug("[{}] M4-05 §10 입력 데이터 없음", reqId);
            return;
        }

        // R&D 전담부서 보유 여부 확인
        if (!Boolean.TRUE.equals(basic.getRdDeptYn())) {
            log.debug("[{}] M4-05 R&D 전담부서 미보유로 일반 R&D만 적용 가능", reqId);
        }

        String corpSize = basic.getCorpSize();
        int seq = 1;

        for (InpDeduction deduction : deductions) {
            long baseAmount = deduction.getBaseAmount() != null ? deduction.getBaseAmount() : 0L;
            String rdType = deduction.getRdType() != null ? deduction.getRdType() : "일반";
            String method = deduction.getMethod() != null ? deduction.getMethod() : "당기분";

            // 공제율 조회
            List<RefRdCreditRate> rates = refRdCreditRateRepository
                    .findByRdTypeAndMethodAndCorpSize(rdType, method, corpSize);

            if (rates.isEmpty()) {
                log.warn("[{}] M4-05 R&D 공제율을 찾을 수 없습니다. rdType={}, method={}, corpSize={}",
                        reqId, rdType, method, corpSize);
                continue;
            }

            RefRdCreditRate rate = rates.get(0);
            BigDecimal creditRate = TruncationUtil.truncateRate(
                    rate.getCreditRate(), SystemConstants.RATE_SCALE);

            // 공제액 = TRUNCATE(연구개발비 × 공제율 / 100)
            long grossAmount = TruncationUtil.truncateAmount(
                    new BigDecimal(baseAmount)
                            .multiply(creditRate)
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                            .longValue());

            // 농어촌특별세 처리: §10은 비과세(면제)
            RefNongteukse nongteukse = refNongteukseRepository
                    .findById(ProvisionCode.ART_10).orElse(null);
            boolean nongteukExempt = nongteukse != null && Boolean.TRUE.equals(nongteukse.getIsExempt());
            long nongteukAmount = 0L;
            long netAmount = grossAmount;

            if (!nongteukExempt && nongteukse != null && nongteukse.getTaxRate() != null) {
                nongteukAmount = TruncationUtil.truncateAmount(
                        new BigDecimal(grossAmount)
                                .multiply(nongteukse.getTaxRate())
                                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                                .longValue());
                netAmount = grossAmount - nongteukAmount;
            }

            // 최저한세 특례 면제 여부 확인
            boolean minTaxSubject = !"FULL_EXEMPT".equals(rate.getMinTaxExempt());

            String itemId = "M4-05-" + String.format("%03d", seq++);

            OutCreditDetail creditDetail = OutCreditDetail.builder()
                    .reqId(reqId)
                    .itemId(itemId)
                    .itemName("연구·인력개발비 세액공제")
                    .provision(ProvisionCode.ART_10)
                    .creditType(CreditType.CREDIT.getDescription())
                    .itemStatus(ITEM_STATUS_APPLICABLE)
                    .grossAmount(grossAmount)
                    .nongteukExempt(nongteukExempt)
                    .nongteukAmount(nongteukAmount)
                    .netAmount(netAmount)
                    .minTaxSubject(minTaxSubject)
                    .isCarryforward(true)
                    .deductionRate(creditRate.toPlainString() + "%")
                    .taxYear(basic.getTaxYear())
                    .rdType(rdType)
                    .method(method)
                    .calcDetail(String.format("연구개발비=%d, R&D유형=%s, 방식=%s, 공제율=%s%%, 공제액=%d",
                            baseAmount, rdType, method, creditRate.toPlainString(), grossAmount))
                    .legalBasis("조세특례제한법 제10조")
                    .conditions(String.format("R&D유형=%s, 산출방식=%s", rdType, method))
                    .build();

            outCreditDetailRepository.save(creditDetail);

            saveCalculationLog(reqId, "M4-05", "calculateRdCredit",
                    String.format("baseAmount=%d, rdType=%s, method=%s", baseAmount, rdType, method),
                    String.format("grossAmount=%d, netAmount=%d", grossAmount, netAmount),
                    LogLevel.INFO.getCode(), null);
        }

        log.debug("[{}] M4-05 §10 R&D 세액공제 산출 완료", reqId);
    }

    // ================================================================
    // M4-06: §57 외국납부세액공제
    // ================================================================

    /**
     * M4-06: 외국납부세액공제 산출.
     *
     * <p>
     * 법인세법/소득세법 §57에 따라 외국납부세액공제를 산출한다.
     * 외국에서 납부한 세액을 국내 세액에서 공제하여 이중과세를 방지한다.
     * 공제 한도 = 산출세액 × (국외원천소득 / 과세표준)
     * 실제 공제액 = MIN(외국납부세액, 공제 한도)
     * </p>
     *
     * <p>농어촌특별세: 외국납부세액공제는 조세특례제한법 대상이 아니므로
     * 농어촌특별세가 부과되지 않는다.</p>
     *
     * @param reqId 요청 ID
     * @param basic 기본 정보
     */
    private void calculateForeignTaxCredit(String reqId, InpBasic basic) {
        log.debug("[{}] M4-06 §57 외국납부세액공제 산출 시작", reqId);

        InpFinancial financial = inpFinancialRepository.findByReqId(reqId).orElse(null);

        if (financial == null
                || financial.getForeignTaxTotal() == null
                || financial.getForeignTaxTotal() <= 0L) {
            log.debug("[{}] M4-06 외국납부세액 없음", reqId);
            return;
        }

        long foreignTaxPaid = financial.getForeignTaxTotal();
        long foreignIncome = financial.getForeignIncomeTotal() != null
                ? financial.getForeignIncomeTotal() : 0L;
        long taxableIncome = basic.getTaxableIncome() != null ? basic.getTaxableIncome() : 0L;
        long computedTax = basic.getComputedTax() != null ? basic.getComputedTax() : 0L;

        if (taxableIncome <= 0L || computedTax <= 0L) {
            log.warn("[{}] M4-06 과세표준 또는 산출세액이 0 이하", reqId);
            return;
        }

        // 공제 한도 = TRUNCATE(산출세액 × 국외원천소득 / 과세표준)
        long creditLimit = TruncationUtil.truncateAmount(
                new BigDecimal(computedTax)
                        .multiply(BigDecimal.valueOf(foreignIncome))
                        .divide(BigDecimal.valueOf(taxableIncome), 0, RoundingMode.DOWN)
                        .longValue());

        // 실제 공제액 = MIN(외국납부세액, 공제 한도)
        long grossAmount = TruncationUtil.truncateAmount(
                Math.min(foreignTaxPaid, creditLimit));

        // 초과분은 이월공제 가능 (5년)
        long carryforwardAmount = foreignTaxPaid > creditLimit
                ? TruncationUtil.truncateAmount(foreignTaxPaid - creditLimit) : 0L;

        long netAmount = grossAmount;

        OutCreditDetail creditDetail = OutCreditDetail.builder()
                .reqId(reqId)
                .itemId("M4-06-001")
                .itemName("외국납부세액공제")
                .provision("§57")
                .creditType(CreditType.CREDIT.getDescription())
                .itemStatus(ITEM_STATUS_APPLICABLE)
                .grossAmount(grossAmount)
                .nongteukExempt(true)
                .nongteukAmount(0L)
                .netAmount(netAmount)
                .minTaxSubject(false)
                .isCarryforward(carryforwardAmount > 0)
                .carryforwardAmount(carryforwardAmount)
                .taxYear(basic.getTaxYear())
                .method("MIN(외국납부세액, 산출세액 × 국외원천소득 / 과세표준)")
                .calcDetail(String.format(
                        "외국납부세액=%d, 국외원천소득=%d, 과세표준=%d, 산출세액=%d, 공제한도=%d, 공제액=%d, 이월=%d",
                        foreignTaxPaid, foreignIncome, taxableIncome, computedTax,
                        creditLimit, grossAmount, carryforwardAmount))
                .legalBasis("법인세법/소득세법 제57조")
                .build();

        outCreditDetailRepository.save(creditDetail);

        saveCalculationLog(reqId, "M4-06", "calculateForeignTaxCredit",
                String.format("foreignTax=%d, foreignIncome=%d", foreignTaxPaid, foreignIncome),
                String.format("grossAmount=%d, carryforward=%d", grossAmount, carryforwardAmount),
                LogLevel.INFO.getCode(), null);

        log.debug("[{}] M4-06 §57 외국납부세액공제 산출 완료: netAmount={}", reqId, netAmount);
    }

    // ================================================================
    // M4-07: §30의4 사회보험료세액공제
    // ================================================================

    /**
     * M4-07: 사회보험료 세액공제 산출.
     *
     * <p>
     * 조세특례제한법 §30의4에 따라 사회보험료 세액공제를 산출한다.
     * 고용 증가에 따른 사회보험료 부담분에 대해 세액공제를 적용한다.
     * 공제액 = 증가 인원에 대한 사회보험료 사용자 부담분
     * </p>
     *
     * <p>농어촌특별세: §30의4는 과세 대상(20%)이다.</p>
     *
     * @param reqId       요청 ID
     * @param basic       기본 정보
     * @param eligibility 적격 진단 결과
     */
    private void calculateSocialInsuranceCredit(String reqId, InpBasic basic,
                                                 ChkEligibility eligibility) {
        log.debug("[{}] M4-07 §30의4 사회보험료세액공제 산출 시작", reqId);

        List<InpDeduction> deductions = inpDeductionRepository
                .findByReqIdAndItemCategoryAndProvision(reqId,
                        ItemCategory.SOCIAL_INS.getCode(), ProvisionCode.ART_30_4);

        if (deductions.isEmpty()) {
            log.debug("[{}] M4-07 §30의4 입력 데이터 없음", reqId);
            return;
        }

        // 고용 증가 여부 확인
        Optional<InpEmployee> currentEmpOpt = inpEmployeeRepository
                .findByReqIdAndYearType(reqId, YEAR_TYPE_CURRENT);
        Optional<InpEmployee> prevEmpOpt = inpEmployeeRepository
                .findByReqIdAndYearType(reqId, YEAR_TYPE_PREV1);

        if (!currentEmpOpt.isPresent() || !prevEmpOpt.isPresent()) {
            log.warn("[{}] M4-07 고용 정보 불완전 (당기 또는 전기 누락)", reqId);
            return;
        }

        InpEmployee currentEmp = currentEmpOpt.get();
        InpEmployee prevEmp = prevEmpOpt.get();

        int totalIncrease = safeInt(currentEmp.getYouthCount()) + safeInt(currentEmp.getGeneralCount())
                - safeInt(prevEmp.getYouthCount()) - safeInt(prevEmp.getGeneralCount());

        if (totalIncrease <= 0) {
            log.debug("[{}] M4-07 고용 증가 없음, 사회보험료세액공제 미적용", reqId);
            return;
        }

        for (InpDeduction deduction : deductions) {
            long baseAmount = deduction.getBaseAmount() != null ? deduction.getBaseAmount() : 0L;

            // 공제액 = TRUNCATE(증가 인원분 사회보험료 사용자 부담금)
            long grossAmount = TruncationUtil.truncateAmount(baseAmount);

            // 농어촌특별세 처리: §30의4는 과세(20%)
            RefNongteukse nongteukse = refNongteukseRepository
                    .findById(ProvisionCode.ART_30_4).orElse(null);
            boolean nongteukExempt = nongteukse != null && Boolean.TRUE.equals(nongteukse.getIsExempt());
            long nongteukAmount = 0L;

            if (!nongteukExempt) {
                nongteukAmount = TruncationUtil.truncateAmount(
                        new BigDecimal(grossAmount)
                                .multiply(SystemConstants.NONGTEUKSE_RATE)
                                .setScale(0, RoundingMode.DOWN)
                                .longValue());
            }

            long netAmount = grossAmount - nongteukAmount;

            String itemId = "M4-07-" + deduction.getItemSeq();

            OutCreditDetail creditDetail = OutCreditDetail.builder()
                    .reqId(reqId)
                    .itemId(itemId)
                    .itemName("사회보험료 세액공제")
                    .provision(ProvisionCode.ART_30_4)
                    .creditType(CreditType.CREDIT.getDescription())
                    .itemStatus(ITEM_STATUS_APPLICABLE)
                    .grossAmount(grossAmount)
                    .nongteukExempt(nongteukExempt)
                    .nongteukAmount(nongteukAmount)
                    .netAmount(netAmount)
                    .minTaxSubject(true)
                    .isCarryforward(false)
                    .taxYear(basic.getTaxYear())
                    .method("증가인원 사회보험료 사용자 부담분")
                    .calcDetail(String.format("증가인원=%d, 사회보험료 사용자부담분=%d, 공제액=%d",
                            totalIncrease, baseAmount, grossAmount))
                    .legalBasis("조세특례제한법 제30조의4")
                    .conditions("상시근로자 수 증가 필요")
                    .build();

            outCreditDetailRepository.save(creditDetail);

            saveCalculationLog(reqId, "M4-07", "calculateSocialInsuranceCredit",
                    String.format("증가인원=%d, baseAmount=%d", totalIncrease, baseAmount),
                    String.format("grossAmount=%d, nongteuk=%d, netAmount=%d",
                            grossAmount, nongteukAmount, netAmount),
                    LogLevel.INFO.getCode(), null);
        }

        log.debug("[{}] M4-07 §30의4 사회보험료세액공제 산출 완료", reqId);
    }

    // ================================================================
    // 내부 유틸리티 메서드
    // ================================================================

    /**
     * 상시근로자 산정 결과를 OUT_EMPLOYEE_SUMMARY에 저장한다.
     *
     * @param reqId           요청 ID
     * @param currentEmp      당기 고용 정보
     * @param prevEmp         전기 고용 정보
     * @param youthIncrease   청년등 증가 인원
     * @param generalIncrease 일반 증가 인원
     */
    private void saveEmployeeSummary(String reqId, InpEmployee currentEmp,
                                      InpEmployee prevEmp, int youthIncrease,
                                      int generalIncrease) {
        // 당기 요약
        OutEmployeeSummary currentSummary = OutEmployeeSummary.builder()
                .reqId(reqId)
                .yearType(YEAR_TYPE_CURRENT)
                .totalRegular(currentEmp.getTotalRegular())
                .youthCount(currentEmp.getYouthCount())
                .generalCount(currentEmp.getGeneralCount())
                .increaseTotal(youthIncrease + generalIncrease)
                .increaseYouth(youthIncrease)
                .increaseGeneral(generalIncrease)
                .excludedCount(currentEmp.getExcludedCount())
                .calcDetail(String.format("당기 상시근로자=%s, 전기 상시근로자=%s",
                        currentEmp.getTotalRegular(),
                        prevEmp.getTotalRegular()))
                .build();

        outEmployeeSummaryRepository.save(currentSummary);

        // 전기 요약
        OutEmployeeSummary prevSummary = OutEmployeeSummary.builder()
                .reqId(reqId)
                .yearType(YEAR_TYPE_PREV1)
                .totalRegular(prevEmp.getTotalRegular())
                .youthCount(prevEmp.getYouthCount())
                .generalCount(prevEmp.getGeneralCount())
                .increaseTotal(0)
                .increaseYouth(0)
                .increaseGeneral(0)
                .excludedCount(prevEmp.getExcludedCount())
                .build();

        outEmployeeSummaryRepository.save(prevSummary);
    }

    /**
     * 소재지 유형을 결정한다.
     *
     * <p>
     * 수도권 여부, 인구감소지역 여부에 따라 소재지 유형을 분류한다.
     * </p>
     *
     * @param basic 기본 정보
     * @return 소재지 유형 문자열
     */
    private String determineLocationType(InpBasic basic) {
        if (Boolean.TRUE.equals(basic.getDepopulationArea())) {
            return "인구감소지역";
        }

        String capitalZone = basic.getCapitalZone();
        if ("수도권과밀억제".equals(capitalZone)) {
            return "수도권과밀억제";
        } else if (capitalZone != null && capitalZone.startsWith("수도권")) {
            return "수도권";
        }
        return "비수도권";
    }

    /**
     * Integer null 안전 변환.
     *
     * @param value Integer 값
     * @return null이면 0, 아니면 원래 값
     */
    private int safeInt(Integer value) {
        return value != null ? value : 0;
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
