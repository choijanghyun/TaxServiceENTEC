package com.entec.tax.engine.credit.service;

/**
 * M4 개별 공제·감면 산출 서비스 인터페이스.
 * <p>
 * STEP 1-2 단계에서 각 항목별 공제/감면액을 산출한다.
 * 고용증대 세액공제, 연구개발비 세액공제, 투자 세액공제 등
 * 개별 공제·감면 항목을 산출한다.
 * </p>
 */
public interface CreditCalculationService {

    /**
     * 개별 공제·감면액을 산출한다.
     *
     * @param reqId 요청 ID
     */
    void calculateCredits(String reqId);
}
