# intent — Decouple the meat type from the recipe (issue #5)

Status: v0.2, validated by the owner (2026-11-23). Extends `docs/intent.md` (v0.3); where the two
differ, this file wins for the points below once validated. Design and requirements updates follow in the next step.

## 1. What I want

A recipe is a method: how much salt, sugar and pepper per kg, the vacuum rule, the minimum fridge time.
It can be applied to any meat. The meat type is a property of the physical piece I am preparing.

So when I start a piece I choose **a recipe and a meat type**, independently. The same recipe can serve
a beef piece today and a duck breast tomorrow, without duplicating it per meat.

```
meat type 1 ── * piece * ── 1 recipe
```

## 2. Why

- Today a recipe belongs to exactly one meat type, so I must duplicate "my usual dosage" once per meat.
- A piece's meat is only known indirectly (piece → recipe → meat), which is conceptually wrong:
  the meat is what I put in the vacuum bag, not a property of the dosage.
- Statistics (intent §4.5, "compare across pieces of the same meat type") are about the piece's meat, and
  should stay correct even if the recipe is shared.

## 3. Who

Same as `intent.md`: me first, possibly friends later, all data local. Release v0.1.0 is already installed
on my phone, so **existing data must survive** the change.

## 4. Behaviour I expect

1. **Recipes have no meat type.** I can create, edit, duplicate and archive a recipe without picking a meat.
   The recipes list is no longer grouped by meat.
2. **A piece has both.** "Nouvelle pièce" asks for a recipe *and* a meat type. Both are required.
   The suggested label ("Bœuf 1", "Bœuf 2"…) follows the **meat chosen for the piece**.
3. **Both are shown everywhere a piece is shown**: pieces list, piece detail, statistics (per meat type and
   recipe), and the reminder notification, taken from the piece.
4. **Meat types stay manageable** (add, rename, delete; beef, pork, duck seeded), from **Réglages** (moved from the Recettes screen). A meat type used by at
   least one piece cannot be deleted (it used to be "used by a recipe"). The message says "pièces".
5. **Pickers for recipes no longer show a meat name** (calculator, settings): a recipe is just its name.
6. **Nothing is lost on update.** Every existing piece keeps the meat type it had through its recipe.
   A backup made with the current version still imports, with each piece taking its recipe's old meat.
7. Doses, durations, stage rules and reminders are **unchanged** (out of scope).

## 5. Constraints

- Local only, French UI, strings in resources; no new permission, no new dependency.
- The database moves to a new version with an explicit migration (no destructive fallback). The
  one-open-run-per-piece guarantee and all indices keep working.
- The backup file format gets a new version; version 1 files remain readable.

## 6. Non-goals

- Restricting which recipes are offered for which meat (no compatibility rules).
- Any change to dose maths, stage durations, reminder logic.

## 7. Success looks like

- I can create one recipe and use it for a beef piece and a duck piece.
- After updating the app, all my pieces show the same meat as before; reminders still fire.
- An old backup file restores correctly; a new backup restores identically.
- I cannot delete a meat type that a piece uses.
- `mise run check` passes.

## 8. Decisions on the open questions (owner, 2026-11-23)

1. Meat-type management moves from Recettes to **Réglages**.
2. Notification text keeps "meat · recipe", now taken from the piece.
3. Archived meat types are hidden in the new-piece picker; no new archiving UI.
