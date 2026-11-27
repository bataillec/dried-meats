# Device checklist (manual, before each release)

Automated on an emulator (API 37, `mise run test:emulator`, run locally: not part of CI): real alarm posts the notification
within 60 s, first-launch battery wizard, leave-vacuum dialog. Not automatable there: Doze with a real battery,
reboot persistence on hardware, vendor battery killers, contrast/large-font review.

Run on a real phone with the release APK installed over the previous version (data must survive).

- [ ] Run the emulator tests locally: `mise run test:emulator` (not run in CI).
- [ ] Create a piece with a 1-day vacuum duration set to end within a few minutes (edit the duration, or use a test recipe): the notification arrives within 60 s with the screen off and the phone idle (N5).
- [ ] Réglages → Heure du rappel: the row shows `HH:mm`, the picker opens (dial, keyboard toggle), a time like 07:35 can be set; "Annuler" changes nothing. With TalkBack the picker is announced and dismissible; at 200 % font nothing clips. Set a reminder time a few minutes ahead on a piece ending today: it fires at the new time (R26, R12).
- [ ] Reboot the phone: the pending reminder still fires (R14).
- [ ] Change the time zone and the clock: the reminder still fires at the same absolute time (R14).
- [ ] Revoke the notification permission, let a reminder come due, grant it again, reopen the app: the piece is pinned in "En retard" and no duplicate loop occurs (R15, R16).
- [ ] Vacuum notification: "Sortir du sous-vide…" opens the app on the piece with the seasoning prompt prefilled from the recipe; Annuler leaves the piece in vacuum with the reminder pending; confirming (also with an empty field) moves it to the fridge (R8, R13).
- [ ] Fridge notification: "Fait" finishes the piece in one tap; "Reporter 1 jour" postpones and reschedules (R13).
- [ ] Deny exact alarms: the home banner shows and links to the system screen.
- [ ] Battery exemption banner clears after granting.
- [ ] Export to JSON, uninstall/reinstall, import: all pieces (with their meat type) and recipes are back and reminders are rescheduled.
- [ ] Themes Clair, Sombre, Sombre OLED, Système; OLED background is pure black; check contrast of the alert orange and chips (N4).
- [ ] Couleur principale (R27): each swatch applies at once in Clair, Sombre, Sombre OLED and Système, OLED stays pure black, the choice survives a restart, the check mark shows the selected swatch, alert orange and chips stay distinct (N4).
- [ ] Font scale 200 %: no clipped buttons. Recette → "Durée du sous-vide": the formula and example label wraps without truncation or overlap with the fields and "Frigo" (R28).

## Last run (2026-11-22, debug build, AGP 9.5.2)

OnePlus 6T (Android 15, USB/Wi-Fi adb, app package only) and an API 37 emulator.

| Item | Result |
|---|---|
| Real alarm posts the notification within 60 s | pass on phone and emulator (`ReminderOnDeviceTest`) |
| Doze (`deviceidle force-idle`, screen off) | pass on emulator: arrived 1 s after due |
| Reboot with a pending reminder | pass on emulator: arrived 2 s after due |
| Time zone change (Europe/Paris -> Asia/Tokyo) | pass on emulator: same absolute instant, arrived 2 s after due |
| Notification permission revoked then granted | pass on phone: banner appears/disappears |
| Notification buttons ("Sortir du sous-vide…" prompt, fridge "Fait", "Reporter 1 jour") | to tap by hand; behaviour covered by `ReceiversTest` and `NotificationFinishVacuumTest` |
| Themes Clair / Sombre / OLED / Système | screenshots reviewed on phone; one bug fixed (status-bar icons in Clair on a dark phone) |
| Font scale 200 % | emulator: bottom navigation labels split mid-word, fixed by capping the bar's text scale |
| Battery wizard | pass on phone and emulator |
| Exact-alarm denial | not testable: with `USE_EXACT_ALARM` (API 33+) the permission cannot be revoked |
| Reboot / Doze on the real phone | not done (system-wide changes on Cam's phone were deliberately avoided) |
| Vendor battery killer (OnePlus) after days idle | still needs a human: install and leave a reminder 2-3 days out |
| Réglages > À propos: tap "Soutenir sur Ko-fi" and "Code source sur GitHub" (also at 200 % font scale, and with no browser if testable) | to do (R29) |
