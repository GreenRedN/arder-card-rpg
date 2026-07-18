#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Keep the downloaded Maven distribution outside the repository. This makes
# the source package independent from a dot-prefixed .mvn upload.
WRAPPER_DIR="${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists/fantasy-sim"
MVN_VERSION="3.9.6"
MVN_DIST="apache-maven-${MVN_VERSION}"
MVN_HOME="$WRAPPER_DIR/${MVN_DIST}"
MVN_BIN="$MVN_HOME/bin/mvn"

if [ ! -x "$MVN_BIN" ]; then
  mkdir -p "$WRAPPER_DIR"
  URL="https://archive.apache.org/dist/maven/maven-3/${MVN_VERSION}/binaries/${MVN_DIST}-bin.tar.gz"
  echo "[mvnw] Downloading Maven ${MVN_VERSION} ..." >&2
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$URL" | tar -xz -C "$WRAPPER_DIR"
  elif command -v wget >/dev/null 2>&1; then
    wget -qO- "$URL" | tar -xz -C "$WRAPPER_DIR"
  else
    echo "[mvnw] ERROR: curl or wget is required to download Maven." >&2
    exit 1
  fi
fi

exec "$MVN_BIN" "$@"
