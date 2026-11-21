#!/usr/bin/env bash
# PreToolUse hook on Bash: block `git commit` / `git push` unless `mise run check` passes.
# Exit code 2 blocks the tool call and shows stderr to Claude.
cmd="$(jq -r '.tool_input.command // empty')"
case "$cmd" in
  *"git commit"*|*"git push"*)
    if ! out="$(mise run check 2>&1)"; then
      echo "Blocked: 'mise run check' failed. Fix these before committing:" >&2
      echo "$out" | tail -n 40 >&2
      exit 2
    fi
    ;;
esac
exit 0
