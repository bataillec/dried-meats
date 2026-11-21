# Assistant Viandes Séchées

Android app (French UI) for dried-meat preparation: dose calculator, stage reminders, tracking.
Everything stays on the phone. Specs are in [`docs/`](docs/).

## Setup

1. Install [mise](https://mise.jdx.dev) and activate it in your shell.
2. `mise install` — JDK, Android command-line tools (`jdx/vfox-android-sdk` plugin), jq, ktlint.
3. `mise run setup` — accepts SDK licenses, installs the platform and build-tools.
4. `mise run doctor` — must print `OK`. If it fails, the message says what is missing.
5. `mise run check` — lint and unit tests.

## Everyday commands

`mise run format | lint | test | check | build | release`

## Install on your phone

Download the APK from the GitHub Releases page and open it on the phone (allow installs from
this source when Android asks). Data stays local; updating over a previous version keeps it.

## Working with Claude Code

`CLAUDE.md` holds the project rules; skills and the reviewer agent are in `.claude/`.
Flow: intent → requirements/design (`docs/`) → plan mode → small PRs → `reviewer` agent → merge.
