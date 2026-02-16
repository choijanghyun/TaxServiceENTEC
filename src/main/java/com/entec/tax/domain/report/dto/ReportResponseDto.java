package com.entec.tax.domain.report.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * API-04 보고서 응답 DTO.
 * <p>
 * 세액공제 환급 분석 보고서의 전체 JSON 내용을 반환하는 DTO이다.
 * report 필드에 보고서 전체 구조가 Map 형태로 담긴다.
 * </p>
 */
@Getter
@Setter
public class ReportResponseDto {

    /** 보고서 전체 JSON (키-값 구조) */
    private Map<String, Object> report;
}
