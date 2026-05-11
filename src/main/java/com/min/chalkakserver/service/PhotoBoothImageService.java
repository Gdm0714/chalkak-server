package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PhotoBoothImageDto;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.PhotoBoothImage;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.repository.PhotoBoothImageRepository;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoBoothImageService {

    private final PhotoBoothImageRepository photoBoothImageRepository;
    private final PhotoBoothRepository photoBoothRepository;
    private final FileUploadService fileUploadService;

    @Transactional(readOnly = true)
    public List<PhotoBoothImageDto> getImages(Long photoBoothId) {
        if (!photoBoothRepository.existsById(photoBoothId)) {
            throw new PhotoBoothNotFoundException(photoBoothId);
        }
        return photoBoothImageRepository.findByPhotoBoothIdOrderByDisplayOrder(photoBoothId)
                .stream()
                .map(PhotoBoothImageDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PhotoBoothImageDto uploadImage(Long photoBoothId, MultipartFile file,
                                          PhotoBoothImage.ImageType imageType, Integer displayOrder) throws IOException {
        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
                .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        String imageUrl = fileUploadService.uploadFile(file, "photo-booths/" + photoBoothId);

        PhotoBoothImage image = PhotoBoothImage.builder()
                .photoBooth(photoBooth)
                .imageUrl(imageUrl)
                .imageType(imageType)
                .displayOrder(displayOrder)
                .build();

        PhotoBoothImage saved = photoBoothImageRepository.save(image);
        log.info("Photo booth image uploaded: photoBoothId={}, imageId={}", photoBoothId, saved.getId());
        return PhotoBoothImageDto.from(saved);
    }

    @Transactional
    public void deleteImage(Long photoBoothId, Long imageId) {
        if (!photoBoothRepository.existsById(photoBoothId)) {
            throw new PhotoBoothNotFoundException(photoBoothId);
        }
        photoBoothImageRepository.deleteByPhotoBoothIdAndId(photoBoothId, imageId);
        log.info("Photo booth image deleted: photoBoothId={}, imageId={}", photoBoothId, imageId);
    }
}
