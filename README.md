# Application de Maintenance

Application web de gestion de maintenance pour machines industrielles.

## 🚀 Technologies utilisées

- **Backend**: Spring Boot, Java
- **Frontend**: Thymeleaf, HTML, CSS, JavaScript
- **Base de données**: Firebase Realtime Database
- **Authentification**: Firebase Authentication
- **Stockage**: Firebase Storage (pour les fichiers)

## 📋 Prérequis

- Java 17 ou supérieur
- Maven 3.6+
- Compte Firebase avec projet configuré

## ⚙️ Configuration

### 1. Configuration Firebase

1. Créez un projet sur [Firebase Console](https://console.firebase.google.com/)
2. Activez Firebase Authentication (Email/Password)
3. Créez une Realtime Database
4. Téléchargez le fichier de credentials Admin SDK
5. Placez le fichier dans `src/main/resources/` avec le nom: `YOUR_PROJECT_ID-firebase-adminsdk-XXXXX.json`

### 2. Configuration de l'application

1. Copiez le fichier `src/main/resources/application.properties.example` vers `src/main/resources/application.properties`
2. Remplissez les valeurs suivantes dans `application.properties`:

```properties
# Firebase
firebase.api.key=VOTRE_CLE_API_FIREBASE
firebase.project.id=VOTRE_PROJECT_ID
firebase.realtime.database.url=https://VOTRE_PROJECT_ID-default-rtdb.REGION.firebasedatabase.app/
firebase.credentials.path=classpath:VOTRE_FICHIER_CREDENTIALS.json

# Email (optionnel)
spring.mail.username=VOTRE_EMAIL@gmail.com
spring.mail.password=VOTRE_MOT_DE_PASSE_APP
```

### 3. Configuration Email (optionnel)

Pour configurer l'envoi d'emails:
1. Créez un mot de passe d'application Gmail: [Google Account Security](https://myaccount.google.com/apppasswords)
2. Utilisez ce mot de passe dans `spring.mail.password`

## 🏃 Démarrage

```bash
# Compiler le projet
mvn clean install

# Lancer l'application
mvn spring-boot:run
```

L'application sera accessible sur: `http://localhost:9001`

## 📁 Structure du projet

```
maintenance/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/maintenance/maintenance/
│   │   │       ├── config/          # Configuration Spring Security, Firebase
│   │   │       ├── controller/      # Contrôleurs REST/Web
│   │   │       ├── model/           # Entités et DTOs
│   │   │       ├── repository/      # Repositories JPA
│   │   │       └── service/         # Services métier
│   │   └── resources/
│   │       ├── static/              # CSS, JS, images
│   │       ├── templates/           # Templates Thymeleaf
│   │       └── application.properties.example
│   └── test/
└── pom.xml
```

## 🔐 Sécurité

⚠️ **IMPORTANT**: Ne commitez jamais les fichiers suivants:
- `application.properties` (contient les clés secrètes)
- `*firebase-adminsdk*.json` (credentials Firebase)
- Fichiers dans `uploads/` (contenu utilisateur)

Ces fichiers sont déjà exclus dans `.gitignore`.

## 📝 Fonctionnalités

- ✅ Gestion des machines et équipements
- ✅ Gestion des catégories et sous-catégories
- ✅ Gestion du stock et inventaire
- ✅ Système de tickets de maintenance
- ✅ Alertes et rappels de maintenance
- ✅ Gestion des utilisateurs et rôles
- ✅ Rapports et statistiques
- ✅ Authentification Firebase
- ✅ Gestion de session sécurisée

## 👥 Rôles utilisateurs

- **Superadmin**: Accès complet à toutes les fonctionnalités
- **Admin**: Gestion des machines, tickets, stock
- **Technicien**: Consultation et mise à jour des tickets
- **Utilisateur**: Consultation uniquement

## 🐛 Dépannage

### Problème de session expirée
Si vous rencontrez des problèmes de redirection après déconnexion/reconnexion, assurez-vous que:
- La session est correctement invalidée lors du logout
- Le SecurityContext est correctement mis à jour après login

### Problème de connexion Firebase
Vérifiez que:
- Le fichier de credentials est bien placé dans `src/main/resources/`
- Les clés API dans `application.properties` sont correctes
- Les règles de sécurité Firebase permettent les opérations nécessaires

## 📄 Licence

Ce projet est privé et confidentiel.

## 👤 Auteur

Application développée pour la gestion de maintenance industrielle.

