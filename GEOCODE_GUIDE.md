# 네컷사진관 데이터 추가 가이드

## 준비사항

### 1. 카카오 API 키 발급

1. [카카오 개발자 사이트](https://developers.kakao.com/) 접속
2. 로그인 후 "내 애플리케이션" → "애플리케이션 추가하기"
3. 앱 이름 입력 후 생성
4. 생성된 앱 선택 → "REST API 키" 복사

### 2. Python 패키지 설치

```bash
pip install requests
```

## 사용 방법

### 방법 1: 로컬에서 지오코딩 후 서버 업로드

로컬 환경에서 주소를 위도/경도로 변환한 후 서버에 업로드합니다.

```bash
# 1. 환경변수 설정
export KAKAO_API_KEY="your_kakao_rest_api_key"

# 2. 스크립트 실행
cd chalkak-server
python geocode_and_upload.py

# 3. 서버 업로드 확인
# 스크립트가 지오코딩 완료 후 업로드 여부를 물어봅니다.
# 'y'를 입력하면 http://localhost:8082로 업로드됩니다.
```

**SSH로 서버에 접속한 상태라면:**
```bash
ssh chalkak
cd /path/to/chalkak-server
export KAKAO_API_KEY="your_key"
python geocode_and_upload.py
# 서버가 localhost:8082에서 실행 중이어야 합니다
```

### 방법 2: 로컬에서 지오코딩만 하고 CSV 확인 후 수동 업로드

```bash
# 1. 지오코딩만 실행
export KAKAO_API_KEY="your_kakao_rest_api_key"
python geocode_and_upload.py
# 업로드 여부 묻는 프롬프트에서 'n' 입력

# 2. 생성된 CSV 파일 확인
cat photobooth_data_geocoded.csv

# 3. 서버에 CSV 파일 전송
scp photobooth_data_geocoded.csv user@server:/opt/chalkak/

# 4. 서버에서 수동으로 curl 실행
ssh chalkak
# 각 행마다 curl 명령 실행
```

### 방법 3: CSV를 JSON으로 변환 후 일괄 업로드 스크립트

```python
# csv_to_json_upload.py 사용
python csv_to_json_upload.py photobooth_data_geocoded.csv
```

## 출력 파일

- `photobooth_data_raw.csv` - 원본 데이터 (이름, 주소, 전화번호)
- `photobooth_data_geocoded.csv` - 지오코딩된 데이터 (위도/경도 포함)

## 데이터 형식

### 입력 (photobooth_data_raw.csv)
```csv
name,address,phone_number
돈독업 남산서울타워,서울특별시 용산구 남산공원길 105 YTN서울타워,0507-1387-8613
```

### 출력 (photobooth_data_geocoded.csv)
```csv
name,brand,address,roadAddress,latitude,longitude,phoneNumber
돈독업 남산서울타워,돈독업,서울 용산구 용산동2가 1-2,서울 용산구 남산공원길 105,37.5512,126.9882,0507-1387-8613
```

## API 호출 제한

- 카카오 로컬 API: **초당 10회**, **하루 300,000회**
- 스크립트는 자동으로 0.15초 간격으로 호출하여 제한을 준수합니다.

## 서버 API 엔드포인트

```
POST http://localhost:8082/api/photo-booths
Content-Type: application/json

{
  "name": "돈독업 남산서울타워",
  "brand": "돈독업",
  "series": null,
  "address": "서울 용산구 용산동2가 1-2",
  "roadAddress": "서울 용산구 남산공원길 105",
  "latitude": 37.5512,
  "longitude": 126.9882,
  "operatingHours": null,
  "phoneNumber": "0507-1387-8613",
  "description": null,
  "priceInfo": null
}
```

## 문제 해결

### "주소를 찾을 수 없음" 오류
- 주소가 정확하지 않을 수 있습니다
- `photobooth_data_raw.csv`에서 해당 주소를 수정 후 재실행

### "이미 존재" 경고
- 이미 DB에 동일한 데이터가 있습니다
- 정상이며, 중복 생성을 방지합니다

### 서버 연결 오류
- 서버가 실행 중인지 확인: `curl http://localhost:8082/actuator/health`
- SSH 터널링이 필요한 경우: `ssh -L 8082:localhost:8082 chalkak`

## 새로운 데이터 추가

1. `photobooth_data_raw.csv`에 새 행 추가:
   ```csv
   돈독업 신규지점,서울특별시 강남구 테헤란로 123,02-1234-5678
   ```

2. 스크립트 재실행:
   ```bash
   python geocode_and_upload.py
   ```

## 참고

- 브랜드명은 자동으로 "돈독업"으로 설정됩니다
- 다른 브랜드를 추가하려면 스크립트에서 `brand` 값을 수정하세요
- `series`, `operatingHours`, `description`, `priceInfo`는 나중에 업데이트 가능합니다
