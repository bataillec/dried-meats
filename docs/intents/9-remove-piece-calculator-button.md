# Intent — Remove "Ouvrir dans le calculateur" from the piece detail screen

Status: v0.1 — draft for the owner's validation. Issue: #9.

## 1. What I want

The piece detail screen no longer shows the "Ouvrir dans le calculateur" button. It sits
below the history section and above the "cancel" button, and I never use it.

## 2. Why

- The doses of a piece are already shown on its detail screen (snapshotted at creation),
  so opening the calculator from there adds nothing.
- Less clutter next to the destructive "cancel" action.

## 3. Scope

In:
- Remove the button and its `onOpenCalculator` parameter from `PieceDetailScreen`, and the
  navigation hook in `MainActivity`.
- Remove the string `open_in_calculator` and the `onOpenCalculator = {}` arguments in tests.
- Remove the now-dead "open the calculator prefilled from a piece" path: the optional
  `pieceId` route argument and the prefill logic in `CalculatorViewModel`, once nothing
  else navigates with it.
- Reword R5 ("works standalone, and also from a piece") to "works standalone", and keep
  `design.md` consistent.

Out:
- The "Calculateur" bottom-bar tab and the whole calculator screen: that is issue #10.
- The notification flow (`EXTRA_PIECE_ID`), which is unrelated to the calculator route.
- Any change to dose maths or to how doses are shown on the new-piece and detail screens.

## 4. Relation to #10

#10 removes the Calculateur tab, and with it the standalone calculator and R5 altogether.
#9 only removes the piece entry point and the `pieceId` prefill, which #10 would delete
anyway. The two are independent: #9 stands alone (the calculator stays reachable from the
tab), and if #10 lands afterwards, it simply has less to remove. If both land, expect
small textual conflicts in `MainActivity.kt`, `CalculatorScreen.kt` and R5.

## 5. Success looks like

- The button no longer appears on the piece detail screen.
- No unused string, parameter, route argument or test argument is left behind.
- `mise run check` passes (lint, unit tests, coverage >= 90 %).

## 6. Open questions (each with a default)

1. Remove the `pieceId` prefill path too? Default: yes, after checking that nothing else
   (notification intents included) navigates to `calculator?pieceId=...`.
2. Reword R5 now, even though #10 may drop it? Default: yes, to "works standalone".
3. Re-capture the README piece detail screenshot? Default: only if the button is visible
   in it.
