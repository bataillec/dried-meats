# intent — Formule et exemple sous « Durée du sous-vide » (issue #8)

Status: draft, awaiting owner validation.

## 1. What I want

In the recipe form, the « Durée du sous-vide » section shows, right under the two fields
« Diviseur » and « Jours ajoutés », a short label that:

1. writes the formula with the values currently typed
   (« Durée = ⌈ Ø ÷ 2 + 1 ⌉ jours, arrondie au jour supérieur »);
2. gives a worked example for a fixed 6 cm diameter
   (« Exemple : Ø 6 cm ÷ 2 + 1 = 4 → 4 jours »).

It updates on every keystroke.

## 2. Why

The two bare number fields do not say how they combine, so I cannot tell what to enter.
The Réglages screen already explains the same rule with a live example (R25); the recipe
form, where the values are first chosen, does not.

## 3. Behaviour

- Both fields valid: full formula and example, live.
- A field empty, not a number, or divisor ≤ 0: no example, never « 0 », « NaN » or a stale
  value, and no crash. Divisor ≤ 0 shows « Saisissez un diviseur supérieur à 0 pour voir
  l'exemple. ».
- Raw result below 1: the raw value is shown, the final result is 1 jour (existing floor).
- Maths come from the domain (`StageDurationRule`); the UI only formats. French number
  format (comma), « 1 jour » / « 4 jours ».
- Purely informational: never blocks saving, existing error states unchanged.
- Accessible: wraps at 200 % font without truncation or overlap, `bodyMedium` in
  `onSurfaceVariant` (AA in Clair / Sombre / OLED), and a spoken sentence for TalkBack
  because ⌈ ⌉ ÷ Ø read poorly.

## 4. Out of scope

- Choosing the example diameter, changing the formula or its defaults, touching existing
  pieces (their durations stay snapshotted, R6).

## 5. Success looks like

- « Nouvelle recette » shows « Ø 6 cm ÷ 2 + 1 = 4 → 4 jours » under « Jours ajoutés ».
- Editing the fields updates it live; bad input never shows a wrong number.
- Readable at 200 % font and with TalkBack; `mise run check` passes.

## 6. Open questions (each with a default)

1. **Invalid empty field**: hide the label entirely, or show a neutral hint?
   Default: hide the formula and example; only divisor ≤ 0 shows the invalid hint.
2. **Requirement id**: new R-id, or extend R7/R25? Default: a new requirement (next R-id)
   that references R7 and R25.
3. **Example diameter**: the Réglages example uses 9 cm, this one 6 cm. Keep them different
   or align? Default: keep them different (as the issue says) and record it in
   `docs/assumptions.md`.
4. **TalkBack announcements**: polite live region or not? Default: not live (read on
   focus), to avoid chatter on each keystroke.
5. **Formula in the label**: show the user's current values (« ÷ 1,5 + 0,5 ») only when
   valid, or the generic « Ø ÷ diviseur + jours ajoutés » otherwise? Default: current
   values only when valid, otherwise nothing (see question 1).
