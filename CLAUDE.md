# Chalkak Server (찰칵 서버)

## 프로젝트 개요

네컷사진관 위치 찾기 및 혼잡도 서비스 백엔드 API 서버

- **Framework**: Spring Boot 3.5.0
- **Language**: Java 21
- **Database**: MySQL 8.0+ (JPA/Hibernate)
- **Cache**: Redis 7.2 (Lettuce)
- **Security**: Spring Security + JWT (JJWT 0.12.6)
- **Rate Limiting**: Bucket4j 8.10.1 (in-memory)
- **Docs**: SpringDoc OpenAPI 2.7.0 (Swagger UI)
- **Mail**: Spring Mail (Gmail SMTP)
- **Build**: Gradle 8.x
- **Deploy**: GitHub Actions → EC2 (JAR 직접 배포)

---

## 프로젝트 구조

```
src/main/java/com/min/chalkakserver/
├── ChalkakServerApplication.java
├── config/
│   ├── SecurityConfig.java          # Security 필터 체인, CORS, 엔드포인트 인가
│   ├── WebMvcConfig.java            # 인터셉터 등록
│   ├── OpenApiConfig.java           # Swagger 설정
│   ├── PasswordEncoderConfig.java   # BCrypt
│   ├── RateLimitConfig.java         # Bucket4j 토큰 버킷 정의
│   ├── RateLimitInterceptor.java    # 요청별 레이트 리밋 적용
│   ├── RestTemplateConfig.java      # RestTemplate 빈
│   └── cache/
│       ├── RedisCacheConfig.java    # Lettuce 연결, 캐시 TTL 매트릭스
│       ├── LocationKeyGenerator.java # 위치 기반 캐시 키 생성 (3자리 절삭)
│       ├── CacheWarmupRunner.java   # 앱 시작 시 캐시 워밍업
│       └── CachePerformanceAspect.java # 캐시 성능 AOP
├── controller/
│   ├── AuthController.java          # 인증 API (/api/auth)
│   ├── PhotoBoothController.java    # 사진관 API (/api/photo-booths)
│   ├── ReviewController.java        # 리뷰 API (/api/reviews)
│   ├── FavoriteController.java      # 즐겨찾기 API (/api/favorites)
│   ├── CongestionController.java    # 혼잡도 API (/api/congestion)
│   ├── AdminController.java         # 관리자 API (/api/admin) [ROLE_ADMIN]
│   ├── CacheController.java         # 캐시 관리 (/api/cache) [ROLE_ADMIN]
│   └── HealthCheckController.java   # 헬스체크
├── service/
│   ├── AuthService.java             # 소셜/이메일 로그인, 토큰 로테이션
│   ├── SocialAuthService.java       # Kakao/Naver REST + Apple JWT 검증
│   ├── PhotoBoothService.java       # 사진관 CRUD + 캐시
│   ├── ReviewService.java           # 리뷰 CRUD
│   ├── FavoriteService.java         # 즐겨찾기
│   ├── CongestionService.java       # 시간 가중 혼잡도 집계
│   ├── CacheService.java            # 캐시 관리
│   └── EmailService.java            # 제보 이메일 발송
├── repository/
│   ├── UserRepository.java
│   ├── PhotoBoothRepository.java    # Haversine 네이티브 SQL 지오쿼리
│   ├── ReviewRepository.java
│   ├── FavoriteRepository.java
│   ├── CongestionReportRepository.java
│   └── RefreshTokenRepository.java
├── entity/
│   ├── User.java                    # provider/role enum, 소셜 연동
│   ├── PhotoBooth.java              # 위치 인덱스, @DynamicUpdate
│   ├── Review.java                  # 1인 1리뷰 제약 (user_id, photo_booth_id UNIQUE)
│   ├── Favorite.java                # 1인 1즐겨찾기 제약 (user_id, photo_booth_id UNIQUE)
│   ├── CongestionReport.java        # 레벨 enum (점수 매핑), append-only
│   └── RefreshToken.java            # family 기반 토큰 로테이션
├── dto/
│   ├── ErrorResponse.java           # 표준 에러 응답 envelope
│   ├── PagedResponseDto.java        # Spring Page 래퍼
│   ├── PhotoBoothDTO.java
│   ├── PhotoBoothRequestDto.java
│   ├── PhotoBoothResponseDto.java
│   ├── PhotoBoothReportDto.java
│   ├── auth/
│   │   ├── SocialLoginRequestDto.java    # @NotBlank + @Pattern 검증
│   │   ├── EmailLoginRequestDto.java
│   │   ├── EmailRegisterRequestDto.java
│   │   ├── RefreshTokenRequestDto.java
│   │   ├── ProfileUpdateRequestDto.java
│   │   ├── AuthResponseDto.java          # accessToken + refreshToken + UserResponseDto
│   │   ├── UserResponseDto.java
│   │   └── SocialUserInfo.java
│   ├── review/
│   │   ├── ReviewRequestDto.java
│   │   ├── ReviewResponseDto.java
│   │   └── ReviewStatsDto.java           # 평균 평점, 분포
│   ├── congestion/
│   │   ├── CongestionReportRequestDto.java
│   │   ├── CongestionReportResponseDto.java
│   │   └── CongestionResponseDto.java
│   ├── favorite/
│   │   └── FavoriteResponseDto.java
│   └── admin/
│       ├── AdminStatsDto.java
│       └── UserListResponseDto.java
├── security/
│   ├── jwt/
│   │   ├── JwtTokenProvider.java         # HS256, 토큰 생성/검증
│   │   └── JwtAuthenticationFilter.java  # OncePerRequestFilter
│   └── CustomUserDetails.java            # UserDetails 래퍼
├── exception/
│   ├── GlobalExceptionHandler.java       # @RestControllerAdvice
│   ├── AuthException.java               # code 기반 (UNAUTHORIZED/NOT_FOUND/CONFLICT)
│   ├── PhotoBoothNotFoundException.java  # → 404
│   ├── ReviewNotFoundException.java      # → 404
│   ├── InvalidLocationException.java     # → 400
│   ├── DuplicatePhotoBoothException.java # → 409
│   ├── DuplicateReviewException.java     # → 409
│   └── DuplicateCongestionReportException.java # → 409
├── scheduler/
│   └── TokenCleanupScheduler.java        # 만료 토큰 정리, 버킷 정리
└── util/
    └── LocationSearchBenchmark.java
```

