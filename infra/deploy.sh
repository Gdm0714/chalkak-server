#!/usr/bin/env bash
# ============================================================================
# 무중단 배포 스크립트 (서버 측)
# GitHub Actions가 SCP로 JAR 보낸 후 SSH로 이걸 실행
#
# 위치: /opt/chalkak/repo/infra/deploy.sh
# 호출: ssh ubuntu@host "bash /opt/chalkak/repo/infra/deploy.sh"
# ============================================================================

set -euo pipefail

cd /opt/chalkak/repo

echo "──────────────────────────────────────────"
echo "  배포 시작: $(date '+%Y-%m-%d %H:%M:%S')"
echo "──────────────────────────────────────────"

# ─── 1. 최신 코드 pull ─────────────────────────────────────────
echo "[1/4] git pull..."
git fetch --all --prune
git reset --hard origin/main

# ─── 2. 새 JAR이 있으면 사용 (CI/CD 경로) ─────────────────────
# GitHub Actions가 SCP로 보낸 jar.new가 있으면 적용
if [[ -f /opt/chalkak/chalkak-server.jar.new ]]; then
    echo "[2/4] CI/CD JAR 감지 → 도커 이미지 재빌드..."
    cp /opt/chalkak/chalkak-server.jar.new build/libs/app.jar 2>/dev/null || true
    rm -f /opt/chalkak/chalkak-server.jar.new
fi

# ─── 3. 이미지 빌드 ────────────────────────────────────────────
echo "[3/4] Docker 이미지 빌드..."
docker compose -f infra/docker-compose.prod.yml build app

# ─── 4. 무중단 재시작 ──────────────────────────────────────────
echo "[4/4] 컨테이너 재시작 (rolling)..."

# 기존 컨테이너를 새 이미지로 재생성. depends_on 덕분에 mysql/redis는 그대로.
docker compose -f infra/docker-compose.prod.yml up -d --no-deps --force-recreate app
docker compose -f infra/docker-compose.prod.yml up -d --no-deps caddy

# 헬스 체크 대기 (최대 90초)
echo "헬스 체크 대기..."
for i in {1..18}; do
    if docker exec chalkak-app curl -fsS http://localhost:8082/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
        echo "  ✓ 헬스 체크 성공 (${i}회차)"
        break
    fi
    if [[ $i -eq 18 ]]; then
        echo "  ❌ 헬스 체크 실패 — 로그 확인:"
        docker compose -f infra/docker-compose.prod.yml logs --tail=50 app
        exit 1
    fi
    sleep 5
done

# 사용 안 하는 이미지/볼륨 정리
docker image prune -f >/dev/null 2>&1 || true

echo ""
echo "──────────────────────────────────────────"
echo "  ✅ 배포 완료: $(date '+%Y-%m-%d %H:%M:%S')"
echo "──────────────────────────────────────────"
docker compose -f infra/docker-compose.prod.yml ps
