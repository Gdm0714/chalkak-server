package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.post.PostRequestDto;
import com.min.chalkakserver.dto.post.PostResponseDto;
import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.Post;
import com.min.chalkakserver.entity.PostLike;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.exception.AuthException;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.exception.PostNotFoundException;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.PostLikeRepository;
import com.min.chalkakserver.repository.PostRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PhotoBoothRepository photoBoothRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponseDto<PostResponseDto> getFeed(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
        return toPagedResponse(postPage, currentUser);
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<PostResponseDto> getPhotoBoothPosts(Long photoBoothId, Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByPhotoBoothIdOrderByCreatedAtDesc(photoBoothId, pageable);
        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
        return toPagedResponse(postPage, currentUser);
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<PostResponseDto> getPopularPosts(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        Page<Post> postPage = postRepository.findPopularSince(weekAgo, pageable);
        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
        return toPagedResponse(postPage, currentUser);
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<PostResponseDto> getNearbyPosts(
        double latitude, double longitude, double radiusKm,
        Long currentUserId, int page, int size) {
        // Bounding box pre-filter for index use. 1 degree latitude ≈ 111 km.
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));
        double minLat = latitude - latDelta;
        double maxLat = latitude + latDelta;
        double minLon = longitude - lonDelta;
        double maxLon = longitude + lonDelta;

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findNearbyPosts(
            latitude, longitude, minLat, maxLat, minLon, maxLon, radiusKm, pageable);
        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
        return toPagedResponse(postPage, currentUser);
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<PostResponseDto> getMyPosts(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return toPagedResponse(postPage, user);
    }

    @Transactional
    public PostResponseDto createPost(Long userId, PostRequestDto request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        PhotoBooth photoBooth = null;
        if (request.getPhotoBoothId() != null) {
            photoBooth = photoBoothRepository.findById(request.getPhotoBoothId())
                .orElseThrow(() -> new PhotoBoothNotFoundException(request.getPhotoBoothId()));
        }

        Post post = Post.builder()
            .user(user)
            .photoBooth(photoBooth)
            .imageUrl(request.getImageUrl())
            .caption(request.getCaption())
            .build();

        Post saved = postRepository.save(post);
        log.info("Post created: userId={}, postId={}", userId, saved.getId());
        return PostResponseDto.from(saved, false);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getUser().getId().equals(userId)) {
            throw new AuthException("게시물을 삭제할 권한이 없습니다.");
        }

        postRepository.delete(post);
        log.info("Post deleted: postId={}, userId={}", postId, userId);
    }

    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        if (postLikeRepository.existsByPostAndUser(post, user)) {
            postLikeRepository.deleteByPostAndUser(post, user);
            post.decrementLikes();
            log.info("Post unliked: postId={}, userId={}", postId, userId);
            return false;
        } else {
            PostLike like = PostLike.builder()
                .post(post)
                .user(user)
                .build();
            postLikeRepository.save(like);
            post.incrementLikes();
            log.info("Post liked: postId={}, userId={}", postId, userId);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public boolean checkLiked(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));
        return postLikeRepository.existsByPostAndUser(post, user);
    }

    private PagedResponseDto<PostResponseDto> toPagedResponse(Page<Post> postPage, User currentUser) {
        List<PostResponseDto> content = postPage.getContent().stream()
            .map(post -> {
                boolean isLiked = currentUser != null && postLikeRepository.existsByPostAndUser(post, currentUser);
                return PostResponseDto.from(post, isLiked);
            })
            .collect(Collectors.toList());

        return new PagedResponseDto<>(
            content,
            postPage.getNumber(),
            postPage.getSize(),
            postPage.getTotalElements(),
            postPage.getTotalPages(),
            postPage.isFirst(),
            postPage.isLast(),
            postPage.hasNext(),
            postPage.hasPrevious()
        );
    }
}
