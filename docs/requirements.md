# requirements.md — Meat Me Later

Derived from intent.md v0.2. Each requirement has an ID used in tests and PRs.

## Glossary

- **Meat type**: beef, pork, duck, user-defined others.
- **Recipe**: a method, independent of any meat type: dosage (g/kg of salt, sugar, pepper) + stage rules + notes.
- **Piece**: one physical piece of meat being processed (meat type, recipe, weight, diameter).
- **Stage run**: one piece's time in one stage (VACUUM or FRIDGE).

## Functional requirements

### Recipes and meat types
- **R1** The user can create, edit and delete meat types, from the settings screen. Beef, pork and duck are pre-seeded.
  A meat type used by at least one piece cannot be deleted. Archived meat types are not offered for new pieces.
- **R2** The user can create, edit, duplicate and archive recipes. A recipe holds: name,
  salt/sugar/pepper in g/kg (decimals allowed), optional default fridge-seasoning note (free text),
  vacuum rule (divisor, default 2; offset days, default 1), minimum fridge duration in days (default 14; afterwards the piece is judged by touch).
  A recipe has no meat type: the same recipe can be used for any meat.
- **R3** A recipe cannot be deleted while pieces use it; it can be archived.

### Doses
- **R4** Given a weight (g or kg input) and a recipe, show grams of salt, sugar and pepper, live on the new-piece screen (R6).
  Formula: `grams = weight_kg × dose_g_per_kg`, rounded to 0.1 g.
  - *Acceptance:* 1.250 kg with salt 25 g/kg → 31.3 g (31.25 rounds half-up). 0 kg or empty → no result, no crash.
- **R5** *(removed, issue #10)* There is no standalone calculator: no bottom-bar tab, no calculator screen or route. Doses are only seen when creating a piece (R4, R6) and on its detail screen (snapshot, R6). The id is not reused.
  - *Acceptance:* the bottom bar shows Pièces, Recettes, Stats only; no screen or route named calculator exists.

### Pieces and stages
- **R6** Creating a piece requires: meat type, recipe, label (auto-suggested from the chosen meat type), weight, diameter in cm.
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
- **R17** The piece detail screen shows its meat type, its recipe, the snapshotted stage-1 doses (salt, sugar,
  pepper) and the stage-2 seasoning chosen for this piece (or "aucun").
  The piece list shows meat type and recipe name on each row.

### Tracking
- **R18** Every stage run stores start, planned end and actual end. The piece detail shows
  planned vs. actual per stage.
- **R19** A statistics screen shows, per meat type and recipe (the meat type being the piece's own), the average actual duration
  per stage, and the list of finished pieces.

### Data
- **R20** All data stays on the device (Room database). No network permission is requested.
- **R21** The user can export all data to a JSON file and import it back (backup / new phone).
  Files exported by earlier versions (where the meat type belonged to the recipe) still import: each piece takes its recipe's former meat type.

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
  with a live example ("Ø 6 cm ÷ 2 + 1 = 4 → 4 jours", same diameter as R28). Changes apply to new pieces only; running
  pieces keep their snapshotted doses (R6).
- **R26** The Réglages screen also exposes the minimum fridge duration (default 14 days) and the
  reminder time of day (default 18:00). The reminder time is applied when computing a stage's planned end
  timestamp. It is shown as one `HH:mm` row (24 h, any minute) that opens a time picker dialog; confirming
  saves hour and minute together and reconciles the reminders once, cancelling changes nothing. These are the global defaults used when creating recipes.
- **R27** The Réglages screen offers a "Couleur principale" choice, under "Thème": six swatches
  (Viande séchée, the default and today's red, plus vert, bleu, violet, ambre and brun-gris).
  - The choice applies immediately to the whole UI in Clair, Sombre, Sombre OLED and Système, and is
    kept after a restart. OLED keeps its pure black background (R24); only the accent roles change.
  - The accent drives the primary and secondary roles (and the neutral tints); the tertiary roles are the
    semantic alert orange (overdue cards) and never follow it. The sous-vide and frigo chips use the
    secondary and primary containers and always carry their text label, so they never rely on colour alone.
  - The selected swatch is shown by a check mark and a spoken label, not by colour alone; targets are
    48 dp and swatches wrap onto a second line when they do not fit (N4).
  - Stored in DataStore, not exported with the data; an unknown stored value falls back to the default.
- **R29** The "À propos" block of Réglages (last block, after the version and the "application locale" text)
  shows two links: "Soutenir sur Ko-fi" (https://ko-fi.com/O0P52B4FJ1) and "Code source sur GitHub"
  (https://github.com/bataillec/dried-meats).
  - Tapping a link hands the URL to the phone's browser (`ACTION_VIEW`); the app itself makes no network
    request and adds no network permission or WebView (R20). If no app can open it, a short message
    "Aucun navigateur disponible" is shown and the app does not crash.
  - Each link is a single touch target of at least 48 dp, announced by TalkBack as a link, with a label that
    wraps (never truncates) at 200 % font scale (N4). URLs are constants, labels are string resources.
  - Outside the app: the README has a "Support" section with the Ko-fi button, and `.github/FUNDING.yml`
    holds `ko_fi: O0P52B4FJ1` for the repository's Sponsor button.
  - *Acceptance:* both links are visible in Réglages > À propos; tapping each fires an `ACTION_VIEW` intent on
    the right URL; with no handler, the message appears and nothing crashes.

## Non-functional requirements

- **N1** Language: French UI; all strings in resources.
- **N2** Minimum Android 8.0 (API 26); target the latest stable API level.
- **N3** Cold start to list in < 2 s on a mid-range phone with 100 pieces.
- **N4** Accessibility: touch targets ≥ 48 dp, contrast AA, supports font scaling up to 200 %.
- **N5** Reminders fire within 60 s of their planned time with the app closed and the phone idle (Doze).
- **N6** Domain logic (dosage, durations, state machine) is pure Kotlin with ≥ 90 % unit-test coverage.
- **N7** CI blocks merge on: build, lint (ktlint, detekt, Android lint), unit tests.
- **N8** Release: pushing tag `vX.Y.Z` builds a signed APK and attaches it to a GitHub Release.
- **R28** The recipe form, under "Durée du sous-vide", shows a label under "Diviseur" and "Jours ajoutés"
  with the rule written with the typed values and a worked example for a fixed 6 cm diameter
  (see R7, R25).
  - Both fields valid: "Durée = ⌈ Ø ÷ 2 + 1 ⌉ jours, arrondie au jour supérieur" and
    "Exemple : Ø 6 cm ÷ 2 + 1 = 4 → 4 jours", updated on every keystroke, French number format.
  - A field empty or not a number: nothing is shown. Divisor ≤ 0: "Saisissez un diviseur supérieur à 0
    pour voir l'exemple." No crash, no « 0 » or « NaN ».
  - The example is computed by `StageDurationRule` (raw value, then days with the 1-day floor); the UI only formats.
  - Informational only: it never blocks saving. Wraps at 200 % font, `bodyMedium` / `onSurfaceVariant` (N4),
    and exposes one spoken sentence to TalkBack (not a live region).
  - *Acceptance:* defaults → "Ø 6 cm ÷ 2 + 1 = 4 → 4 jours"; divisor 1,5 and added days 0,5 →
    "Ø 6 cm ÷ 1,5 + 0,5 = 4,5 → 5 jours".
