package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.DeviceToken;
import com.min.chalkakserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByUser(User user);

    Optional<DeviceToken> findByFcmToken(String fcmToken);

    void deleteByUser(User user);
}
