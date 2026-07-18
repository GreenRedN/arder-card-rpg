#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

./mvnw -pl fantasy-sim-cli -am -DskipTests package
java -jar fantasy-sim-cli/target/fantasy-sim-cli-2.1.0-all.jar
