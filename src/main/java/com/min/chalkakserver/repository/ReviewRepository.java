package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.Review;
import com.min.chalkakserver.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserAndPhotoBooth(User user, PhotoBooth photoBooth);

    boolean existsByUserAndPhotoBooth(User user, PhotoBooth photoBooth);

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.photoBooth = :photoBooth ORDER BY r.createdAt DESC")
    List<Review> findByPhotoBoothWithUser(@Param("photoBooth") PhotoBooth photoBooth);

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.photoBooth = :photoBooth ORDER BY r.createdAt DESC")
    Page<Review> findByPhotoBoothWithUserPaged(@Param("photoBooth") PhotoBooth photoBooth, Pageable pageable);

    @Query("SELECT r FROM Review r JOIN FETCH r.photoBooth WHERE r.user = :user ORDER BY r.createdAt DESC")
    List<Review> findByUserWithPhotoBooth(@Param("user") User user);

    @Query("SELECT r FROM Review r JOIN FETCH r.photoBooth WHERE r.user = :user ORDER BY r.createdAt DESC")
    Page<Review> findByUserWithPhotoBoothPaged(@Param("user") User user, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.photoBooth = :photoBooth")
    long countByPhotoBooth(@Param("photoBooth") PhotoBooth photoBooth);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.photoBooth = :photoBooth")
    Double getAverageRatingByPhotoBooth(@Param("photoBooth") PhotoBooth photoBooth);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.photoBooth = :photoBooth GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistribution(@Param("photoBooth") PhotoBooth photoBooth);

    void deleteAllByUser(User user);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.user = :user")
    long countByUser(@Param("user") User user);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.createdAt >= :dateTime")
    long countByCreatedAtAfter(@Param("dateTime") java.time.LocalDateTime dateTime);
}
