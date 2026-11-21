---
name: reminder-change
description: Use when touching alarms, notifications, receivers, permissions or stage timing. Reminders are the critical feature (R12-R16, N5): follow these rules and the device checklist.
---

# Changing reminder behaviour

Rules:
- The database is the source of truth. Alarms are a cache: every change ends with `ReminderPlanner.reconcile()`.
- Alarm times are absolute UTC timestamps from `stage_run.planned_end_at`. Never schedule relative delays.
- `reconcile()` must also run on `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, and at app start.
- Exact alarms through the `AlarmScheduler` interface only. Tests use `FakeAlarmScheduler` and `FakeClock`.
- Notification actions: "Fait" (finish the stage) and "Reporter 1 jour" (+24 h, `postponed_days` + 1). They call the same use cases as the UI.
- Fridge stage text is a check ("Vérifier la pièce au toucher"), not a deadline.
- No advance warnings (intent.md §9).

Tests required for any change: scheduling after create, after finish, after postpone, after edit, after simulated reboot (planner rebuilt from an empty scheduler), after time-zone change.

Device checklist (manual, before each release; record the result in the PR):
1. Reminder fires with the screen off, after at least 30 min idle (Doze).
2. Reminder still fires after a reboot.
3. Reminder time is correct after changing the time zone.
4. Revoke the notification permission, then re-grant it: the home banner appears, then disappears, and pending reminders still fire.
5. "Fait" and "Reporter 1 jour" from the notification update the piece correctly.
