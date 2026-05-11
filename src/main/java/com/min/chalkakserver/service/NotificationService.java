package com.min.chalkakserver.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.min.chalkakserver.dto.PagedResponseDto;
import com.min.chalkakserver.dto.notification.NotificationResponseDto;
import com.min.chalkakserver.entity.DeviceToken;
import com.min.chalkakserver.entity.Notification;
import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.repository.DeviceTokenRepository;
import com.min.chalkakserver.repository.FavoriteRepository;
import com.min.chalkakserver.repository.NotificationRepository;
import com.min.chalkakserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    // FCM은 선택적: 서비스 계정 JSON 없으면 null로 주입되고 푸시는 스킵.
    @Autowired(required = false)
    @Nullable
    private FirebaseMessaging firebaseMessaging;

    public void registerDeviceToken(Long userId, String fcmToken, String platform) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        DeviceToken.Platform platformEnum;
        try {
            platformEnum = DeviceToken.Platform.valueOf(platform.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 플랫폼입니다: " + platform);
        }

        deviceTokenRepository.findByFcmToken(fcmToken).ifPresentOrElse(
            existing -> {
                // Token already registered - no-op
            },
            () -> {
                DeviceToken token = DeviceToken.builder()
                        .user(user)
                        .fcmToken(fcmToken)
                        .platform(platformEnum)
                        .build();
                deviceTokenRepository.save(token);
                log.info("Device token registered: userId={}, platform={}", userId, platform);
            }
        );
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<NotificationResponseDto> getNotifications(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Page<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(
                user, PageRequest.of(page, size));
        return PagedResponseDto.from(notifications.map(NotificationResponseDto::from));
    }

    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        notification.markAsRead();
    }

    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        notificationRepository.markAllAsReadByUser(user);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    public void sendPushNotification(User user, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.warn("FirebaseMessaging is not initialized. Skipping push notification for userId={}", user.getId());
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUser(user);
        if (tokens.isEmpty()) {
            return;
        }

        for (DeviceToken deviceToken : tokens) {
            try {
                Message.Builder messageBuilder = Message.builder()
                        .setToken(deviceToken.getFcmToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build());

                if (data != null && !data.isEmpty()) {
                    messageBuilder.putAllData(data);
                }

                firebaseMessaging.send(messageBuilder.build());
                log.debug("Push sent to userId={}", user.getId());
            } catch (FirebaseMessagingException e) {
                log.warn("Failed to send push to userId={}: {}", user.getId(), e.getMessage());
            }
        }
    }

    @Async
    public void notifyFavoriteUsers(Long photoBoothId, String title, String body) {
        List<User> users = favoriteRepository.findUsersByPhotoBoothId(photoBoothId);
        for (User user : users) {
            try {
                Notification notification = Notification.builder()
                        .user(user)
                        .type(Notification.NotificationType.CONGESTION_CHANGE)
                        .title(title)
                        .body(body)
                        .build();
                notificationRepository.save(notification);
                sendPushNotification(user, title, body, null);
            } catch (Exception e) {
                log.warn("Failed to notify userId={} for photoBoothId={}: {}", user.getId(), photoBoothId, e.getMessage());
            }
        }
    }
}
