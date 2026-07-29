package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Photo> findByUserIdAndFavoriteTrueOrderByCreatedAtDesc(Long userId);

    Page<Photo> findByUserId(Long userId, Pageable pageable);

    Page<Photo> findByUserIdAndFavoriteTrue(Long userId, Pageable pageable);

    Optional<Photo> findByIdAndUserId(Long id, Long userId);

    List<Photo> findByIdInAndUserId(List<Long> ids, Long userId);
}
