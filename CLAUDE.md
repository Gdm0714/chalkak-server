# Chalkak Server (찰칵 서버)

## 프로젝트 개요

네컷사진관 위치 찾기 및 웨이팅 서비스 백엔드 API 서버

---

## 기술 스택

- **Framework**: Spring Boot 3.5.0
- **Language**: Java 21
- **Database**: MySQL
- **Cache**: Redis
- **Security**: Spring Security + JWT
- **Docs**: SpringDoc OpenAPI (Swagger)

---

## 프로젝트 구조

```
src/main/java/com/min/chalkakserver/
├── ChalkakServerApplication.java
├── config/
│   ├── SecurityConfig.java       # Spring Security 설정
│   ├── WebMvcConfig.java         # CORS, Interceptor 설정
│   ├── OpenApiConfig.java        # Swagger 설정
│   ├── RateLimitConfig.java      # Rate Limiting
│   └── cache/
│       ├── RedisCacheConfig.java
│       └── ...
├── controller/
│   ├── AuthController.java       # 인증 API
│   ├── PhotoBoothController.java # 사진관 API
│   ├── ReviewController.java     # 리뷰 API
│   ├── FavoriteController.java   # 즐겨찾기 API
│   └── AdminController.java      # 관리자 API
├── service/
│   ├── AuthService.java
│   ├── SocialAuthService.java    # 소셜 로그인 처리
│   ├── PhotoBoothService.java
│   ├── ReviewService.java
│   └── FavoriteService.java
├── repository/
│   ├── UserRepository.java
│   ├── PhotoBoothRepository.java
│   └── ...
├── entity/
│   ├── User.java
│   ├── PhotoBooth.java
│   ├── Review.java
│   ├── Favorite.java
│   └── RefreshToken.java
├── dto/
│   ├── auth/
│   ├── review/
│   └── ...
├── security/
│   ├── jwt/
│   │   ├── JwtTokenProvider.java
│   │   └── JwtAuthenticationFilter.java
│   └── CustomUserDetails.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── ...
└── scheduler/
    └── TokenCleanupScheduler.java
```

---

## 개발 규칙

### 레이어 아키텍처
```
Controller → Service → Repository → Entity
    ↓           ↓
   DTO         DTO
```

### 코드 작성 규칙

1. **Entity 작성 시**
   - `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 사용
   - `@Builder` 패턴 사용
   - 연관관계는 지연 로딩(LAZY) 기본
   - `@PrePersist`, `@PreUpdate`로 생성/수정 시간 관리

2. **Controller 작성 시**
   - RESTful 규칙 준수
   - 요청/응답은 DTO 사용
   - 인증 필요 API는 `@PreAuthorize` 또는 SecurityConfig에서 설정

3. **Service 작성 시**
   - `@Transactional` 적절히 사용
   - 읽기 전용은 `@Transactional(readOnly = true)`
   - 비즈니스 로직만 담당, Controller에 로직 넣지 않기

4. **DTO 작성 시**
   - 요청: `~RequestDto`
   - 응답: `~ResponseDto`
   - record 또는 class + Lombok 사용

5. **예외 처리**
   - 커스텀 예외는 `exception/` 패키지에
   - `GlobalExceptionHandler`에서 일괄 처리

### 네이밍 컨벤션
- 클래스: PascalCase (`PhotoBoothService`)
- 메서드/변수: camelCase (`findByLocation`)
- 상수: SCREAMING_SNAKE_CASE (`MAX_RETRY_COUNT`)
- 테이블: snake_case (`photo_booth`)

---

## API 엔드포인트

### 인증 (`/auth`)
- `POST /auth/login` - 소셜 로그인
- `POST /auth/refresh` - 토큰 갱신
- `POST /auth/logout` - 로그아웃
- `GET /auth/me` - 현재 사용자 정보
- `DELETE /auth/withdraw` - 회원 탈퇴

### 사진관 (`/api/photo-booths`)
- `GET /api/photo-booths` - 목록 조회
- `GET /api/photo-booths/{id}` - 상세 조회
- `GET /api/photo-booths/nearby` - 주변 사진관 검색

### 리뷰 (`/api/reviews`)
- `GET /api/reviews/photo-booth/{id}` - 사진관별 리뷰
- `POST /api/reviews` - 리뷰 작성
- `PUT /api/reviews/{id}` - 리뷰 수정
- `DELETE /api/reviews/{id}` - 리뷰 삭제

### 즐겨찾기 (`/api/favorites`)
- `GET /api/favorites` - 목록 조회
- `POST /api/favorites/{photoBoothId}` - 추가
- `DELETE /api/favorites/{photoBoothId}` - 삭제

---

## 인증 & 보안

### JWT 토큰
- Access Token: 1시간 만료
- Refresh Token: 14일 만료, DB 저장
- 토큰 갱신 시 Refresh Token도 재발급 (Rotation)

### User Role
```java
public enum Role {
    USER,       // 일반 사용자
    ADMIN       // 관리자
}
// [예정] STORE_OWNER 추가
```

### 소셜 로그인 지원
- KAKAO
- NAVER  
- APPLE

---

## 캐싱 전략

- Redis 사용
- 사진관 목록/상세: 캐싱 적용
- 위치 기반 검색: LocationKeyGenerator로 키 생성

---

## 빌드 & 실행

```bash
# 개발 실행
./gradlew bootRun

# 빌드
./gradlew build

# 테스트
./gradlew test
```

### 환경 설정
- `application.yml`: 기본 설정
- `application-prod.yml`: 프로덕션 설정

---

## 웨이팅 MVP 개발 가이드 (예정)

### 추가할 Entity
```java
// Store - 사진관 사업자
- id, ownerId (User FK), photoBoothId (PhotoBooth FK)
- businessName, businessNumber
- isWaitingEnabled, maxWaitingCount

// WaitingQueue - 웨이팅 대기열
- id, storeId (Store FK), userId (User FK)
- queueNumber, status (WAITING/CALLED/COMPLETED/CANCELLED)
- estimatedWaitTime
- createdAt, calledAt, completedAt
```

### 추가할 API
```
POST /api/waiting/{storeId}        - 웨이팅 등록
GET /api/waiting/my                - 내 웨이팅 조회
DELETE /api/waiting/{id}           - 웨이팅 취소
GET /api/waiting/store/{storeId}   - 사진관 웨이팅 목록 (사장님용)
POST /api/waiting/{id}/call        - 호출 (사장님용)
POST /api/waiting/{id}/complete    - 완료 처리 (사장님용)
```

### User Role 확장
```java
public enum Role {
    USER,
    STORE_OWNER,  // 추가
    ADMIN
}
```
