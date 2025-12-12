package com.min.chalkakserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이지네이션 응답 DTO
 * @param <T> 데이터 타입
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponseDto<T> {
    
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;

    /**
     * Spring Data Page 객체로부터 PagedResponseDto 생성
     */
    public static <T> PagedResponseDto<T> from(Page<T> page) {
        return PagedResponseDto.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * 단순 리스트를 페이지네이션 응답으로 변환 (전체 데이터)
     */
    public static <T> PagedResponseDto<T> of(List<T> content) {
        return PagedResponseDto.<T>builder()
                .content(content)
                .page(0)
                .size(content.size())
                .totalElements(content.size())
                .totalPages(1)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}
