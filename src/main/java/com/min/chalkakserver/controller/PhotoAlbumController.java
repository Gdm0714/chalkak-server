package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.album.PhotoBulkDeleteRequestDto;
import com.min.chalkakserver.dto.album.PhotoResponseDto;
import com.min.chalkakserver.dto.album.PhotoUpdateRequestDto;
import com.min.chalkakserver.security.CustomUserDetails;
import com.min.chalkakserver.service.PhotoAlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Tag(name = "Photo Album", description = "네컷 사진첩 API")
@RestController
@RequestMapping("/api/album/photos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class PhotoAlbumController {

    private final PhotoAlbumService photoAlbumService;

    @Operation(summary = "사진 업로드", description = "네컷 이미지 파일 업로드 (로그인 필요)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoResponseDto> uploadPhoto(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "memo", required = false) String memo,
            @RequestParam(value = "photoDate", required = false) String photoDate) throws IOException {
        LocalDateTime parsedPhotoDate = null;
        if (photoDate != null && !photoDate.isBlank()) {
            try {
                parsedPhotoDate = LocalDateTime.parse(photoDate);
            } catch (DateTimeParseException e) {
                log.warn("Invalid photoDate ignored: value={}, reason={}", photoDate, e.getMessage());
            }
        }
        PhotoResponseDto response = photoAlbumService.uploadPhoto(userDetails.getId(), file, memo, parsedPhotoDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "내 사진 목록", description = "내 사진첩 목록 조회 (createdAt DESC), favorite=true 시 찜만 (로그인 필요)")
    @GetMapping
    public ResponseEntity<List<PhotoResponseDto>> getMyPhotos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "favorite", defaultValue = "false") boolean favorite) {
        List<PhotoResponseDto> response = photoAlbumService.getMyPhotos(userDetails.getId(), favorite);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 사진 목록 (페이징)", description = "내 사진첩 목록 페이징 조회 (로그인 필요)")
    @GetMapping("/paged")
    public ResponseEntity<PagedResponseDto<PhotoResponseDto>> getMyPhotosPaged(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "favorite", defaultValue = "false") boolean favorite) {
        PagedResponseDto<PhotoResponseDto> response =
            photoAlbumService.getMyPhotosPaged(userDetails.getId(), favorite, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사진 수정", description = "메모/찜 부분 수정 (로그인 필요)")
    @PatchMapping("/{id}")
    public ResponseEntity<PhotoResponseDto> updatePhoto(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody PhotoUpdateRequestDto request) {
        PhotoResponseDto response = photoAlbumService.updatePhoto(userDetails.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사진 삭제", description = "내 사진 단건 삭제 (S3 객체 포함, 로그인 필요)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePhoto(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        photoAlbumService.deletePhoto(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "사진 다중 삭제", description = "선택한 사진 벌크 삭제 (본인 소유만, 로그인 필요)")
    @DeleteMapping
    public ResponseEntity<Void> deletePhotos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PhotoBulkDeleteRequestDto request) {
        photoAlbumService.deletePhotos(userDetails.getId(), request.getIds());
        return ResponseEntity.noContent().build();
    }
}
