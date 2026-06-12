package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.photobook.PhotobookCollectionResponseDto;
import com.min.chalkakserver.dto.photobook.PhotobookEntryRequestDto;
import com.min.chalkakserver.dto.photobook.PhotobookEntryResponseDto;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.PhotobookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Photobook", description = "디지털 포토북/도감 API")
@RestController
@RequestMapping("/api/photobook")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Validated
public class PhotobookController {

    private final PhotobookService photobookService;

    @Operation(summary = "포토북 사진 등록", description = "업로드한 이미지 URL로 포토북에 사진을 등록합니다. 콜라보/매장 태깅 가능.")
    @PostMapping
    public ResponseEntity<PhotobookEntryResponseDto> createEntry(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PhotobookEntryRequestDto request
    ) {
        return ResponseEntity.ok(photobookService.createEntry(userDetails.getId(), request));
    }

    @Operation(summary = "내 포토북 조회", description = "내가 등록한 사진을 최신순으로 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<PagedResponseDto<PhotobookEntryResponseDto>> getMyEntries(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(photobookService.getMyEntries(userDetails.getId(), page, size));
    }

    @Operation(summary = "콜라보별 내 포토북 조회", description = "특정 콜라보로 태깅된 내 사진을 조회합니다.")
    @GetMapping("/my/collab/{frameCollabId}")
    public ResponseEntity<PagedResponseDto<PhotobookEntryResponseDto>> getMyEntriesByCollab(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long frameCollabId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(photobookService.getMyEntriesByCollab(
                userDetails.getId(), frameCollabId, page, size));
    }

    @Operation(summary = "내 도감 조회", description = "콜라보별 수집 현황(사진 수, 수집 여부)을 반환합니다.")
    @GetMapping("/my/collection")
    public ResponseEntity<PhotobookCollectionResponseDto> getMyCollection(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(photobookService.getMyCollection(userDetails.getId()));
    }

    @Operation(summary = "포토북 사진 삭제", description = "내가 등록한 사진을 삭제합니다.")
    @DeleteMapping("/{entryId}")
    public ResponseEntity<Map<String, String>> deleteEntry(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long entryId
    ) {
        photobookService.deleteEntry(userDetails.getId(), entryId);
        return ResponseEntity.ok(Map.of("message", "포토북 사진이 삭제되었습니다."));
    }
}
