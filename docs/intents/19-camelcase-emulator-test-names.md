# intent — Emulator test names must dex: camelCase in androidTest (issue #19)

Status: v0.1, draft for the owner's validation. Extends `docs/intent.md` (v0.3); where the two
differ, this file wins for the points below once validated. Requirements and design updates follow in the next step
(none is expected: this is a test and docs fix).

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
   backtick names stay allowed in `app/src/test`; keep the R-id prefix in camelCase.

## 5. Constraints

- No change to app code, test logic, dependencies, permissions or minSdk.
- `mise run check` still passes and still does not need an emulator.
- Commands go through mise.

## 6. Non-goals

- Raising minSdk to 30+ (dex 040) just to allow spaces.
- Adding, removing or rewriting instrumented tests beyond renaming these six.
- Bringing the emulator job back in CI (decided in #13).

## 7. Success looks like

- `mise run test:emulator` with an emulator running compiles and executes all four instrumented test classes.
- `grep -rnE 'fun `[^`]* [^`]*`' app/src/androidTest` finds nothing.
- `CLAUDE.md` contains the rule.
- `mise run check` passes.

## 8. Open questions

1. **Should something prevent recurrence automatically, beyond the CLAUDE.md note?** The note alone relies on
   remembering, and CI no longer compiles `androidTest`. Options:
   a. CLAUDE.md note only (the issue's ask). Cheapest, but a miss is only found on my next local emulator run.
   b. Add `assembleDebugAndroidTest` to `mise run check`. Catches this and any other dexing or compile error in
      `androidTest` with no device, but makes `check` and CI slower.
   c. A cheap static guard on `app/src/androidTest` (a detekt `FunctionNaming` override scoped to that source set,
      or a grep step in `mise run lint`). Fast, targeted to this rule only.

   **Recommendation: (a) in this change, plus (b) in a separate small PR**, because (b) also protects `androidTest`
   from compile errors that today go unnoticed in CI; if its cost on `check` is too high, fall back to (c).
   This keeps issue #19 a one-concern fix.
2. **Naming of the id prefix**: `r26RowShows…` (lowercase r) is proposed. Confirm, or prefer `R26_rowShows…`
   (underscore, which dex accepts but ktlint/detekt naming rules may flag).
