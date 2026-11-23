# Intent — Remove the "Calculateur" tab and the standalone calculator

Status: v0.2 — validated by the owner (2026-11-23); open-question defaults applied. Issue: #10. Extends `docs/intent.md` (v0.3);
where the two differ, this file wins for the points below once validated. No design or code yet.

## 1. What I want

The bottom navigation bar shows **Pièces, Recettes, Stats** only. The "Calculateur" tab is gone,
and so is the standalone calculator screen behind it.

## 2. Why

- I never use the tab: I get my doses when I create a piece, and they stay visible on the piece.
- The tab is the only way into the standalone calculator. Without it the screen would be
  unreachable, and an orphaned screen is dead code to maintain and test. So the screen goes too.

## 3. Behaviour I expect

1. The bottom bar has three items: Pièces, Recettes, Stats. No screen or route named
   calculator remains.
2. Doses are still computed and shown **live when creating a piece** (recipe + weight, R6/R7),
   and shown on the piece detail screen (snapshotted at creation).
3. The standalone use case ("compute doses without creating a piece", R5) is **dropped**: to see
   doses, I start a new piece.

## 4. Scope

In:
- Remove the tab, the `calculator` route, `CalculatorScreen`, `CalculatorViewModel` and its UI state.
- **Keep `DosesCard`** (used by the new-piece screen) and `DosageCalculator` (domain, used when a
  piece is created). `DosesCard` moves next to the new-piece screen.
- Remove the strings `nav_calculator` and `calculator_hint`; keep the "no recipe" hint still used
  by the new-piece and settings screens.
- Remove the two calculator UI tests; R4 (dose maths) stays covered by `DosageCalculatorTest` and
  gets a new-piece screen test.
- Drop R5 and reword the "Dosage calculator" wording in `requirements.md`, `design.md`,
  `intent.md` (§4.1), `assumptions.md`, `README.md` (feature list, layout, calculator screenshot),
  `CLAUDE.md` and `CHANGELOG.md`.

Out:
- Any change to dose maths, rounding, durations or reminders.
- The new-piece and piece-detail dose display, which stay as they are.

## 5. Relation to #9

#9 is merged on `main` (button, `onOpenCalculator` and the `pieceId` prefill are already gone), and
this branch is rebased on it. #10 therefore only removes what is left: the tab, the `calculator`
route, `CalculatorScreen`/`CalculatorViewModel`, and R5 altogether. No order constraint remains.

## 6. Success looks like

- The bottom bar shows Pièces, Recettes, Stats only.
- Creating a piece still shows the right doses live, and the piece keeps its snapshot.
- No unused string, import, composable or route is left behind; specs and README match the app.
- `mise run check` passes (lint, unit tests, coverage >= 90 %).

## 7. Open questions (each with a default)

1. Is dropping the standalone calculator (R5) really what I want, rather than keeping it
   reachable another way (e.g. from Réglages)? Default: drop it entirely.
2. Rename the string `calculator_no_recipe` (still used elsewhere) to drop the "calculator" prefix?
   Default: yes, rename.
3. Delete the calculator screenshot and re-capture the other README screenshots that show the
   bottom bar? Default: yes, re-capture those showing the bar.
