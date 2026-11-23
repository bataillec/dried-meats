#!/bin/sh
# Fails if an androidTest method uses a backtick name with spaces: dex rejects it below DEX 040 (minSdk 26).
# Backtick names stay allowed in app/src/test (JVM). See issue #19.
bad=$(grep -rnE 'fun `[^`]* [^`]*`' app/src/androidTest 2>/dev/null || true)
if [ -n "$bad" ]; then
  echo "FAIL: spaces in androidTest method names (use camelCase, e.g. r26RowShowsHhMm):"
  echo "$bad"
  exit 1
fi