---

## 레이어 아키텍처

```
Controller → Service → Repository → Entity
    ↓           ↓
   DTO         DTO
```

- **Controller**: REST 엔드포인트, `@Validated` DTO 입출력
- **Service**: `@Transactional` 비즈니스 로직, 캐시 어노테이션
- **Repository**: `JpaRepository` + 네이티브 SQL (Haversine 지오쿼리)
- **Entity**: JPA, `@PrePersist`/`@PreUpdate`, `@Builder`, Lazy 로딩
- **DTO**: `*RequestDto` / `*ResponseDto`, Bean Validation, static `from(entity)` 팩토리

---

## 보안 설정

### JWT

- 알고리즘: HS256 (JJWT 0.12.6)
- `JWT_SECRET`: 최소 32자 필수 (`@PostConstruct`에서 검증 → 미달 시 앱 시작 실패)
- **Access Token** (기본 1시간): claims = `sub`(userId), `email`, `nickname`, `role`, `provider`
- **Refresh Token** (기본 14일): claims = `sub`(userId), `type`="refresh"
- 헤더: `Authorization: Bearer <token>`

### Refresh Token Rotation

- 로그인 시 UUID `tokenFamily` 생성
- 갱신 시: 이전 토큰 `used=true` 마킹, 같은 family에 새 토큰 저장
- **재사용 감지 (Replay Attack)**: used 토큰 재사용 시 해당 family 전체 삭제
- 매일 03:00 만료 토큰 삭제, 04:00 사용된 토큰 정리

### 공개 엔드포인트 (JWT 불필요)

```
GET  /api/photo-booths/**
GET  /api/congestion/**
POST /api/photo-booths/report
POST /api/auth/login, /api/auth/login/email, /api/auth/register
POST /api/auth/refresh, /api/auth/logout
/swagger-ui/**, /v3/api-docs/**, /actuator/health, /actuator/info
```

### 역할 기반 접근

```
/api/cache/**   → ROLE_ADMIN only
/api/admin/**   → ROLE_ADMIN only (@PreAuthorize)
나머지           → authenticated (모든 role)
```

### CORS

- `CORS_ALLOWED_ORIGINS` 환경변수 (기본: `http://localhost:*,https://localhost:*`)
- 모든 HTTP 메서드 허용, credentials 허용, preflight 1시간 캐시
- `CorsConfigurationSource` 빈으로 글로벌 적용

