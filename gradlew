#!/bin/sh
#
# Simple Gradle launcher wrapper that delegates to installed gradle binary
#

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
elif [ -n "$GRADLE_HOME" ] && [ -x "$GRADLE_HOME/bin/gradle" ]; then
  exec "$GRADLE_HOME/bin/gradle" "$@"
else
  echo "ERROR: Gradle executable not found in PATH." >&2
  exit 1
fi
