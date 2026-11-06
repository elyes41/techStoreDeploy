# Mise en ligne gratuite : Netlify + Render + MongoDB Atlas

```
Navigateur ──▶ Netlify (Angular + photos des catalogues, CDN)
                 │  /api/*  et /uploads/* absents  (proxy, même domaine)
                 ▼
               Render Free (Quarkus, Docker) ──▶ MongoDB Atlas M0 (données + photos ajoutées depuis l'admin, GridFS)
                 │
                 └──▶ Brevo (API HTTP) pour l'e-mail « colis expédié »
```

| Service | Rôle | Limites gratuites à connaître |
|---|---|---|
| Netlify | Frontend + 3 288 photos des catalogues (`frontend/public/uploads`) | 300 crédits/mois (déploiement = 15, 1 Go de trafic = 20). Site en pause si épuisé. |
| Render Free | Backend Quarkus | 512 Mo RAM / 0,1 CPU, **veille après 15 min sans requête**, pas de disque persistant, **ports SMTP bloqués**, 750 h/mois |
| MongoDB Atlas M0 | Base de données | Petit cluster partagé (~512 Mo) : largement suffisant pour ~6 400 produits |
| Brevo | E-mails transactionnels | ~300 e-mails/jour |
| UptimeRobot (ou cron-job.org) | Garde Render éveillé | Ping toutes les 5 min |

Ce que le code prévoit pour ces limites :
- `STORAGE=mongo` : les images ajoutées dans l'admin vont dans MongoDB (GridFS), pas sur le disque éphémère de Render.
- `BREVO_API_KEY` : les e-mails partent par l'API HTTPS de Brevo (le SMTP est bloqué sur Render Free).
- `PORT` : Quarkus écoute sur le port fourni par Render.
- Photos des catalogues servies par le CDN Netlify : elles s'affichent même quand le backend se réveille.

---

## 1. MongoDB Atlas
1. Créez un compte sur cloud.mongodb.com → **Create cluster → M0 (Free)**, région Europe (Francfort, Paris…).
2. **Database Access** : créez un utilisateur (`techstore` + mot de passe généré, sans caractères spéciaux pour simplifier l'URI).
3. **Network Access** : ajoutez `0.0.0.0/0` (Render Free n'a pas d'IP fixe ; la sécurité repose sur le mot de passe).
4. **Connect → Drivers** : copiez l'URI `mongodb+srv://techstore:<mdp>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority`.

## 2. Clés JWT de production (sur votre PC, hors du dépôt)
```bash
mkdir -p ~/techstore-secrets && sh backend/generate-jwt-keys.sh ~/techstore-secrets
```

## 3. Brevo (e-mails)
1. Compte gratuit sur brevo.com → **Senders, domains** : validez votre adresse d'expédition (ou votre domaine).
2. **SMTP & API → API keys** : créez une clé.

## 4. Backend sur Render
1. Poussez le projet sur GitHub (ou GitLab).
2. render.com → **New → Blueprint** → choisissez le dépôt : `render.yaml` est détecté.
3. Renseignez les valeurs demandées :
   - `MONGODB_URI` : l'URI Atlas
   - `JWT_ISSUER` et `PUBLIC_URL` : `https://<votre-site>.netlify.app` (vous pourrez les corriger après l'étape 6)
   - `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `MAIL_FROM` (`TechStore <adresse validée chez Brevo>`), `BREVO_API_KEY`
4. Dans le service → **Environment → Secret Files** : ajoutez `privateKey.pem` et `publicKey.pem` (contenu des fichiers de l'étape 2).
5. Attendez le premier déploiement, puis ouvrez `https://techstore-api.onrender.com/q/health/ready` → `"status": "UP"`.
   Au démarrage, le backend crée les collections, validateurs, index et le compte admin dans Atlas.

## 5. Importer les catalogues dans Atlas (depuis votre PC)
Les photos des catalogues sont déjà dans `frontend/public/uploads` : il ne reste que les données.
```bash
URI="mongodb+srv://techstore:<mdp>@cluster0.xxxxx.mongodb.net/techstore"
MARGE=0.25 npx mongosh "$URI" import/gembird/import-gembird.js
STOCK=0    npx mongosh "$URI" import/cecotec/import-cecotec.js
```
(Windows PowerShell : `$env:MARGE="0.25"; npx mongosh $env:URI import/gembird/import-gembird.js`)

## 6. Frontend sur Netlify
1. Dans `netlify.toml`, remplacez `techstore-api.onrender.com` (2 fois) par l'adresse de **votre** service Render, puis commit + push.
2. app.netlify.com → **Add new site → Import an existing project** → le dépôt. Les réglages sont lus dans `netlify.toml` : ne changez rien.
3. Notez l'adresse du site (`https://xxx.netlify.app`, renommable dans *Site configuration*) et mettez-la dans `JWT_ISSUER` et `PUBLIC_URL` sur Render (le service redémarre).
4. Testez : `https://xxx.netlify.app/admin/connexion`.

## 7. Garder le backend éveillé
Sans requête pendant 15 min, Render met le backend en veille et Netlify coupe les appels proxy au bout de 26 s :
la première visite afficherait une erreur. Créez un moniteur gratuit sur **UptimeRobot** (ou cron-job.org) :
- type HTTP(s), URL `https://techstore-api.onrender.com/q/health/live`, intervalle **5 minutes**.

Un seul service tourné 24 h/24 consomme ~744 h : c'est dans les 750 h gratuites, à condition de n'avoir **qu'un** service Free sur le compte Render.

## 8. Domaine personnalisé (optionnel)
Netlify → *Domain management → Add a domain* : suivez les instructions DNS (chez OVH : enregistrement `CNAME www` → `xxx.netlify.app`, et `A @` vers l'IP du load balancer Netlify indiquée). HTTPS gratuit automatique.
Mettez ensuite le domaine dans `JWT_ISSUER` / `PUBLIC_URL` sur Render.

## Déploiements suivants
- Push sur `main` → Render redéploie le backend si `backend/` a changé, Netlify le frontend si `frontend/` a changé.
- Le pipeline GitHub Actions / GitLab CI continue de vérifier les builds ; la partie OVH reste désactivée (variable `DEPLOY_TARGET` / `DEPLOY_HOST` absente).

## Quand passer à une offre payante
Dès que la boutique a de vrais clients : un backend toujours actif (Render payant ou VPS OVH, voir `DEPLOIEMENT-OVH.md`) supprime la veille, le blocage SMTP et la limite de 26 s.