---

## 소셜 로그인

### Kakao

1. 클라이언트가 Kakao access token 획득
2. `POST /api/auth/login` with `{provider: "kakao", accessToken, deviceInfo}`
3. 서버가 `https://kapi.kakao.com/v2/user/me` 호출
4. `id`, `email`, `nickname`, `profile_image_url` 추출
5. `(provider=KAKAO, providerId)` 기준 upsert

### Naver

1. `provider: "naver"`, 같은 엔드포인트
2. 서버가 `https://openapi.naver.com/v1/nid/me` 호출
3. `response` 노드에서 `id`, `email`, `nickname`, `profile_image` 추출

### Apple

1. 클라이언트가 Apple `identityToken` (JWT) 획득
2. `provider: "apple"`, accessToken 필드에 identityToken 전달
3. 서버가 JWT 헤더에서 `kid` 추출
4. `https://appleid.apple.com/auth/keys` (JWKS) 조회, RSA 키 24시간 캐시
5. RSA 서명, issuer, audience (`APPLE_CLIENT_ID`), 만료 검증
6. `sub`를 stable Apple user ID로 사용

---

## 데이터베이스 스키마

### Entity 공통 패턴

- `@NoArgsConstructor(access = PROTECTED)`, `@Builder`, `@Getter`
- `@PrePersist`/`@PreUpdate`로 생성/수정 시간 관리
- 모든 `@ManyToOne`은 `LAZY` 로딩
- `@OneToMany` 컬렉션 없음 (N+1 방지)

### 테이블

| 테이블 | 주요 컬럼 | 제약/인덱스 |
|--------|----------|------------|
| `users` | id, email, password(BCrypt), nickname, provider(ENUM), providerId, role(ENUM) | `(provider, providerId)` UNIQUE |
| `photo_booths` | id, name, brand, series, address, latitude, longitude, operatingHours | `(latitude, longitude)`, `(brand)`, `(name)` |
| `reviews` | id, user_id FK, photo_booth_id FK, rating(1-5), content | `(user_id, photo_booth_id)` UNIQUE |
| `favorites` | id, user_id FK, photo_booth_id FK | `(user_id, photo_booth_id)` UNIQUE |
| `congestion_reports` | id, user_id FK, photo_booth_id FK, congestion_level(ENUM+score) | `(photo_booth_id, created_at)` |
| `refresh_tokens` | id, user_id FK, token, deviceInfo, expiresAt, is_used, token_family(UUID) | `(token)` UNIQUE |

### Hibernate 설정

- Dev: `ddl-auto: update`
- Prod: `ddl-auto: validate` (application-prod.yml)
- OSIV 비활성화: `open-in-view: false`

---

## 캐싱 전략

### Redis (Lettuce)

- 직렬화: 키 `StringRedisSerializer`, 값 `GenericJackson2JsonRedisSerializer` (JavaTimeModule)
- Null 캐시 안 함

### 캐시 이름 & TTL

| 캐시 이름 | TTL | 용도 |
|-----------|-----|------|
| `photoBooths` | 1시간 | 전체 목록 |
| `photoBooth` | 2시간 | ID별 단건 |
| `nearbyPhotoBooths` | 30분 | 근처 검색 |
| `searchResults` | 30분 | 키워드 검색 |
| `brandPhotoBooths` | 1시간 | 브랜드 필터 |
| `brandSeriesPhotoBooths` | 30분 (기본) | 브랜드+시리즈 |
| `seriesPhotoBooths` | 30분 (기본) | 시리즈 필터 |

### LocationKeyGenerator

위도/경도를 소수점 3자리로 절삭 (~111m 그리드) → 캐시 히트율 향상

키 포맷: `lat:37.123_lon:127.456_rad:3.0`

### 캐시 무효화

`createPhotoBooth`, `updatePhotoBooth`, `deletePhotoBooth` → `photoBooths`, `nearbyPhotoBooths`, `searchResults`, `brandPhotoBooths` 전체 evict + 해당 `photoBooth` ID evict

---

## 레이트 리밋

### Bucket4j (in-memory, ConcurrentHashMap by IP)

