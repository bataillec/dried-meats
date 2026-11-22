# Release signing

The app is distributed only as an APK attached to a GitHub Release. Pushing a tag `vX.Y.Z` runs
`.github/workflows/release.yml`, which builds the release APK, signs it and publishes it.

## One-time setup (Cam)

1. Create the keystore on your machine, outside the repository. Keep a backup: if you lose it,
   installed copies can no longer be updated, only reinstalled (which wipes their data).

   ```sh
   keytool -genkeypair -v -keystore meat-me-later.jks -alias meatmelater \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

   `keytool` asks for a keystore password, a key password and your details. Check it with
   `keytool -list -v -keystore meat-me-later.jks`.

2. Add four repository secrets (Settings > Secrets and variables > Actions), or with `gh`:

   | Secret | Value |
   | --- | --- |
   | `KEYSTORE_BASE64` | the keystore, base64 encoded |
   | `KEYSTORE_PASSWORD` | keystore password |
   | `KEY_ALIAS` | `meatmelater` (the `-alias` used above) |
   | `KEY_PASSWORD` | key password |

   ```sh
   base64 -w0 meat-me-later.jks | gh secret set KEYSTORE_BASE64 --repo bataillec/dried-meats   # macOS: base64 -i meat-me-later.jks
   gh secret set KEYSTORE_PASSWORD --repo bataillec/dried-meats
   gh secret set KEY_ALIAS --repo bataillec/dried-meats
   gh secret set KEY_PASSWORD --repo bataillec/dried-meats
   ```

3. Never commit the keystore or the passwords.

## Releasing

Follow `.claude/skills/release/SKILL.md` (bump version, changelog, tag, push the tag). The workflow then
runs `mise run check`, builds, verifies the signature with `apksigner` and creates the GitHub Release with
`meat-me-later-vX.Y.Z.apk`.

## Without secrets

If `KEYSTORE_BASE64` is absent (a fork, or before step 2), the workflow does not fail: it logs a warning and
publishes `meat-me-later-vX.Y.Z-unsigned.apk`, which Android refuses to install. Locally, `mise run release`
without the `RELEASE_KEYSTORE_*` variables likewise produces `app-release-unsigned.apk`.

Gradle reads `RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` and
`RELEASE_KEY_PASSWORD` (see `app/build.gradle.kts`). To sign a local build, export them and run `mise run release`.
