#!/usr/bin/env python3
"""Builds the Android launcher assets from docs/assets/icon-sheet.jpeg (needs Pillow).

Source: the dark "maskable" square of the sheet (cleanest, artwork on a flat background), ~193 px.
Steps: crop -> key the flat background out to get the artwork with soft alpha -> scale it so that
every visible pixel lies inside the adaptive-icon safe circle (66 % of the 108 dp canvas) -> render
adaptive layers, themed (monochrome) layer, legacy square/round PNGs, notification icon and the
512 px Play Store icon. Run from the repo root:  python3 scripts/generate-icons.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
SHEET = ROOT / "docs/assets/icon-sheet.jpeg"
RES = ROOT / "app/src/main/res"
PLAYSTORE = ROOT / "app/src/main/playstore/ic_launcher-playstore.png"

CROP = (864, 407, 864 + 193, 407 + 193)  # maskable square on the sheet
BG = (0x2C, 0x35, 0x3F)  # sampled flat background of that square
CANVAS = 432  # 108 dp at xxxhdpi
SAFE_RADIUS = CANVAS * 0.33  # 66 % safe zone, as a radius
LO, HI = 14, 70  # colour distance from BG mapped to alpha 0..1


def key_out_background(img: Image.Image) -> Image.Image:
    """Returns RGBA artwork: alpha from the distance to the flat background, colours un-premultiplied."""
    px = img.load()
    out = Image.new("RGBA", img.size)
    po = out.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b = px[x, y]
            d = max(abs(r - BG[0]), abs(g - BG[1]), abs(b - BG[2]))
            a = max(0.0, min(1.0, (d - LO) / (HI - LO)))
            if a <= 0:
                po[x, y] = (0, 0, 0, 0)
                continue
            c = [max(0, min(255, round((v - (1 - a) * bg) / a))) for v, bg in zip((r, g, b), BG)]
            po[x, y] = (c[0], c[1], c[2], round(a * 255))
    return out


def clear_border(img: Image.Image, margin: int) -> None:
    """The crop edge carries JPEG/shadow residue; the artwork itself sits well inside."""
    alpha = img.getchannel("A")
    d = ImageDraw.Draw(alpha)
    w, h = img.size
    d.rectangle((0, 0, w, margin - 1), fill=0)
    d.rectangle((0, h - margin, w, h), fill=0)
    d.rectangle((0, 0, margin - 1, h), fill=0)
    d.rectangle((w - margin, 0, w, h), fill=0)
    img.putalpha(alpha)


def alpha_bbox_and_radius(art: Image.Image):
    alpha = art.getchannel("A").point(lambda v: 255 if v > 80 else 0)
    x0, y0, x1, y1 = alpha.getbbox()
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    px = alpha.load()
    max_r = 0.0
    for y in range(art.height):
        for x in range(art.width):
            if px[x, y]:
                max_r = max(max_r, ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5)
    return (x0, y0, x1, y1), (cx, cy), max_r


def place(art, center, scale, size):
    """Resizes the artwork by [scale] and centres its bbox centre on a transparent [size] canvas."""
    w, h = round(art.width * scale), round(art.height * scale)
    big = art.convert("RGBa").resize((w, h), Image.LANCZOS).convert("RGBA")
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(big, (round(size / 2 - center[0] * scale), round(size / 2 - center[1] * scale)))
    return canvas


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, optimize=True)
    print("wrote", path.relative_to(ROOT), img.size)


def main():
    sheet = Image.open(SHEET).convert("RGB")
    art = key_out_background(sheet.crop(CROP))
    clear_border(art, 16)
    bbox, center, max_r = alpha_bbox_and_radius(art)
    scale = SAFE_RADIUS / max_r  # fills the safe circle exactly
    print(f"art bbox {bbox}, farthest pixel {max_r:.1f}px, adaptive scale x{scale:.2f}")

    foreground = place(art, center, scale, CANVAS)
    save(foreground, RES / "drawable-nodpi/ic_launcher_foreground.png")

    # Themed icon: white artwork whose opacity follows brightness, so details survive tinting.
    mono = Image.new("RGBA", foreground.size, (255, 255, 255, 0))
    fp, mp = foreground.load(), mono.load()
    for y in range(CANVAS):
        for x in range(CANVAS):
            r, g, b, a = fp[x, y]
            if a:
                lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
                mp[x, y] = (255, 255, 255, round(a * min(1.0, 0.45 + 0.9 * lum)))
    save(mono, RES / "drawable-nodpi/ic_launcher_monochrome.png")

    bbox_w = bbox[2] - bbox[0]
    bbox_h = bbox[3] - bbox[1]
    for folder, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        ss = 4  # supersample the masks for smooth edges
        for round_icon in (False, True):
            big = px * ss
            base = Image.new("RGBA", (big, big), (0, 0, 0, 0))
            mask = Image.new("L", (big, big), 0)
            d = ImageDraw.Draw(mask)
            if round_icon:
                d.ellipse((0, 0, big - 1, big - 1), fill=255)
            else:
                d.rounded_rectangle((0, 0, big - 1, big - 1), radius=round(big * 0.22), fill=255)
            base.paste(Image.new("RGBA", (big, big), BG + (255,)), (0, 0), mask)
            fill = 0.62 if round_icon else 0.72
            s = big * fill / max(bbox_w, bbox_h)
            base.alpha_composite(place(art, center, s, big))
            base.putalpha(Image.composite(base.getchannel("A"), Image.new("L", (big, big), 0), mask))
            icon = base.resize((px, px), Image.LANCZOS)
            name = "ic_launcher_round.png" if round_icon else "ic_launcher.png"
            save(icon, RES / f"mipmap-{folder}" / name)

    # Notification small icon: white silhouette of the artwork (Android only uses the alpha).
    for folder, px in {"mdpi": 24, "hdpi": 36, "xhdpi": 48, "xxhdpi": 72, "xxxhdpi": 96}.items():
        sil = art.copy()
        sil.putalpha(art.getchannel("A").point(lambda v: 255 if v > 90 else 0))
        s = px * 0.92 / max(bbox_w, bbox_h)
        icon = place(sil, center, s, px)
        white = Image.new("RGBA", icon.size, (255, 255, 255, 0))
        white.putalpha(icon.getchannel("A"))
        save(white, RES / f"drawable-{folder}" / "ic_notification.png")

    # Play Store: full-bleed square (Play applies its own mask), same composition as the adaptive icon.
    store = Image.new("RGBA", (CANVAS, CANVAS), BG + (255,))
    store.alpha_composite(foreground)
    save(store.resize((512, 512), Image.LANCZOS), PLAYSTORE)


if __name__ == "__main__":
    main()
