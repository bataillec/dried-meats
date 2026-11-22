# intent — Couleur principale de l'interface (issue #6)

Status: draft v0.1 (2026-11-22), waiting for the owner's validation. Extends R24 (themes).

## 1. What I want

Choose the dominant colour of the app myself, from Réglages, next to the theme. Today the
app is always dried-meat red-brown; I want to pick another colour and see the whole UI
change immediately, in every theme (Clair, Sombre, Sombre OLED, Système).

## 2. Why

- The app is mine (and maybe friends' later): a bit of personalisation makes it feel like it is.
- Cheap to do now that themes already exist, as long as readability does not suffer.

## 3. Who

- Primary user: me. Possibly friends who install the same APK, each with their own choice.

## 4. What it does

1. Réglages shows a "Couleur principale" section under "Thème": a row of swatches.
2. Picking one applies at once to the whole app and is kept after a restart.
3. It works with all four theme modes. Sombre OLED keeps its pure black background;
   only the accent changes. "Système" still maps to Clair or Sombre, never OLED.
4. The default is today's palette, so nobody sees a change after updating.
5. The selected swatch is shown by more than colour (check mark and a spoken label).

## 5. Domain facts

- A small curated set of swatches (the current red as default, plus about 4 to 6 others),
  each hand-tuned for light, dark and OLED, rather than a free colour picker.
- Alert orange and the sous-vide/frigo chips are semantic colours: they do not follow the
  accent and must stay distinguishable from it.
- The setting is local and not exported with the data (like the other settings, assumption #10).

## 6. Constraints

- French UI, strings in resources; swatch targets of 48 dp; usable at 200 % font scale.
- AA contrast (N4) for every accent in every mode: 4.5:1 for text, 3:1 for UI components,
  checked by unit tests so a later palette change cannot regress it.
- No new permission, no new dependency, no Room change (so no migration).

## 7. Non-goals

- Free colour picker, per-screen colours, custom background colours, font changes.
- Material You / "Couleur du système" option: possible follow-up, not in this change.

## 8. Success looks like

- I pick a colour and the whole app follows immediately, in all four modes.
- After a restart the choice is still there; after an update from v0.1.0 nothing looks different.
- Every accent and mode pair passes the AA contrast tests.

## 9. Open questions (each has a default, none blocking)

1. How many swatches? Default: 6 in total (current red plus 5: green, blue, purple, amber, grey-brown).
2. Accent also tints secondary/tertiary roles? Default: yes, derived per accent, so chips and
   toggles do not stay red.
3. Name of the default swatch? Default: "Viande séchée".
4. Material You option now or later? Default: later, separate issue.
5. Swatch layout when a row does not fit (large font)? Default: wrap onto a second line.
