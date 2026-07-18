#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

./mvnw -pl fantasy-sim-api -am -DskipTests spring-boot:run
