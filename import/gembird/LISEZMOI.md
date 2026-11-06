# Import du catalogue GEMBIRD (SBC – 28/10/2026)

102 produits, 7 catégories, 102 photos, extraits du PDF fournisseur.

| Fichier | Rôle |
|---|---|
| `import-gembird.js` | **Script mongosh recommandé** : crée les catégories, importe/met à jour les produits. Ré-exécutable. |
| `categories.json`, `products.json` | Mêmes données en Extended JSON pour `mongoimport` / Compass (**base vide uniquement**, prix = HT + 19 %) |
| `images/` | Photos produits, à copier dans le dossier d'upload du backend |
| `extract-pdf.py` | Script d'extraction (pour un prochain catalogue : `pip install pymupdf`) |

## 1. Copier les photos
```bash
# Docker
docker compose cp import/gembird/images/. backend:/data/uploads/
# ou en dev (mvn quarkus:dev) : copier dans backend/uploads/
cp import/gembird/images/* backend/uploads/
```

## 2. Importer en base
```bash
docker compose cp import/gembird/import-gembird.js mongo:/tmp/
docker compose exec -e MARGE=0.25 mongo mongosh \
  "mongodb://techstore:techstore@localhost:27017/techstore?authSource=admin" /tmp/import-gembird.js
```
Variables : `TVA` (défaut 0.19), `MARGE` (défaut 0 → prix de vente = prix d'achat HT + TVA), `DRY_RUN=1` pour simuler.

Relancer le script avec un nouveau catalogue met à jour prix, stock, nom, description, photo et catégorie
sans toucher aux ventes, avis, slug, ni aux cases « actif » / « mis en avant » modifiées dans l'admin.

## Variante mongoimport (base vide)
```bash
mongoimport --uri "mongodb://techstore:techstore@localhost:27017/techstore?authSource=admin" -c categories --jsonArray categories.json
mongoimport --uri "mongodb://techstore:techstore@localhost:27017/techstore?authSource=admin" -c products   --jsonArray products.json
```
