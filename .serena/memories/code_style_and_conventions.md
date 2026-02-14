# Chalkak Server - 코드 스타일 및 컨벤션

## 코딩 컨벤션

### 네이밍
- **클래스**: PascalCase (예: PhotoBoothService, PhotoBoothController)
- **메서드/변수**: camelCase (예: getAllPhotoBooths, photoBoothService)
- **상수**: UPPER_SNAKE_CASE
- **패키지**: 소문자 (예: com.min.chalkakserver)

### 레이어별 네이밍 패턴
- **Controller**: `*Controller` (예: PhotoBoothController)
- **Service**: `*Service` (예: PhotoBoothService)
- **Repository**: `*Repository` (예: PhotoBoothRepository)
- **Entity**: 도메인명 그대로 (예: PhotoBooth)
- **DTO**: `*RequestDto`, `*ResponseDto`
- **Exception**: `*Exception` (예: PhotoBoothNotFoundException)

## Lombok 사용
프로젝트에서 Lombok을 적극 활용:
- `@Getter`: getter 메서드 자동 생성
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`: 기본 생성자
- `@Builder`: 빌더 패턴
- `@RequiredArgsConstructor`: final 필드 생성자
- `@Slf4j`: 로거

## 어노테이션 스타일
- **Service**: `@Service`, `@Transactional`, `@Slf4j`, `@RequiredArgsConstructor`
- **Repository**: `@Repository` (JpaRepository 상속)
- **Controller**: `@RestController`, `@RequestMapping("/api/...")`
- **Entity**: `@Entity`, `@Table`, `@Getter`, `@NoArgsConstructor`

## JPA 컨벤션
- Entity는 불변성을 위해 protected 기본 생성자 사용
- `@PrePersist`, `@PreUpdate`로 생성/수정 시간 자동 관리
- 인덱스는 `@Table(indexes = ...)` 또는 마이그레이션 파일에 정의
- Native Query 사용 시 명확한 주석 추가

## 트랜잭션 관리
- Service 레이어에서 `@Transactional` 사용
- 읽기 전용: `@Transactional(readOnly = true)`
- 쓰기 작업: `@Transactional` (기본)

## 캐시 어노테이션
- `@Cacheable`: 조회 시 캐시 활용
- `@CacheEvict`: 수정/삭제 시 캐시 무효화
- `@Caching`: 여러 캐시 작업 조합
- `unless` 조건으로 null/빈 결과는 캐싱 제외

## 예외 처리
- 커스텀 예외: RuntimeException 상속
- `@RestControllerAdvice`와 `@ExceptionHandler`로 글로벌 처리
- ErrorResponse DTO로 일관된 에러 응답

## 로깅
- `@Slf4j` 사용
- 주요 작업 시작/완료 시 INFO 레벨 로그
- 에러 발생 시 ERROR 레벨 로그
- 메서드 파라미터 로깅으로 디버깅 용이성 확보

## REST API 규칙
- GET: 조회
- POST: 생성
- PUT: 전체 수정
- DELETE: 삭제
- URL: `/api/{리소스}`
- 응답: ResponseEntity 사용 권장

## 테스트 컨벤션
- 테스트 클래스: `*Test` (예: PhotoBoothControllerTest)
- 테스트 메서드: `메서드명_상황_기대결과` (예: `getPhotoBoothById_NotFound_ShouldReturn404`)
- `@WebMvcTest`: Controller 단위 테스트
- `@MockBean`: 의존성 모킹
- MockMvc로 HTTP 요청 시뮬레이션

## 코드 스타일
- 들여쓰기: 스페이스 4칸
- 줄 길이: 권장 120자
- 메서드 간 빈 줄 1줄
- 임포트 정리: 사용하지 않는 import 제거
