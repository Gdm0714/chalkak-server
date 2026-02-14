package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.Favorite;
import com.min.chalkakserver.entity.PhotoBooth;
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
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserAndPhotoBooth(User user, PhotoBooth photoBooth);

    boolean existsByUserAndPhotoBooth(User user, PhotoBooth photoBooth);

    @Query("SELECT f FROM Favorite f JOIN FETCH f.photoBooth WHERE f.user = :user ORDER BY f.createdAt DESC")
    List<Favorite> findByUserWithPhotoBooth(@Param("user") User user);

    @Query("SELECT f FROM Favorite f JOIN FETCH f.photoBooth WHERE f.user = :user ORDER BY f.createdAt DESC")
    Page<Favorite> findByUserWithPhotoBoothPaged(@Param("user") User user, Pageable pageable);

    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.photoBooth = :photoBooth")
    long countByPhotoBooth(@Param("photoBooth") PhotoBooth photoBooth);

    void deleteByUserAndPhotoBooth(User user, PhotoBooth photoBooth);

    void deleteAllByUser(User user);

    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.user = :user")
    long countByUser(@Param("user") User user);
}
