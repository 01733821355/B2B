#!/bin/sh
#
# Robust Gradle launcher wrapper that delegates to installed gradle binary
# or downloads Gradle distribution if missing.
#

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
elif [ -n "$GRADLE_HOME" ] && [ -x "$GRADLE_HOME/bin/gradle" ]; then
  exec "$GRADLE_HOME/bin/gradle" "$@"
fi

for candidate in \
  /opt/gradle/bin/gradle \
  /opt/gradle/*/bin/gradle \
  /usr/local/bin/gradle \
  /usr/bin/gradle \
  ~/.gradle/wrapper/dists/*/*/bin/gradle \
  ~/.gradle/wrapper/dists/gradle-9.3.1/bin/gradle \
  /opt/hostedtoolcache/Java_Temurin-bin/*/bin/gradle \
  /opt/hostedtoolcache/gradle/*/bin/gradle; do
  if [ -x "$candidate" ]; then
    exec "$candidate" "$@"
  fi
done

# Fallback: Auto-download Gradle 9.3.1 if missing
DIST_URL="https://services.gradle.org/distributions/gradle-9.3.1-bin.zip"
TARGET_DIR="$HOME/.gradle/wrapper/dists/gradle-9.3.1"
if [ ! -x "$TARGET_DIR/bin/gradle" ]; then
  echo "Gradle not found locally. Auto-downloading Gradle 9.3.1..."
  mkdir -p "$TARGET_DIR"
  TMP_ZIP="/tmp/gradle-9.3.1-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -sSL "$DIST_URL" -o "$TMP_ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DIST_URL" -O "$TMP_ZIP"
  fi
  if [ -f "$TMP_ZIP" ]; then
    TMP_EXTRACT="/tmp/gradle-extract"
    mkdir -p "$TMP_EXTRACT"
    unzip -q "$TMP_ZIP" -d "$TMP_EXTRACT"
    cp -r "$TMP_EXTRACT"/gradle-9.3.1/* "$TARGET_DIR/" 2>/dev/null || true
    chmod +x "$TARGET_DIR/bin/gradle" || true
    rm -rf "$TMP_ZIP" "$TMP_EXTRACT"
  fi
fi

if [ -x "$TARGET_DIR/bin/gradle" ]; then
  exec "$TARGET_DIR/bin/gradle" "$@"
fi

echo "ERROR: Gradle executable not found in PATH and auto-download failed." >&2
exit 1
