#!/usr/bin/env python3
"""
Nominatim (OpenStreetMap) API를 사용한 지오코딩
API 키 불필요, 무료 사용 가능
"""

import csv
import json
import os
import time
import requests
from typing import Optional, Dict


def get_coordinates_nominatim(address: str) -> Optional[Dict]:
    """Nominatim API를 사용하여 주소를 위도/경도로 변환"""
    url = "https://nominatim.openstreetmap.org/search"
    params = {
        "q": address,
        "format": "json",
        "limit": 1,
        "countrycodes": "kr"  # 한국으로 제한
    }
    headers = {
        "User-Agent": "ChalkakServer/1.0"  # Nominatim은 User-Agent 필수
    }

    try:
        response = requests.get(url, params=params, headers=headers, timeout=10)
        response.raise_for_status()
        data = response.json()

        if data and len(data) > 0:
            result = data[0]
            return {
                'latitude': float(result['lat']),
                'longitude': float(result['lon']),
                'address': result.get('display_name', address)
            }
        else:
            print(f"⚠️  주소를 찾을 수 없음: {address}")
            return None
    except Exception as e:
        print(f"❌ API 오류 ({address}): {e}")
        return None


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    input_file = os.path.join(script_dir, "photobooth_data_raw.csv")
    output_file = os.path.join(script_dir, "photobooth_data_geocoded.csv")

    if not os.path.exists(input_file):
        print(f"❌ {input_file} 파일을 찾을 수 없습니다.")
        return

    results = []
    success_count = 0
    fail_count = 0

    print(f"\n🚀 Nominatim API로 지오코딩 시작...\n")
    print("⚠️  Nominatim API는 초당 1회 제한이 있어 시간이 걸립니다 (약 1분)\n")

    with open(input_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for idx, row in enumerate(reader, 1):
            name = row['name']
            address = row['address']
            phone_number = row['phone_number']

            print(f"[{idx}/36] {name}...")

            # 지오코딩
            coords = get_coordinates_nominatim(address)

            if coords:
                photobooth_data = {
                    'name': name,
                    'brand': '돈독업',
                    'series': None,
                    'address': address,  # 원본 주소 유지
                    'roadAddress': None,
                    'latitude': coords['latitude'],
                    'longitude': coords['longitude'],
                    'operatingHours': None,
                    'phoneNumber': phone_number,
                    'description': None,
                    'priceInfo': None
                }

                results.append(photobooth_data)
                success_count += 1
                print(f"    ✅ {coords['latitude']:.6f}, {coords['longitude']:.6f}")
            else:
                fail_count += 1

            # API 호출 제한 준수 (초당 1회)
            time.sleep(1.1)

    # 결과를 CSV 파일로 저장
    if results:
        print(f"\n💾 결과 저장 중: {output_file}")
        with open(output_file, 'w', encoding='utf-8', newline='') as f:
            fieldnames = ['name', 'brand', 'address', 'latitude', 'longitude', 'phoneNumber']
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            for data in results:
                writer.writerow({
                    'name': data['name'],
                    'brand': data['brand'],
                    'address': data['address'],
                    'latitude': data['latitude'],
                    'longitude': data['longitude'],
                    'phoneNumber': data['phoneNumber']
                })

        print(f"✅ CSV 저장 완료: {output_file}")

    print(f"\n📊 지오코딩 결과: 성공 {success_count}개, 실패 {fail_count}개")
    print("\n✨ 완료!")

    if results:
        print(f"\n생성된 파일: {output_file}")
        print("이 파일을 확인한 후 서버에 업로드하시면 됩니다.")


if __name__ == "__main__":
    main()
