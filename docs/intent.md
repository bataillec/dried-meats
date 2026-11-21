# intent.md — Assistant Viandes Séchées (Android)

Status: v0.3 — updated with the owner's answers (2026-11-14). No blocking open items (§9).

## 1. What I want

An Android app that guides me through making dried meats (vacuum-drying stage, then
fridge-drying stage), so I never have to compute doses by hand, forget to take a piece
out on time, or lose track of which seasoning goes with which meat.

## 2. Why

- Dosing salt, sugar and pepper by weight is repetitive and error-prone.
- Stages last days or weeks; missing a removal date spoils or ruins a piece.
- I want to learn from past batches (how long did each stage really take?).

## 3. Who

- Primary user: me, on my own Android phone.
- Possible later: friends, who install the same APK. Each user has their own local data.

## 4. Features (v1)

1. **Dosage calculator** — given a piece's weight and the recipe's dosage, show grams
   of salt, sugar and pepper.
2. **Vacuum-stage reminder** — notify me when a piece must come out of the vacuum-drying stage.
3. **Fridge-drying reminder** — notify me when a piece must come out of the fridge-drying stage.
4. **Seasoning memo** — remind me which recipe/seasoning matches which piece.
5. **Stage time tracking** — record elapsed time in each stage per piece, and compare
   across pieces of the same meat type.
6. **Several pieces at once** — all of the above works for many pieces in parallel.
7. **Optional calendar appointment** — from a piece, add its stage-end date to an external
   calendar app.

## 5. Domain facts given by the owner

- Meats today: beef, pork, duck. The list must be extensible (the owner will add more).
- Dosage unit: **grams per kg** of meat, for salt, sugar and pepper, configured per recipe.
- Stage 1 (vacuum) seasoning is only salt, sugar and pepper. Stage 2 (fridge) seasoning is
  free: anything the owner wants, or nothing at all.
- Vacuum-drying duration rule (original recipe): **diameter in cm / 2 + 1 days**
  (e.g. 12 cm → 7 days). It can be a week or more. The dosage is minimal, so the stage
  must not be overrun: reminders must be reliable and the owner can override the duration per piece.
- Fridge-drying (stage 2): the piece sits on a grill/rack in the fridge (not directly on
  the shelf). **Minimum 2 full weeks** (14 days); after that the end is decided by feel
  (touching the meat). So the reminder at day 14 means "go and check the piece", and the
  owner can postpone it by one day at a time until the piece feels ready.

## 6. Constraints

- Android only (no iOS). Installed by APK sideload; GitHub Releases for distribution.
- Fully local: no account, no backend, no sync, no analytics. Works offline.
- UI in French (strings externalised so other languages can be added later).
- Reminders must fire on time even after days/weeks, after reboot, and under battery
  optimisation. Reminder times are stored as absolute timestamps.
- Stack: Kotlin, Jetpack Compose, Room. Built and checked in GitHub Actions.

## 7. Non-goals (v1)

- Cloud sync, accounts, social features, recipe sharing inside the app.
- Play Store publication.
- Food-safety guarantees: the app computes what I configure; I am responsible for the
  recipe values.
- Weight-loss tracking during drying (candidate for v2).

## 8. Success looks like

- I can enter a weight and get correct gram amounts in under 10 seconds.
- Every reminder fires within 1 minute of its time on my phone, including after a reboot.
- After a piece is done, I can see actual vs. planned time for each stage.
- A tagged release produces an installable APK without manual steps.

## 9. Decisions (owner answers, 2026-11-14)

1. Fridge stage: minimum 14 days, then judged by feel (see §5). No weight-loss tracking.
2. No advance warning: one reminder at the end of each stage. Optional "add to calendar".
3. Stage 1: salt, sugar, pepper only. Stage 2: free seasoning, possibly none. No curing salt.
4. One notification per piece, with buttons "Fait" and "Reporter 1 jour".

Still to confirm later (not blocking): whether the optional calendar appointment should
be offered automatically when a stage starts or only on demand (design assumes on demand).
