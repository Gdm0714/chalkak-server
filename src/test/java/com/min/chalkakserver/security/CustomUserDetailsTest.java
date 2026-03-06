package com.min.chalkakserver.security;

import com.min.chalkakserver.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.lang.reflect.Field;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomUserDetails 테스트")
class CustomUserDetailsTest {

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() throws Exception {
        User user = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .provider(User.AuthProvider.EMAIL)
                .providerId("test@test.com")
                .role(User.Role.USER)
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);

        userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("사용자 ID를 반환한다")
    void getId() {
        assertThat(userDetails.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이메일을 반환한다")
    void getEmail() {
        assertThat(userDetails.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("닉네임을 반환한다")
    void getNickname() {
        assertThat(userDetails.getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("프로바이더를 반환한다")
    void getProvider() {
        assertThat(userDetails.getProvider()).isEqualTo(User.AuthProvider.EMAIL);
    }

    @Test
    @DisplayName("역할을 반환한다")
    void getRole() {
        assertThat(userDetails.getRole()).isEqualTo(User.Role.USER);
    }

    @Test
    @DisplayName("ROLE_USER 권한을 가진다")
    void getAuthorities() {
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("ADMIN 역할은 ROLE_ADMIN 권한을 가진다")
    void getAuthorities_Admin() throws Exception {
        User adminUser = User.builder()
                .email("admin@test.com")
                .nickname("admin")
                .provider(User.AuthProvider.EMAIL)
                .providerId("admin@test.com")
                .role(User.Role.ADMIN)
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(adminUser, 2L);

        CustomUserDetails adminDetails = new CustomUserDetails(adminUser);
        assertThat(adminDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("비밀번호는 null을 반환한다")
    void getPassword() {
        assertThat(userDetails.getPassword()).isNull();
    }

    @Test
    @DisplayName("사용자 이름은 ID를 문자열로 반환한다")
    void getUsername() {
        assertThat(userDetails.getUsername()).isEqualTo("1");
    }

    @Test
    @DisplayName("계정이 만료되지 않았다")
    void isAccountNonExpired() {
        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("계정이 잠기지 않았다")
    void isAccountNonLocked() {
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("자격 증명이 만료되지 않았다")
    void isCredentialsNonExpired() {
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("계정이 활성화되어 있다")
    void isEnabled() {
        assertThat(userDetails.isEnabled()).isTrue();
    }
}
