---
name: release
description: Use when preparing a release of the APK: version bump, checks, tag, GitHub Release.
---

# Releasing

1. Be on `main`, clean tree, `mise run check` green.
2. Run the device checklist from the reminder-change skill if reminders or alarms changed since the last release.
3. Bump `versionName` (SemVer) and increment `versionCode` in `app/build.gradle.kts`. Update `CHANGELOG.md`.
4. Commit `chore: release vX.Y.Z`, then tag `vX.Y.Z` and push the tag.
5. The `release` workflow builds the signed APK and attaches it to the GitHub Release. Check the release page, download the APK and install it on a real phone over the previous version (the data must survive the update).
6. Signing: keystore and passwords are repository secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Never print, read or commit them.
