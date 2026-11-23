# design.md — Meat Me Later

Implements requirements.md. Single Android app module, Kotlin, Jetpack Compose.

## 1. Architecture

```
ui (Compose screens, ViewModels, StateFlow)
  └─ domain (pure Kotlin: DosageCalculator, StageDurationRule, PieceStateMachine, use cases)
       └─ data (Room DAOs/entities, repositories, DataStore settings, JSON export/import)
platform (AlarmScheduler, ReminderReceiver, BootReceiver, NotificationFactory, permissions)
```

- Unidirectional data flow: ViewModel exposes one `UiState` per screen; UI sends events.
- `domain` has no Android imports, so it is fast to test and easy for an AI agent to change safely.
- `AlarmScheduler` is an interface; a fake is used in tests.
- Time comes from an injected `Clock` (never `System.currentTimeMillis()` in domain code).
- Dependency injection: manual (constructor injection + a small `AppContainer`). Add Hilt only if it becomes painful.

## 2. Data model (Room)

| Table | Columns |
|---|---|
| `meat_type` | id, name, archived |
| `recipe` | id, name, salt_g_per_kg, sugar_g_per_kg, pepper_g_per_kg, fridge_seasoning_default (nullable text), vacuum_divisor (def 2.0), vacuum_offset_days (def 1), fridge_min_days (def 14), archived |
| `piece` | id, label, meat_type_id (FK, indexed), recipe_id, weight_g, diameter_cm, salt_g, sugar_g, pepper_g (snapshot), fridge_seasoning (nullable text, chosen when leaving VACUUM), status (VACUUM / FRIDGE / DONE / CANCELLED), created_at |
| `stage_run` | id, piece_id, stage (VACUUM / FRIDGE), started_at, planned_end_at, ended_at (nullable), postponed_days (int, def 0), note (nullable text) |

- Timestamps are UTC epoch millis. Weights are stored in grams as integers. Doses in g/kg are decimals.
- Money-style rounding: use `BigDecimal` with `HALF_UP` for dose results.
- Migrations are explicit from v1 (Room exported schemas committed, migration tests).
- v1 → v2 (#5): `piece.meat_type_id` added and backfilled from `recipe.meat_type_id`, then `recipe.meat_type_id` dropped by rebuilding `recipe` (FK, index); the one-open-run trigger and all indices are recreated. Backup format version 2 puts `meatTypeId` on pieces; version 1 files are read by deriving it from the recipe.
- A meat type is deletable only if no piece uses it (`MeatTypeDao` counts pieces).

## 3. Piece state machine

```
VACUUM --finish--> FRIDGE --finish--> DONE
   \                  \
    +----cancel--------+--> CANCELLED
```

- `finish` closes the open stage run (`ended_at = now`) and opens the next at `now`.
- `finish` from VACUUM takes the stage-2 seasoning as input (nullable = no seasoning) and
  creates the FRIDGE run with `planned_end_at = now + fridge_min_days`. That date is the
  first check, not a deadline: the user postpones day by day until the piece feels ready.
- `planned_end_at` = start + duration; changing duration recomputes it from the run's start.
- Only one open stage run per piece (enforced by use case and a partial unique index).

## 4. Reminder strategy (the critical part)

- **Source of truth is the database**, not the alarm. Alarms are a cache that can always be rebuilt.
- `ReminderPlanner.reconcile()` reads open stage runs and (re)schedules one exact alarm per run
  (`setExactAndAllowWhileIdle`, request code = stage_run id). It runs on: app start,
  `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, and after every edit.
- Permissions: `POST_NOTIFICATIONS` (Android 13+) and exact alarms. Because the APK is
  sideloaded, `USE_EXACT_ALARM` (granted automatically on Android 13+) can be declared; Google Play
  restricts it to alarm/calendar apps, so revisit this if the app ever goes to the Play Store.
  On Android 12 the app uses `SCHEDULE_EXACT_ALARM` with a `canScheduleExactAlarms()` check.
- Notification channel `stage_reminders` at high importance. Actions "Fait" and "Reporter 1 jour" (adds 24 h to `planned_end_at`, increments `postponed_days`, reschedules)
  go through a `BroadcastReceiver` that calls the same use cases as the UI.
- Overdue safety net (R15): the home screen queries `planned_end_at <= now` and pins them on top.
- Optional later: a daily `WorkManager` check as a second line of defence.
- Battery: on first launch, explain and deep-link to the battery-optimisation settings. Show a persistent banner if exemptions are missing.

### Calendar appointment (R22, optional)

- Launched with `Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)` and extras
  for title, begin/end time and description. The calendar app shows its own confirmation
  screen, so the app needs no calendar permission and no network.
- Offered on demand from the piece detail screen, for the current stage end. Not kept in sync.

## 5. Screens (all French)

1. **Pièces** (home): permission banner, "En retard" section, then running pieces sorted by next deadline; each row = label, the piece's meat/recipe, stage, remaining time. FAB "Nouvelle pièce".
2. **Nouvelle pièce**: meat type picker (non-archived), recipe picker (names only), weight, diameter → live doses and planned vacuum duration → confirm.
3. **Détail pièce**: stage-1 doses, stage-2 seasoning (or "aucun"), stage timeline planned vs. actual with postponements and notes, buttons "Sortir du sous-vide / du frigo", "Reporter 1 jour", "Ajouter au calendrier", edit duration, cancel. Leaving the vacuum stage opens a dialog asking for the stage-2 seasoning (prefilled, may be empty).
4. **Calculateur**: weight + recipe → doses, no piece created.
5. **Recettes**: flat list (no meat grouping); edit form with doses, rule, fridge days.
6. **Statistiques**: averages per recipe/stage, finished pieces.
7. **Réglages** (R24–R26): four blocks: *Apparence* (Clair / Sombre / OLED / Système), *Dosages au sous-vide* (recipe picker, salt/sugar/pepper g/kg with − / + steppers, note that running pieces keep their doses), *Durée du sous-vide* (divisor, added days, live example with round-up), *Frigo et rappels* (minimum fridge days, reminder hour), and *Types de viande* (add, rename, delete; moved from Recettes). Also permissions status, export/import JSON, about/version.

Navigation: Navigation-Compose with bottom bar (Pièces, Calculateur, Recettes, Stats); Réglages from the top bar.

### Themes (R24)

- Four theme modes stored in DataStore: `LIGHT`, `DARK`, `OLED`, `SYSTEM`. `SYSTEM` resolves to `LIGHT` or `DARK` from `isSystemInDarkTheme()`.
- OLED is a third Material 3 `ColorScheme` derived from the dark one: `background`/`surface` = `#000000`, containers, outlines and chips = very dark greys, same accents. Contrast (AA, N4) must be checked on the alert orange, the sous-vide/frigo chips and the primary buttons.
- Mockups for all six screens exist in the three palettes on the design canvas.

