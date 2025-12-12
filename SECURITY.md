# Politique de Sécurité

## 🔒 Fichiers sensibles exclus du dépôt

Les fichiers suivants sont automatiquement exclus du contrôle de version via `.gitignore`:

### Configuration
- `src/main/resources/application.properties` - Contient les clés API et mots de passe
- `src/main/resources/application-local.properties`
- `src/main/resources/application-prod.properties`

### Credentials Firebase
- `src/main/resources/*firebase-adminsdk*.json` - Fichiers de credentials Firebase Admin SDK

### Fichiers utilisateur
- `uploads/` - Tous les fichiers uploadés par les utilisateurs

### Autres
- `*.env` - Fichiers d'environnement
- `*.keystore`, `*.p12`, `*.pem`, `*.jks` - Certificats et clés

## ⚠️ Instructions pour les contributeurs

### Avant de commiter

1. **Vérifiez que `application.properties` n'est pas dans le dépôt**:
   ```bash
   git status
   ```
   Si `application.properties` apparaît, il ne doit PAS être commité.

2. **Utilisez `application.properties.example` comme référence**:
   - Copiez `application.properties.example` vers `application.properties`
   - Remplissez avec vos propres valeurs de configuration

3. **Ne commitez jamais**:
   - Clés API Firebase
   - Mots de passe
   - Tokens d'authentification
   - Fichiers de credentials
   - Données utilisateur

### Si vous avez accidentellement commité des secrets

1. **Supprimez immédiatement les secrets du dépôt**:
   ```bash
   git rm --cached src/main/resources/application.properties
   git commit -m "Remove sensitive configuration file"
   ```

2. **Régénérez les clés compromises**:
   - Changez tous les mots de passe exposés
   - Régénérez les clés API Firebase
   - Créez de nouveaux tokens d'authentification

3. **Vérifiez l'historique Git**:
   ```bash
   git log --all --full-history -- src/main/resources/application.properties
   ```

## 🔑 Gestion des secrets

### Pour le développement local

Créez un fichier `application.properties` local (non versionné) avec vos propres valeurs.

### Pour la production

Utilisez des variables d'environnement ou un système de gestion de secrets sécurisé:
- Variables d'environnement système
- Secrets managers (AWS Secrets Manager, Azure Key Vault, etc.)
- Fichiers de configuration externes non versionnés

## 📧 Signaler une faille de sécurité

Si vous découvrez une faille de sécurité, contactez immédiatement l'équipe de développement.

## ✅ Checklist avant push

- [ ] Aucun fichier `application.properties` dans le commit
- [ ] Aucun fichier `*firebase-adminsdk*.json` dans le commit
- [ ] Aucun mot de passe ou clé API en dur dans le code
- [ ] Aucun fichier dans `uploads/` dans le commit
- [ ] `.gitignore` est à jour

