# 작업 완료 시 체크리스트

## 코드 작성 후 필수 확인 사항

### 1. 빌드 확인
```bash
./gradlew build
```
- 컴파일 에러 없는지 확인
- 경고(warning) 최소화

### 2. 테스트 실행
```bash
./gradlew test
```
- 모든 테스트 통과 확인
- 새로운 기능 추가 시 테스트 작성 필수

### 3. 코드 스타일 확인
- Lombok 어노테이션 적절히 사용했는지
- 네이밍 컨벤션 준수
- 불필요한 import 제거
- 로깅 추가 (주요 작업 시)

### 4. API 변경 시
- Swagger 문서 자동 업데이트 확인
- DTO validation 적용 (`@Valid`, `@NotNull` 등)
- 에러 처리 추가

### 5. 데이터베이스 변경 시
- 마이그레이션 파일 작성 (필요 시)
- 인덱스 고려
- Entity 변경 시 DTO도 함께 수정

### 6. 캐시 관련 작업 시
- 캐시 키 전략 확인
- 캐시 무효화 로직 추가
- 캐시 성능 테스트

### 7. 보안 체크
- application.yml에 민감 정보 하드코딩 금지
- CORS 설정 확인
- SQL Injection 방지 (Prepared Statement 사용)

### 8. 로컬 테스트
- Redis 실행 중인지 확인
- MySQL 연결 확인
- 실제 API 호출 테스트
- Swagger UI에서 API 동작 확인

### 9. 코드 리뷰 전
- 주석 추가 (복잡한 로직)
- TODO 주석 제거 또는 이슈 생성
- 디버깅용 코드 제거 (System.out.println 등)

### 10. Git 커밋 전
- 변경 사항 재확인
- 커밋 메시지 명확히 작성
- .gitignore에 의해 제외되어야 할 파일 확인

## 배포 전 추가 체크
- application.yml 환경별 설정 확인
- 로그 레벨 적절히 설정
- 성능 테스트 (부하 테스트)
- Docker 이미지 빌드 확인 (해당 시)
