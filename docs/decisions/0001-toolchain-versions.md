# 0001 — Toolchain versions (decided 2026-11-14)

Goal: be as current as possible without running pre-release tooling where a stable one exists.
Versions come from web research on 2026-11-14 and are **starting points**: the first real build
(`mise run check`) is the proof. If something does not combine, fall back as noted.

| Area | Choice | Why / caveat | Fallback |
|---|---|---|---|
| JDK | Temurin 25 (LTS, via mise) | Gradle 9.1+ runs on Java 25; Kotlin 2.3+ can target it. AGP's documented JDK minimum is 17; Java 25 is not explicitly listed by AGP, so the first build is the test. AGP 9.3 lint is reported to crash on JDK 17, which argues against 17. | Temurin 21 |
| Gradle | 9.8.2 (wrapper) | AGP 9.3 needs Gradle 9.5.0 or later. | 9.5.x |
| AGP | 9.3.x | Max API level 37. Built-in Kotlin. | 9.2.x |
| Kotlin | 2.4.x (2.4.10 seen) | AGP 9 bundles an older Kotlin Gradle plugin; override it to 2.4.x and check the build. | Version bundled with AGP |
| compileSdk / targetSdk | 37 (Android 17) | Stable since 2026-06-16. Google Play's target-36 rule does not apply to sideloaded APKs, but becomes relevant if the app ever goes on Play. | 36 |
| Build-tools | 36.0.0 | AGP 9.3 default. | |
| Compose BOM | 2026.09.00 or newer | Compose 1.12 stable (Aug 2026); one source says 1.12 needs compileSdk 37, which we use. Look for a newer BOM at the first build. | |
| Room | 3.0.x (`androidx.room3`) | Room 2.x is in maintenance mode. Room 3: new package, KSP only, no SupportSQLite, Kotlin codegen. Check the migration-testing API in the Room 3 docs before writing the first migration test. | Room 2.8.5 |
| KSP | 2.3.x (2.3.11 seen) | Versioned independently of Kotlin since 2.3.0. | |
| ktlint | latest via mise + Gradle plugin `org.jlleitschuh.gradle.ktlint` | Gradle plugin version to confirm at the first build. | |
| detekt | 2.0.0-alpha (latest alpha), plugin id `dev.detekt` | 1.23.8 targets Kotlin 2.0.21 and AGP 8.8. The 2.0 alphas are built on Kotlin 2.4, Gradle 9.5, AGP 9.2 and tested on JDK 25, but detekt says breaking changes can still happen. | Drop detekt, keep ktlint + Android lint |
| GitHub Actions | checkout v7, mise-action v5, setup-gradle v6 (`cache-provider: basic`), wrapper-validation v6, upload-artifact v7, action-gh-release v3 | `mise generate github-action` uses checkout v7 and mise-action v5. setup-gradle v6's default cache is closed-source under Gradle's Terms of Use; `basic` is MIT. | |

## Keeping current

- `mise outdated` for tools; Dependabot for GitHub Actions now and for Gradle once the version catalog exists.
- Re-run this research before each minor release of the app; update this file in the same PR.

## Sources

- https://docs.gradle.org/current/userguide/compatibility.html · https://endoflife.date/gradle
- https://developer.android.com/build/releases/agp-9-3-0-release-notes
- https://kotlinlang.org/docs/whatsnew23.html · https://blog.jetbrains.com/kotlin/2026/09/kotlin-toolchain-0-12-multiplatform-library-publishing-wasm-apps-and-more/
- https://developer.android.com/jetpack/androidx/releases/room3 · https://android-developers.googleblog.com/2026/03/room-30-modernizing-room.html
- https://detekt.dev/changelog-2.0.0/ · https://detekt.dev/docs/introduction/compatibility
- https://blog.gradle.org/github-actions-for-gradle-v6 · https://newreleases.io/project/github/jdx/mise/release/v2026.10.5
