#!/usr/bin/env python3
"""
네컷사진관 데이터를 지오코딩하고 서버에 업로드하는 스크립트

사용법:
1. 카카오 API 키 발급: https://developers.kakao.com/
2. 환경변수 설정: export KAKAO_API_KEY="your_api_key"
3. 실행: python geocode_and_upload.py
"""

import csv
import json
import os
import time
import requests
from typing import Optional, Dict


def get_coordinates_kakao(address: str, api_key: str) -> Optional[Dict]:
    """카카오 로컬 API를 사용하여 주소를 위도/경도로 변환"""
    url = "https://dapi.kakao.com/v2/local/search/address.json"
    headers = {"Authorization": f"KakaoAK {api_key}"}
    params = {"query": address}

    try:
        response = requests.get(url, headers=headers, params=params, timeout=10)
        response.raise_for_status()
        data = response.json()

        if data['documents']:
            result = data['documents'][0]
            return {
                'latitude': float(result['y']),
                'longitude': float(result['x']),
                'road_address': result.get('road_address', {}).get('address_name') if result.get('road_address') else None,
                'address': result['address']['address_name']
            }
        else:
            print(f"⚠️  주소를 찾을 수 없음: {address}")
            return None
    except Exception as e:
        print(f"❌ API 오류 ({address}): {e}")
        return None


def upload_to_server(photobooth_data: Dict, server_url: str) -> bool:
    """네컷사진관 데이터를 서버에 업로드"""
    try:
        response = requests.post(
            f"{server_url}/api/photo-booths",
            json=photobooth_data,
            headers={"Content-Type": "application/json"},
            timeout=10
        )
        response.raise_for_status()
        print(f"✅ 업로드 성공: {photobooth_data['name']}")
        return True
    except requests.exceptions.HTTPError as e:
        if e.response.status_code == 409:
            print(f"⚠️  이미 존재: {photobooth_data['name']}")
        else:
            print(f"❌ 업로드 실패 ({photobooth_data['name']}): {e}")
        return False
    except Exception as e:
        print(f"❌ 업로드 오류 ({photobooth_data['name']}): {e}")
        return False


def main():
    # 환경변수에서 API 키 가져오기
    kakao_api_key = os.getenv("KAKAO_API_KEY")
    if not kakao_api_key:
        print("❌ KAKAO_API_KEY 환경변수를 설정해주세요.")
        print("   export KAKAO_API_KEY='your_api_key'")
        return

    # 서버 URL (ssh chalkak으로 접속한 경우 localhost)
    server_url = os.getenv("SERVER_URL", "http://localhost:8082")

    # CSV 파일 읽기
    script_dir = os.path.dirname(os.path.abspath(__file__))
    input_file = os.path.join(script_dir, "photobooth_data_raw.csv")
    output_file = os.path.join(script_dir, "photobooth_data_geocoded.csv")

    if not os.path.exists(input_file):
        print(f"❌ {input_file} 파일을 찾을 수 없습니다.")
        return

    results = []
    success_count = 0
    fail_count = 0

    print(f"\n🚀 지오코딩 시작...\n")

    with open(input_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for idx, row in enumerate(reader, 1):
            name = row['name']
            address = row['address']
            phone_number = row['phone_number']

            print(f"[{idx}/36] {name}...")

            # 지오코딩
            coords = get_coordinates_kakao(address, kakao_api_key)

            if coords:
                photobooth_data = {
                    'name': name,
                    'brand': '돈독업',
                    'series': None,
                    'address': coords['address'],
                    'roadAddress': coords['road_address'],
                    'latitude': coords['latitude'],
                    'longitude': coords['longitude'],
                    'operatingHours': None,
                    'phoneNumber': phone_number,
                    'description': None,
                    'priceInfo': None
                }

                results.append(photobooth_data)
                success_count += 1
            else:
                fail_count += 1

            # API 호출 제한 방지 (초당 10회)
            time.sleep(0.15)

    # 결과를 CSV 파일로 저장
    if results:
        print(f"\n💾 결과 저장 중: {output_file}")
        with open(output_file, 'w', encoding='utf-8', newline='') as f:
            fieldnames = ['name', 'brand', 'address', 'roadAddress', 'latitude',
                         'longitude', 'phoneNumber']
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            for data in results:
                writer.writerow({
                    'name': data['name'],
                    'brand': data['brand'],
                    'address': data['address'],
                    'roadAddress': data['roadAddress'],
                    'latitude': data['latitude'],
                    'longitude': data['longitude'],
                    'phoneNumber': data['phoneNumber']
                })

        print(f"✅ CSV 저장 완료: {output_file}")

    # 서버 업로드 여부 확인
    print(f"\n📊 지오코딩 결과: 성공 {success_count}개, 실패 {fail_count}개")

    if results and input("\n🌐 서버에 업로드하시겠습니까? (y/n): ").lower() == 'y':
        print(f"\n📤 서버 업로드 시작 ({server_url})...\n")
        upload_success = 0
        upload_fail = 0

        for data in results:
            if upload_to_server(data, server_url):
                upload_success += 1
            else:
                upload_fail += 1
            time.sleep(0.1)

        print(f"\n📊 업로드 결과: 성공 {upload_success}개, 실패 {upload_fail}개")

    print("\n✨ 완료!")


if __name__ == "__main__":
    main()
