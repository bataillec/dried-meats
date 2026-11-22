# requirements.md — Meat Me Later

Derived from intent.md v0.2. Each requirement has an ID used in tests and PRs.

## Glossary

- **Meat type**: beef, pork, duck, user-defined others.
- **Recipe**: a meat type + dosage (g/kg of salt, sugar, pepper) + stage rules + notes.
- **Piece**: one physical piece of meat being processed (weight, diameter, recipe).
- **Stage run**: one piece's time in one stage (VACUUM or FRIDGE).

## Functional requirements

### Recipes and meat types
- **R1** The user can create, edit and delete meat types. Beef, pork and duck are pre-seeded.
- **R2** The user can create, edit, duplicate and archive recipes. A recipe holds: name,
  meat type, salt/sugar/pepper in g/kg (decimals allowed), optional default fridge-seasoning note (free text),
  vacuum rule (divisor, default 2; offset days, default 1), minimum fridge duration in days (default 14; afterwards the piece is judged by touch).
- **R3** A recipe cannot be deleted while pieces use it; it can be archived.

### Dosage calculator
- **R4** Given a weight (g or kg input) and a recipe, show grams of salt, sugar and pepper.
  Formula: `grams = weight_kg × dose_g_per_kg`, rounded to 0.1 g.
  - *Acceptance:* 1.250 kg with salt 25 g/kg → 31.3 g (31.25 rounds half-up). 0 kg or empty → no result, no crash.
- **R5** The calculator works standalone (no piece created) and also from a piece.

### Pieces and stages
- **R6** Creating a piece requires: label (auto-suggested), recipe, weight, diameter in cm.
  The doses are shown and snapshotted on the piece at creation, so later recipe edits don't alter them.
- **R7** On creation, the piece enters VACUUM. Planned duration =
  `ceil(diameter_cm / divisor + offset)` days (always rounded up to the next whole day,
  never down and never in half-days), user-overridable (days, ≥ 1).
  - *Acceptance:* 12 cm with defaults → 7 days; 9 cm → ceil(5.5) = 6 days.
- **R8** The user can mark a piece's current stage as finished ("Sortir du sous-vide").
  The piece moves to FRIDGE with planned duration from the recipe (default 14 days,
  overridable), then to DONE. When leaving VACUUM, the app asks for the fridge-stage
  seasoning, prefilled from the recipe note; the user may leave it empty (no seasoning).
- **R9** Marking a stage finished early or late records the actual end time; the next
  stage starts at that moment, not at the planned time.
- **R10** The user can edit planned duration of a running stage; the reminder is rescheduled.
- **R11** The user can cancel or delete a piece. Its reminders are cancelled.

### Reminders
- **R12** One reminder is scheduled for each running stage at its planned end, as an absolute timestamp.
- **R13** The notification names the piece, recipe and the action ("Sortir du sous-vide"
  / "Sortir du frigo"), and offers actions "Fait" and "Reporter 1 jour" (postpones the planned end by 24 h and
  reschedules). For the fridge stage the text is a check ("Vérifier la pièce au toucher"),
  since the minimum is 14 days and the real end is decided by feel. There is no advance warning.
- **R14** Reminders fire after device reboot, app update, time-zone change and clock change
  (reschedule on boot / package replaced / time changed).
- **R15** On app open, overdue stages are shown at the top of the list, even if the
  notification was missed or dismissed.
- **R16** The app guides the user to grant notification and exact-alarm permissions and
  to exempt the app from battery optimisation if needed, and shows a warning on the home screen when any is missing.

### Seasoning memo
- **R17** The piece detail screen shows its recipe, the snapshotted stage-1 doses (salt, sugar,
  pepper) and the stage-2 seasoning chosen for this piece (or "aucun").
  The piece list shows meat type and recipe name on each row.

### Tracking
- **R18** Every stage run stores start, planned end and actual end. The piece detail shows
  planned vs. actual per stage.
- **R19** A statistics screen shows, per meat type and recipe, the average actual duration
  per stage, and the list of finished pieces.

### Data
- **R20** All data stays on the device (Room database). No network permission is requested.
- **R21** The user can export all data to a JSON file and import it back (backup / new phone).

### Calendar and notes
- **R22** (optional) From a piece, "Ajouter au calendrier" opens the phone's calendar app with
  a prefilled event (title, stage-end date, notes). It uses the system insert-event screen,
  so no calendar permission is needed and nothing is created without the user's confirmation.
  The event is a one-off copy: the app does not update it if the stage is postponed.
- **R23** Each stage run has an optional free-text note (e.g. how the meat felt when touched).
  Postponements ("Reporter 1 jour") are counted per stage run and shown in the history.

### Settings and themes
- **R24** A Réglages screen lets the user choose the theme: Clair, Sombre, Sombre OLED or Système.
  "Système" follows the phone's light/dark setting (it maps to Clair or Sombre, never to OLED,
  because Android cannot distinguish the two dark variants); OLED is chosen manually.
  - Sombre OLED uses a pure black background (#000000) with very dark grey cards, dividers and chips;
    accent colours are the same as in Sombre.
  - The choice is stored in DataStore and applies immediately.
- **R25** The Réglages screen exposes the vacuum-stage settings: for a chosen recipe, salt, sugar and
  pepper in g/kg (− / + steppers), and the duration rule (divisor, default 2; added days, default 1)
  with a live example ("Ø 9 cm ÷ 2 + 1 = 5,5 → 6 jours"). Changes apply to new pieces only; running
  pieces keep their snapshotted doses (R6).
- **R26** The Réglages screen also exposes the minimum fridge duration (default 14 days) and the
  reminder time of day (default 18:00). The reminder time is applied when computing a stage's planned end
  timestamp. These are the global defaults used when creating recipes.

## Non-functional requirements

- **N1** Language: French UI; all strings in resources.
- **N2** Minimum Android 8.0 (API 26); target the latest stable API level.
- **N3** Cold start to list in < 2 s on a mid-range phone with 100 pieces.
- **N4** Accessibility: touch targets ≥ 48 dp, contrast AA, supports font scaling up to 200 %.
- **N5** Reminders fire within 60 s of their planned time with the app closed and the phone idle (Doze).
- **N6** Domain logic (dosage, durations, state machine) is pure Kotlin with ≥ 90 % unit-test coverage.
- **N7** CI blocks merge on: build, lint (ktlint, detekt, Android lint), unit tests.
- **N8** Release: pushing tag `vX.Y.Z` builds a signed APK and attaches it to a GitHub Release.
