# intent — Formule et exemple sous « Durée du sous-vide » (issue #8)

Status: validated by the owner (2026-11-23). Answers to §6 are below; Réglages is aligned on 6 cm.

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

In scope by decision (§6.3): the Réglages example (R25) also moves from 9 cm to 6 cm, so
both screens share one constant and show the same example.

## 5. Success looks like

- « Nouvelle recette » shows « Ø 6 cm ÷ 2 + 1 = 4 → 4 jours » under « Jours ajoutés ».
- Editing the fields updates it live; bad input never shows a wrong number.
- Readable at 200 % font and with TalkBack; `mise run check` passes.

## 6. Decisions on the open questions (owner, 2026-11-23)

1. **Invalid or empty field**: hide the formula and the example. Divisor ≤ 0 shows the invalid hint.
2. **Requirement id**: a new requirement (R28), referencing R7 and R25.
3. **Example diameter**: aligned, 6 cm in the recipe form and in Réglages.
4. **TalkBack**: not a live region; read on focus.
5. **Formula values**: the user's current values, shown only when valid.
