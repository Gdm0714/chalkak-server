# Chalkak Server - 프로젝트 개요

## 프로젝트 목적
네컷사진관 찾기 서비스 '찰칵'의 백엔드 API 서버

## 주요 기능
- 네컷사진관 위치 정보 관리 (CRUD)
- 위치 기반 근처 사진관 검색 (Haversine 공식 사용)
- 브랜드별, 키워드 검색
- Redis 캐싱을 통한 성능 최적화

## 기술 스택
- **언어**: Java 21
- **프레임워크**: Spring Boot 3.5.0
- **데이터베이스**: MySQL 8.0+
- **캐시**: Redis 7.2
- **빌드 도구**: Gradle 8.x
- **테스트**: JUnit 5, MockMvc
- **문서화**: SpringDoc OpenAPI 3 (Swagger)

## 주요 라이브러리
- Spring Data JPA (Hibernate)
- Spring Security (CORS 설정용)
- Spring Cache + Redis
- Lombok
- MySQL Connector
- Jackson (JSON 처리)

## 아키텍처
```
Controller (REST API)
    ↓
Service (비즈니스 로직 + 캐싱)
    ↓
Repository (JPA + Native Query)
    ↓
Entity (PhotoBooth)
```

## 프로젝트 구조
```
src/
├── main/
│   ├── java/com/min/chalkakserver/
│   │   ├── config/          # 설정 (Security, Cache)
│   │   │   └── cache/       # Redis 캐시 설정
│   │   ├── controller/      # REST API 엔드포인트
│   │   ├── service/         # 비즈니스 로직
│   │   ├── repository/      # 데이터 액세스
│   │   ├── entity/          # JPA 엔티티
│   │   ├── dto/             # 요청/응답 DTO
│   │   ├── exception/       # 예외 처리
│   │   └── util/            # 유틸리티
│   └── resources/
│       ├── application.yml  # 설정 파일
│       └── db/migration/    # DB 마이그레이션
└── test/                    # 테스트 코드
```

## 핵심 엔티티
### PhotoBooth
- 네컷사진관 정보 저장
- 필드: id, name, brand, address, roadAddress, latitude, longitude, operatingHours, phoneNumber, description, priceInfo
- 인덱스: location(lat, lon), brand, name
