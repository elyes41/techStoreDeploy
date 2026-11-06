# Import de la liste de prix CECOTEC (avril 2026)

6 272 produits, 16 catégories, 3 186 photos, extraits du PDF « Price list with photos ».

| Fichier | Rôle |
|---|---|
| `import-cecotec.js` | **Script mongosh recommandé** : crée les catégories, importe/met à jour les produits. Ré-exécutable. |
| `categories.json`, `products.json` | Mêmes données en Extended JSON pour `mongoimport` / Compass (**base vide uniquement** ; prix = MSRP1 × 4, stock 0) |
| `images/` | Photos produits, à copier dans le dossier d'upload du backend |
| `extract-pdf.py`, `generate-import.py` | Scripts utilisés (pour une prochaine liste de prix : `pip install pymupdf`) |

## Prix
Prix en DT = prix en € × 4. Par défaut on prend **MSRP1** (prix public conseillé le plus haut).
`COLONNE=MSRP2|MSRP3|MSRP4|FCA` pour une autre colonne (FCA = prix d'achat), `TAUX=…` pour un autre taux.

## Stock
Le fichier ne contient pas de stock : les nouveaux produits sont créés avec `STOCK` (défaut 0 = « rupture »).
Relancer le script ne modifie jamais le stock déjà saisi dans l'admin.

## 1. Copier les photos
```bash
docker compose cp import/cecotec/images/. backend:/data/uploads/
# ou en dev (mvn quarkus:dev) :
cp import/cecotec/images/* backend/uploads/
```

## 2. Importer en base
```bash
docker compose cp import/cecotec/import-cecotec.js mongo:/tmp/
docker compose exec -e STOCK=10 mongo mongosh \
  "mongodb://techstore:techstore@localhost:27017/techstore?authSource=admin" /tmp/import-cecotec.js
```
Ajouter `-e DRY_RUN=1` pour voir les prix calculés sans rien écrire.

## Corrections faites sur le fichier source
- 71 variantes `A90_EU01_…` classées dans une mauvaise famille (ex. congélateurs en « SUV Electric Bikes ») → famille de la référence jumelle
- 2 familles `#VALEUR!` et 5 familles collées à la sous-famille → corrigées
- noms coupés à l'affichage dans le PDF → texte complet récupéré
- 2 lignes sans nom de produit ignorées : A01_EU01_122473, A01_EU01_122462
- EAN non repris : le PDF les affiche en notation scientifique (8,43548E+12), les chiffres sont perdus
