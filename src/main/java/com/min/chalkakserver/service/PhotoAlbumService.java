package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.album.PhotoResponseDto;
import com.min.chalkakserver.dto.album.PhotoUpdateRequestDto;
import com.min.chalkakserver.entity.Photo;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.repository.PhotoRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoAlbumService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    /**
     * 사진 업로드 (파일 → S3 → 사진첩 저장)
     */
    @Transactional
    public PhotoResponseDto uploadPhoto(Long userId, MultipartFile file, String memo, LocalDateTime photoDate) throws IOException {
        validateImageFile(file);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("사용자를 찾을 수 없습니다.", "NOT_FOUND"));

        String dir = "albums/" + userId;
        String imageUrl = fileUploadService.uploadFile(file, dir);

        String thumbnailUrl = fileUploadService.uploadThumbnail(file, dir);
        if (thumbnailUrl == null) {
            thumbnailUrl = imageUrl;
        }

        Photo photo = Photo.builder()
            .user(user)
            .imageUrl(imageUrl)
            .thumbnailUrl(thumbnailUrl)
            .memo(memo)
            .favorite(false)
            .source(Photo.PhotoSource.FILE_UPLOAD)
            .photoDate(photoDate)
            .build();

        Photo savedPhoto = photoRepository.save(photo);
        log.info("Album photo uploaded: userId={}, photoId={}", userId, savedPhoto.getId());

        return PhotoResponseDto.from(savedPhoto);
    }

    /**
     * 내 사진 목록 조회 (createdAt DESC)
     */
    @Transactional(readOnly = true)
    public List<PhotoResponseDto> getMyPhotos(Long userId, boolean favoriteOnly) {
        List<Photo> photos = favoriteOnly
            ? photoRepository.findByUserIdAndFavoriteTrueOrderByCreatedAtDesc(userId)
            : photoRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return photos.stream()
            .map(PhotoResponseDto::from)
            .collect(Collectors.toList());
    }

    /**
     * 내 사진 목록 조회 (페이징, createdAt DESC)
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<PhotoResponseDto> getMyPhotosPaged(
            Long userId, boolean favoriteOnly, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Photo> photoPage = favoriteOnly
            ? photoRepository.findByUserIdAndFavoriteTrue(userId, pageable)
            : photoRepository.findByUserId(userId, pageable);

        return PagedResponseDto.from(photoPage.map(PhotoResponseDto::from));
    }

    /**
     * 사진 부분 수정 (memo / favorite, null이 아닌 필드만 반영)
     */
    @Transactional
    public PhotoResponseDto updatePhoto(Long userId, Long photoId, PhotoUpdateRequestDto request) {
        Photo photo = photoRepository.findByIdAndUserId(photoId, userId)
            .orElseThrow(() -> new AuthException("해당 사진을 찾을 수 없습니다.", "NOT_FOUND"));

        if (request.getMemo() != null) {
            photo.updateMemo(request.getMemo());
        }
        if (request.getFavorite() != null) {
            photo.updateFavorite(request.getFavorite());
        }

        log.info("Album photo updated: userId={}, photoId={}", userId, photoId);
        return PhotoResponseDto.from(photo);
    }

    /**
     * 사진 단건 삭제 (소유자만, S3 객체도 정리)
     */
    @Transactional
    public void deletePhoto(Long userId, Long photoId) {
        Photo photo = photoRepository.findByIdAndUserId(photoId, userId)
            .orElseThrow(() -> new AuthException("해당 사진을 찾을 수 없습니다.", "NOT_FOUND"));

        deletePhotoObjects(photo);
        photoRepository.delete(photo);
        log.info("Album photo deleted: userId={}, photoId={}", userId, photoId);
    }

    /**
     * 사진 다중 삭제 (본인 소유만 삭제, 타인/없는 id는 조용히 스킵)
     */
    @Transactional
    public void deletePhotos(Long userId, List<Long> ids) {
        List<Photo> photos = photoRepository.findByIdInAndUserId(ids, userId);

        for (Photo photo : photos) {
            deletePhotoObjects(photo);
        }

        photoRepository.deleteAll(photos);
        log.info("Album photos bulk deleted: userId={}, requested={}, deleted={}",
            userId, ids.size(), photos.size());
    }

    /**
     * 사진에 연결된 S3 객체(원본 + 썸네일)를 정리한다.
     * 썸네일이 원본 URL로 폴백된 경우(동일 URL)는 중복 삭제하지 않는다.
     */
    private void deletePhotoObjects(Photo photo) {
        String imageUrl = photo.getImageUrl();
        String thumbnailUrl = photo.getThumbnailUrl();

        fileUploadService.deleteFile(imageUrl);
        if (thumbnailUrl != null && !thumbnailUrl.equals(imageUrl)) {
            fileUploadService.deleteFile(thumbnailUrl);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }
    }
}
