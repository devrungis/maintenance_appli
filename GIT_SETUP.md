# Guide de configuration Git et GitHub

## 📋 Étapes pour mettre le code sur GitHub de manière sécurisée

### 1. Vérifier que les fichiers sensibles sont exclus

Avant d'initialiser Git, vérifiez que votre fichier `.gitignore` est à jour et que `application.properties` n'est pas tracké.

### 2. Initialiser le dépôt Git

```bash
# Initialiser le dépôt Git
git init

# Vérifier que les fichiers sensibles sont bien ignorés
git status
```

**Important**: Si `application.properties` apparaît dans `git status`, NE L'AJOUTEZ PAS. Il doit être ignoré.

### 3. Ajouter les fichiers au dépôt

```bash
# Ajouter tous les fichiers (sauf ceux dans .gitignore)
git add .

# Vérifier ce qui sera commité
git status
```

**Vérifiez que ces fichiers NE SONT PAS dans la liste**:
- ❌ `src/main/resources/application.properties`
- ❌ `src/main/resources/*firebase-adminsdk*.json`
- ❌ Fichiers dans `uploads/` (sauf `.gitkeep`)

### 4. Créer le premier commit

```bash
git commit -m "Initial commit: Application de maintenance"
```

### 5. Créer un dépôt sur GitHub

1. Allez sur [GitHub](https://github.com)
2. Cliquez sur "New repository"
3. Nommez votre dépôt (ex: `maintenance-app`)
4. **NE COCHEZ PAS** "Initialize this repository with a README" (vous avez déjà un README)
5. Cliquez sur "Create repository"

### 6. Connecter le dépôt local à GitHub

```bash
# Remplacez USERNAME et REPO_NAME par vos valeurs
git remote add origin https://github.com/USERNAME/REPO_NAME.git

# Vérifier la connexion
git remote -v
```

### 7. Pousser le code sur GitHub

```bash
# Pousser sur la branche main
git branch -M main
git push -u origin main
```

## 🔒 Vérifications de sécurité avant le push

### Checklist de sécurité

- [ ] `application.properties` n'est PAS dans `git status`
- [ ] Aucun fichier `*firebase-adminsdk*.json` n'est tracké
- [ ] Aucun mot de passe en dur dans le code (vérifiez `script.js`)
- [ ] Le fichier `.gitignore` est à jour
- [ ] Le fichier `application.properties.example` est présent
- [ ] Le fichier `SECURITY.md` est présent

### Commandes de vérification

```bash
# Vérifier qu'aucun fichier sensible n'est tracké
git ls-files | grep -E "(application\.properties|firebase-adminsdk|\.env)"

# Si cette commande retourne des résultats, ces fichiers sont trackés !
# Supprimez-les avec: git rm --cached <fichier>
```

## ⚠️ Si vous avez déjà commité des secrets

Si vous avez accidentellement commité `application.properties` ou d'autres fichiers sensibles:

```bash
# 1. Supprimer le fichier du tracking Git (mais le garder localement)
git rm --cached src/main/resources/application.properties

# 2. Commit la suppression
git commit -m "Remove sensitive configuration file"

# 3. Régénérez immédiatement toutes les clés exposées:
#    - Changez les mots de passe
#    - Régénérez les clés API Firebase
#    - Créez de nouveaux tokens

# 4. Si vous avez déjà pushé, forcez le push (ATTENTION: cela réécrit l'historique)
git push --force
```

## 📝 Structure recommandée des commits

```bash
# Exemple de commits bien structurés
git commit -m "feat: Ajout de la gestion des machines"
git commit -m "fix: Correction du problème de session expirée"
git commit -m "docs: Mise à jour du README"
git commit -m "security: Exclusion des fichiers sensibles du dépôt"
```

## 🔐 Protection de la branche main (optionnel mais recommandé)

Sur GitHub, allez dans Settings > Branches et ajoutez une règle de protection:
- Require pull request reviews before merging
- Require status checks to pass before merging
- Do not allow bypassing the above settings

## 📚 Ressources

- [Git Documentation](https://git-scm.com/doc)
- [GitHub Documentation](https://docs.github.com)
- [Gitignore Patterns](https://git-scm.com/docs/gitignore)

