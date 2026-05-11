package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.PhotoBoothImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoBoothImageRepository extends JpaRepository<PhotoBoothImage, Long> {

    List<PhotoBoothImage> findByPhotoBoothIdOrderByDisplayOrder(Long photoBoothId);

    void deleteByPhotoBoothIdAndId(Long photoBoothId, Long imageId);
}
