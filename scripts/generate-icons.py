#!/usr/bin/env python3
"""Builds the Android launcher assets from docs/assets/image.svg (needs Pillow and a `chromium` binary).

The SVG is a 108x108 adaptive-icon drawing: group #adaptive-background and group #adaptive-foreground.
Outputs (all regenerated, never edited by hand):
  - res/drawable/ic_launcher_foreground.xml    vector drawable of the foreground, fitted into the safe zone
  - res/drawable/ic_launcher_monochrome.xml    themed-icon layer (outlines opaque, light fills dropped)
  - res/drawable/ic_notification.xml           status-bar silhouette (cropped to the artwork)
  - res/values/ic_launcher_colors.xml          the background colour taken from the SVG
  - res/mipmap-*dpi/ic_launcher(.round).png    legacy icons, rendered by Chromium from the SVG
  - app/src/main/playstore/ic_launcher-playstore.png   512 px store icon
Run from the repo root:  python3 scripts/generate-icons.py
"""
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SVG = ROOT / "docs/assets/image.svg"
RES = ROOT / "app/src/main/res"
PLAYSTORE = ROOT / "app/src/main/playstore/ic_launcher-playstore.png"
NS = "http://www.w3.org/2000/svg"
ET.register_namespace("", NS)
SAFE_TARGET_RADIUS = 34.0  # dp; the guaranteed-visible disc is 33 dp, a circular mask shows 36 dp
VIEW = 108.0


def q(tag):
    return f"{{{NS}}}{tag}"


# ---------------------------------------------------------------- SVG helpers
def load():
    root = ET.parse(SVG).getroot()
    groups = {g.get("id"): g for g in root.iter(q("g")) if g.get("id")}
    bg = groups["adaptive-background"]
    fg = groups["adaptive-foreground"]
    defs = root.find(q("defs"))
    bg_rect = bg.find(q("rect"))
    return defs, bg_rect.get("fill"), fg


def svg_doc(size_px, inner, defs=None, view=VIEW):
    d = ET.tostring(defs, encoding="unicode") if defs is not None else ""
    return (
        f'<svg xmlns="{NS}" viewBox="0 0 {view} {view}" width="{size_px}" height="{size_px}">{d}{inner}</svg>'
    )


def render(svg_text, px, out):
    with tempfile.TemporaryDirectory() as tmp:
        html = Path(tmp) / "i.html"
        html.write_text(f'<!doctype html><html><body style="margin:0;background:transparent">{svg_text}</body></html>')
        shot = Path(tmp) / "o.png"
        subprocess.run(
            ["chromium", "--headless=new", "--no-sandbox", "--disable-gpu", "--hide-scrollbars",
             "--force-device-scale-factor=1", "--default-background-color=00000000",
             f"--window-size={px},{px}", f"--screenshot={shot}", f"file://{html}"],
            check=True, capture_output=True, timeout=120,
        )
        img = Image.open(shot).convert("RGBA").crop((0, 0, px, px))
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, optimize=True)
    return img


