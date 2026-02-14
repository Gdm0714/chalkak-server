#!/usr/bin/env python3
"""
홍대네컷 데이터 지오코딩 및 DB 추가
"""

import csv
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


def main():
    api_key = "a778877dc4d2689d7bb51d860b584c38"

    script_dir = os.path.dirname(os.path.abspath(__file__))
    input_file = os.path.join(script_dir, "hongdae4cut_data_raw.csv")
    output_file = os.path.join(script_dir, "hongdae4cut_geocoded.csv")

    if not os.path.exists(input_file):
        print(f"❌ {input_file} 파일을 찾을 수 없습니다.")
        return

    results = []
    success_count = 0
    fail_count = 0

    print(f"\n🚀 홍대네컷 지오코딩 시작...\n")

    with open(input_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for idx, row in enumerate(reader, 1):
            name = row['name']
            address = row['address']
            phone_number = row['phone_number']

            print(f"[{idx}/11] {name}...")

            # 지오코딩
            coords = get_coordinates_kakao(address, api_key)

            if coords:
                photobooth_data = {
                    'name': name,
                    'brand': '홍대네컷',
                    'series': None,
                    'address': coords['address'],
                    'roadAddress': coords['road_address'],
                    'latitude': coords['latitude'],
                    'longitude': coords['longitude'],
                    'operatingHours': None,
                    'phoneNumber': phone_number if phone_number else None,
                    'description': None,
                    'priceInfo': None
                }

                results.append(photobooth_data)
                success_count += 1
                print(f"    ✅ {coords['latitude']:.6f}, {coords['longitude']:.6f}")
            else:
                fail_count += 1

            # API 호출 제한 방지
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

    print(f"\n📊 지오코딩 결과: 성공 {success_count}개, 실패 {fail_count}개")
    print("\n✨ 완료!")

    return results


if __name__ == "__main__":
    main()
