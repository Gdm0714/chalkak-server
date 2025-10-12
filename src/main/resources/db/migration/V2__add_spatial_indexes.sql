-- Spatial Index 추가 (MySQL 5.7+)
-- 복합 인덱스로 위도/경도 검색 성능 최적화
ALTER TABLE photo_booths ADD SPATIAL INDEX idx_spatial_location (POINT(longitude, latitude));

-- 또는 일반 인덱스 사용 (모든 MySQL 버전)
-- CREATE INDEX idx_lat_lon ON photo_booths (latitude, longitude);

-- Bounding Box 검색을 위한 개별 인덱스
CREATE INDEX idx_latitude ON photo_booths (latitude);
CREATE INDEX idx_longitude ON photo_booths (longitude);

-- 추가 인덱스
CREATE INDEX idx_brand ON photo_booths (brand);
CREATE INDEX idx_name ON photo_booths (name);
CREATE INDEX idx_created_at ON photo_booths (created_at);
