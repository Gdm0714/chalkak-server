package com.min.chalkakserver.service;

import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.ReviewRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final PhotoBoothRepository photoBoothRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public Map<String, Long> getSummary() {
        return Map.of(
                "photoBoothCount", photoBoothRepository.count(),
                "activeUserCount", userRepository.count(),
                "reviewCount", reviewRepository.count()
        );
    }
}
