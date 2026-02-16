package com.entec.tax.domain.request.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * API-03 상태 조회 응답 DTO.
 * <p>
 * 요청 건의 현재 처리 상태와 진행 단계를 조회할 때 반환되는 응답이다.
 * </p>
 */
@Getter
@Setter
public class RequestStatusDto {

    /** 요청 ID */
    private String reqId;

    /** 요청 처리 상태 */
    private String status;

    /** 현재 진행 단계 설명 */
    private String progress;
}
