# Assistant Viandes Séchées — project guide

Android app (Kotlin, Jetpack Compose, Room) to manage dried-meat preparation: dosage
calculator, stage reminders, tracking. Local only, no account, no network. French UI.
Specs live in `docs/`: read `intent.md`, then `requirements.md` (R-ids) and `design.md`.

## Commands (always through mise)

- `mise run doctor` toolchain check · `mise run setup` install SDK pieces
- `mise run check` lint + unit tests (must pass before any commit)
- `mise run format` · `mise run lint` · `mise run test` · `mise run build` · `mise run release`
- Never call `java`, `gradle` or `sdkmanager` directly: use `mise run` or `mise exec --`.
  Hooks and agents run in non-interactive shells where mise is not activated.

Tool and library versions: `docs/decisions/0001-toolchain-versions.md` (JDK 25, Gradle 9.x,
AGP 9.3, Kotlin 2.4, Room 3, compileSdk 37). Update it in the same PR as any version change.

## Architecture rules

- Layers: `ui` → `domain` ← `data`; `platform` (alarms, receivers, notifications) sits beside them.
- `domain` is pure Kotlin: no Android imports. Time only via an injected `Clock`.
- Dose maths uses `BigDecimal`, `HALF_UP`, rounded to 0.1 g. Weights stored as integer grams.
- Reminders: the database is the source of truth, alarms are a rebuildable cache.
  Anything that changes a stage run must end with `ReminderPlanner.reconcile()`.
- Pieces snapshot their doses and durations at creation; recipe edits never alter them.
- All user-visible text is French and lives in string resources.

## Conventions

- Reference requirement ids (R4, N5 …) in test names and PR descriptions.
- Tests first for `domain`; fakes (`FakeClock`, `FakeAlarmScheduler`) over mocks.
- Small PRs, one concern each. No new dependency without a line in `docs/decisions/`.
- Room: schema changes need a migration and a migration test (see the room-migration skill).

## Never

- Never edit `app/schemas/` by hand (generated), or read/commit keystores and `.env*` files.
- Never request network or calendar permissions (R20, R22).
- Never use `System.currentTimeMillis()` outside the `Clock` implementation.

## Skills and agents

- Skills: `room-migration`, `reminder-change`, `release` (in `.claude/skills/`).
- Agent: `reviewer`, to review a diff against the requirements before merging.
