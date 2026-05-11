package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.Post;
import com.min.chalkakserver.entity.PostLike;
import com.min.chalkakserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostAndUser(Post post, User user);

    boolean existsByPostAndUser(Post post, User user);

    void deleteByPostAndUser(Post post, User user);
}
