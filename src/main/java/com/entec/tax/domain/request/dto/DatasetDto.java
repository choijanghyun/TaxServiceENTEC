package com.entec.tax.domain.request.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * 데이터셋 단위 DTO.
 * <p>
 * 요청 접수 시 카테고리별 데이터를 전달하기 위한 DTO이다.
 * data 필드는 단건(Map) 또는 다건(List&lt;Map&gt;) JSON 데이터를 수용한다.
 * </p>
 */
@Getter
@Setter
public class DatasetDto {

    /** 데이터 카테고리 (BASIC, EMPLOYEE, DEDUCTION 등) */
    @NotBlank(message = "카테고리는 필수입니다")
    private String category;

    /** 데이터 서브 카테고리 (nullable) */
    private String subCategory;

    /** JSON 데이터 (단건: Map, 다건: List&lt;Map&gt;) */
    private Object data;
}
