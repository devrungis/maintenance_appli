# 📋 Diagramme de Cas d'Utilisation - Application de Maintenance

## 🎯 Vue d'ensemble

Ce document décrit le diagramme de cas d'utilisation UML pour l'application de maintenance avec 3 acteurs principaux : **Super Admin**, **Admin**, et **Technicien**.

## 👥 Acteurs

### 1. Super Admin
- **Description** : Administrateur système avec accès total à toutes les entreprises et fonctionnalités
- **Permissions** : Accès complet à toutes les fonctionnalités sans restriction

### 2. Admin
- **Description** : Administrateur d'une entreprise avec droits de gestion complète de son entreprise
- **Permissions** : Accès à toutes les fonctionnalités de son entreprise, sauf création de Super Admin/Admin

### 3. Technicien
- **Description** : Utilisateur technique qui effectue les maintenances et réparations
- **Permissions** : Accès limité aux tâches qui lui sont assignées et consultation

---

## 📦 Packages de Cas d'Utilisation

### 1. Authentification
- **Se connecter** : Connexion à l'application avec identifiants
- **Se déconnecter** : Déconnexion de l'application
- **Gérer son profil** : Modification des informations personnelles

### 2. Gestion des Entreprises
| Cas d'utilisation | Super Admin | Admin | Technicien |
|-------------------|-------------|-------|------------|
| Créer une entreprise | ✅ | ❌ | ❌ |
| Modifier une entreprise | ✅ | ✅ (son entreprise) | ❌ |
| Supprimer une entreprise | ✅ | ❌ | ❌ |
| Consulter les entreprises | ✅ | ✅ | ❌ |
| Changer d'entreprise | ✅ | ✅ | ❌ |

### 3. Gestion des Utilisateurs
| Cas d'utilisation | Super Admin | Admin | Technicien |
|-------------------|-------------|-------|------------|
| Créer un utilisateur | ✅ | ✅ (sauf Admin/SuperAdmin) | ❌ |
| Modifier un utilisateur | ✅ | ✅ (sauf Admin/SuperAdmin) | ❌ |
| Supprimer un utilisateur | ✅ | ✅ (sauf Admin/SuperAdmin) | ❌ |
| Consulter les utilisateurs | ✅ | ✅ | ❌ |
| Créer un Admin | ✅ | ❌ | ❌ |
| Créer un Super Admin | ✅ | ❌ | ❌ |
| Gérer les rôles | ✅ | ❌ | ❌ |

### 4. Gestion des Machines
| Cas d'utilisation | Super Admin | Admin | Technicien |
|-------------------|-------------|-------|------------|
| Créer une machine | ✅ | ✅ | ❌ |
| Modifier une machine | ✅ | ✅ | ❌ |
| Supprimer une machine | ✅ | ✅ | ❌ |
| Consulter les machines | ✅ | ✅ | ✅ |
| Consulter détails machine | ✅ | ✅ | ✅ |

### 5. Gestion des Catégories
| Cas d'utilisation | Super Admin | Admin | Technicien |
|-------------------|-------------|-------|------------|
| Créer une catégorie | ✅ | ✅ | ❌ |
| Modifier une catégorie | ✅ | ✅ | ❌ |
| Supprimer une catégorie | ✅ | ✅ | ❌ |
| Consulter les catégories | ✅ | ✅ | ✅ |
| Gérer les sous-catégories | ✅ | ✅ | ❌ |

### 6. Gestion des Maintenances
| Cas d'utilisation | Super Admin | Admin | Technicien |
|-------------------|-------------|-------|------------|
| Programmer une maintenance | ✅ | ✅ | ❌ |
| Modifier une maintenance | ✅ | ✅ | ❌ |
| Annuler une maintenance | ✅ | ✅ | ❌ |
| Consulter les maintenances | ✅ | ✅ | ✅ (assignées) |
| Effectuer une maintenance | ✅ | ✅ | ✅ (assignées) |
| Marquer maintenance terminée | ✅ | ✅ | ✅ (assignées) |

### 7. Gestion des Réparations
| Cas d'utilisation | Super Admin | Admin | Technicien |
|-------------------|-------------|-------|------------|
| Créer une réparation | ✅ | ✅ | ❌ |
| Modifier une réparation | ✅ | ✅ | ❌ |
| Supprimer une réparation | ✅ | ✅ | ❌ |
| Consulter les réparations | ✅ | ✅ | ✅ (assignées) |
| Assigner un technicien | ✅ | ✅ | ❌ |
| Effectuer une réparation | ✅ | ✅ | ✅ (assignées) |
| Marquer réparation terminée | ✅ | ✅ | ✅ (assignées) |

