package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.User;
import com.min.chalkakserver.entity.User.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    Optional<User> findByEmail(String email);

    // 동일 이메일로 가입된 모든 계정 조회 (email은 unique 제약이 없음 - provider별 중복 가능)
    List<User> findAllByEmail(String email);
    
    // 이메일 회원가입 시 중복 체크용
    Optional<User> findByEmailAndProvider(String email, AuthProvider provider);
    
    boolean existsByEmail(String email);
    
    boolean existsByProviderAndProviderId(AuthProvider provider, String providerId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :dateTime")
    long countByCreatedAtAfter(@Param("dateTime") LocalDateTime dateTime);
}