### Accent colour (R27)

- Domain: `AccentColor` enum (`VIANDE_SECHEE` default, `VERT`, `BLEU`, `VIOLET`, `AMBRE`, `BRUN_GRIS`) in `domain/Stores.kt`, a new `accent` field on `AppSettings`. DataStore key `accent`, enum name as a string, tolerant parse with fallback to the default (same as `theme`). No Room change, no migration.
- UI: `resolveColorScheme(mode, systemDark, accent)`. Each accent holds a hand-tuned palette for light and dark (primary, secondary and their containers, plus neutral tints for background, surface and outline). OLED derives from the accent's dark palette and keeps the black and grey surfaces of R24, swapping only the accent containers. `usesDarkBars` ignores the accent.
- Tertiary roles are the alert orange (overdue cards) and are identical for every accent. The sous-vide chip uses `secondaryContainer`, the frigo chip `primaryContainer`: both chips carry a text label (colour is never the only cue) and a test checks the two containers differ for every accent and mode.
- Settings: "Couleur principale" under "Thème", a `FlowRow` of round swatches (48 dp, check mark on the selected one, `contentDescription` = the French name). Strings in `strings.xml`.
- Contrast (N4): a unit test computes WCAG ratios for every (accent, mode) pair: onPrimary/primary, onPrimaryContainer/primaryContainer, onSecondary/secondary, onSurface/surface ≥ 4.5; primary vs background/surface ≥ 3.

## 6. Tech choices

| Area | Choice |
|---|---|
| Language/UI | Kotlin, Jetpack Compose, Material 3 (dynamic color off by default; own palette with Clair, Sombre and OLED schemes) |
| Persistence | Room 3 (`androidx.room3`) + KSP; DataStore (Preferences) for settings (theme, accent colour, reminder hour, global defaults) |
| Concurrency | Coroutines + Flow |
| Serialization | kotlinx.serialization (export/import) |
| Build | Gradle Kotlin DSL, version catalog, versions per `docs/decisions/0001-toolchain-versions.md`, R8 minify on release |
| Tests | JUnit5 or JUnit4 + Turbine (Flow), Room in-memory tests, Compose UI tests, Robolectric for receivers |
| Static checks | ktlint, detekt, Android lint |
| CI/CD | GitHub Actions: PR checks (`check`, `wrapper-validation`; no emulator, instrumented tests run locally with `mise run test:emulator`); tag → signed APK → GitHub Release (keystore in repo secrets) |

## 7. Test plan (maps to requirements)

- **Unit (domain):** R4 rounding cases, R7 duration cases (incl. non-integer results, tiny and large diameters), state machine transitions R8/R9/R11, snapshot immutability R6.
- **Integration:** Room DAOs and migrations; `ReminderPlanner` with fake `AlarmScheduler` + fake `Clock` for R10, R12, R14.
- **UI:** create-piece flow (R6/R7), finish-stage flow, overdue ordering (R15).
- **Device checklist (manual, before each release):** reminder fires with screen off and Doze, after reboot, after changing time zone, with notification permission revoked then regranted.

## 8. Risks

| Risk | Mitigation |
|---|---|
| Missed reminder ruins a piece | DB as source of truth, reconcile on many triggers, overdue list, permission banner, device checklist |
| Vendor battery killers (aggressive Android skins) | Onboarding screen with deep links; mention dontkillmyapp-style steps in docs |
| Wrong doses from bad rounding | Pure, heavily tested `DosageCalculator` using BigDecimal |
| Recipe edits changing history | Doses and durations snapshotted on the piece |
| Lost data on phone change | JSON export/import (R21) |

## 9. Build order (feeds the Stage 3 plan)

1. Repo scaffold, `CLAUDE.md`, CI, empty app builds.
2. Domain: `DosageCalculator`, `StageDurationRule`, state machine + tests.
3. Data layer: Room, repositories, seed meat types.
4. Recipes + calculator screens.
5. Pieces: create, list, detail, stage transitions.
6. Reminders: planner, alarms, receivers, boot, permissions.
7. Statistics, export/import.
8. Release pipeline and device checklist.
