package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.Post;
import com.min.chalkakserver.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByPhotoBoothIdOrderByCreatedAtDesc(Long photoBoothId, Pageable pageable);

    Page<Post> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 인기 게시물 — 지정 시점 이후의 글 중 좋아요 수 내림차순, 최신순.
     */
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since "
        + "ORDER BY p.likesCount DESC, p.createdAt DESC")
    Page<Post> findPopularSince(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * 주변 게시물 — 포토부스가 연결된 글 중 Haversine 거리 :radius km 이내.
     * 거리순 정렬.
     */
    @Query(value = "SELECT p.* FROM posts p "
        + "JOIN photo_booths pb ON p.photo_booth_id = pb.id "
        + "WHERE p.photo_booth_id IS NOT NULL "
        + "  AND pb.latitude BETWEEN :minLat AND :maxLat "
        + "  AND pb.longitude BETWEEN :minLon AND :maxLon "
        + "  AND (6371 * acos(cos(radians(:latitude)) * cos(radians(pb.latitude)) "
        + "       * cos(radians(pb.longitude) - radians(:longitude)) "
        + "       + sin(radians(:latitude)) * sin(radians(pb.latitude)))) <= :radius "
        + "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(pb.latitude)) "
        + "       * cos(radians(pb.longitude) - radians(:longitude)) "
        + "       + sin(radians(:latitude)) * sin(radians(pb.latitude)))) ASC, "
        + "p.created_at DESC",
        countQuery = "SELECT count(*) FROM posts p "
        + "JOIN photo_booths pb ON p.photo_booth_id = pb.id "
        + "WHERE p.photo_booth_id IS NOT NULL "
        + "  AND pb.latitude BETWEEN :minLat AND :maxLat "
        + "  AND pb.longitude BETWEEN :minLon AND :maxLon "
        + "  AND (6371 * acos(cos(radians(:latitude)) * cos(radians(pb.latitude)) "
        + "       * cos(radians(pb.longitude) - radians(:longitude)) "
        + "       + sin(radians(:latitude)) * sin(radians(pb.latitude)))) <= :radius",
        nativeQuery = true)
    Page<Post> findNearbyPosts(
        @Param("latitude") double latitude,
        @Param("longitude") double longitude,
        @Param("minLat") double minLat,
        @Param("maxLat") double maxLat,
        @Param("minLon") double minLon,
        @Param("maxLon") double maxLon,
        @Param("radius") double radius,
        Pageable pageable);
}
