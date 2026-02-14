# Chalkak Server - 개발 명령어

## 빌드 & 실행

### Gradle 빌드
```bash
./gradlew build
```

### 테스트 제외 빌드
```bash
./gradlew build -x test
```

### 애플리케이션 실행
```bash
./gradlew bootRun
```

또는 JAR 파일 실행:
```bash
java -jar build/libs/chalkak-server-0.0.1-SNAPSHOT.jar
```

## 테스트

### 전체 테스트 실행
```bash
./gradlew test
```

### 특정 테스트 클래스 실행
```bash
./gradlew test --tests "PhotoBoothControllerTest"
./gradlew test --tests "PhotoBoothServiceCacheTest"
```

### 테스트 결과 확인
```bash
open build/reports/tests/test/index.html
```

## Docker (Redis)

### Redis 시작
```bash
docker-compose up -d
```

### Redis 중지
```bash
docker-compose down
```

### Redis CLI 접속
```bash
docker exec -it chalkak-redis redis-cli
```

### Redis Commander (GUI) 접속
```
http://localhost:8081
```

## 데이터베이스

### MySQL 접속
```bash
mysql -u root -p -h localhost -P 3306
use chalkak_db;
```

## API 문서

### Swagger UI 접속
```
http://localhost:8082/swagger-ui.html
```

서버 실행 후 접속 가능

## 기타 유용한 명령어

### 의존성 확인
```bash
./gradlew dependencies
```

### 프로젝트 정보
```bash
./gradlew projects
./gradlew tasks
```

### 캐시 클리어
```bash
./gradlew clean
```

## 포트 정보
- 애플리케이션: 8082
- Redis: 6379
- Redis Commander: 8081
- MySQL: 3306