| 버킷 | 적용 대상 | 제한 |
|------|----------|------|
| General | 모든 `/api/**` | 10 req/sec AND 60 req/min |
| Auth | `/auth/login`, `/auth/refresh` | 10 req/min AND 30 req/hour |
| Report | `/report` | 5 req/min |

### IP 추출 순서

`X-Forwarded-For` (첫 값) → `X-Real-IP` → `request.getRemoteAddr()`

### 응답

- 초과 시: HTTP 429 + `X-Rate-Limit-Retry-After-Seconds` 헤더
- 정상 시: `X-Rate-Limit-Remaining` 헤더
- 메모리 관리: 매시간 정리, 10,000 엔트리 초과 시 전체 초기화

**주의**: in-memory이므로 다중 인스턴스 배포 시 상태 미공유

---

## 혼잡도 기능

### 데이터 모델

`CongestionReport`: append-only. 레벨 enum (RELAXED=1, NORMAL=2, BUSY=3, VERY_BUSY=4, UNKNOWN=0)

### 쿨다운

사용자당 사진관당 60분에 1회만 제출 가능 → 위반 시 `DuplicateCongestionReportException` (409)

### 시간 가중 집계 알고리즘

최근 60분 리포트 대상:

```
weight = exp(-(minutesAgo / 30))   // 지수 감쇠, 30분 전 ~37% 가중치
score = Σ(level.score × weight) / Σ(weight)
```

### 점수 → 레벨 매핑

| 점수 범위 | 레벨 | 예상 대기 |
|-----------|------|----------|
| < 1.75 | RELAXED | 0-10분 |
| < 2.50 | NORMAL | 10-20분 |
| < 3.25 | BUSY | 20-35분 |
| >= 3.25 | VERY_BUSY | 35-60분 |

### 신뢰도

| 리포트 수 | 레벨 |
|-----------|------|
| < 3 | LOW |
| 3-5 | MEDIUM |
| 6+ | HIGH |

데이터 없으면: `UNKNOWN` + "아직 혼잡도 데이터가 부족해요."

---

## 에러 처리

### 표준 에러 응답

```json
{
  "timestamp": "2025-...",
  "status": 404,
  "error": "Not Found",
  "message": "해당 사진관을 찾을 수 없습니다.",
  "path": "/api/photo-booths/99",
  "details": { "photoBoothId": 99 }
}
```

`details`는 `@JsonInclude(NON_NULL)` — 없으면 생략

### 예외 → HTTP 매핑

| 예외 | HTTP |
|------|------|
| `PhotoBoothNotFoundException` | 404 |
| `ReviewNotFoundException` | 404 |
| `AuthException(NOT_FOUND)` | 404 |
| `InvalidLocationException` | 400 |
| `ConstraintViolationException` | 400 |
| `MethodArgumentNotValidException` | 400 |
| `DuplicatePhotoBoothException` | 409 |
| `DuplicateReviewException` | 409 |
| `DuplicateCongestionReportException` | 409 |
| `AuthException(UNAUTHORIZED)` | 401 |
| `AuthException(CONFLICT)` | 409 |
| `Exception` (catch-all) | 500 |

---

## API 엔드포인트

### 인증 (`/api/auth`)

```
POST   /api/auth/login           # 소셜 로그인
POST   /api/auth/login/email     # 이메일 로그인
POST   /api/auth/register        # 이메일 회원가입
POST   /api/auth/refresh         # 토큰 갱신 (rotation)
POST   /api/auth/logout          # 로그아웃 (단일 기기)
POST   /api/auth/logout-all      # 전체 기기 로그아웃 [AUTH]
GET    /api/auth/me              # 현재 사용자 정보 [AUTH]
PUT    /api/auth/profile         # 프로필 수정 [AUTH]
DELETE /api/auth/withdraw        # 회원 탈퇴 [AUTH]
```

### 사진관 (`/api/photo-booths`)

```
GET    /api/photo-booths                              # 전체 목록
GET    /api/photo-booths/paged?page=0&size=20         # 페이지네이션
GET    /api/photo-booths/{id}                          # 단건 조회
GET    /api/photo-booths/nearby?latitude=&longitude=&radius=3.0  # 근처 검색
GET    /api/photo-booths/search?keyword=               # 키워드 검색
GET    /api/photo-booths/search/paged                  # 페이지네이션 검색
GET    /api/photo-booths/brand/{brand}                 # 브랜드별
GET    /api/photo-booths/brand/{brand}/paged           # 브랜드 페이지네이션
GET    /api/photo-booths/brand/{brand}/series/{series} # 브랜드+시리즈
GET    /api/photo-booths/series/{series}               # 시리즈별
POST   /api/photo-booths/report                        # 사진관 제보 (이메일 발송)
```

