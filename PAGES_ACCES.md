# 📋 Pages d'Accès - Application de Maintenance

## 🔐 Authentification

| Méthode | Route | Description | Accès |
|---------|-------|-------------|-------|
| GET | `/` | Redirection vers `/login` | Public |
| GET | `/login` | Page de connexion | Public |
| POST | `/login` | Traitement de la connexion | Public |
| GET | `/logout` | Déconnexion (GET) | Authentifié |
| POST | `/logout` | Déconnexion (POST) | Authentifié |

---

## 🏠 Pages Principales

| Méthode | Route | Description | Accès |
|---------|-------|-------------|-------|
| GET | `/dashboard` | Tableau de bord | Authentifié |
| GET | `/machines` | Gestion des machines | Authentifié |
| GET | `/categories` | Gestion des catégories | Authentifié |
| GET | `/alerts` | Alertes de maintenance | Authentifié |
| GET | `/repairs` | Gestion des réparations | Authentifié |
| GET | `/reports` | Rapports | Authentifié |
| GET | `/calendar` | Calendrier | Authentifié |
| GET | `/inventory` | Gestion du stock | Authentifié |
| GET | `/tickets` | Gestion des tickets | Authentifié |

---

## 👥 Gestion des Utilisateurs (`/users`)

| Méthode | Route | Description | Accès |
|---------|-------|-------------|-------|
| GET | `/users` | Liste des utilisateurs | **Superadmin uniquement** |
| GET | `/users/create` | Formulaire de création d'utilisateur | **Superadmin uniquement** |
| POST | `/users/create` | Création d'un utilisateur | **Superadmin uniquement** |
| GET | `/users/{userId}/edit` | Formulaire de modification | **Superadmin uniquement** |
| POST | `/users/{userId}/edit` | Modification d'un utilisateur | **Superadmin uniquement** |
| POST | `/users/{userId}/delete` | Suppression d'un utilisateur | **Superadmin uniquement** |

### API REST Utilisateurs (`/api/users`)

| Méthode | Route | Description | Accès |
|---------|-------|-------------|-------|
| GET | `/api/users/check-role` | Vérifier le rôle de l'utilisateur | Authentifié |
| GET | `/api/users/list` | Liste JSON des utilisateurs | **Superadmin uniquement** |
| GET | `/api/users/{userId}` | Détails d'un utilisateur | **Superadmin uniquement** |
| POST | `/api/users/create` | Création via API | **Superadmin uniquement** |
| POST | `/api/users/sync/{userId}` | Synchroniser un utilisateur | **Superadmin uniquement** |
| POST | `/api/users/sync-all` | Synchroniser tous les utilisateurs | **Superadmin uniquement** |

---

## 🏢 Gestion des Entreprises (`/enterprises`)

### Pages Thymeleaf (CRUD Complet)

| Méthode | Route | Description | Accès |
|---------|-------|-------------|-------|
| GET | `/enterprises` | **Liste des entreprises** (depuis Firebase) | Authentifié |
| GET | `/enterprises/create` | **Formulaire de création** | Authentifié |
| POST | `/enterprises/create` | **Création d'une entreprise** | Authentifié |
| GET | `/enterprises/{id}/edit` | **Formulaire de modification** | Authentifié |
| POST | `/enterprises/{id}/edit` | **Modification d'une entreprise** | Authentifié |
| POST | `/enterprises/{id}/delete` | **Suppression d'une entreprise** | Authentifié |
| POST | `/enterprises/sync` | Synchronisation depuis Firebase | **Superadmin uniquement** |

### Notes importantes :
- **`{id}`** = Firebase ID (String) de l'entreprise
- Les données sont récupérées directement depuis **Firebase Realtime Database** (`/entreprises`)
- La création, modification et suppression se font directement dans Firebase
- Structure Firebase : `/entreprises/{entrepriseId}` avec `nom`, `adresse`, `numero`, `dateCreation`

---

## 🔄 Redirections

| Route | Redirection vers |
|-------|------------------|
| `/login.html` | `/login` |
| `/index.html` | `/dashboard` |
| `/error` (404) | `/login` |

---

## 📝 Notes d'Accès

### 🔴 Accès Restreint (Superadmin uniquement)
- Toutes les routes `/users/*` (gestion des utilisateurs)
- `/api/users/*` (API utilisateurs)
- `/enterprises/sync` (synchronisation Firebase)

### 🟢 Accès Authentifié (Tous les utilisateurs connectés)
- `/dashboard`
- `/machines`, `/categories`, `/alerts`, `/repairs`, `/reports`, `/calendar`, `/inventory`, `/tickets`
- `/enterprises` (liste, création, modification, suppression)

### 🟡 Accès Public
- `/login` (GET et POST)
- `/` (redirection)

---

## 🔗 URLs Complètes

### Base URL
```
http://localhost:9001
```

### Exemples d'URLs complètes

#### Authentification
- `http://localhost:9001/login`
- `http://localhost:9001/logout`

#### Entreprises (CRUD)
- Liste : `http://localhost:9001/enterprises`
- Création : `http://localhost:9001/enterprises/create`
- Modification : `http://localhost:9001/enterprises/{firebaseId}/edit`
- Suppression : POST vers `http://localhost:9001/enterprises/{firebaseId}/delete`

#### Utilisateurs (Superadmin)
- Liste : `http://localhost:9001/users`
- Création : `http://localhost:9001/users/create`
- Modification : `http://localhost:9001/users/{userId}/edit`
- Suppression : POST vers `http://localhost:9001/users/{userId}/delete`

---

## 📊 Structure des Données

### Entreprises (Firebase)
```
/entreprises/
  └── {entrepriseId}/
      ├── nom: string
      ├── adresse: string
      ├── numero: string
      └── dateCreation: timestamp
```

### Utilisateurs (Firebase)
```
/utilisateurs/
  └── {userId}/
      ├── nom: string
      ├── email: string
      ├── nomUtilisateur: string
      ├── role: string (superadmin, admin, utilisateur, technicien)
      ├── statut: string (actif, inactif)
      ├── telephone: string
      ├── dateCreation: timestamp
      ├── horairesTravail: object
      └── planning: object
```

---

## ⚠️ Important

1. **Toutes les routes nécessitent une authentification** sauf `/login`
2. **Les routes `/users/*` sont réservées aux superadmins**
3. **Les entreprises utilisent les Firebase IDs** (String) et non les IDs JPA (Long)
4. **Les données sont synchronisées avec Firebase Realtime Database**
5. **Le port par défaut est 9001** (configuré dans `application.properties`)

---

## 🚀 Démarrage Rapide

1. **Démarrer l'application** : `mvn spring-boot:run`
2. **Accéder à** : `http://localhost:9001/login`
3. **Se connecter** avec un compte Firebase
4. **Accéder aux entreprises** : `http://localhost:9001/enterprises`
5. **Accéder aux utilisateurs** (superadmin) : `http://localhost:9001/users`

---

*Dernière mise à jour : Configuration actuelle de l'application*

