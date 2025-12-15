package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.RefreshToken;
import com.min.chalkakserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    List<RefreshToken> findByUser(User user);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    void deleteAllByUser(@Param("user") User user);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteAllExpiredTokens(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.token = :token")
    void deleteByToken(@Param("token") String token);

    /**
     * 토큰 패밀리의 모든 토큰 삭제 (재사용 감지 시 보안 조치)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.tokenFamily = :tokenFamily")
    void deleteAllByTokenFamily(@Param("tokenFamily") String tokenFamily);

    /**
     * 사용자의 특정 기기 토큰 조회
     */
    Optional<RefreshToken> findByUserAndDeviceInfo(User user, String deviceInfo);

    /**
     * 사용된 오래된 토큰 삭제 (Token Rotation으로 인한 사용된 토큰들)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.used = true AND rt.createdAt < :cutoff")
    void deleteUsedTokensOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /**
     * 사용된 토큰 수 조회 (모니터링용)
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.used = true")
    long countUsedTokens();
}
