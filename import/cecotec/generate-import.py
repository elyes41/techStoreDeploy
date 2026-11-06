"""rows.json (price list Cecotec) -> import-cecotec.js (mongosh) + categories.json / products.json (Extended JSON)."""
import collections, hashlib, json, re, sys, unicodedata
from decimal import Decimal
from pathlib import Path

RAW, OUT = Path(sys.argv[1]), Path(sys.argv[2])
OUT.mkdir(parents=True, exist_ok=True)
rows = json.loads(RAW.read_text())
NOW = "2026-11-06T00:00:00Z"
RATE = Decimal(4)  # 1 € = 4 DT

FAMILIES = {  # famille du fichier -> catégorie de la boutique
    "MDA": "Gros électroménager", "GAE": "Gros électroménager",
    "SDA Kitchen": "Petit électroménager cuisine", "PAE Kitchen": "Petit électroménager cuisine",
    "Air care": "Climatisation & chauffage", "Vacuuming and Cleaning": "Aspiration & nettoyage",
    "Personal care": "Soin & beauté", "Tools and DIY": "Outillage & bricolage", "Garden": "Jardin",
    "Fitness": "Fitness & sport", "Mobility": "Mobilité électrique", "Household": "Ustensiles de cuisine",
    "Clothing care": "Soin du linge", "Pets": "Animaux", "Childcare": "Puériculture", "Brown Range": "TV",
    "Energy": "Énergie & recharge", "Home": "Maison",
}
STRATEGY = {"Entry": "Entrée de gamme", "Best Value": "Meilleur rapport qualité-prix",
            "Best Performance": "Haute performance", "FlagShip": "Haut de gamme"}


def slugify(s):
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


def oid(seed):
    return hashlib.sha1(seed.encode()).hexdigest()[:24]


def eur(s):  # "1 239,00€" -> Decimal("1239.00")
    return Decimal(s.replace("€", "").replace(" ", "").replace(" ", "").replace(",", "."))


fixes = collections.Counter()
by_sku = {r["sku"]: r for r in rows}

# --- corrections des erreurs du fichier ---
for r in rows:
    fam = r.get("family")
    if fam is None and r.get("subfamily"):  # « Vacuuming and CleaningAccessories for… » : famille collée
        for f in FAMILIES:
            if r["subfamily"].startswith(f):
                r["family"], r["subfamily"] = f, r["subfamily"][len(f):].strip()
                fixes["famille collée à la sous-famille"] += 1
    if r["sku"] == "A90_EU01_116307":  # libellé fusionné avec la sous-famille dans le PDF
        r["name"] = "SkinCare Depil-Action 6000 4in1 Expert"
        r["subfamily"] = by_sku["A01_EU01_116307"]["subfamily"]
        fixes["libellé reconstitué"] += 1
    # variante A90_EU01_… : famille fausse (ex. congélateurs classés « vélos électriques ») -> celle de la référence jumelle
    m = re.fullmatch(r"A90_EU01_(\d+)", r["sku"])
    if m:
        twin = by_sku.get(m.group(1).lstrip("0")) or by_sku.get("A01_EU01_" + m.group(1))
        if twin and twin["name"] == r["name"] and (twin.get("family"), twin.get("subfamily")) != (r.get("family"), r.get("subfamily")):
            r["family"], r["subfamily"] = twin.get("family"), twin.get("subfamily")
            fixes["famille corrigée d'après la référence jumelle"] += 1

major = collections.defaultdict(collections.Counter)
for r in rows:
    if r.get("family") in FAMILIES:
        major[r.get("subfamily")][r["family"]] += 1
for r in rows:
    if r.get("family") not in FAMILIES:  # « #VALEUR! »
        r["family"] = major[r.get("subfamily")].most_common(1)[0][0] if major[r.get("subfamily")] else "Personal care"
        fixes["famille « #VALEUR! » remplacée"] += 1

skipped = [r["sku"] for r in rows if not r.get("name")]
rows = [r for r in rows if r.get("name")]

cat_names = list(dict.fromkeys(FAMILIES[r["family"]] for r in rows))
categories = [dict(name=n, slug=slugify(n)) for n in cat_names]

products = []
for r in rows:
    specs = [{"name": "Référence", "value": r["sku"]}]
    if r.get("subfamily"):
        specs.append({"name": "Type de produit", "value": r["subfamily"].rstrip(".")})
    if r.get("strategy"):
        specs.append({"name": "Gamme", "value": STRATEGY.get(r["strategy"], r["strategy"])})
    products.append(dict(
        sku=r["sku"], name=r["name"][:200], slug=f"{slugify(r['name'])[:100]}-{slugify(r['sku'])}".strip("-"),
        cat=slugify(FAMILIES[r["family"]]),
        eur={k.upper(): str(eur(r[k])) for k in ("fca", "msrp1", "msrp2", "msrp3", "msrp4")},
        image=f"/uploads/{r['image']['file']}" if r.get("image") else None,
        specs=specs,
    ))

assert len({p["slug"] for p in products}) == len(products), "slugs en double"
assert len({p["sku"] for p in products}) == len(products), "références en double"

