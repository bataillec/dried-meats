# docs/assets

- `icon-sheet.jpeg`: the owner's launcher icon sheet (vacuum bag, steak, stopwatch). Source of all launcher assets.
- Regenerate the assets with `python3 scripts/generate-icons.py` (needs Pillow). The script crops the dark
  "maskable" square (the cleanest, flat background), separates the artwork from its background, scales it so
  everything stays inside the 66 % adaptive-icon safe zone, and writes: adaptive foreground and monochrome
  layers (`res/drawable-nodpi`), legacy square and round PNGs (`res/mipmap-*dpi`), the notification
  silhouette (`res/drawable-*dpi/ic_notification.png`) and the 512 px Play Store icon
  (`app/src/main/playstore/ic_launcher-playstore.png`).
- The source is only about 200 px per icon, so the large layers are an upscale: the foreground is soft at xxxhdpi.
  Replace the sheet with a higher-resolution original (same layout) or vector art when available and rerun the script.
