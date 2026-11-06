# TechStore – boutique en ligne + dashboard d'administration

Application e-commerce complète pour vendre des produits high-tech en Tunisie.

| Partie | Techno | Dossier |
|---|---|---|
| API REST | Quarkus 3.27 · Java 21 · MongoDB Panache | `backend/` |
| Base de données | MongoDB 8.0 · validateurs `$jsonSchema`, index et agrégations en JSON | `backend/src/main/resources/mongo` |
| Boutique + dashboard | Angular 22 (standalone, signals) | `frontend/` |
| Déploiement | Docker Compose + Nginx | `docker-compose.yml` |

## Fonctionnalités

**Boutique (public)**
- Accueil : produits phares, promotions, catégories
- Catalogue avec filtres : catégorie, marque (multi-choix), fourchette de prix, en stock, en promo, recherche texte, tri (prix, nouveautés, nom), pagination. Les filtres sont dans l'URL (partageables, bons pour le SEO).
- Fiche produit : galerie photos, caractéristiques, quantité, produits similaires
- Panier (conservé dans le navigateur) et commande : adresse avec les 24 gouvernorats, téléphone tunisien validé, choix du paiement
- Frais de livraison, livraison offerte au-delà d'un seuil, timbre fiscal (réglables dans `application.properties`)
- Page de confirmation et suivi de commande (référence + téléphone)

