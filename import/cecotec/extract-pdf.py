"""Price list (export Excel en PDF) -> rows.json + images/ (miniatures de la colonne FOTO)."""
import json, re, sys, uuid
from pathlib import Path
import pymupdf

PDF, OUT = Path(sys.argv[1]), Path(sys.argv[2])
(OUT / "images").mkdir(parents=True, exist_ok=True)
NS = uuid.UUID("0b4d3a52-7c1e-4e8a-9f3d-2c5e00000002")

# bornes des colonnes (x) d'après l'en-tête
COLS = [("foto", 0, 62), ("sku", 62, 155), ("ean", 155, 242), ("family", 242, 330), ("subfamily", 330, 492),
        ("name", 492, 682), ("strategy", 682, 752), ("type", 752, 812), ("fca", 812, 905),
        ("msrp1", 905, 990), ("msrp2", 990, 1050), ("msrp3", 1050, 1108), ("msrp4", 1108, 1200)]


def colof(x):
    for n, a, b in COLS:
        if a <= x < b:
            return n


def clipped_cells(page, yc):
    """Texte tel qu'affiché (coupé aux bords des cellules) pour une ligne donnée."""
    cells = {}
    for x0, y0, x1, y1, w, *_ in page.get_text("words"):
        if abs((y0 + y1) / 2 - yc) < 4 and colof((x0 + x1) / 2):
            cells.setdefault(colof((x0 + x1) / 2), []).append(w)
    return {k: " ".join(v) for k, v in cells.items()}


doc = pymupdf.open(PDF)
rows, seen_img = [], {}
for pno in range(len(doc)):
    page = doc[pno]
    # flags=0 : on récupère aussi le texte qui déborde de la cellule (masqué à l'affichage par Excel)
    spans = [sp for b in page.get_text("dict", flags=0)["blocks"] if b["type"] == 0
             for l in b["lines"] for sp in l["spans"] if sp["text"].strip()]
    lines = {}
    for sp in spans:
        x0, y0, x1, y1 = sp["bbox"]
        yc = (y0 + y1) / 2
        key = next((k for k in lines if abs(k - yc) < 4), yc)
        lines.setdefault(key, []).append((x0, x1, sp["text"].strip()))
    imgs = [(im["xref"], im["bbox"]) for im in page.get_image_info(xrefs=True) if im["bbox"][2] < 70]
    for yc in sorted(lines):
        cells = {}
        for x0, x1, w in sorted(lines[yc]):
            c = colof((x0 + x1) / 2)
            if c:
                cells.setdefault(c, []).append(w)
        cells = {k: " ".join(v) for k, v in cells.items()}
        if cells.get("sku") == "SKU" or "msrp4" not in cells:
            continue
        if "name" not in cells:  # libellé fusionné avec la sous-famille : on prend le texte affiché
            clipped = clipped_cells(page, yc)
            print(f"  p{pno+1} {cells.get('sku')}: nom repris du texte affiché -> {clipped.get('name')!r}")
            cells["name"] = clipped.get("name", "")
            if clipped.get("subfamily"):
                cells["subfamily"] = clipped["subfamily"]
        # cellules voisines fusionnées par le PDF : « …Healthy BlackBest Value », « accessoriesPack of… »
        merged = (cells.get("name", "") + " " + cells.pop("strategy", "")).strip()
        m = re.fullmatch(r"(.*?)\s*(Best Performance|Best Value|FlagShip|Entry|Performance)", merged)
        cells["name"], cells["strategy"] = (m.group(1), m.group(2)) if m else (merged, None)
        if "subfamily" not in cells:
            clipped = clipped_cells(page, yc)
            sub = clipped.get("subfamily")
            if sub and cells["name"].startswith(sub):
                cells["subfamily"], cells["name"] = sub, cells["name"][len(sub):].strip()
        r = dict(page=pno + 1, y=round(yc, 1), **cells)
        hit = [x for x, bb in imgs if bb[1] - 6 <= yc <= bb[3] + 6]
        if hit:
            xref = hit[0]
            if xref not in seen_img:
                ext = doc.extract_image(xref)
                pix = pymupdf.Pixmap(doc, xref)
                if pix.alpha or pix.n > 3:  # PNG avec transparence/CMJN -> RVB sur fond blanc
                    pix = pymupdf.Pixmap(pymupdf.csRGB, pix) if pix.n - pix.alpha > 3 else pix
                    if pix.alpha:
                        pix = pymupdf.Pixmap(pix, 0)
                name = f"{uuid.uuid5(NS, str(xref))}.jpg"
                (OUT / "images" / name).write_bytes(pix.tobytes("jpeg", jpg_quality=90))
                seen_img[xref] = dict(file=name, w=pix.width, h=pix.height)
            r["image"] = seen_img[xref]
        rows.append(r)

(OUT / "rows.json").write_text(json.dumps(rows, ensure_ascii=False, indent=0))
print(len(rows), "lignes,", sum(1 for r in rows if "image" in r), "avec photo,", len(seen_img), "photos distinctes")
