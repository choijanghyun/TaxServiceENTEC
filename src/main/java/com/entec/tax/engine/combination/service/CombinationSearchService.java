package com.entec.tax.engine.combination.service;

import com.entec.tax.common.constants.CreditType;
import com.entec.tax.common.constants.SystemConstants;
import com.entec.tax.common.exception.CalculationException;
import com.entec.tax.common.exception.ErrorCode;
import com.entec.tax.common.util.JsonUtil;
import com.entec.tax.common.util.TruncationUtil;
import com.entec.tax.domain.check.entity.ChkEligibility;
import com.entec.tax.domain.check.repository.ChkEligibilityRepository;
import com.entec.tax.domain.input.entity.InpBasic;
import com.entec.tax.domain.input.entity.InpFinancial;
import com.entec.tax.domain.input.repository.InpBasicRepository;
import com.entec.tax.domain.input.repository.InpFinancialRepository;
import com.entec.tax.domain.log.entity.LogCalculation;
import com.entec.tax.domain.log.repository.LogCalculationRepository;
import com.entec.tax.domain.output.entity.OutCombination;
import com.entec.tax.domain.output.entity.OutCreditDetail;
import com.entec.tax.domain.output.entity.OutExclusionVerify;
import com.entec.tax.domain.output.repository.OutCombinationRepository;
import com.entec.tax.domain.output.repository.OutCreditDetailRepository;
import com.entec.tax.domain.output.repository.OutExclusionVerifyRepository;
import com.entec.tax.domain.reference.entity.RefMinTaxRate;
import com.entec.tax.domain.reference.entity.RefMutualExclusion;
import com.entec.tax.domain.reference.entity.RefNongteukse;
import com.entec.tax.domain.reference.entity.RefRdMinTaxExempt;
import com.entec.tax.domain.reference.entity.RefSystemParam;
import com.entec.tax.domain.reference.repository.RefMinTaxRateRepository;
import com.entec.tax.domain.reference.repository.RefMutualExclusionRepository;
import com.entec.tax.domain.reference.repository.RefNongteukseRepository;
import com.entec.tax.domain.reference.repository.RefRdMinTaxExemptRepository;
import com.entec.tax.domain.reference.repository.RefSystemParamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * M5 최적 조합 탐색 서비스 구현체 (STEP 3 - 최적 조합 탐색).
 *
 * <p>
 * 상호배제 규칙, 최저한세 제약, 농어촌특별세를 반영하여
 * 환급액을 최대화하는 공제·감면 조합을 탐색한다.
 * </p>
 *
 * <h3>주요 처리 단계</h3>
 * <ul>
 *   <li><b>M5-01:</b> 상호배제 그룹 분리 (Group A / Group B)</li>
 *   <li><b>M5-02:</b> 조합 탐색 (B&amp;B ≤15개, Greedy &gt;15개)</li>
 *   <li><b>M5-03:</b> 최저한세 적용 (법인: 과세표준 기준, 개인: 산출세액 기준)</li>
 * </ul>
 *
 * <h3>핵심 비즈니스 규칙</h3>
 * <ul>
 *   <li>최저한세율: 중소 7%, 중견/대기업(~100억 10%, ~1000억 12%, 초과 17%)</li>
 *   <li>R&amp;D 최저한세 배제 3단계: 국가전략 100%, 신성장중소 100%, 일반중소 50%</li>
 *   <li>법인세법 §59 적용순서: 감면 → 이월불가공제 → 이월가능공제</li>
 *   <li>감면 초과분 → 소멸, 공제 초과분 → 이월 (10년)</li>
 *   <li>실질환급액 = 총적용액 - 농특세</li>
 *   <li>순환참조 해결: 최대 5회 반복, 1원 수렴</li>
 * </ul>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CombinationSearchService {

    // ──────────────────────────────────────────────
    // 의존성 주입 (생성자 주입 via @RequiredArgsConstructor)
    // ──────────────────────────────────────────────

    private final OutCreditDetailRepository outCreditDetailRepository;
    private final OutCombinationRepository outCombinationRepository;
    private final OutExclusionVerifyRepository outExclusionVerifyRepository;
    private final InpBasicRepository inpBasicRepository;
    private final InpFinancialRepository inpFinancialRepository;
    private final ChkEligibilityRepository chkEligibilityRepository;
    private final LogCalculationRepository logCalculationRepository;
    private final RefMutualExclusionRepository refMutualExclusionRepository;
    private final RefMinTaxRateRepository refMinTaxRateRepository;
    private final RefNongteukseRepository refNongteukseRepository;
    private final RefRdMinTaxExemptRepository refRdMinTaxExemptRepository;
    private final RefSystemParamRepository refSystemParamRepository;

    // ──────────────────────────────────────────────
    // 상수
    // ──────────────────────────────────────────────

    /** 기본 Greedy 전환 임계값 (항목 수) */
    private static final int DEFAULT_GREEDY_THRESHOLD = 15;

    /** 기본 조합 탐색 타임아웃 (초) */
    private static final int DEFAULT_COMBO_TIMEOUT_SEC = 120;

    /** 이월공제 최대 연수 */
    private static final int CARRYFORWARD_MAX_YEARS = 10;

    /** 법인세 세목 코드 */
    private static final String TAX_TYPE_CORP = "CORP";

    /** 소득세 세목 코드 */
    private static final String TAX_TYPE_INC = "INC";

    /** 중소기업 규모 코드 */
    private static final String CORP_SIZE_SME = "중소";

    /** 계산 단계 식별자 */
    private static final String CALC_STEP = "M5";

    // ══════════════════════════════════════════════
    // 메인 엔트리 포인트
    // ══════════════════════════════════════════════

    /**
     * 최적 공제·감면 조합을 탐색한다 (STEP 3 전체 수행).
     *
     * <p>
     * M5-01(상호배제 그룹 분리) → M5-02(조합 탐색) → M5-03(최저한세 적용)
     * 순서로 처리하며, 결과를 OUT_COMBINATION 및 OUT_EXCLUSION_VERIFY 테이블에 저장한다.
     * </p>
     *
     * @param reqId 요청 ID (INP_BASIC.req_id)
     * @throws CalculationException 필수 데이터 누락 또는 계산 오류 시
     */
    public void findOptimalCombination(String reqId) {
        long startTime = System.currentTimeMillis();
        log.info("[M5] 최적 조합 탐색 시작 - reqId: {}", reqId);

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

            // 적용 가능(applicable) 상태의 공제·감면 항목만 추출
            List<OutCreditDetail> applicableItems =
                    outCreditDetailRepository.findByReqIdAndItemStatus(reqId, "applicable");

            if (applicableItems.isEmpty()) {
                log.warn("[M5] 적용 가능한 공제·감면 항목이 없습니다. reqId: {}", reqId);
                saveEmptyCombination(reqId);
                saveCalcLog(reqId, "M5", "findOptimalCombination",
                        "적용 가능 항목 0건", "빈 조합 저장", startTime);
                return;
            }

            // ── 2. 기존 결과 삭제 (재시도 지원) ──
            outCombinationRepository.deleteByReqId(reqId);
            outExclusionVerifyRepository.deleteByReqId(reqId);

            // ── 3. 시스템 파라미터 로딩 ──
            int greedyThreshold = getIntSystemParam("greedy_fallback_threshold", DEFAULT_GREEDY_THRESHOLD);
            int comboTimeoutSec = getIntSystemParam("combination_search_timeout", DEFAULT_COMBO_TIMEOUT_SEC);
            long comboDeadline = System.currentTimeMillis() + (comboTimeoutSec * 1000L);

            // ── 4. M5-01: 상호배제 그룹 분리 ──
            String taxYear = basic.getTaxYear();
            MutualExclusionResult exclusionResult = separateMutualExclusionGroups(reqId, applicableItems, taxYear);

            // ── 5. M5-02: 조합 탐색 ──
            List<List<OutCreditDetail>> candidateCombinations;
            if (exclusionResult.independentItems.size() <= greedyThreshold) {
                candidateCombinations = searchByBranchAndBound(
                        exclusionResult, greedyThreshold, comboDeadline);
            } else {
                log.info("[M5-02] 항목 수 {}개 > 임계값 {}개, Greedy 탐색으로 전환",
                        exclusionResult.independentItems.size(), greedyThreshold);
                candidateCombinations = searchByGreedy(exclusionResult);
            }

            // ── 6. M5-03: 최저한세 적용 및 순 환급액 계산 ──
            String corpSize = basic.getCorpSize();
            String taxType = basic.getTaxType();
            Long taxableIncome = basic.getTaxableIncome();
            Long computedTax = basic.getComputedTax();

            List<CombinationCandidate> rankedCandidates = new ArrayList<>();
            AtomicInteger comboSeq = new AtomicInteger(1);

            for (List<OutCreditDetail> combo : candidateCombinations) {
                CombinationCandidate candidate = applyMinimumTaxAndCalculateNet(
                        reqId, combo, corpSize, taxType, taxableIncome, computedTax, comboSeq.getAndIncrement());
                rankedCandidates.add(candidate);
            }

            // 순 환급액(netRefund) 기준 내림차순 정렬
            rankedCandidates.sort(Comparator.comparingLong(CombinationCandidate::getNetRefund).reversed());

            // ── 7. 결과 저장 ──
            int rank = 1;
            for (CombinationCandidate candidate : rankedCandidates) {
                saveOutCombination(reqId, candidate, rank);
                rank++;
            }

            // 배제 검증 결과 저장
            saveExclusionVerifyResults(reqId, exclusionResult);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[M5] 최적 조합 탐색 완료 - reqId: {}, 후보 수: {}, 소요: {}ms",
                    reqId, rankedCandidates.size(), elapsed);

            saveCalcLog(reqId, "M5", "findOptimalCombination",
                    "적용 가능 항목 " + applicableItems.size() + "건",
                    "후보 조합 " + rankedCandidates.size() + "건, 최적 순환급액="
                            + (rankedCandidates.isEmpty() ? 0 : rankedCandidates.get(0).getNetRefund()),
                    startTime);

        } catch (CalculationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[M5] 최적 조합 탐색 중 오류 발생 - reqId: {}", reqId, e);
            throw new CalculationException(
                    ErrorCode.CALCULATION_STEP_FAILED,
                    "최적 조합 탐색 중 오류 발생: " + e.getMessage(),
                    reqId, CALC_STEP, e);
        }
    }

    // ══════════════════════════════════════════════
    // M5-01: 상호배제 그룹 분리
    // ══════════════════════════════════════════════

    /**
     * 상호배제 그룹을 분리한다 (M5-01).
     *
     * <p>
     * REF_MUTUAL_EXCLUSION 테이블을 참조하여 동시 적용 불가 항목 쌍을 식별하고,
     * Group A(독립 항목)과 Group B(상호배제 그룹) 로 분리한다.
     * </p>
     *
     * @param reqId           요청 ID
     * @param applicableItems 적용 가능한 공제·감면 항목 목록
     * @param taxYear         귀속 연도
     * @return 상호배제 분리 결과 (독립 항목 + 배제 그룹 쌍 목록)
     */
    private MutualExclusionResult separateMutualExclusionGroups(
            String reqId, List<OutCreditDetail> applicableItems, String taxYear) {

        log.debug("[M5-01] 상호배제 그룹 분리 시작 - reqId: {}, 항목 수: {}", reqId, applicableItems.size());

        // 귀속 연도에 유효한 배제 규칙 조회
        List<RefMutualExclusion> exclusionRules = refMutualExclusionRepository.findByYear(taxYear);

        // 항목의 조항 코드 집합
        Set<String> itemProvisions = applicableItems.stream()
                .map(OutCreditDetail::getProvision)
                .collect(Collectors.toSet());

        // 실제 적용 가능한 배제 쌍 필터링
        List<ExclusionPair> exclusionPairs = new ArrayList<>();
        Set<String> involvedProvisions = new HashSet<>();

        for (RefMutualExclusion rule : exclusionRules) {
            // 배제 규칙의 양쪽 조항이 모두 적용 가능 항목에 존재하고, 동시 적용 불가인 경우
            if (!Boolean.TRUE.equals(rule.getIsAllowed())
                    && itemProvisions.contains(rule.getProvisionA())
                    && itemProvisions.contains(rule.getProvisionB())) {

                exclusionPairs.add(new ExclusionPair(
                        rule.getProvisionA(), rule.getProvisionB(),
                        rule.getConditionNote(), rule.getLegalBasis()));

                involvedProvisions.add(rule.getProvisionA());
                involvedProvisions.add(rule.getProvisionB());
            }
        }

        // Group A: 상호배제에 관여하지 않는 독립 항목
        List<OutCreditDetail> independentItems = applicableItems.stream()
                .filter(item -> !involvedProvisions.contains(item.getProvision()))
                .collect(Collectors.toList());

        // Group B: 상호배제에 관여하는 항목
        List<OutCreditDetail> exclusionInvolvedItems = applicableItems.stream()
                .filter(item -> involvedProvisions.contains(item.getProvision()))
                .collect(Collectors.toList());

        log.debug("[M5-01] 상호배제 그룹 분리 완료 - 독립 항목: {}건, 배제 관여 항목: {}건, 배제 쌍: {}건",
                independentItems.size(), exclusionInvolvedItems.size(), exclusionPairs.size());

        MutualExclusionResult result = new MutualExclusionResult();
        result.independentItems = independentItems;
        result.exclusionInvolvedItems = exclusionInvolvedItems;
        result.exclusionPairs = exclusionPairs;
        return result;
    }

    // ══════════════════════════════════════════════
    // M5-02: 조합 탐색 (Branch & Bound)
    // ══════════════════════════════════════════════

    /**
     * Branch &amp; Bound 알고리즘으로 최적 조합을 탐색한다 (항목 수 ≤ 임계값).
     *
     * <p>
     * 상호배제 관여 항목에 대해 가능한 모든 유효 조합을 생성하고,
     * 독립 항목과 결합하여 후보 조합 리스트를 반환한다.
     * 타임아웃 초과 시 탐색을 중단하고 현재까지의 최선 결과를 반환한다.
     * </p>
     *
     * @param exclusionResult 상호배제 분리 결과
     * @param threshold       Greedy 전환 임계값
     * @param deadline        탐색 마감 시각 (System.currentTimeMillis 기준)
     * @return 후보 조합 리스트 (각 조합은 OutCreditDetail 항목 목록)
     */
    private List<List<OutCreditDetail>> searchByBranchAndBound(
            MutualExclusionResult exclusionResult, int threshold, long deadline) {

        log.debug("[M5-02] Branch & Bound 탐색 시작");

        List<OutCreditDetail> exclusionItems = exclusionResult.exclusionInvolvedItems;
        List<ExclusionPair> pairs = exclusionResult.exclusionPairs;
        List<OutCreditDetail> independentItems = exclusionResult.independentItems;

        // 배제 관여 항목이 없으면 독립 항목만으로 단일 조합 생성
        if (exclusionItems.isEmpty()) {
            List<List<OutCreditDetail>> result = new ArrayList<>();
            result.add(new ArrayList<>(independentItems));
            return result;
        }

        // 배제 관여 항목의 유효 조합 생성 (2^n 탐색)
        List<List<OutCreditDetail>> validExclusionCombos = new ArrayList<>();
        int n = exclusionItems.size();
        int totalSubsets = 1 << n;

        for (int mask = 0; mask < totalSubsets; mask++) {
            // 타임아웃 체크
            if (System.currentTimeMillis() > deadline) {
                log.warn("[M5-02] Branch & Bound 탐색 타임아웃 - 현재까지 {}건 확보", validExclusionCombos.size());
                break;
            }

            List<OutCreditDetail> subset = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(exclusionItems.get(i));
                }
            }

            // 상호배제 규칙 위반 검사
            if (isValidCombination(subset, pairs)) {
                validExclusionCombos.add(subset);
            }
        }

        // 각 유효 배제 조합에 독립 항목을 결합
        List<List<OutCreditDetail>> result = new ArrayList<>();
        for (List<OutCreditDetail> exclusionCombo : validExclusionCombos) {
            List<OutCreditDetail> fullCombo = new ArrayList<>(independentItems);
            fullCombo.addAll(exclusionCombo);
            result.add(fullCombo);
        }

        log.debug("[M5-02] Branch & Bound 탐색 완료 - 유효 조합: {}건", result.size());
        return result;
    }

    /**
     * Greedy 알고리즘으로 조합을 탐색한다 (항목 수 &gt; 임계값).
     *
     * <p>
     * 순 공제 금액(netAmount) 기준 내림차순 정렬 후,
     * 상호배제 규칙을 위반하지 않는 항목을 순차적으로 선택한다.
     * </p>
     *
     * @param exclusionResult 상호배제 분리 결과
     * @return 후보 조합 리스트 (Greedy 결과 1건)
     */
    private List<List<OutCreditDetail>> searchByGreedy(MutualExclusionResult exclusionResult) {
        log.debug("[M5-02] Greedy 탐색 시작");

        List<OutCreditDetail> allItems = new ArrayList<>(exclusionResult.independentItems);
        allItems.addAll(exclusionResult.exclusionInvolvedItems);

        // 순 공제 금액 기준 내림차순 정렬
        allItems.sort(Comparator.comparingLong(
                (OutCreditDetail item) -> item.getNetAmount() != null ? item.getNetAmount() : 0L).reversed());

        List<OutCreditDetail> selected = new ArrayList<>();
        Set<String> selectedProvisions = new HashSet<>();

        for (OutCreditDetail item : allItems) {
            // 현재 선택된 항목과 상호배제 규칙 위반 여부 확인
            boolean conflictsExist = false;
            for (ExclusionPair pair : exclusionResult.exclusionPairs) {
                String prov = item.getProvision();
                if ((pair.provisionA.equals(prov) && selectedProvisions.contains(pair.provisionB))
                        || (pair.provisionB.equals(prov) && selectedProvisions.contains(pair.provisionA))) {
                    conflictsExist = true;
                    break;
                }
            }
            if (!conflictsExist) {
                selected.add(item);
                selectedProvisions.add(item.getProvision());
            }
        }

        List<List<OutCreditDetail>> result = new ArrayList<>();
        result.add(selected);

        log.debug("[M5-02] Greedy 탐색 완료 - 선택 항목: {}건", selected.size());
        return result;
    }

    /**
     * 주어진 항목 조합이 상호배제 규칙을 위반하지 않는지 검사한다.
     *
     * @param subset 항목 부분 집합
     * @param pairs  상호배제 쌍 목록
     * @return 유효하면 true, 위반이 있으면 false
     */
    private boolean isValidCombination(List<OutCreditDetail> subset, List<ExclusionPair> pairs) {
        Set<String> provisions = subset.stream()
                .map(OutCreditDetail::getProvision)
                .collect(Collectors.toSet());

        for (ExclusionPair pair : pairs) {
            if (provisions.contains(pair.provisionA) && provisions.contains(pair.provisionB)) {
                return false;
            }
        }
        return true;
    }

    // ══════════════════════════════════════════════
    // M5-03: 최저한세 적용 및 순 환급액 계산
    // ══════════════════════════════════════════════

    /**
     * 최저한세를 적용하고 순 환급액을 계산한다 (M5-03).
     *
     * <p>
     * 법인세법 §59 적용순서에 따라 감면 → 이월불가공제 → 이월가능공제 순으로 적용하며,
     * 최저한세 제약 내에서 공제·감면 한도를 산출한다.
     * R&amp;D 세액공제의 최저한세 초과 적용 특례(3단계)를 반영한다.
     * 순환참조 해결을 위해 최대 5회 반복하며 1원 수렴 시 중단한다.
     * </p>
     *
     * <h4>최저한세 기준</h4>
     * <ul>
     *   <li>법인(CORP): 과세표준(taxableIncome) 기준으로 최저한세율 적용</li>
     *   <li>개인(INC): 산출세액(computedTax) 기준으로 최저한세율 적용</li>
     * </ul>
     *
     * @param reqId         요청 ID
     * @param comboItems    조합에 포함된 공제·감면 항목 목록
     * @param corpSize      기업 규모 (중소/중견/대)
     * @param taxType       세목 코드 (CORP/INC)
     * @param taxableIncome 과세표준 (원)
     * @param computedTax   산출세액 (원)
     * @param comboSeq      조합 순번
     * @return 조합 후보 결과 (순 환급액, 최저한세 조정액, 농특세 등 포함)
     */
    private CombinationCandidate applyMinimumTaxAndCalculateNet(
            String reqId, List<OutCreditDetail> comboItems,
            String corpSize, String taxType,
            Long taxableIncome, Long computedTax, int comboSeq) {

        log.debug("[M5-03] 최저한세 적용 시작 - reqId: {}, 조합 #{}, 항목 수: {}",
                reqId, comboSeq, comboItems.size());

        // 최저한세 기준 금액 결정: 법인=과세표준, 개인=산출세액
        long baseAmount;
        if (TAX_TYPE_CORP.equals(taxType)) {
            baseAmount = taxableIncome != null ? taxableIncome : 0L;
        } else {
            baseAmount = computedTax != null ? computedTax : 0L;
        }

        long currentComputedTax = computedTax != null ? computedTax : 0L;

        // 최저한세율 조회
        BigDecimal minTaxRate = resolveMinTaxRate(corpSize, baseAmount);

        // 최저한세액 산출: TRUNCATE(산출세액 × 최저한세율, 0)
        long minTaxAmount = TruncationUtil.truncateAmount(
                new BigDecimal(currentComputedTax)
                        .multiply(minTaxRate)
                        .setScale(0, RoundingMode.DOWN)
                        .longValue());

        // 공제·감면 가능 한도 = 산출세액 - 최저한세액
        long maxDeductible = Math.max(0L, currentComputedTax - minTaxAmount);

        // 법인세법 §59 적용순서에 따라 항목 분류
        List<OutCreditDetail> exemptions = new ArrayList<>();       // 감면
        List<OutCreditDetail> nonCarryCredits = new ArrayList<>();  // 이월불가 공제
        List<OutCreditDetail> carryCredits = new ArrayList<>();     // 이월가능 공제

        for (OutCreditDetail item : comboItems) {
            if (CreditType.EXEMPTION.getCode().equals(item.getCreditType())) {
                exemptions.add(item);
            } else if (Boolean.TRUE.equals(item.getIsCarryforward())) {
                carryCredits.add(item);
            } else {
                nonCarryCredits.add(item);
            }
        }

        // ── 순환참조 해결 루프 (최대 5회, 1원 수렴) ──
        long prevNetRefund = Long.MIN_VALUE;
        long totalExemption = 0L;
        long totalCredit = 0L;
        long minTaxAdj = 0L;
        long totalNongteuk = 0L;
        long totalCarryforward = 0L;
        List<Map<String, Object>> applicationOrder = new ArrayList<>();
        List<Map<String, Object>> carryforwardItems = new ArrayList<>();

        for (int iteration = 0; iteration < SystemConstants.MAX_COMBO_ITERATIONS; iteration++) {
            long remainingDeductible = maxDeductible;

            totalExemption = 0L;
            totalCredit = 0L;
            totalNongteuk = 0L;
            totalCarryforward = 0L;
            applicationOrder = new ArrayList<>();
            carryforwardItems = new ArrayList<>();

            // (1) 감면 적용: 초과분은 소멸
            for (OutCreditDetail item : exemptions) {
                long grossAmt = item.getGrossAmount() != null ? item.getGrossAmount() : 0L;
                long appliedAmt = Math.min(grossAmt, remainingDeductible);
                long expiredAmt = grossAmt - appliedAmt;
                remainingDeductible -= appliedAmt;

                totalExemption += appliedAmt;

                // 농특세 계산
                long nongteuk = calculateNongteukse(item, appliedAmt);
                totalNongteuk += nongteuk;

                Map<String, Object> orderEntry = new LinkedHashMap<>();
                orderEntry.put("itemId", item.getItemId());
                orderEntry.put("type", "EXEMPTION");
                orderEntry.put("grossAmount", grossAmt);
                orderEntry.put("appliedAmount", appliedAmt);
                orderEntry.put("expiredAmount", expiredAmt);
                orderEntry.put("nongteukAmount", nongteuk);
                applicationOrder.add(orderEntry);
            }

            // (2) 이월불가 공제 적용: 초과분은 소멸
            for (OutCreditDetail item : nonCarryCredits) {
                long grossAmt = item.getGrossAmount() != null ? item.getGrossAmount() : 0L;
                long appliedAmt = Math.min(grossAmt, remainingDeductible);
                long expiredAmt = grossAmt - appliedAmt;
                remainingDeductible -= appliedAmt;

                totalCredit += appliedAmt;

                long nongteuk = calculateNongteukse(item, appliedAmt);
                totalNongteuk += nongteuk;

                Map<String, Object> orderEntry = new LinkedHashMap<>();
                orderEntry.put("itemId", item.getItemId());
                orderEntry.put("type", "CREDIT_NON_CARRY");
                orderEntry.put("grossAmount", grossAmt);
                orderEntry.put("appliedAmount", appliedAmt);
                orderEntry.put("expiredAmount", expiredAmt);
                orderEntry.put("nongteukAmount", nongteuk);
                applicationOrder.add(orderEntry);
            }

            // (3) R&D 최저한세 배제 특례 적용 (추가 공제 여력 확보)
            long rdExtraDeductible = calculateRdMinTaxExemption(
                    comboItems, corpSize, minTaxAmount, currentComputedTax, remainingDeductible);
            remainingDeductible += rdExtraDeductible;

            // (4) 이월가능 공제 적용: 초과분은 이월 (최대 10년)
            for (OutCreditDetail item : carryCredits) {
                long grossAmt = item.getGrossAmount() != null ? item.getGrossAmount() : 0L;
                long appliedAmt = Math.min(grossAmt, remainingDeductible);
                long carryAmt = grossAmt - appliedAmt;
                remainingDeductible -= appliedAmt;

                totalCredit += appliedAmt;

                long nongteuk = calculateNongteukse(item, appliedAmt);
                totalNongteuk += nongteuk;

                Map<String, Object> orderEntry = new LinkedHashMap<>();
                orderEntry.put("itemId", item.getItemId());
                orderEntry.put("type", "CREDIT_CARRY");
                orderEntry.put("grossAmount", grossAmt);
                orderEntry.put("appliedAmount", appliedAmt);
                orderEntry.put("carryforwardAmount", carryAmt);
                orderEntry.put("nongteukAmount", nongteuk);
                applicationOrder.add(orderEntry);

                if (carryAmt > 0) {
                    totalCarryforward += carryAmt;
                    Map<String, Object> cfEntry = new LinkedHashMap<>();
                    cfEntry.put("itemId", item.getItemId());
                    cfEntry.put("provision", item.getProvision());
                    cfEntry.put("carryforwardAmount", carryAmt);
                    cfEntry.put("maxYears", CARRYFORWARD_MAX_YEARS);
                    carryforwardItems.add(cfEntry);
                }
            }

            // 최저한세 조정액 계산
            long totalApplied = totalExemption + totalCredit;
            long grossTotal = 0L;
            for (OutCreditDetail item : comboItems) {
                grossTotal += (item.getGrossAmount() != null ? item.getGrossAmount() : 0L);
            }
            minTaxAdj = grossTotal - totalApplied;

            // 순 환급액 = 총 적용액 - 농특세
            long netRefund = TruncationUtil.truncateAmount(totalApplied - totalNongteuk);

            // 수렴 판정: 이전 반복과의 차이가 1원 이하이면 수렴
            if (Math.abs(netRefund - prevNetRefund) <= SystemConstants.CONVERGENCE_EPSILON) {
                log.debug("[M5-03] 수렴 달성 - 반복 횟수: {}, 순환급액: {}", iteration + 1, netRefund);
                break;
            }
            prevNetRefund = netRefund;
        }

        // 결과 조립
        long netRefund = TruncationUtil.truncateAmount(totalExemption + totalCredit - totalNongteuk);

        CombinationCandidate candidate = new CombinationCandidate();
        candidate.comboSeq = comboSeq;
        candidate.items = comboItems;
        candidate.exemptionTotal = TruncationUtil.truncateAmount(totalExemption);
        candidate.creditTotal = TruncationUtil.truncateAmount(totalCredit);
        candidate.minTaxAdj = TruncationUtil.truncateAmount(minTaxAdj);
        candidate.nongteukTotal = TruncationUtil.truncateAmount(totalNongteuk);
        candidate.netRefund = netRefund;
        candidate.applicationOrder = applicationOrder;
        candidate.carryforwardItems = carryforwardItems;
        candidate.totalCarryforward = totalCarryforward;

        log.debug("[M5-03] 최저한세 적용 완료 - 조합 #{}, 순환급액: {}, 최저한세조정: {}, 농특세: {}",
                comboSeq, netRefund, minTaxAdj, totalNongteuk);

        return candidate;
    }

    // ══════════════════════════════════════════════
    // R&D 최저한세 배제 특례
    // ══════════════════════════════════════════════

    /**
     * R&amp;D 세액공제의 최저한세 초과 적용 특례를 산출한다.
     *
     * <p>
     * 조특법에 따른 R&amp;D 최저한세 배제 3단계를 순차 적용한다:
     * </p>
     * <ol>
     *   <li>국가전략기술 R&amp;D: 100% 배제 (최저한세 적용 제외)</li>
     *   <li>신성장·원천기술 중소기업 R&amp;D: 100% 배제</li>
     *   <li>일반 중소기업 R&amp;D: 50% 배제</li>
     * </ol>
     *
     * @param comboItems       조합 항목 목록
     * @param corpSize         기업 규모
     * @param minTaxAmount     최저한세액
     * @param computedTax      산출세액
     * @param currentRemaining 현재 남은 공제 가능 한도
     * @return R&amp;D 배제 특례로 인한 추가 공제 가능 금액
     */
    private long calculateRdMinTaxExemption(
            List<OutCreditDetail> comboItems, String corpSize,
            long minTaxAmount, long computedTax, long currentRemaining) {

        // R&D 항목 필터링 (rdType이 존재하는 항목)
        List<OutCreditDetail> rdItems = comboItems.stream()
                .filter(item -> item.getRdType() != null && !item.getRdType().isEmpty())
                .collect(Collectors.toList());

        if (rdItems.isEmpty()) {
            return 0L;
        }

        // R&D 유형별 면제율 조회
        List<RefRdMinTaxExempt> exemptRules = refRdMinTaxExemptRepository.findByCorpSize(corpSize);
        Map<String, BigDecimal> exemptRateMap = new HashMap<>();
        for (RefRdMinTaxExempt rule : exemptRules) {
            exemptRateMap.put(rule.getRdType(), rule.getExemptRate());
        }

        long totalRdExemption = 0L;

        // 3단계 순차 적용: 국가전략 → 신성장중소 → 일반중소
        String[] rdPriorityOrder = {"NATIONAL_STRATEGIC", "NEW_GROWTH_SME", "GENERAL_SME"};

        for (String rdType : rdPriorityOrder) {
            BigDecimal exemptRate = exemptRateMap.get(rdType);
            if (exemptRate == null) {
                continue;
            }

            for (OutCreditDetail rdItem : rdItems) {
                if (rdType.equals(rdItem.getRdType())) {
                    long grossAmt = rdItem.getGrossAmount() != null ? rdItem.getGrossAmount() : 0L;
                    // 면제율 적용: TRUNCATE(공제액 × 면제율 / 100, 0)
                    long exemptAmt = TruncationUtil.truncateBigDecimal(
                            new BigDecimal(grossAmt)
                                    .multiply(exemptRate)
                                    .divide(new BigDecimal("100"), 0, RoundingMode.DOWN),
                            0).longValue();
                    totalRdExemption += exemptAmt;
                }
            }
        }

        return TruncationUtil.truncateAmount(totalRdExemption);
    }

    // ══════════════════════════════════════════════
    // 농어촌특별세 계산
    // ══════════════════════════════════════════════

    /**
     * 개별 항목의 농어촌특별세를 계산한다.
     *
     * <p>
     * REF_NONGTEUKSE 테이블을 참조하여 조항별 면제 여부 및 세율을 적용한다.
     * 면제 대상이면 0원, 과세 대상이면 TRUNCATE(적용금액 × 세율, 0)을 반환한다.
     * </p>
     *
     * @param item       공제·감면 항목
     * @param appliedAmt 실제 적용 금액
     * @return 농어촌특별세 금액 (원)
     */
    private long calculateNongteukse(OutCreditDetail item, long appliedAmt) {
        if (appliedAmt <= 0) {
            return 0L;
        }

        // 항목의 농특세 면제 여부 확인
        if (Boolean.TRUE.equals(item.getNongteukExempt())) {
            return 0L;
        }

        // 조항별 농특세율 조회
        String provision = item.getProvision();
        Optional<RefNongteukse> nongteukseOpt = refNongteukseRepository.findById(provision);

        if (nongteukseOpt.isPresent()) {
            RefNongteukse nongteukse = nongteukseOpt.get();
            if (Boolean.TRUE.equals(nongteukse.getIsExempt())) {
                return 0L;
            }
            BigDecimal taxRate = nongteukse.getTaxRate();
            if (taxRate != null) {
                // TRUNCATE(적용금액 × 세율 / 100, 0) → 10원 미만 절사
                return TruncationUtil.truncateAmount(
                        new BigDecimal(appliedAmt)
                                .multiply(taxRate)
                                .divide(new BigDecimal("100"), 0, RoundingMode.DOWN)
                                .longValue());
            }
        }

        // 기본 농특세율 20% 적용
        return TruncationUtil.truncateAmount(
                new BigDecimal(appliedAmt)
                        .multiply(SystemConstants.NONGTEUKSE_RATE)
                        .setScale(0, RoundingMode.DOWN)
                        .longValue());
    }

    // ══════════════════════════════════════════════
    // 최저한세율 조회
    // ══════════════════════════════════════════════

    /**
     * 기업 규모 및 과세표준(또는 산출세액)에 해당하는 최저한세율을 조회한다.
     *
     * <p>
     * REF_MIN_TAX_RATE 테이블에서 기업 규모 및 금액 구간에 맞는 세율을 조회한다.
     * 조회 결과가 없으면 기본값(중소: 7%, 기타: 10%)을 적용한다.
     * </p>
     *
     * @param corpSize   기업 규모 (중소/중견/대)
     * @param baseAmount 기준 금액 (법인=과세표준, 개인=산출세액)
     * @return 최저한세율 (BigDecimal, 예: 0.07 = 7%)
     */
    private BigDecimal resolveMinTaxRate(String corpSize, long baseAmount) {
        List<RefMinTaxRate> rates = refMinTaxRateRepository
                .findByCorpSizeAndTaxableIncome(corpSize, baseAmount);

        if (rates != null && !rates.isEmpty()) {
            BigDecimal minRate = rates.get(0).getMinRate();
            // DB에 % 단위로 저장된 경우 소수로 변환
            if (minRate.compareTo(BigDecimal.ONE) > 0) {
                return TruncationUtil.truncateRate(
                        minRate.divide(new BigDecimal("100"), 4, RoundingMode.DOWN), 4);
            }
            return TruncationUtil.truncateRate(minRate, 4);
        }

        // 기본값: 중소기업 7%, 기타 10%
        if (CORP_SIZE_SME.equals(corpSize)) {
            return new BigDecimal("0.07");
        }
        return new BigDecimal("0.10");
    }

    // ══════════════════════════════════════════════
    // 결과 저장
    // ══════════════════════════════════════════════

    /**
     * 조합 탐색 결과를 OUT_COMBINATION 테이블에 저장한다.
     *
     * @param reqId     요청 ID
     * @param candidate 조합 후보 결과
     * @param rank      조합 순위 (1 = 최적)
     */
    private void saveOutCombination(String reqId, CombinationCandidate candidate, int rank) {
        String comboId = "COMBO-" + String.format("%03d", rank);

        // 포함 항목 ID 목록을 JSON으로 직렬화
        List<String> itemIds = candidate.items.stream()
                .map(OutCreditDetail::getItemId)
                .collect(Collectors.toList());
        String itemsJson = JsonUtil.toJson(itemIds);

        // 적용 순서 JSON 직렬화
        String applicationOrderJson = JsonUtil.toJson(candidate.applicationOrder);

        // 이월공제 항목 JSON 직렬화
        String carryforwardItemsJson = candidate.carryforwardItems.isEmpty()
                ? null : JsonUtil.toJson(candidate.carryforwardItems);

        // 조합명 생성
        String comboName = "조합-" + rank + " (항목 " + candidate.items.size() + "건)";

        OutCombination combination = OutCombination.builder()
                .reqId(reqId)
                .comboId(comboId)
                .comboRank(rank)
                .groupType(rank == 1 ? "OPTIMAL" : "ALTERNATIVE")
                .comboName(comboName)
                .itemsJson(itemsJson)
                .exemptionTotal(candidate.exemptionTotal)
                .creditTotal(candidate.creditTotal)
                .minTaxAdj(candidate.minTaxAdj)
                .nongteukTotal(candidate.nongteukTotal)
                .netRefund(candidate.netRefund)
                .isValid(true)
                .applicationOrder(applicationOrderJson)
                .carryforwardItems(carryforwardItemsJson)
                .build();

        outCombinationRepository.save(combination);
    }

    /**
     * 적용 가능 항목이 없을 때 빈 조합 결과를 저장한다.
     *
     * @param reqId 요청 ID
     */
    private void saveEmptyCombination(String reqId) {
        OutCombination emptyCombination = OutCombination.builder()
                .reqId(reqId)
                .comboId("COMBO-001")
                .comboRank(1)
                .groupType("EMPTY")
                .comboName("적용 가능 항목 없음")
                .itemsJson("[]")
                .exemptionTotal(0L)
                .creditTotal(0L)
                .minTaxAdj(0L)
                .nongteukTotal(0L)
                .netRefund(0L)
                .isValid(true)
                .applicationOrder("[]")
                .carryforwardItems(null)
                .build();
        outCombinationRepository.save(emptyCombination);
    }

    /**
     * 상호배제 검증 결과를 OUT_EXCLUSION_VERIFY 테이블에 저장한다.
     *
     * @param reqId           요청 ID
     * @param exclusionResult 상호배제 분리 결과
     */
    private void saveExclusionVerifyResults(String reqId, MutualExclusionResult exclusionResult) {
        int verifySeq = 1;
        for (ExclusionPair pair : exclusionResult.exclusionPairs) {
            String verifyId = "EXCL-" + String.format("%03d", verifySeq++);

            OutExclusionVerify verify = OutExclusionVerify.builder()
                    .reqId(reqId)
                    .verifyId(verifyId)
                    .comboId("COMBO-001")
                    .provisionA(pair.provisionA)
                    .provisionB(pair.provisionB)
                    .overlapAllowed("N")
                    .conditionNote(pair.conditionNote)
                    .violationDetected(false)
                    .legalBasis(pair.legalBasis)
                    .build();

            outExclusionVerifyRepository.save(verify);
        }
    }

    // ══════════════════════════════════════════════
    // 유틸리티 메서드
    // ══════════════════════════════════════════════

    /**
     * REF_SYSTEM_PARAM에서 정수형 시스템 파라미터를 조회한다.
     *
     * @param paramKey     파라미터 키
     * @param defaultValue 기본값 (조회 실패 시)
     * @return 파라미터 값 (정수)
     */
    private int getIntSystemParam(String paramKey, int defaultValue) {
        try {
            Optional<RefSystemParam> paramOpt = refSystemParamRepository.findById(paramKey);
            if (paramOpt.isPresent() && paramOpt.get().getParamValue() != null) {
                return Integer.parseInt(paramOpt.get().getParamValue().trim());
            }
        } catch (NumberFormatException e) {
            log.warn("[M5] 시스템 파라미터 '{}' 정수 변환 실패, 기본값 {} 사용", paramKey, defaultValue);
        }
        return defaultValue;
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
                .legalBasis("조특법, 법인세법 §59, 농어촌특별세법")
                .executedAt(LocalDateTime.now())
                .logLevel("INFO")
                .executedBy("CombinationSearchService")
                .durationMs(durationMs)
                .build();
        logCalculationRepository.save(logEntry);
    }

    // ══════════════════════════════════════════════
    // 내부 데이터 클래스
    // ══════════════════════════════════════════════

    /**
     * 상호배제 분리 결과를 담는 내부 클래스.
     */
    private static class MutualExclusionResult {
        /** Group A: 상호배제에 관여하지 않는 독립 항목 */
        List<OutCreditDetail> independentItems = new ArrayList<>();
        /** Group B: 상호배제에 관여하는 항목 */
        List<OutCreditDetail> exclusionInvolvedItems = new ArrayList<>();
        /** 상호배제 쌍 목록 */
        List<ExclusionPair> exclusionPairs = new ArrayList<>();
    }

    /**
     * 상호배제 쌍 정보를 담는 내부 클래스.
     */
    private static class ExclusionPair {
        final String provisionA;
        final String provisionB;
        final String conditionNote;
        final String legalBasis;

        ExclusionPair(String provisionA, String provisionB, String conditionNote, String legalBasis) {
            this.provisionA = provisionA;
            this.provisionB = provisionB;
            this.conditionNote = conditionNote;
            this.legalBasis = legalBasis;
        }
    }

    /**
     * 조합 후보 결과를 담는 내부 클래스.
     */
    private static class CombinationCandidate {
        int comboSeq;
        List<OutCreditDetail> items;
        long exemptionTotal;
        long creditTotal;
        long minTaxAdj;
        long nongteukTotal;
        long netRefund;
        long totalCarryforward;
        List<Map<String, Object>> applicationOrder;
        List<Map<String, Object>> carryforwardItems;

        long getNetRefund() {
            return netRefund;
        }
    }
}
