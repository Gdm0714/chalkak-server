# 찰칵 서버 - 네컷사진관 위치 정보 API

네컷사진관의 위치와 정보를 제공하는 Spring Boot 백엔드 서버입니다.

## 기술 스택

- **Java 21**
- **Spring Boot 3.5.0**
- **Spring Data JPA**
- **Spring Security**
- **MySQL 8.0**
- **Gradle**

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/min/chalkakserver/
│   │   ├── ChalkakServerApplication.java
│   │   ├── config/
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   ├── PhotoBoothController.java
│   │   │   └── HealthCheckController.java
│   │   ├── dto/
│   │   │   ├── PhotoBoothRequestDto.java
│   │   │   └── PhotoBoothResponseDto.java
│   │   ├── entity/
│   │   │   └── PhotoBooth.java
│   │   ├── repository/
│   │   │   └── PhotoBoothRepository.java
│   │   └── service/
│   │       └── PhotoBoothService.java
│   └── resources/
│       ├── application.yml
│       └── data.sql
└── test/
```

## 데이터베이스 설정

### 1. MySQL 설치 및 설정

```bash
# MySQL 설치 (macOS)
brew install mysql

# MySQL 서비스 시작
brew services start mysql

# MySQL 접속
mysql -u root -p
```

### 2. 데이터베이스 생성

```sql
CREATE DATABASE IF NOT EXISTS chalkak_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE chalkak_db;
```

### 3. application.yml 설정 확인

MySQL 접속 정보를 환경에 맞게 수정해주세요:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chalkak_db?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
    username: root
    password: password  # 본인의 MySQL 비밀번호로 변경
```

## 실행 방법

### 1. 프로젝트 클론 및 빌드

```bash
cd chalkak-server
./gradlew build
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는

```bash
java -jar build/libs/chalkak-server-0.0.1-SNAPSHOT.jar
```

### 3. 서버 확인

서버가 정상적으로 실행되면 http://localhost:8080 에서 접근 가능합니다.

## API 엔드포인트

### 기본 API

- `GET /api/health` - 서버 상태 확인

### 네컷사진관 API

- `GET /api/photo-booths` - 모든 네컷사진관 조회
- `GET /api/photo-booths/{id}` - 특정 네컷사진관 조회
- `POST /api/photo-booths` - 네컷사진관 생성
- `PUT /api/photo-booths/{id}` - 네컷사진관 수정
- `DELETE /api/photo-booths/{id}` - 네컷사진관 삭제

### 검색 API

- `GET /api/photo-booths/search?keyword={검색어}` - 키워드로 검색
- `GET /api/photo-booths/brand/{브랜드명}` - 브랜드로 검색
- `GET /api/photo-booths/nearby?latitude={위도}&longitude={경도}&radius={반경}` - 근처 네컷사진관 검색

## API 사용 예시

### 1. 모든 네컷사진관 조회

```bash
curl -X GET http://localhost:8080/api/photo-booths
```

### 2. 네컷사진관 생성

```bash
curl -X POST http://localhost:8080/api/photo-booths \
  -H "Content-Type: application/json" \
  -d '{
    "name": "테스트 네컷사진관",
    "brand": "테스트",
    "address": "서울특별시 강남구 테스트로 123",
    "latitude": 37.5665,
    "longitude": 126.9780,
    "operatingHours": "10:00-22:00",
    "phoneNumber": "02-123-4567",
    "description": "테스트용 네컷사진관입니다.",
    "priceInfo": "기본: 6,000원"
  }'
```

### 3. 키워드 검색

```bash
curl -X GET "http://localhost:8080/api/photo-booths/search?keyword=홍대"
```

### 4. 근처 네컷사진관 검색

```bash
curl -X GET "http://localhost:8080/api/photo-booths/nearby?latitude=37.5565&longitude=126.9239&radius=2"
```

## 목데이터

서버 실행 시 자동으로 15개의 서울 지역 네컷사진관 데이터가 삽입됩니다:

- 포토이즘 홍대점
- 인생네컷 강남점
- 포토그레이 명동점
- 하루필름 이태원점
- 셀프스튜디오 신촌점
- 모노마마 압구정점
- 추억제조기 건대점
- 포토매틱 종로점
- 블링샷 용산점
- 스냅타임 성수점
- 포토부스 동대문점
- 셀카존 잠실점
- 레트로샷 을지로점
- 픽스타 노원점
- 인스타샷 마포점

## 주요 기능

1. **CRUD 작업**: 네컷사진관 생성, 조회, 수정, 삭제
2. **검색 기능**: 이름, 주소, 브랜드별 검색
3. **위치 기반 검색**: GPS 좌표를 이용한 근처 네컷사진관 찾기
4. **데이터 검증**: 입력 데이터 유효성 검사
5. **보안 설정**: Spring Security를 통한 API 보안

## 개발 환경

- IDE: IntelliJ IDEA 또는 VS Code
- JDK: OpenJDK 21
- Database: MySQL 8.0
- Build Tool: Gradle 8.x
