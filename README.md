# 찰칵 서버 (Chalkak Server)

네컷사진관 위치 정보를 제공하는 백엔드 API 서버

## 기술 스택

- **Java 21**
- **Spring Boot 3.5.0**
- **MySQL 8.0+**
- **Redis 7.2** (캐싱)
- **Gradle 8.x**

## 주요 기능

- 📍 네컷사진관 위치 정보 관리 (CRUD)
- 🔍 위치 기반 근처 사진관 검색 (Haversine 공식)
- 🏷️ 브랜드별, 키워드 검색
- ⚡ Redis 캐싱을 통한 성능 최적화
- 📝 사용자 제보 기능 (이메일 전송)
- 📊 Actuator를 통한 헬스체크
- 📖 Swagger UI API 문서

## 환경 설정

### 1. 필수 요구사항

- Java 21 이상
- MySQL 8.0 이상
- Redis 7.2 (선택사항, 없으면 메모리 캐시 사용)

### 2. 환경변수 설정

프로젝트 루트에 `.env` 파일을 생성하거나 시스템 환경변수로 설정하세요.

`.env.example` 파일을 참고하여 설정:

```bash
cp .env.example .env
# .env 파일을 열어 실제 값으로 수정
```

**필수 환경변수:**
- `DB_URL` - MySQL 데이터베이스 URL
- `DB_USERNAME` - 데이터베이스 사용자명
- `DB_PASSWORD` - 데이터베이스 비밀번호
- `MAIL_USERNAME` - Gmail SMTP 계정
- `MAIL_PASSWORD` - Gmail 앱 비밀번호
- `ADMIN_EMAIL` - 제보 수신 이메일

**선택 환경변수:**
- `REDIS_HOST` - Redis 서버 주소 (기본값: localhost)
- `REDIS_PORT` - Redis 포트 (기본값: 6379)
- `SERVER_PORT` - 서버 포트 (기본값: 8082)
- `CORS_ALLOWED_ORIGINS` - CORS 허용 Origin

### 3. 데이터베이스 설정

MySQL 데이터베이스를 생성하세요:

```sql
CREATE DATABASE chalkak_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

애플리케이션 실행 시 테이블은 자동으로 생성됩니다 (JPA `ddl-auto: update`).

### 4. Redis 설정 (선택사항)

Redis를 사용하려면 Redis 서버를 실행하세요:

**Docker 사용:**
```bash
docker-compose up -d
```

**Homebrew 사용 (macOS):**
```bash
brew install redis
brew services start redis
```

자세한 내용은 [REDIS_SETUP.md](REDIS_SETUP.md)를 참고하세요.

## 실행 방법

### 개발 환경에서 실행

```bash
# 빌드 및 실행
./gradlew bootRun

# 또는 JAR 파일 생성 후 실행
./gradlew build
java -jar build/libs/chalkak-server-0.0.1-SNAPSHOT.jar
```

서버가 시작되면 다음 주소에서 접근 가능합니다:
- API: `http://localhost:8082/api`
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- Health Check: `http://localhost:8082/actuator/health`

## API 엔드포인트

### 네컷사진관 조회
- `GET /api/photo-booths` - 전체 네컷사진관 조회
- `GET /api/photo-booths/{id}` - ID로 조회
- `GET /api/photo-booths/nearby?latitude={lat}&longitude={lon}&radius={km}` - 근처 네컷사진관 검색
- `GET /api/photo-booths/brand/{brand}` - 브랜드별 조회
- `GET /api/photo-booths/search?keyword={keyword}` - 키워드 검색

### 네컷사진관 관리 (Admin)
- `POST /api/photo-booths` - 네컷사진관 등록
- `PUT /api/photo-booths/{id}` - 네컷사진관 수정
- `DELETE /api/photo-booths/{id}` - 네컷사진관 삭제

### 사용자 제보
- `POST /api/photo-booths/report` - 네컷사진관 제보

상세한 API 문서는 Swagger UI에서 확인하세요.

## 배포

### GitHub Actions를 통한 자동 배포

`main` 또는 `master` 브랜치에 push하면 자동으로 EC2에 배포됩니다.

**필요한 GitHub Secrets:**
- `EC2_SSH_KEY` - EC2 SSH private key
- `EC2_HOST` - EC2 인스턴스 IP 또는 도메인
- `EC2_USER` - EC2 사용자명 (예: ubuntu)

### 수동 배포

```bash
# 빌드 (테스트 제외)
./gradlew clean build -x test

# JAR 파일을 서버로 전송
scp build/libs/chalkak-server-0.0.1-SNAPSHOT.jar user@server:/opt/chalkak/

# 서버에서 실행
java -jar /opt/chalkak/chalkak-server-0.0.1-SNAPSHOT.jar
```

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/min/chalkakserver/
│   │   ├── config/          # 설정 (Security, Cache)
│   │   ├── controller/      # REST API 컨트롤러
│   │   ├── service/         # 비즈니스 로직
│   │   ├── repository/      # 데이터 액세스
│   │   ├── entity/          # JPA 엔티티
│   │   ├── dto/             # 요청/응답 DTO
│   │   ├── exception/       # 예외 처리
│   │   └── util/            # 유틸리티
│   └── resources/
│       └── application.yml  # 설정 파일
└── test/                    # 테스트 코드
```

## 문제 해결

### 데이터베이스 연결 오류
```
Could not create connection to database server
```
- MySQL 서버가 실행 중인지 확인
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경변수 확인
- 데이터베이스가 생성되어 있는지 확인

### Redis 연결 오류
```
Unable to connect to Redis
```
- Redis를 사용하지 않으려면 `application.yml`의 `spring.cache.type`을 `simple`로 변경
- Redis를 사용하려면 Redis 서버가 실행 중인지 확인

### 포트 충돌
```
Port 8082 was already in use
```
- `SERVER_PORT` 환경변수로 다른 포트 지정
- 또는 실행 시: `java -jar app.jar --server.port=8083`

## 라이선스

MIT License

## 문의

이슈나 개선사항이 있으면 GitHub Issues에 등록해주세요.