def measure(defs, fg):
    """Renders the foreground alone and returns (bbox, centre, enclosing radius) in 108-unit space."""
    px = 1080
    img = render(svg_doc(px, ET.tostring(fg, encoding="unicode"), defs), px, Path(tempfile.gettempdir()) / "fg-measure.png")
    alpha = img.getchannel("A").point(lambda v: 255 if v > 128 else 0)
    x0, y0, x1, y1 = alpha.getbbox()
    k = VIEW / px
    pts = []
    a = alpha.load()
    for y in range(0, px, 4):
        for x in range(0, px, 4):
            if a[x, y]:
                pts.append((x * k, y * k))
    cx, cy = (x0 + x1) / 2 * k, (y0 + y1) / 2 * k
    for i in range(1, 4000):  # Badoiu-Clarkson minimal enclosing circle
        fx, fy = max(pts, key=lambda p: (p[0] - cx) ** 2 + (p[1] - cy) ** 2)
        cx += (fx - cx) / (i + 1)
        cy += (fy - cy) / (i + 1)
    r = max(((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 for x, y in pts)
    return (x0 * k, y0 * k, x1 * k, y1 * k), (cx, cy), r


# ---------------------------------------------------------------- SVG -> VectorDrawable
def num(v):
    return f"{float(v):g}"


def lum(hexcolor):
    r, g, b = (int(hexcolor[i:i + 2], 16) for i in (1, 3, 5))
    return (0.3 * r + 0.59 * g + 0.11 * b) / 255, r, g, b


def mono_alpha(color, is_stroke):
    """Themed-icon rule: outlines/strokes opaque, dark fills opaque, reds half, light fills dropped."""
    if is_stroke:
        return 1.0
    lm, r, g, _ = lum(color)
    if lm < 0.2:
        return 1.0
    if r > g + 40:
        return 0.6
    return 0.0


def shape_path_data(el):
    t = el.tag.split("}")[1]
    f = lambda k, d="0": float(el.get(k, d))  # noqa: E731
    if t == "path":
        return el.get("d")
    if t == "circle":
        cx, cy, r = f("cx"), f("cy"), f("r")
        return f"M{num(cx - r)},{num(cy)} A{num(r)},{num(r)} 0 1,0 {num(cx + r)},{num(cy)} A{num(r)},{num(r)} 0 1,0 {num(cx - r)},{num(cy)} Z"
    if t == "ellipse":
        cx, cy, rx, ry = f("cx"), f("cy"), f("rx"), f("ry")
        return f"M{num(cx - rx)},{num(cy)} A{num(rx)},{num(ry)} 0 1,0 {num(cx + rx)},{num(cy)} A{num(rx)},{num(ry)} 0 1,0 {num(cx - rx)},{num(cy)} Z"
    if t == "rect":
        x, y, w, h = f("x"), f("y"), f("width"), f("height")
        rx = f("rx", el.get("ry", "0"))
        if rx <= 0:
            return f"M{num(x)},{num(y)} H{num(x + w)} V{num(y + h)} H{num(x)} Z"
        return (f"M{num(x + rx)},{num(y)} H{num(x + w - rx)} A{num(rx)},{num(rx)} 0 0,1 {num(x + w)},{num(y + rx)} "
                f"V{num(y + h - rx)} A{num(rx)},{num(rx)} 0 0,1 {num(x + w - rx)},{num(y + h)} H{num(x + rx)} "
                f"A{num(rx)},{num(rx)} 0 0,1 {num(x)},{num(y + h - rx)} V{num(y + rx)} A{num(rx)},{num(rx)} 0 0,1 {num(x + rx)},{num(y)} Z")
    if t == "polygon":
        nums = re.findall(r"-?[\d.]+", el.get("points"))
        pts = [f"{nums[i]},{nums[i + 1]}" for i in range(0, len(nums), 2)]
        return "M" + " L".join(pts) + " Z"
    if t == "line":
        return f"M{num(f('x1'))},{num(f('y1'))} L{num(f('x2'))},{num(f('y2'))}"
    return None


def group_attrs(el):
    tr = el.get("transform", "")
    attrs = []
    for name, args in re.findall(r"(\w+)\(([^)]*)\)", tr):
        v = [float(x) for x in re.findall(r"-?[\d.]+", args)]
        if name == "translate":
            attrs += [f'android:translateX="{num(v[0])}"', f'android:translateY="{num(v[1] if len(v) > 1 else 0)}"']
        elif name == "rotate":
            attrs.append(f'android:rotation="{num(v[0])}"')
            if len(v) == 3:
                attrs += [f'android:pivotX="{num(v[1])}"', f'android:pivotY="{num(v[2])}"']
        else:
            raise ValueError(f"unsupported transform {name}")
    return attrs


def vd_elements(el, indent, mono, floor=0.0):
    pad = "    " * indent
    out = []
    for child in el:
        tag = child.tag.split("}")[1]
        if tag == "g":
            attrs = " ".join(group_attrs(child))
            out.append(f"{pad}<group {attrs}>" if attrs else f"{pad}<group>")
            out += vd_elements(child, indent + 1, mono, floor)
            out.append(f"{pad}</group>")
            continue
        data = shape_path_data(child)
        if data is None:
            continue
        opacity = float(child.get("opacity", "1"))
        fill = child.get("fill")  # SVG would paint a missing fill black; here that only occurs on line-only paths
        stroke = child.get("stroke")
        a = [f'android:pathData="{" ".join(data.split())}"']
        if fill and fill != "none":
            if mono:
                al = max(mono_alpha(fill, False), 0.0)
                al = max(al, floor) if al > 0 else 0.0
                if al > 0:
                    a += ['android:fillColor="#FFFFFF"', f'android:fillAlpha="{num(al * opacity)}"']
            else:
                a.append(f'android:fillColor="{fill}"')
                if opacity != 1:
                    a.append(f'android:fillAlpha="{num(opacity)}"')
        if stroke and stroke != "none":
            a += [f'android:strokeColor="{"#FFFFFF" if mono else stroke}"',
                  f'android:strokeWidth="{num(child.get("stroke-width", "1"))}"']
            if child.get("stroke-linecap"):
                a.append(f'android:strokeLineCap="{child.get("stroke-linecap")}"')
            if child.get("stroke-linejoin"):
                a.append(f'android:strokeLineJoin="{child.get("stroke-linejoin")}"')
            if opacity != 1:
                a.append(f'android:strokeAlpha="{num(opacity)}"')
        if len(a) == 1:  # nothing visible
            continue
        out.append(f"{pad}<path " + ("\n" + pad + "    ").join(a) + " />")
    return out


def vector_drawable(fg, fit, mono=False, size_dp=108, viewport=None, floor=0.0):
    """fit = (translateX, translateY, scale, pivotX, pivotY) applied as one wrapping group."""
    tx, ty, s, px_, py_ = fit
    vx0, vy0, vw, vh = viewport or (0, 0, VIEW, VIEW)
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!-- Generated by scripts/generate-icons.py from docs/assets/image.svg. Do not edit by hand. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{num(size_dp)}dp"',
        f'    android:height="{num(size_dp * vh / vw)}dp"',
        f'    android:viewportWidth="{num(vw)}"',
        f'    android:viewportHeight="{num(vh)}">',
    ]
    if viewport:
        lines.append(f'    <group android:translateX="{num(-vx0)}" android:translateY="{num(-vy0)}">')
    lines.append(
        f'    <group android:pivotX="{num(px_)}" android:pivotY="{num(py_)}" android:scaleX="{num(s)}" '
        f'android:scaleY="{num(s)}" android:translateX="{num(tx)}" android:translateY="{num(ty)}">'
    )
    lines += vd_elements(fg, 2, mono, floor)
    lines.append("    </group>")
    if viewport:
        lines.append("    </group>")
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    print("wrote", path.relative_to(ROOT))


def main():
    if not shutil.which("chromium"):
        raise SystemExit("chromium is required to render the PNGs")
    defs, bg_color, fg = load()
    bbox, (cx, cy), r = measure(defs, fg)
    s = SAFE_TARGET_RADIUS / r
    fit = (VIEW / 2 - cx, VIEW / 2 - cy, s, cx, cy)
    print(f"artwork bbox {tuple(round(v, 1) for v in bbox)}, enclosing radius {r:.1f} dp, fit scale x{s:.2f}")

    write(RES / "values/ic_launcher_colors.xml",
          '<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated by scripts/generate-icons.py -->\n<resources>\n'
          f'    <color name="ic_launcher_background">{bg_color.upper()}</color>\n</resources>\n')
    write(RES / "drawable/ic_launcher_foreground.xml", vector_drawable(fg, fit))
    write(RES / "drawable/ic_launcher_monochrome.xml", vector_drawable(fg, fit, mono=True))
    # Status-bar icon: 24 dp, viewport cropped to the artwork, light fills kept at >= 60 % so it reads solid.
    pad = 2.0
    vp = (bbox[0] - pad, bbox[1] - pad, bbox[2] - bbox[0] + 2 * pad, bbox[3] - bbox[1] + 2 * pad)
    side = max(vp[2], vp[3])
    vp = (vp[0] - (side - vp[2]) / 2, vp[1] - (side - vp[3]) / 2, side, side)
    write(RES / "drawable/ic_notification.xml",
          vector_drawable(fg, (0, 0, 1, 0, 0), mono=True, size_dp=24, viewport=vp, floor=0.6))

    fg_inner = ET.tostring(fg, encoding="unicode")
    wrap = f'<g transform="translate({VIEW / 2 - cx:.3f} {VIEW / 2 - cy:.3f}) translate({cx:.3f} {cy:.3f}) scale({s:.4f}) translate({-cx:.3f} {-cy:.3f})">{fg_inner}</g>'
    bg_full = f'<rect width="{VIEW}" height="{VIEW}" fill="{bg_color}"/>'
    render(svg_doc(512, bg_full + wrap, defs), 512, PLAYSTORE)
    print("wrote", PLAYSTORE.relative_to(ROOT))

    bw, bh = bbox[2] - bbox[0], bbox[3] - bbox[1]
    for folder, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        for round_icon in (False, True):
            fill = 0.60 if round_icon else 0.70  # share of the icon edge taken by the artwork
            k = VIEW * fill / max(bw, bh)
            shape = (f'<circle cx="54" cy="54" r="54" fill="{bg_color}"/>' if round_icon
                     else f'<rect width="108" height="108" rx="24" fill="{bg_color}"/>')
            art = (f'<g transform="translate(54 54) scale({k:.4f}) translate({-(bbox[0] + bbox[2]) / 2:.3f} '
                   f'{-(bbox[1] + bbox[3]) / 2:.3f})">{fg_inner}</g>')
            name = "ic_launcher_round.png" if round_icon else "ic_launcher.png"
            render(svg_doc(px, shape + art, defs), px, RES / f"mipmap-{folder}" / name)
            print("wrote", (RES / f"mipmap-{folder}" / name).relative_to(ROOT), (px, px))


if __name__ == "__main__":
    main()
