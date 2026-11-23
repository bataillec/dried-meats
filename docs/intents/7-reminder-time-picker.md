# intent — Sélecteur d'heure pour le rappel (issue #7)

Status: v0.2 (2026-11-23), open questions answered by the owner. Refines R26 (reminder time of day).

## 1. What I want

Set the reminder time in Réglages with one real time picker instead of two separate −/+
steppers for hours and minutes. The time is shown as a single value (e.g. `07:30`), and
tapping it opens a Material 3 time picker.

## 2. Why

- Reaching 07:35 with steppers takes many taps (hours by 1, minutes by 5), and the time is
  never displayed as one value.
- A time is one thing: choosing it should be one gesture.
- Today the minute stepper does not reschedule the alarms, so changing only the minutes
  leaves stale alarms until something else triggers a reconcile. That is a reliability bug
  for the critical feature (R12-R16, N5), and a single confirm path fixes it.

## 3. Who

- Primary user: me. Possibly friends who install the same APK, each with their own setting.

## 4. What it does

1. In Réglages → "Frigo et rappels", one row "Heure du rappel" shows the current time as `HH:mm`.
2. Tapping the row opens a Material 3 time picker dialog with "Annuler" and "Valider".
3. "Valider" saves hour and minute together, then reschedules the reminders once.
   "Annuler" (or dismissing the dialog) changes nothing.
4. The picker is 24 h and any minute is allowed (no longer multiples of 5).
5. The picker can switch between the dial and keyboard input.
6. The two old rows ("Heure du rappel" and "Minutes du rappel") are removed.

## 5. Domain facts

- The reminder time of day (default 18:00) is the global default applied when computing a
  stage's planned end timestamp (R26). Changing it only affects the plans that are
  recomputed on reconcile; the stored format (hour and minute in DataStore) does not change.
- No migration and no new setting.

## 6. Constraints

- French UI, all text in string resources; unused `reminder_hour` / `reminder_minute` strings removed.
- The row is a single large touch target with a spoken label such as "Heure du rappel, 07:30".
- Usable at 200 % font scale: neither the row nor the dialog clips or truncates; the dialog
  scrolls or falls back to keyboard input when space is short. Announced and dismissible with TalkBack.
- No new permission, no new dependency, no Room change.

## 7. Non-goals

- Per-recipe or per-piece reminder times.
- Changing how planned end timestamps are computed.
- Following the system 12 h / 24 h setting (see open question 1).

## 8. Success looks like

- I set 07:35 in a few taps and the row shows `07:35`.
- After confirming, the reminders fire at the new time; after cancelling, nothing changed.
- Changing only the minutes now reschedules alarms immediately.
- The picker is usable at 200 % font and with TalkBack.

## 9. Decisions (owner answers, 2026-11-23)

1. 24 h always, whatever the system setting.
2. Dial when the dialog opens, with a toggle to keyboard input; keyboard directly at large font scale.
3. The note under the row ("Les rappels sonnent à cette heure le jour prévu.") stays unchanged.
4. Buttons "Valider" / "Annuler", dialog title "Choisir l'heure".
5. No extra hint on the row.
