package com.entec.tax.domain.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 페이징 응답 DTO.
 * <p>
 * 목록 조회 API에서 페이징 처리된 결과를 반환할 때 사용하는 공통 DTO이다.
 * 콘텐츠 목록과 페이징 메타 정보를 포함한다.
 * </p>
 *
 * @param <T> 콘텐츠 항목 타입
 */
@Getter
@Setter
public class PageResponse<T> {

    /** 현재 페이지 콘텐츠 목록 */
    private List<T> content;

    /** 현재 페이지 번호 (0-based) */
    private int page;

    /** 페이지 크기 */
    private int pageSize;

    /** 전체 항목 수 */
    private long totalElements;

    /** 전체 페이지 수 */
    private int totalPages;
}