### 8. Gestion des Tickets
| Cas d'utilisation | Super Admin | Admin | Technicien |
|-------------------|-------------|-------|------------|
| Créer un ticket | ✅ | ✅ | ✅ |
| Modifier un ticket | ✅ | ✅ | ❌ |
| Supprimer un ticket | ✅ | ✅ | ❌ |
| Consulter les tickets | ✅ | ✅ | ✅ (assignés) |
| Assigner un ticket | ✅ | ✅ | ❌ |
| Résoudre un ticket | ✅ | ✅ | ✅ (assignés) |
| Fermer un ticket | ✅ | ✅ | ❌ |

### 9. Gestion du Stock
| Cas d'utilisation | Super Admin | Admin | Technicien |
|-------------------|-------------|-------|------------|
| Ajouter une pièce | ✅ | ✅ | ❌ |
| Modifier une pièce | ✅ | ✅ | ❌ |
| Supprimer une pièce | ✅ | ✅ | ❌ |
| Consulter le stock | ✅ | ✅ | ✅ |
| Gérer les alertes de stock | ✅ | ✅ | ❌ |

### 10. Calendrier et Planning
| Cas d'utilisation | Super Admin | Admin | Technicien |
|-------------------|-------------|-------|------------|
| Consulter le calendrier | ✅ | ✅ | ✅ |
| Ajouter un événement | ✅ | ✅ | ✅ (son planning) |
| Modifier un événement | ✅ | ✅ | ✅ (son planning) |
| Supprimer un événement | ✅ | ✅ | ✅ (son planning) |
| Gérer son planning | ✅ | ✅ | ✅ |

### 11. Rapports et Statistiques
| Cas d'utilisation | Super Admin | Admin | Technicien |
|-------------------|-------------|-------|------------|
| Consulter le dashboard | ✅ | ✅ | ✅ (ses tâches) |
| Générer un rapport maintenance | ✅ | ✅ | ❌ |
| Générer un rapport réparations | ✅ | ✅ | ❌ |
| Consulter les statistiques | ✅ | ✅ | ❌ |
| Exporter les rapports | ✅ | ✅ | ❌ |

---

## 🔑 Règles Métier

### Super Admin
- Peut accéder à toutes les entreprises
- Peut créer, modifier et supprimer des entreprises
- Peut créer des Admins et Super Admins
- Accès total sans restriction

### Admin
- Accès limité à son entreprise
- Peut créer des utilisateurs (sauf Admin et Super Admin)
- Peut modifier les informations de son entreprise
- Accès à toutes les fonctionnalités de gestion de son entreprise

### Technicien
- Accès en lecture seule pour la plupart des ressources
- Peut consulter les machines, tickets et maintenances
- Peut effectuer uniquement les tâches qui lui sont assignées
- Peut créer des tickets
- Peut gérer son propre planning et profil

---

## 📊 Résumé des Permissions

| Fonctionnalité | Super Admin | Admin | Technicien |
|----------------|-------------|-------|------------|
| **Gestion Entreprises** | ✅ Total | ✅ Son entreprise | ❌ |
| **Gestion Utilisateurs** | ✅ Total | ✅ Sauf Admin/SuperAdmin | ❌ |
| **Gestion Machines** | ✅ Total | ✅ Total | 🔍 Lecture seule |
| **Gestion Catégories** | ✅ Total | ✅ Total | 🔍 Lecture seule |
| **Maintenances** | ✅ Total | ✅ Total | ✅ Assignées |
| **Réparations** | ✅ Total | ✅ Total | ✅ Assignées |
| **Tickets** | ✅ Total | ✅ Total | ✅ Assignés + Création |
| **Stock** | ✅ Total | ✅ Total | 🔍 Lecture seule |
| **Calendrier** | ✅ Total | ✅ Total | ✅ Son planning |
| **Rapports** | ✅ Total | ✅ Total | ❌ |

**Légende :**
- ✅ = Accès complet
- 🔍 = Lecture seule
- ❌ = Aucun accès

---

## 📝 Notes Importantes

1. **Sécurité** : Les permissions sont vérifiées côté serveur pour éviter les accès non autorisés
2. **Héritage** : Super Admin hérite de toutes les permissions d'Admin
3. **Assignation** : Seuls les Admin et Super Admin peuvent assigner des tâches aux Techniciens
4. **Entreprises multiples** : Un Super Admin peut gérer plusieurs entreprises, un Admin gère une entreprise

---

## 🔄 Flux Principaux

### Flux 1 : Création d'une maintenance
1. Admin/Super Admin programme une maintenance
2. Admin/Super Admin assigne un technicien
3. Technicien consulte la maintenance assignée
4. Technicien effectue la maintenance
5. Technicien marque la maintenance comme terminée

### Flux 2 : Gestion d'un ticket
1. Technicien/Admin crée un ticket
2. Admin assigne le ticket à un technicien
3. Technicien consulte le ticket assigné
4. Technicien résout le ticket
5. Admin ferme le ticket

### Flux 3 : Création d'utilisateur
1. Super Admin crée une entreprise
2. Super Admin crée un Admin pour l'entreprise
3. Admin crée des techniciens pour son entreprise
4. Les techniciens peuvent se connecter et travailler



