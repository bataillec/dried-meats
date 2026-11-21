---
name: room-migration
description: Use when changing a Room entity, DAO query shape or database version in this project. Covers the migration, its test and the schema export rules.
---

# Changing the Room schema

This project uses Room 3 (`androidx.room3`, KSP only, Gradle plugin `androidx.room3` with a `room3 {}` block).
Before the first migration test, read the Room 3 migration-testing docs: the helper class names below come
from Room 2.x and may differ.

1. Change the entity. Bump `version` in the `@Database` annotation by exactly one.
2. Write an explicit `Migration(old, new)` with plain SQL. No `fallbackToDestructiveMigration`: user data is real.
3. Build once so Room exports the new `app/schemas/<db>/<version>.json`. Never edit those files by hand; commit them.
4. Add a migration test with `MigrationTestHelper`: create the old schema, insert representative rows (a piece mid-VACUUM with a stage run, one in FRIDGE, one DONE), migrate, assert the rows and the new columns.
5. Keep snapshot columns (`piece.salt_g`, ...) untouched unless the requirement says otherwise (R6).
6. If stage-run timing columns change, run `ReminderPlanner.reconcile()` in the migration test and assert alarms are rebuilt from the data.
7. `mise run check` must pass. Mention the requirement ids in the PR.