### 리뷰 (`/api/reviews`)

```
POST   /api/reviews/photo-booth/{id}         # 작성 [AUTH]
PUT    /api/reviews/{id}                      # 수정 [AUTH]
DELETE /api/reviews/{id}                      # 삭제 [AUTH]
GET    /api/reviews/photo-booth/{id}          # 사진관별 리뷰 목록
GET    /api/reviews/photo-booth/{id}/paged    # 페이지네이션
GET    /api/reviews/photo-booth/{id}/stats    # 평점 통계
GET    /api/reviews/{id}                      # 단건 조회
GET    /api/reviews/my                        # 내 리뷰 [AUTH]
GET    /api/reviews/my/paged                  # 내 리뷰 페이지네이션 [AUTH]
GET    /api/reviews/my/photo-booth/{id}       # 특정 사진관 내 리뷰 [AUTH]
```

### 혼잡도 (`/api/congestion`)

```
GET    /api/congestion/photo-booth/{id}      # 현재 혼잡도 (공개)
POST   /api/congestion/photo-booth/{id}      # 혼잡도 제보 [AUTH]
```

### 즐겨찾기 (`/api/favorites`)

```
POST   /api/favorites/{photoBoothId}         # 추가 [AUTH]
DELETE /api/favorites/{photoBoothId}         # 삭제 [AUTH]
GET    /api/favorites                        # 내 목록 [AUTH]
GET    /api/favorites/paged                  # 페이지네이션 [AUTH]
GET    /api/favorites/check/{photoBoothId}   # 즐겨찾기 여부 [AUTH]
GET    /api/favorites/count/{photoBoothId}   # 총 카운트
```

### 관리자 (`/api/admin`) [ROLE_ADMIN]

```
GET    /api/admin/stats                      # 통계
POST   /api/admin/photo-booths              # 사진관 등록
PUT    /api/admin/photo-booths/{id}         # 사진관 수정
DELETE /api/admin/photo-booths/{id}         # 사진관 삭제
GET    /api/admin/users                     # 사용자 목록
GET    /api/admin/users/{userId}            # 사용자 상세
PATCH  /api/admin/users/{userId}/role       # 역할 변경
DELETE /api/admin/users/{userId}            # 사용자 삭제
DELETE /api/admin/reviews/{reviewId}        # 리뷰 삭제
```

### 페이지네이션 규칙

- `?page=0&size=20` (0-indexed)
- `PagedResponseDto<T>`로 래핑
- 기본 정렬: `createdAt DESC`
- 최대 size: `@Max(100)`

---

## 코드 작성 규칙

### Entity

- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 필수
- `@Builder` 패턴 사용
- 연관관계는 `LAZY` 기본
- `@PrePersist`, `@PreUpdate`로 시간 관리

### Controller

- RESTful 규칙 준수
- 요청/응답은 반드시 DTO
- `@Validated` 또는 `@Valid`로 입력 검증

### Service

- `@Transactional` 적절히 사용
- 읽기 전용: `@Transactional(readOnly = true)`
- Controller에 비즈니스 로직 넣지 않기

### DTO

- 요청: `~RequestDto`, 응답: `~ResponseDto`, 통계: `~StatsDto`
- Lombok: `@Getter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Bean Validation (`@NotBlank`, `@Pattern`, `@Valid`)
- Response DTO에 static `from(entity)` 팩토리

### 네이밍

- 클래스: PascalCase (`PhotoBoothService`)
- 메서드/변수: camelCase (`findByLocation`)
- 상수: SCREAMING_SNAKE_CASE (`MAX_RETRY_COUNT`)
- 테이블: snake_case (`photo_booth`)

---

## 환경 변수

### 필수 (앱 시작 실패)

| 변수 | 설명 |
|------|------|
| `JWT_SECRET` | 최소 32자, `@PostConstruct` 검증 |
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` | DB 사용자 |
| `DB_PASSWORD` | DB 비밀번호 |
| `REDIS_HOST` | Redis 호스트 |
| `REDIS_PORT` | Redis 포트 |
| `MAIL_USERNAME` | Gmail SMTP 계정 |
| `MAIL_PASSWORD` | Gmail 앱 비밀번호 |
| `APPLE_CLIENT_ID` | Apple 번들 ID (JWT audience 검증) |

