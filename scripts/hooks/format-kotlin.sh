#!/usr/bin/env bash
# PostToolUse hook: format a Kotlin file right after Claude edits it.
# Input: JSON on stdin. Never blocks (formatting problems are caught by `mise run check`).
file="$(jq -r '.tool_input.file_path // empty')"
case "$file" in
  *.kt|*.kts) ktlint --format "$file" >/dev/null 2>&1 || true ;;
esac
exit 0
