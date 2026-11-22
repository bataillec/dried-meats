# Device checklist (manual, before each release)

Automated on an emulator (API 37, `mise run` + `./gradlew connectedDebugAndroidTest`): real alarm posts the notification
within 60 s, first-launch battery wizard, leave-vacuum dialog. Not automatable there: Doze with a real battery,
reboot persistence on hardware, vendor battery killers, contrast/large-font review.

Run on a real phone with the release APK installed over the previous version (data must survive).

- [ ] Create a piece with a 1-day vacuum duration set to end within a few minutes (edit the duration, or use a test recipe): the notification arrives within 60 s with the screen off and the phone idle (N5).
- [ ] Reboot the phone: the pending reminder still fires (R14).
- [ ] Change the time zone and the clock: the reminder still fires at the same absolute time (R14).
- [ ] Revoke the notification permission, let a reminder come due, grant it again, reopen the app: the piece is pinned in "En retard" and no duplicate loop occurs (R15, R16).
- [ ] "Fait" and "Reporter 1 jour" buttons on the notification update the piece and reschedule.
- [ ] Deny exact alarms: the home banner shows and links to the system screen.
- [ ] Battery exemption banner clears after granting.
- [ ] Export to JSON, uninstall/reinstall, import: all pieces and recipes are back and reminders are rescheduled.
- [ ] Themes Clair, Sombre, Sombre OLED, Système; OLED background is pure black; check contrast of the alert orange and chips (N4).
- [ ] Font scale 200 %: no clipped buttons.

## Last run (2026-11-22, debug build, AGP 9.5.2)

OnePlus 6T (Android 15, USB/Wi-Fi adb, app package only) and an API 37 emulator.

| Item | Result |
|---|---|
| Real alarm posts the notification within 60 s | pass on phone and emulator (`ReminderOnDeviceTest`) |
| Doze (`deviceidle force-idle`, screen off) | pass on emulator: arrived 1 s after due |
| Reboot with a pending reminder | pass on emulator: arrived 2 s after due |
| Time zone change (Europe/Paris -> Asia/Tokyo) | pass on emulator: same absolute instant, arrived 2 s after due |
| Notification permission revoked then granted | pass on phone: banner appears/disappears |
| "Fait" / "Reporter 1 jour" buttons on the notification | buttons shown on the phone; behaviour covered by `ReceiversTest` (not tapped by hand) |
| Themes Clair / Sombre / OLED / Système | screenshots reviewed on phone; one bug fixed (status-bar icons in Clair on a dark phone) |
| Font scale 200 % | emulator: bottom navigation labels split mid-word, fixed by capping the bar's text scale |
| Battery wizard | pass on phone and emulator |
| Exact-alarm denial | not testable: with `USE_EXACT_ALARM` (API 33+) the permission cannot be revoked |
| Reboot / Doze on the real phone | not done (system-wide changes on Cam's phone were deliberately avoided) |
| Vendor battery killer (OnePlus) after days idle | still needs a human: install and leave a reminder 2-3 days out |
