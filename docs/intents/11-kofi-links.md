# intent — Liens de soutien Ko-fi et code source (issue #11)

Status: draft v0.1 (2026-11-22), waiting for the owner's validation. Touches R20 (no network) and R24-R26 (Réglages).

## 1. What I want

Let people who like the app find my Ko-fi page and the source code. Three places:
the README, the GitHub "Sponsor" button, and the "À propos" block of Réglages in the app,
which also gets a link to the GitHub repository.

## 2. Why

- The app is free and sideloaded; Ko-fi is the simple way for friends to say thanks.
- A visible link to the repository helps anyone who wants to report a bug or read the code.

## 3. Who

- Primary user: me, as the maintainer.
- Friends who install the APK and open Réglages > À propos.

## 4. What it does

1. README: a Ko-fi button (the standard Ko-fi image linking to my page), in a short "Soutenir" section.
2. `.github/FUNDING.yml` with `ko_fi: O0P52B4FJ1`, which enables the Sponsor button on the repository.
3. Réglages > À propos shows two tappable links below the existing text: "Soutenir sur Ko-fi"
   and "Code source sur GitHub". Each opens the phone's browser on the right page.
4. If no browser is installed, tapping a link does nothing visible beyond a short message; the app never crashes.

## 5. Domain facts

- Ko-fi page: https://ko-fi.com/O0P52B4FJ1
- Repository: https://github.com/bataillec/dried-meats
- "À propos" already exists as the last block of Réglages (version and "application locale" text).

## 6. Constraints

- The app stays fully local: the links only hand a URL to the browser (`ACTION_VIEW`); the app
  itself makes no network request, adds no network permission and no WebView (R20 holds).
- French UI, strings in resources, URLs kept as constants rather than inline.
- Accessibility (N4): touch targets of 48 dp, AA contrast, labels wrap at 200 % font scale,
  announced as links by TalkBack.
- No new dependency, no Room change.

## 7. Non-goals

- In-app donations, payment flows, "rate the app" prompts, or reminders to donate.
- Other support platforms, a changelog link or a privacy-policy page.
- Any analytics on link taps.

## 8. Success looks like

- The README shows the Ko-fi button and it leads to my page.
- The repository shows a Sponsor button pointing to Ko-fi.
- In Réglages > À propos both links are visible, readable at 200 % font scale, and open the browser.
- `mise run check` passes and the docs (requirements, design, assumptions, device checklist) are updated.

## 9. Open questions (each has a default, none blocking)

1. Where in the README? Default: a short "Soutenir" section near the end, not among the badges.
2. Link look in the app? Default: two text-button rows with an icon-free label, full-width tap target.
3. Message when no browser is available? Default: a short toast/snackbar "Aucun navigateur disponible".
4. Show the full URL under each label? Default: no, label only.
5. New requirement id or amend R24-R26? Default: a new requirement under Réglages (R27 and R28 are now taken, so the next free id).
