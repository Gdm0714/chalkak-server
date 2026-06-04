package com.min.chalkakserver.service;

import com.min.chalkakserver.dto.stats.AppStatsSummaryDto;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.ReviewRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final PhotoBoothRepository photoBoothRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public AppStatsSummaryDto getSummary() {
        return AppStatsSummaryDto.builder()
                .photoBoothCount(photoBoothRepository.count())
                .activeUserCount(userRepository.count())
                .reviewCount(reviewRepository.count())
                .build();
    }
}
