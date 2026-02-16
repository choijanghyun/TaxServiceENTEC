package com.entec.tax.common.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 결산조정 차단(Hard-Fail) 발생 시 예외
 * <p>
 * 차단된 항목 목록을 {@code blockedItems}로 포함하며,
 * 해당 항목이 해소되지 않으면 계산을 진행할 수 없음을 나타낸다.
 */
public class HardFailException extends TaxServiceException {

    private static final long serialVersionUID = 1L;

    private final List<String> blockedItems;

    /**
     * @param errorCode    에러 코드
     * @param message      상세 메시지
     * @param reqId        요청 추적 ID
     * @param blockedItems 결산조정 차단 항목 목록
     */
    public HardFailException(ErrorCode errorCode, String message, String reqId,
                             List<String> blockedItems) {
        super(errorCode, message, reqId);
        this.blockedItems = blockedItems != null
                ? Collections.unmodifiableList(new ArrayList<String>(blockedItems))
                : Collections.<String>emptyList();
    }

    /**
     * @param message      상세 메시지
     * @param reqId        요청 추적 ID
     * @param blockedItems 결산조정 차단 항목 목록
     */
    public HardFailException(String message, String reqId, List<String> blockedItems) {
        this(ErrorCode.HARD_FAIL, message, reqId, blockedItems);
    }

    public List<String> getBlockedItems() {
        return blockedItems;
    }
}