**Recommandations « à la Amazon »** (agrégations MongoDB, sans donnée inventée : un bloc sans historique ne s'affiche pas)
- Produits similaires : même catégorie ou marque, classés par proximité de prix, stock, ventes et note
- Souvent achetés ensemble : produits présents dans les mêmes commandes, avec ajout groupé au panier
- Les clients ayant consulté ce produit ont aussi regardé : consultations anonymes des 90 derniers jours
- Recommandé pour vous (accueil) : d'après les produits récemment consultés par le visiteur
- Tendances de la semaine : ventes et visiteurs uniques des 7 derniers jours
- Complétez votre commande (panier) et récemment consultés

**Fonctionnalités tendance**
- Avis clients avec notes, répartition des étoiles, badge « Achat vérifié » et modération dans le dashboard
- Recherche instantanée avec suggestions de produits, rayons et marques
- Liste d'envies (favoris) et comparateur jusqu'à 4 produits, avec mise en évidence des différences
- Codes promo (pourcentage ou montant, minimum d'achat, dates, nombre d'utilisations)
- Barre « Plus que X DT pour la livraison offerte »
- Badges « Meilleure vente », « Plus que X en stock », économie réalisée, nombre réel de visiteurs sur 24 h
- Livraison estimée, partage du produit, bouton « Commander sur WhatsApp » (variable `WHATSAPP_NUMBER`)
- Filtres par note et tris « Meilleures ventes » / « Mieux notés »

**Dashboard admin (`/admin`)**
- Connexion sécurisée (JWT, mot de passe haché en bcrypt)
- Tableau de bord : chiffre d'affaires 30 jours, commandes à traiter, ventes des 7 derniers jours, alertes de stock
- Produits : liste avec recherche et filtres, création/modification, upload de photos (choix de la photo principale), caractéristiques libres (RAM, stockage…), prix barré pour les promos, produit visible/masqué, produit phare, modification rapide du stock depuis la liste
- Catégories : créer, renommer, supprimer (bloqué si des produits l'utilisent)
- Avis clients : publier, refuser, supprimer (note moyenne recalculée)
- Codes promo : créer, modifier, désactiver, suivre les utilisations
- Tableau de bord : meilleures ventes et avis à modérer
- Commandes : filtres par statut, recherche, détail, workflow En attente → Confirmée → Expédiée → Livrée, annulation avec remise en stock automatique, statut de paiement
- Expédition : saisie du transporteur, du n° et du lien de suivi ; **e-mail automatique au client « Votre colis est en route »** (transporteur, suivi, contenu du colis, montant à régler) ; statut d'envoi visible et bouton « Renvoyer l'e-mail »

**Côté serveur**
- Le stock est réservé par une mise à jour atomique MongoDB qui n'aboutit que s'il reste assez de stock (pas de survente si deux clients achètent le dernier article en même temps) ; si une étape de la commande échoue, le stock et le code promo déjà réservés sont restitués
- Les changements de statut de commande sont conditionnés au statut lu (pas de double annulation par deux administrateurs)
- Les prix sont toujours recalculés par le serveur (le client ne peut pas modifier le montant)
- Les lignes de commande gardent le nom et le prix même si le produit est supprimé ensuite

## Démarrage rapide (Docker)

```bash
cp .env.example .env        # puis changez ADMIN_PASSWORD (et WHATSAPP_NUMBER si besoin)
docker compose up --build
```

- Boutique : http://localhost
- Dashboard : http://localhost/admin (identifiants du fichier `.env`)
- Documentation de l'API (Swagger) : http://localhost:8080/q/swagger-ui

## Développement

```bash
# 1. Base de données
docker compose up -d mongo

# 2. Backend (rechargement à chaud)
cd backend
mvn quarkus:dev                 # http://localhost:8080

# 3. Frontend
cd frontend
npm install
npm start                       # http://localhost:4200 (proxy vers le backend)
```

Au démarrage, le backend applique la structure de la base (validateurs et index, opération sans risque à chaque redémarrage) puis, si la base est vide, insère des **données de démonstration** (5 catégories, 13 produits sans photo, quelques avis, le code `BIENVENUE10`). Mettez `DEMO_DATA=false` avant de créer votre base de production.

Pour explorer la base : MongoDB Compass ou `mongosh "mongodb://techstore:techstore@localhost:27017/techstore?authSource=admin"`.

Le compte admin est créé automatiquement au premier démarrage à partir de `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

## Avant la mise en production

1. **Clés JWT** : le projet contient une paire de clés de développement. Générez-en une nouvelle et ne versionnez jamais la clé privée :
   ```bash
   cd backend && sh generate-jwt-keys.sh
   ```
   Ou montez-les dans le conteneur et pointez `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` vers ces fichiers.
2. **Mots de passe** : changez `ADMIN_PASSWORD` et `MONGO_PASSWORD` (sans caractères spéciaux `@ : / ?`, ou encodés en URL, car il est inséré dans l'URI de connexion). `MONGO_PASSWORD` n'est pris en compte qu'à la création du volume `mongodata`.
3. **HTTPS** : placez un reverse proxy (Caddy, Traefik ou Nginx + Let's Encrypt) devant le conteneur `frontend`.
4. **Sauvegardes** : les volumes `mongodata` (base, avec `mongodump`) et `uploads` (photos) sont à sauvegarder.
5. **Données de démo** : `DEMO_DATA=false`.
6. **Paiement en ligne** : les commandes « carte », « e-Dinar » et « portefeuille mobile » sont enregistrées avec le statut « non payée ». Le point d'intégration de la passerelle (ClicToPay, Konnect, Flouci…) est marqué dans `OrderService.checkout()`.
7. **Textes à compléter** : nom de la boutique, adresse, téléphone, délais de garantie et de retour (repérés par des crochets `[ ]` dans `frontend/src/app/shop/`).

## Structure de la base MongoDB

Toute la structure est décrite en JSON dans `backend/src/main/resources/mongo/` et appliquée au démarrage par `MongoSetup` :

| Fichier | Contenu |
|---|---|
| `validators.json` | schéma `$jsonSchema` de chaque collection (champs obligatoires, types, stock ≥ 0, note 1-5, statuts autorisés…) |
| `indexes.json` | index : uniques (slug, SKU, référence, code promo, e-mail admin), catalogue, tris, TTL de 90 jours sur les consultations |
| `pipelines/*.json` | agrégations : recommandations, tendances, statistiques du dashboard, avis |
| `demo-data.json` | données de démonstration |

Les pipelines s'ouvrent tels quels dans Compass ou mongosh ; un paramètre s'écrit `{"@param": "nom"}`.

| Collection | Document | Choix de modélisation |
|---|---|---|
| `products` | produit + `category: {_id, name, slug}` + `specs: [{name, value}]` + `rating: {average, count}` + `soldCount` | catégorie, note et ventes recopiées dans le produit : le catalogue se lit sans jointure |
| `categories` | `name`, `slug` | un renommage met à jour la copie dans les produits |
| `orders` | `customer`, `delivery`, `payment`, `items[]`, `amounts`, `shipment`, `history[]` | une commande = un document ; les lignes figent nom et prix |
| `reviews` | `productId`, note, texte, statut de modération | collection à part (croissance illimitée) |
| `coupons` | `code`, type, valeur, minimum, plafond d'utilisations, dates | consommation atomique |
| `product_views` | `productId`, `visitorId`, `viewedAt` | supprimées automatiquement après 90 jours (index TTL) |
| `admin_users` | `email`, `passwordHash` (bcrypt) | |

Exemple de commande :

```json
{
  "_id": ObjectId("6aee2b5d7c1f0f0274c779c1"),
  "reference": "CMD-20261106-K7P2Q",
  "status": "SHIPPED",
  "customer": { "name": "Amine Ben Salah", "phone": "22 123 456", "email": "amine@example.tn" },
  "delivery": { "governorate": "Tunis", "city": "La Marsa", "address": "12 rue de Carthage" },
  "payment": { "method": "CASH_ON_DELIVERY", "status": "UNPAID" },
  "items": [ { "productId": ObjectId("…"), "name": "Smartphone 6,7\" 256 Go 5G", "unitPrice": 1899.000, "quantity": 1, "lineTotal": 1899.000 } ],
  "amounts": { "subtotal": 1899.000, "discount": 0, "shippingFee": 0, "fiscalStamp": 1.000, "total": 1900.000 },
  "shipment": { "carrier": "Aramex", "trackingNumber": "AR123456789TN", "shippedAt": ISODate("…"), "emailSentAt": ISODate("…") },
  "history": [ { "status": "PENDING", "at": ISODate("…") }, { "status": "CONFIRMED", "at": ISODate("…") }, { "status": "SHIPPED", "at": ISODate("…") } ],
  "createdAt": ISODate("…"), "updatedAt": ISODate("…")
}
```

Les montants sont stockés en `Decimal128` (pas d'erreur d'arrondi) et les identifiants exposés par l'API sont les `ObjectId` sous forme de chaîne.

## Organisation du code backend

```
backend/src/main/java/tn/techstore/
├── catalog/          produits, catégories, photos      (collections products, categories)
├── orders/           commande, stock, suivi            (orders)
├── reviews/          avis et modération                 (reviews)
├── coupons/          codes promo                        (coupons)
├── recommendations/  similaires, achetés ensemble, tendances, pour vous (product_views + agrégations)
├── admin/            comptes admin, connexion JWT, tableau de bord   (admin_users)
├── mail/             e-mail « votre colis est en route »
├── mongo/            application du schéma, pipelines JSON, données de démo
└── common/           configuration, erreurs, utilitaires
```

## E-mails aux clients

Quand une commande passe au statut **Expédiée**, le client reçoit un e-mail (s'il a indiqué son adresse à la commande).
Gabarits modifiables : `backend/src/main/resources/templates/mail/order-shipped.html` et `.txt`.

- En développement (`mvn quarkus:dev`), rien n'est envoyé : l'e-mail s'affiche dans la console.
- En production, renseignez un serveur SMTP dans `.env` :

| Variable | Exemple |
|---|---|
| `SMTP_HOST` / `SMTP_PORT` | `smtp-relay.brevo.com` / `587` |
| `SMTP_USER` / `SMTP_PASSWORD` | identifiants fournis par le service |
| `MAIL_FROM` | `TechStore <commandes@votre-domaine.tn>` |
| `PUBLIC_URL` | `https://votre-domaine.tn` (liens dans l'e-mail) |
| `SMTP_SSL=true` | uniquement pour le port 465 |

Pour une bonne délivrabilité, utilisez une adresse de votre propre domaine et configurez SPF/DKIM chez votre fournisseur (Brevo, Mailgun, Amazon SES, ou le SMTP de votre hébergeur).

## API principale

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/categories` | catégories avec nombre de produits |
| GET | `/api/products?category=&brand=&minPrice=&maxPrice=&inStock=&promo=&minRating=&q=&sort=&page=&size=` | catalogue filtré et paginé |
| GET | `/api/products/facets?category=` | marques et fourchette de prix disponibles |
| GET | `/api/products/{slug}` | fiche produit |
| POST | `/api/orders` | passer commande |
| POST | `/api/auth/login` | connexion admin |
| GET | `/api/admin/stats` | indicateurs du dashboard |
| CRUD | `/api/admin/products`, `/api/admin/categories` | gestion du catalogue |
| POST | `/api/admin/uploads` | upload d'une photo (multipart, champ `file`) |
| GET/PATCH | `/api/admin/orders` | gestion des commandes |
| GET | `/api/products/{slug}/recommendations` | similaires, achetés ensemble, aussi consultés |
| GET | `/api/products/trending`, `/api/recommendations/for-you?ids=` | tendances, recommandé pour vous |

Les identifiants (`id`, `productId`, `categoryId`…) sont des chaînes de 24 caractères (ObjectId MongoDB).
