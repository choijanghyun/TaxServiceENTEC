package com.entec.tax.domain.request.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * API-01 요청 접수 응답 DTO.
 * <p>
 * 요청 접수가 완료된 후 클라이언트에게 반환되는 응답이다.
 * 요청 ID, 처리 상태, 수신된 데이터셋 수, 생성 일시를 포함한다.
 * </p>
 */
@Getter
@Setter
public class RequestResponseDto {

    /** 요청 ID */
    private String reqId;

    /** 요청 처리 상태 */
    private String status;

    /** 수신된 데이터셋 수 */
    private int datasetsReceived;

    /** 요청 생성 일시 */
    private String createdAt;
}