### 선택 (기본값 있음)

| 변수 | 기본값 |
|------|-------|
| `SERVER_PORT` | 8082 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:*,https://localhost:*` |
| `ADMIN_EMAIL` | `admin@chalkak.co.kr` |
| `JWT_ACCESS_TOKEN_VALIDITY` | 3600000 (1시간, ms) |
| `JWT_REFRESH_TOKEN_VALIDITY` | 1209600000 (14일, ms) |
| `RATE_LIMIT_PER_MINUTE` | 60 |
| `RATE_LIMIT_PER_SECOND` | 10 |
| `DDL_AUTO` | update |

### 프로덕션

`SPRING_PROFILES_ACTIVE=prod` 설정 필요 → `ddl-auto: validate`, actuator 상세 숨김

---

## 빌드 & 실행

```bash
# 개발 실행
./gradlew bootRun

# 빌드
./gradlew build

# 테스트
./gradlew test

# 프로덕션 빌드 (테스트 제외)
./gradlew clean build -x test

# JAR 실행
java -jar build/libs/chalkak-server-0.0.1-SNAPSHOT.jar
```

### Docker (Redis만)

```bash
docker-compose up -d    # Redis + Redis Commander
# Redis Commander: http://localhost:8081
```

### 접속 URL (기본)

- API: `http://localhost:8082/api`
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- Health Check: `http://localhost:8082/actuator/health`

---

## 배포 (CI/CD)

### GitHub Actions

`main` 또는 `master` 브랜치 push 시 자동 배포:

1. JDK 21 (Temurin) 설정
2. `./gradlew clean build -x test` (테스트 미실행)
3. SCP로 JAR → EC2 `/opt/chalkak/chalkak-server.jar.new`
4. SSH로 EC2 접속, `/opt/chalkak/deploy.sh` 실행
5. 클린업: runner에서 `private_key.pem` 삭제

### GitHub Secrets

- `EC2_SSH_KEY` - SSH private key (PEM)
- `EC2_HOST` - EC2 IP 또는 도메인
- `EC2_USER` - SSH 사용자 (보통 `ubuntu`)

**주의**: 배포 스크립트 `/opt/chalkak/deploy.sh`는 EC2 서버에만 존재 (repo 미포함)

---

## 스케줄러

| 시각 | 작업 |
|------|------|
| 매일 03:00 | 만료된 refresh token 삭제 |
| 매일 04:00 | 24시간 이상 된 used refresh token 삭제 |
| 매시간 | Bucket4j 메모리 정리 (10,000 초과 시) |

---

## 테스트

- JUnit 5 + Spring Security Test
- 테스트 DB: H2 in-memory (`application-test.yml`)
- 현재 테스트 파일: `PhotoBoothControllerTest`, `PhotoBoothServiceTest`, `PhotoBoothServiceCacheTest`, `CongestionServiceTest`
- CI에서는 `-x test`로 스킵됨

---

## 지오쿼리

`PhotoBoothRepository.findNearbyPhotoBooths`:

1. Bounding box 필터: `latitude BETWEEN minLat AND maxLat AND longitude BETWEEN minLon AND maxLon` (인덱스 활용)
2. Haversine 공식: `acos(cos(...)cos(...)cos(...) + sin(...)sin(...)) * 6371`
3. `HAVING distance <= :radius`
4. 거리순 정렬

---

## 웨이팅 MVP (예정)

### 추가 Entity

- `Store`: ownerId(User FK), photoBoothId(PhotoBooth FK), businessName, isWaitingEnabled
- `WaitingQueue`: storeId(Store FK), userId(User FK), status(WAITING/CALLED/COMPLETED/CANCELLED)

### 추가 API

```
POST   /api/waiting/{storeId}          # 웨이팅 등록
GET    /api/waiting/my                 # 내 웨이팅
DELETE /api/waiting/{id}               # 취소
GET    /api/waiting/store/{storeId}    # 사진관 웨이팅 목록 (사장님)
POST   /api/waiting/{id}/call          # 호출 (사장님)
POST   /api/waiting/{id}/complete      # 완료 (사장님)
```

### Role 확장

`USER`, `STORE_OWNER` (추가), `ADMIN`
