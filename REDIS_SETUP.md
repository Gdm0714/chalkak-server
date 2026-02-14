# Redis 설정 가이드

## 1. Redis 없이 개발하기 (기본 설정)

기본적으로 서버는 Redis 없이도 실행됩니다. Simple Cache(메모리 캐시)를 사용합니다.

```bash
./gradlew bootRun
```

## 2. Redis와 함께 실행하기

### 옵션 1: Docker 사용 (권장)

```bash
# Redis 컨테이너 실행
docker run -d --name chalkak-redis -p 6379:6379 redis:7.2-alpine

# 또는 Docker Compose 사용
docker-compose up -d
```

### 옵션 2: Homebrew로 설치 (macOS)

```bash
# Redis 설치
brew install redis

# Redis 서비스 시작
brew services start redis

# 또는 수동으로 실행
redis-server
```

### 옵션 3: 직접 다운로드 및 컴파일

```bash
# Redis 다운로드 및 컴파일
wget https://download.redis.io/redis-stable.tar.gz
tar xzf redis-stable.tar.gz
cd redis-stable
make

# Redis 실행
src/redis-server
```

## 3. Redis 프로필로 서버 실행

Redis가 실행 중일 때, Redis 캐싱을 활성화하려면:

```bash
# Redis 프로필로 실행
./gradlew bootRun --args='--spring.profiles.active=redis'

# 또는 환경 변수로 설정
export SPRING_PROFILES_ACTIVE=redis
./gradlew bootRun

# 캐시 워밍업도 함께 활성화
./gradlew bootRun --args='--spring.profiles.active=redis --cache.warmup.enabled=true'
```

## 4. Redis 연결 확인

```bash
# Redis CLI로 연결
redis-cli

# 연결 테스트
127.0.0.1:6379> ping
PONG

# 캐시 데이터 확인
127.0.0.1:6379> keys *
```

## 5. Redis Commander (웹 UI)

```bash
# npm으로 설치
npm install -g redis-commander

# 실행
redis-commander

# 브라우저에서 접속
http://localhost:8081
```

## 6. 프로필별 설정

### 개발 환경 (Redis 없이)
- 프로필: default
- 캐시: Simple Cache (메모리)
- 장점: 설정 간단, 외부 의존성 없음

### 운영 환경 (Redis 사용)
- 프로필: redis
- 캐시: Redis
- 장점: 분산 캐시, 영속성, 고성능

## 7. 문제 해결

### Redis 연결 오류
```
Unable to connect to Redis
```
해결: Redis 서버가 실행 중인지 확인하고, application-redis.yml의 호스트와 포트 설정을 확인하세요.

### 메모리 부족
```
OOM command not allowed when used memory > 'maxmemory'
```
해결: Redis 설정에서 maxmemory를 늘리거나 불필요한 캐시를 삭제하세요.

```bash
redis-cli
127.0.0.1:6379> CONFIG SET maxmemory 512mb
127.0.0.1:6379> FLUSHDB
```
