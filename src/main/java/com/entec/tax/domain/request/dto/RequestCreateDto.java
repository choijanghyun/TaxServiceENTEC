package com.entec.tax.domain.request.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * API-01 요청 접수 DTO.
 * <p>
 * 세액공제 환급 분석 요청을 접수할 때 클라이언트가 전달하는 요청 본문이다.
 * 신청자 정보와 하나 이상의 데이터셋을 포함한다.
 * </p>
 */
@Getter
@Setter
public class RequestCreateDto {

    /** 신청자 유형 (C: 법인, P: 개인) */
    @NotBlank(message = "신청자 유형은 필수입니다")
    private String applicantType;

    /** 신청자 식별번호 (사업자등록번호/주민등록번호) */
    @NotBlank(message = "신청자 식별번호는 필수입니다")
    private String applicantId;

    /** 세금 유형 코드 (CORP: 법인세, INC: 종합소득세) */
    @NotBlank(message = "세금 유형은 필수입니다")
    private String taxType;

    /** 귀속 연도 */
    @NotBlank(message = "귀속 연도는 필수입니다")
    private String taxYear;

    /** 데이터셋 목록 (1건 이상 필수) */
    @NotEmpty(message = "데이터셋은 1건 이상 필수입니다")
    @Valid
    private List<DatasetDto> datasets;
}