# ---------- 1) Script mongosh ----------
data_js = json.dumps(dict(categories=categories, products=products), ensure_ascii=False, separators=(",", ":"))
script = r"""// =====================================================================
//  Import de la liste de prix CECOTEC (avril 2026) dans la base TechStore
//  -> collections « categories » et « products » (mêmes champs que Category.java / Product.java)
//
//  Ré-exécutable : catégories repérées par slug, produits par référence (sku).
//  Un 2e passage met à jour nom, prix, photo, catégorie, caractéristiques,
//  sans toucher au stock, aux ventes, aux avis, au slug ni à active/featured.
//
//  Paramètres (variables d'environnement, facultatives) :
//    TAUX=4          conversion euro -> dinar (prix en DT = prix en € x TAUX)
//    COLONNE=MSRP1   prix du fichier utilisé : MSRP1 (prix public conseillé, défaut), MSRP2, MSRP3, MSRP4 ou FCA (prix d'achat)
//    STOCK=0         stock donné aux NOUVEAUX produits (le fichier n'a pas de stock)
//    DRY_RUN=1       affiche quelques prix calculés sans rien écrire
// =====================================================================

const TAUX = Number(process.env.TAUX ?? "4");
const COLONNE = (process.env.COLONNE ?? "MSRP1").toUpperCase();
const STOCK = parseInt(process.env.STOCK ?? "0", 10);
const DRY_RUN = process.env.DRY_RUN === "1";
if (!(TAUX > 0) || !["FCA", "MSRP1", "MSRP2", "MSRP3", "MSRP4"].includes(COLONNE) || !(STOCK >= 0))
  throw new Error("Paramètre invalide (TAUX, COLONNE ou STOCK)");

const DATA = __DATA__;

// conversion au millime près, en entiers pour éviter les erreurs d'arrondi
const prixDt = p => NumberDecimal((Math.round(Number(p.eur[COLONNE]) * 100 * TAUX * 10) / 1000).toFixed(3));

print(`Import CECOTEC : ${DATA.categories.length} catégories, ${DATA.products.length} produits — prix = ${COLONNE} x ${TAUX}, stock initial ${STOCK}${DRY_RUN ? " (simulation)" : ""}`);
if (DRY_RUN) {
  DATA.products.slice(0, 8).forEach(p => print(`  ${p.sku.padEnd(17)} ${p.eur[COLONNE].padStart(8)} € -> ${prixDt(p)} DT  ${p.name}`));
  print("  …  (DRY_RUN=1 : rien n'a été écrit)");
  quit(0);
}

const now = new Date();
const cats = {};
let catsCreees = 0;
for (const c of DATA.categories) {
  const r = db.categories.updateOne({ slug: c.slug }, { $setOnInsert: { name: c.name, slug: c.slug, createdAt: now } }, { upsert: true });
  if (r.upsertedCount) catsCreees++;
  const doc = db.categories.findOne({ slug: c.slug });
  cats[c.slug] = { _id: doc._id, name: doc.name, slug: doc.slug };   // = Product.CategoryRef
}

let crees = 0, maj = 0;
for (let i = 0; i < DATA.products.length; i += 1000) {     // par lots de 1000
  const ops = DATA.products.slice(i, i + 1000).map(p => ({
    updateOne: {
      filter: { sku: p.sku },
      update: {
        $set: {
          name: p.name, brand: "Cecotec", price: prixDt(p), category: cats[p.cat],
          images: p.image ? [p.image] : [], specs: p.specs, updatedAt: now,
        },
        $setOnInsert: {
          sku: p.sku, slug: p.slug, description: null, oldPrice: null, stock: NumberInt(STOCK),
          active: true, featured: false, soldCount: NumberInt(0),
          rating: { average: Double(0), count: NumberInt(0) }, createdAt: now,
        },
      },
      upsert: true,
    },
  }));
  const res = db.products.bulkWrite(ops, { ordered: false });
  crees += res.upsertedCount; maj += res.matchedCount;
}
print(`Catégories : ${catsCreees} créée(s), ${DATA.categories.length - catsCreees} déjà présente(s)`);
print(`Produits   : ${crees} créé(s), ${maj} mis à jour`);
print(`Total en base : ${db.products.countDocuments({ brand: "Cecotec" })} produits Cecotec`);
"""
(OUT / "import-cecotec.js").write_text(script.replace("__DATA__", data_js))

# ---------- 2) Extended JSON (mongoimport, base vide) : prix = MSRP1 x 4, stock 0 ----------
date = {"$date": NOW}
cat_docs = [{"_id": {"$oid": oid("cat:" + c["slug"])}, "name": c["name"], "slug": c["slug"], "createdAt": date} for c in categories]
ref = {c["slug"]: {"_id": d["_id"], "name": c["name"], "slug": c["slug"]} for c, d in zip(categories, cat_docs)}
prod_docs = [{
    "_id": {"$oid": oid("cecotec:" + p["sku"])},
    "name": p["name"], "slug": p["slug"], "sku": p["sku"], "brand": "Cecotec", "description": None,
    "price": {"$numberDecimal": str((Decimal(p["eur"]["MSRP1"]) * RATE).quantize(Decimal("0.001")))}, "oldPrice": None,
    "stock": 0, "active": True, "featured": False, "soldCount": 0,
    "category": ref[p["cat"]], "images": [p["image"]] if p["image"] else [], "specs": p["specs"],
    "rating": {"average": {"$numberDouble": "0.0"}, "count": 0}, "createdAt": date, "updatedAt": date,
} for p in products]
(OUT / "categories.json").write_text(json.dumps(cat_docs, ensure_ascii=False, indent=1))
(OUT / "products.json").write_text(json.dumps(prod_docs, ensure_ascii=False, indent=1))

print(f"{len(categories)} catégories, {len(products)} produits ({sum(1 for p in products if p['image'])} avec photo)")
print("Corrections :", dict(fixes), "| lignes sans libellé ignorées :", skipped)
for c in categories:
    print(f"  {c['name']:<32} {sum(1 for p in products if p['cat'] == c['slug'])}")
