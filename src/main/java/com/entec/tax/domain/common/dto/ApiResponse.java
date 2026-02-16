package com.entec.tax.domain.common.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 표준 API 응답 래퍼.
 * <p>
 * 모든 API 응답을 일관된 구조로 감싸기 위한 공통 DTO이다.
 * 성공 여부, 데이터, 메시지를 포함한다.
 * </p>
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
@Setter
public class ApiResponse<T> {

    /** 요청 성공 여부 */
    private boolean success;

    /** 응답 데이터 */
    private T data;

    /** 응답 메시지 */
    private String message;

    /**
     * 성공 응답을 생성한다 (메시지 없음).
     *
     * @param data 응답 데이터
     * @param <T>  응답 데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    /**
     * 성공 응답을 생성한다 (메시지 포함).
     *
     * @param data    응답 데이터
     * @param message 응답 메시지
     * @param <T>     응답 데이터 타입
     * @return 성공 ApiResponse
     */
    public static <T> ApiResponse<T> ok(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        return response;
    }

    /**
     * 실패 응답을 생성한다.
     *
     * @param message 오류 메시지
     * @param <T>     응답 데이터 타입
     * @return 실패 ApiResponse
     */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
