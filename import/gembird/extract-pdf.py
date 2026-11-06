"""Extrait les fiches produits du catalogue GEMBIRD/SBC (PDF) -> raw.json + images/."""
import json, re, sys, uuid
from pathlib import Path
import pymupdf

PDF, OUT = Path(sys.argv[1]), Path(sys.argv[2])
IMG_DIR = OUT / "images"
IMG_DIR.mkdir(parents=True, exist_ok=True)

# Univers du sommaire (page 2) : numéro -> (nom, nombre de références annoncé)
UNIVERS = {
    "01": ("Adaptateurs & convertisseurs", 31),
    "02": ("Câbles vidéo", 8),
    "03": ("Hubs USB, cartes & acquisition", 19),
    "04": ("Supports TV, meubles & projecteurs", 19),
    "05": ("Bras & supports d'écran, PC portable", 14),
    "06": ("Souris & microphones", 8),
    "07": ("Entretien", 3),
}
NS = uuid.UUID("6f1c2a4e-0b7d-4f43-9a51-gembird00000".replace("gembird00000", "5b1d00000000"))

doc = pymupdf.open(PDF)
products, warnings = [], []
section = None

for pno in range(2, len(doc)):
    page = doc[pno]
    lines = []
    for b in page.get_text("dict")["blocks"]:
        if b["type"] != 0:
            continue
        for l in b["lines"]:
            t = " ".join(s["text"] for s in l["spans"]).strip()
            if t:
                x0, y0, x1, y1 = l["bbox"]
                lines.append(dict(x0=x0, y0=y0, x1=x1, y1=y1, t=t, size=round(l["spans"][0]["size"], 1)))

    # Univers de la page : bandeau « 0 1 · A D A P T… » ou grand numéro de l'en-tête d'univers
    for l in lines:
        m = re.match(r"^(\d)\s?(\d)\s+·", l["t"])
        if m and l["y0"] < 90:
            section = m.group(1) + m.group(2)
        if re.fullmatch(r"0[1-7]", l["t"]) and l["size"] > 30:
            section = l["t"]

    col = lambda x: 0 if x < 298 else 1
    stock_lines = [l for l in lines if re.fullmatch(r"\d+ pcs", l["t"])]
    cards = []
    for s in stock_lines:
        sku = [l for l in lines if abs(l["y0"] - s["y0"]) < 3 and col(l["x0"]) == col(s["x0"]) and l["x0"] < s["x0"]
               and re.fullmatch(r"[A-Z0-9][A-Z0-9-]+", l["t"])]
        if len(sku) != 1:
            warnings.append(f"p{pno+1}: référence introuvable près de « {s['t']} »")
            continue
        cards.append(dict(col=col(s["x0"]), top=s["y0"], sku=sku[0]["t"], stock=int(s["t"].split()[0])))

    images = {}
    for im in page.get_image_info(xrefs=True):
        x0, y0, x1, y1 = im["bbox"]
        if im["width"] >= 600 and (y1 - y0) > 100 and (x1 - x0) < 300:  # photos de fiche (pas logos, pas visuel d'univers)
            images[im["xref"]] = (x0, y0, x1, y1)

    for c in cards:
        others = [d["top"] for d in cards if d["col"] == c["col"] and d["top"] > c["top"]]
        bottom = min(others) if others else 812
        inside = sorted([l for l in lines if col(l["x0"]) == c["col"] and c["top"] - 2 <= l["y0"] < bottom - 5],
                        key=lambda l: (round(l["y0"]), l["x0"]))
        name = " ".join(l["t"] for l in inside if l["size"] == 9.6)
        ean_label = next((l for l in inside if l["t"].replace(" ", "") == "EAN"), None)
        name_y = min((l["y0"] for l in inside if l["size"] == 9.6), default=None)
        desc = " ".join(l["t"] for l in inside if l["size"] == 7.0 and name_y and ean_label
                        and name_y < l["y0"] < ean_label["y0"])
        ean = next((l["t"] for l in inside if re.fullmatch(r"\d{8,14}", l["t"])), None)
        price_line = next((l for l in inside if l["size"] == 17.0 and re.fullmatch(r"[\d\s]+,\d{3}", l["t"])), None)
        price = price_line["t"].replace(" ", "").replace(",", ".") if price_line else None

        img = [x for x, (x0, y0, x1, y1) in images.items()
               if col(x0) == c["col"] and c["top"] - 20 <= y0 <= c["top"] + 10]
        image_file = None
        if img:
            ext = doc.extract_image(img[0])
            suffix = {"jpeg": "jpg", "jpg": "jpg", "png": "png"}.get(ext["ext"], ext["ext"])
            image_file = f"{uuid.uuid5(NS, c['sku'])}.{suffix}"
            (IMG_DIR / image_file).write_bytes(ext["image"])
        for field, val in (("nom", name), ("EAN", ean), ("prix", price), ("photo", image_file)):
            if not val:
                warnings.append(f"p{pno+1} {c['sku']}: {field} manquant")

        products.append(dict(page=pno + 1, univers=section, sku=c["sku"], name=name, description=desc or None,
                             ean=ean, priceHt=price, stock=c["stock"], image=image_file))

(OUT / "raw.json").write_text(json.dumps({"univers": UNIVERS, "products": products}, ensure_ascii=False, indent=1))

# --- Contrôles avec les totaux annoncés dans le PDF ---
print(f"{len(products)} produits extraits (catalogue : 102), {sum(p['stock'] for p in products)} pièces (catalogue : 4 088)")
for num, (nom, attendu) in UNIVERS.items():
    n = sum(1 for p in products if p["univers"] == num)
    print(f"  {num} {nom:<40} {n:>3} / {attendu}{'' if n == attendu else '   <-- ÉCART'}")
dups = {p["sku"] for p in products if sum(q["sku"] == p["sku"] for q in products) > 1}
print("Références en double :", dups or "aucune")
eans = [p["ean"] for p in products if p["ean"]]
print("EAN en double :", {e for e in eans if eans.count(e) > 1} or "aucun")
print("Avertissements :", warnings or "aucun")
