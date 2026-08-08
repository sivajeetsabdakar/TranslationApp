#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"

if [ ! -f .env.production ]; then
  echo "Missing .env.production. Copy .env.production.example and fill it." >&2
  exit 1
fi

if [ ! -f secrets/google-sa.json ]; then
  echo "Missing secrets/google-sa.json service account file." >&2
  exit 1
fi

docker network inspect whatsnew_default >/dev/null
docker compose -f docker-compose.oci.yml up -d --build
docker compose -f docker-compose.oci.yml ps
