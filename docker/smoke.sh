#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/docker"

docker compose build
docker compose up -d node-a node-b node-c
docker compose up --abort-on-container-exit --exit-code-from spike spike
status=$?
docker compose down
exit "$status"
