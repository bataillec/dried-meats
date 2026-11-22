# docs/assets

- `image.svg`: the launcher artwork as vector (108x108 adaptive-icon drawing with `#adaptive-background` and
  `#adaptive-foreground` groups). **Source of truth for every icon asset.**
- `icon-sheet.jpeg`: the original low-resolution icon sheet the SVG replaces (kept for reference only).

Regenerate everything with `python3 scripts/generate-icons.py` (needs Pillow and a `chromium` binary). It:
- converts the foreground group to Android vector drawables: `res/drawable/ic_launcher_foreground.xml`,
  `ic_launcher_monochrome.xml` (themed icons: outlines opaque, light fills dropped) and `ic_notification.xml`
  (status bar, cropped to the artwork). The SVG drop shadow filter has no VectorDrawable equivalent and is not
  carried over; supported shapes are path, circle, ellipse, rect, polygon, line, `g` with `translate`/`rotate`.
- scales and centres the artwork so it stays inside the adaptive-icon safe zone (enclosing radius 34 dp of 54);
- writes the background colour to `res/values/ic_launcher_colors.xml`;
- renders legacy square and round PNGs (`res/mipmap-*dpi`) and the 512 px Play Store icon
  (`app/src/main/playstore/ic_launcher-playstore.png`) with headless Chromium, shadow included.
The adaptive icons themselves are `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`.
