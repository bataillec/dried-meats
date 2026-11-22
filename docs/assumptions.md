# assumptions.md — decisions taken while building v0.1

Decisions made without asking the owner (instruction: decide, document, continue). Each one can be
reversed; the requirement ids show what it touches.

| # | Topic | Decision | Req |
|---|---|---|---|
| 1 | Planned end time | A stage's planned end is `start date + N calendar days` at the reminder time of day (default 18:00), in the phone's zone, stored as an absolute UTC timestamp. So "7 days" started at 09:30 ends 7 days later at 18:00. | R7, R8, R12, R26 |
| 2 | Postpone | "Reporter 1 jour" moves the planned end to the same local time one calendar day later (DST-safe, not a strict 24 h). If the run is already overdue by more than a day, it moves to the first reminder time after now, so the alarm is never already in the past. | R13, R23 |
| 3 | One open run per piece | Enforced by the use cases, an atomic `closeIfOpen` update, a mutex around actions, and an SQLite `BEFORE INSERT` trigger installed in `RoomDatabase.Callback.onOpen`. Room cannot declare a partial unique index and an extra index would fail its schema validation, so a trigger is used instead. | design §3 |
| 4 | "Fait" on the notification | For the vacuum stage it moves the piece to the fridge using the recipe's default fridge seasoning (empty if none); the seasoning can be edited afterwards on the piece detail screen. Opening the notification shows the detail screen with the full dialog. | R8, R13 |
| 5 | Notified state | `AlarmScheduler` remembers, per run, the planned end for which a notification was really posted. An overdue run that was never notified fires immediately after reboot/update/permission grant; a notified one is only shown pinned on the home list (R15). | R12, R14, R15 |
| 6 | No exact-alarm permission | The app falls back to an inexact Doze-aware alarm and the home banner asks for the permission. The fallback can be late (N5 is only guaranteed with exact alarms). | R16, N5 |
| 7 | Seed data | Beef, pork and duck meat types are seeded once. No recipes are seeded: dose values are the owner's responsibility (intent.md §7). The calculator and new-piece screens explain how to create a recipe. | R1 |
| 8 | Recipe duplicate | Duplicating copies all values and appends " (copie)" to the name. | R2 |
| 9 | Settings steppers | Dosage steppers step by 0.5 g/kg, divisor and added days by 0.5. They edit the chosen recipe directly (new pieces only). The global defaults (minimum fridge days, reminder time) are used when creating a recipe or a stage. | R25, R26 |
| 10 | Export/import | One JSON file (`format`, `version`, all tables, exact decimal text). Import replaces everything atomically after strict validation (references, ids, statuses vs open runs, positive weights/divisors). Alarms are reset and rebuilt after an import. Settings (theme, reminder time) are not exported. | R21 |
| 11 | Calendar | Offered on demand for the current stage end; the 30-minute event is a one-off copy. | R22 |
| 12 | Language | French only for now; strings are in `values/strings.xml`, plurals via `plurals`. | N1 |
| 13 | Battery exemption | Declared `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (sideloaded APK; Google Play restricts it). The home banner and Réglages show and fix it; there is no separate first-launch wizard. | R16 |
| 14 | Compose size lint | detekt `LongMethod` and `CyclomaticComplexMethod` are not applied under `ui/` and `MainActivity.kt` (declarative Compose functions). | N7 |
