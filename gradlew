#!/bin/sh
#
# Simple Gradle launcher wrapper that delegates to installed gradle binary
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
  /opt/hostedtoolcache/Java_Temurin-bin/*/bin/gradle; do
  if [ -x "$candidate" ]; then
    exec "$candidate" "$@"
  fi
done

echo "ERROR: Gradle executable not found in PATH." >&2
exit 1
