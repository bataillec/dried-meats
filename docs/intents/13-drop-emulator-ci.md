# intent — Drop the emulator tests from CI, run them locally (issue #13)

Status: v0.2, validated by the owner (2026-11-23). Extends `docs/intent.md` (v0.3); where the two
differ, this file wins for the points below once validated. Requirements and design updates follow in the next step.

## 1. What I want

Pull requests are checked in CI by `check` and `wrapper-validation` only. The instrumented tests
(`app/src/androidTest`) are no longer run on GitHub; I run them myself, on a local emulator or phone,
through one documented command: `mise run test:emulator`.

## 2. Why

- The `emulator-tests` job fails on every CI run: KVM setup, about 11 GB of free disk, a system image
  id that does not always resolve.
- It is slow (45 min timeout) and it blocks merges, for a solo, local-only app.
- The tests themselves stay valuable (alarms, receivers, Room on a real device): only where they run changes.

## 3. Who

Me, as the only developer, before a release. Contributors later: they must find the command in the docs.

## 4. Behaviour I expect

1. **CI on a pull request runs `check` and `wrapper-validation`** and goes green. No emulator, no KVM step,
   no AVD cache, no `emulator-reports` artifact.
2. **`mise run test:emulator` runs `connectedDebugAndroidTest`** against a running emulator or device,
   after the toolchain check (`doctor`). If no device is connected, it stops early with a clear message.
3. **The docs say plainly that these tests are not run in CI** and must be run locally before a release:
   README (Tests section), `CLAUDE.md` (commands), `docs/device-checklist.md` (pre-release step).
4. **No mention of the `emulator-tests` CI job remains** in the repository docs.

## 5. Constraints

- No app code change, no new dependency, no new permission.
- `mise run check` still passes and still does not need an emulator.
- Commands go through mise, as everywhere else in the project.

## 6. Non-goals

- Making the emulator tests reliable on GitHub runners (self-hosted runner, other emulator image, retries).
- Adding, removing or rewriting any instrumented test.
- Any change to the release workflow beyond removing the emulator job.

## 7. Success looks like

- A pull request shows two checks, both green.
- `mise run test:emulator` with an emulator running executes the instrumented tests; without one it explains what to do.
- Searching the repository for `emulator-tests` finds nothing outside this file and the git history.
