# intent — Emulator test names must dex: camelCase in androidTest (issue #19)

Status: v0.2, decisions incorporated (2026-11-23), pending the owner's final OK. Extends `docs/intent.md` (v0.3); where the two
differ, this file wins for the points below once validated. Requirements and design updates follow in the next step
(only the tooling guard of §4.4-4.5 may touch `design.md`).

## 1. What I want

`mise run test:emulator` builds and runs the instrumented tests again on my emulator or phone. A rule in the
project guide keeps the same mistake from coming back.

## 2. Why

- `mise run test:emulator` (added by #13) currently fails at `:app:dexBuilderDebugAndroidTest`:
  `Space characters in SimpleName 'R26 the row shows HH-mm with a spoken label' are not allowed prior to DEX version 040`.
- minSdk is 26, so a method name in `app/src/androidTest` cannot contain spaces. Backtick names with spaces
  are fine for the JVM unit tests (`app/src/test`) but not for instrumented tests.
- `ReminderTimePickerTest` (added by #7, PR #15) has six such tests. It is the only androidTest file with
  them: the three others already use camelCase. Since #13 took the emulator job out of CI, nothing
  caught it before I ran the command locally.

## 3. Who

Me, before a release, running `mise run test:emulator`. Contributors later: they must learn the rule from the docs.

## 4. Behaviour I expect

1. **The six tests of `ReminderTimePickerTest` get camelCase names** that keep the requirement id as a prefix
   (convention: reference R-ids in test names), for example `r26RowShowsHhMmWithSpokenLabel`.
   What each test checks does not change.
2. **`mise run test:emulator` builds and runs** the instrumented tests on a device or emulator, with no dexing error.
3. **`CLAUDE.md` (Conventions) states the rule**: no spaces in `androidTest` method names (dex limit at minSdk 26);
   backtick names stay allowed in `app/src/test`; keep the R-id prefix in camelCase, lowercase `r` (`r26RowShows…`).
4. **`mise run check` also compiles and dexes the instrumented tests** (`assembleDebugAndroidTest`), with no
   device needed, so any dexing or compile error in `androidTest` fails before commit and in CI.
5. **A static guard rejects spaces in `androidTest` method names** (a detekt `FunctionNaming` override scoped to
   that source set, or an equivalent grep step in `mise run lint`), so the failure message names the rule and
   the offending test directly instead of a dexer error.

## 5. Constraints

- No change to app code, test logic, dependencies, permissions or minSdk.
- `mise run check` now also compiles and dexes `androidTest` (no device needed), still passes and still does not need an emulator.
- For the guard (4.5), the implementer picks the simpler option that works: detekt `FunctionNaming` scoped to `androidTest`, or a grep step in `mise run lint`.
- Commands go through mise.

## 6. Non-goals

- Raising minSdk to 30+ (dex 040) just to allow spaces.
- Adding, removing or rewriting instrumented tests beyond renaming these six.
- Running the instrumented tests in CI: only compiling them is added.
- Bringing the emulator job back in CI (decided in #13).

## 7. Success looks like

- `mise run test:emulator` with an emulator running compiles and executes all four instrumented test classes.
- `grep -rnE 'fun `[^`]* [^`]*`' app/src/androidTest` finds nothing.
- `CLAUDE.md` contains the rule.
- Reintroducing a spaced backtick name in `androidTest` makes `mise run check` fail, both at the guard and at dexing.
- `mise run check` stays acceptably fast (owner to judge; the extra step is reported in the PR).
- `mise run check` passes.

## 8. Decisions on the open questions (owner, 2026-11-23)

1. **Prevention: all three** layers are in scope: (a) the CLAUDE.md note, (b) `assembleDebugAndroidTest` in
   `mise run check`, (c) a static guard on `androidTest` method names. Done in this issue, not split.
2. **Id prefix: lowercase**, `r26RowShows…`.
