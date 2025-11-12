package com.maintenance.maintenance.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.maintenance.maintenance.model.entity.Machine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseRealtimeService {

    @Autowired
    private DatabaseReference databaseReference;

    /**
     * Vérifie si c'est le premier utilisateur dans la base de données
     */
    public boolean isFirstUser() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CountDownLatch latch = new CountDownLatch(1);

        databaseReference.child("utilisateurs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean isEmpty = !snapshot.exists() || snapshot.getChildrenCount() == 0;
                future.complete(isEmpty);
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(new Exception("Erreur lors de la vérification: " + error.getMessage()));
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
            return future.get();
        } catch (Exception e) {
            throw new Exception("Timeout ou erreur lors de la vérification du premier utilisateur");
        }
    }

    /**
     * Crée la structure complète pour un utilisateur dans Realtime Database
     */
    public void initializeUserStructure(String userId, Map<String, Object> userData, boolean isSuperAdmin) throws Exception {
        try {
            Map<String, Object> userStructure = new HashMap<>();

            // 1. Informations de base de l'utilisateur
            userStructure.put("nom", userData.getOrDefault("nom", ""));
            userStructure.put("prenom", userData.getOrDefault("prenom", ""));
            userStructure.put("email", userData.getOrDefault("email", ""));
            userStructure.put("nomUtilisateur", userData.getOrDefault("nomUtilisateur", ""));
            userStructure.put("motDePasse", userData.getOrDefault("motDePasse", "")); // Devrait être hashé
            userStructure.put("role", isSuperAdmin ? "superadmin" : userData.getOrDefault("role", "utilisateur"));
            userStructure.put("statut", userData.getOrDefault("statut", "actif"));
            userStructure.put("telephone", userData.getOrDefault("telephone", userData.getOrDefault("numeroTelephone", "")));
            userStructure.put("numeroTelephone", userData.getOrDefault("numeroTelephone", userData.getOrDefault("telephone", "")));
            userStructure.put("dateCreation", System.currentTimeMillis());

            // 2. Horaires de travail (structure vide pour chaque jour)
            Map<String, Object> horairesTravail = new HashMap<>();
            String[] jours = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
            for (String jour : jours) {
                Map<String, Object> horaireJour = new HashMap<>();
                horaireJour.put("heureDebut", "");
                horaireJour.put("heureFin", "");
                horaireJour.put("actif", false);
                horairesTravail.put(jour, horaireJour);
            }
            userStructure.put("horairesTravail", horairesTravail);

            // 3. Planning - Firebase Realtime Database supprime les objets vides
            // On crée avec un objet vide qui sera conservé
            Map<String, Object> planning = new HashMap<>();
            userStructure.put("planning", planning);

            // 4. Entreprises - Firebase Realtime Database supprime les objets vides
            Map<String, Object> entreprises = new HashMap<>();
            userStructure.put("entreprises", entreprises);

            // Écrire dans la base de données
            CompletableFuture<Void> future = new CompletableFuture<>();
            CountDownLatch latch = new CountDownLatch(1);

            // Créer le document utilisateur
            databaseReference.child("utilisateurs").child(userId).setValue(userStructure, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError error, DatabaseReference ref) {
                    if (error != null) {
                        future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                        latch.countDown();
                    } else {
                        // Firebase peut avoir supprimé planning s'il était vide
                        // On le recrée explicitement avec updateChildren
                        Map<String, Object> updates = new HashMap<>();
                        
                        // Créer planning avec un marqueur pour que Firebase le conserve
                        Map<String, Object> planningInit = new HashMap<>();
                        planningInit.put("_empty", false); // Marqueur pour conserver la structure
                        updates.put("planning", planningInit);
                        
                        // Supprimer le nœud entreprises s'il existe (ancienne structure)
                        updates.put("entreprises", null);
                        
                        databaseReference.child("utilisateurs").child(userId).updateChildren(updates, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError updateError, DatabaseReference updateRef) {
                                // Note: Firebase Realtime Database supprime automatiquement les objets vides
                                // On garde le placeholder _empty pour que la structure planning soit visible
                                future.complete(null);
                                latch.countDown();
                            }
                        });
                    }
                }
            });

            latch.await(10, TimeUnit.SECONDS);
            future.get();

        } catch (Exception e) {
            throw new Exception("Erreur lors de l'initialisation de la structure utilisateur: " + e.getMessage());
        }
    }

    /**
     * Crée la structure complète pour un utilisateur avec horaires personnalisés
     */
    public void initializeUserStructureWithHoraires(String userId, Map<String, Object> userData, boolean isSuperAdmin) throws Exception {
        try {
            Map<String, Object> userStructure = new HashMap<>();

            // 1. Informations de base de l'utilisateur
            userStructure.put("nom", userData.getOrDefault("nom", ""));
            userStructure.put("prenom", userData.getOrDefault("prenom", ""));
            userStructure.put("email", userData.getOrDefault("email", ""));
            userStructure.put("nomUtilisateur", userData.getOrDefault("nomUtilisateur", ""));
            userStructure.put("motDePasse", userData.getOrDefault("motDePasse", ""));
            userStructure.put("role", isSuperAdmin ? "superadmin" : userData.getOrDefault("role", "utilisateur"));
            userStructure.put("statut", userData.getOrDefault("statut", "actif"));
            userStructure.put("telephone", userData.getOrDefault("telephone", userData.getOrDefault("numeroTelephone", "")));
            userStructure.put("numeroTelephone", userData.getOrDefault("numeroTelephone", userData.getOrDefault("telephone", "")));
            userStructure.put("dateCreation", System.currentTimeMillis());

            // 2. Horaires de travail (utiliser ceux fournis ou créer une structure vide)
            if (userData.containsKey("horairesTravail")) {
                Object horaires = userData.get("horairesTravail");
                System.out.println("=== Horaires fournis pour l'utilisateur: " + horaires);
                userStructure.put("horairesTravail", horaires);
            } else {
                System.out.println("=== Aucun horaire fourni, création d'une structure vide");
                Map<String, Object> horairesTravail = new HashMap<>();
                String[] jours = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
                for (String jour : jours) {
                    Map<String, Object> horaireJour = new HashMap<>();
                    horaireJour.put("heureDebut", "");
                    horaireJour.put("heureFin", "");
                    horaireJour.put("actif", false);
                    horairesTravail.put(jour, horaireJour);
                }
                userStructure.put("horairesTravail", horairesTravail);
            }

            // 3. Planning (spécifique à l'utilisateur - événements exceptionnels)
            Map<String, Object> planning = new HashMap<>();
            userStructure.put("planning", planning);

            // Note: Les entreprises ne sont plus sous utilisateurs/, elles sont dans entreprises/ au niveau racine

            // Écrire dans la base de données
            CompletableFuture<Void> future = new CompletableFuture<>();
            CountDownLatch latch = new CountDownLatch(1);

            databaseReference.child("utilisateurs").child(userId).setValue(userStructure, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError error, DatabaseReference ref) {
                    if (error != null) {
                        future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                        latch.countDown();
                    } else {
                        // Forcer la création de planning avec un marqueur
                        Map<String, Object> updates = new HashMap<>();
                        Map<String, Object> planningInit = new HashMap<>();
                        planningInit.put("_empty", false);
                        updates.put("planning", planningInit);
                        
                        // Supprimer le nœud entreprises s'il existe (migration)
                        updates.put("entreprises", null);
                        
                        databaseReference.child("utilisateurs").child(userId).updateChildren(updates, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError updateError, DatabaseReference updateRef) {
                                future.complete(null);
                                latch.countDown();
                            }
                        });
                    }
                }
            });

            latch.await(10, TimeUnit.SECONDS);
            future.get();

        } catch (Exception e) {
            throw new Exception("Erreur lors de l'initialisation de la structure utilisateur: " + e.getMessage());
        }
    }

    /**
     * Initialise la structure d'une entreprise (structure simplifiée)
     * Nouvelles entreprises dans entreprises/ au même niveau que utilisateurs/
     */
    public void initializeEnterpriseStructure(String entrepriseId, Map<String, Object> entrepriseData) throws Exception {
        try {
            System.out.println("=== initializeEnterpriseStructure: Début ===");
            System.out.println("Entreprise ID: " + entrepriseId);
            System.out.println("Données: " + entrepriseData);
            
            Map<String, Object> entrepriseStructure = new HashMap<>();

            // Informations de base de l'entreprise (structure simplifiée)
            entrepriseStructure.put("nom", entrepriseData.getOrDefault("nom", ""));
            entrepriseStructure.put("rue", entrepriseData.getOrDefault("rue", ""));
            entrepriseStructure.put("codePostal", entrepriseData.getOrDefault("codePostal", ""));
            entrepriseStructure.put("ville", entrepriseData.getOrDefault("ville", ""));
            entrepriseStructure.put("email", entrepriseData.getOrDefault("email", ""));
            entrepriseStructure.put("numero", entrepriseData.getOrDefault("numero", ""));
            entrepriseStructure.put("dateCreation", System.currentTimeMillis());

            // Collections vides initialisées
            entrepriseStructure.put("machines", new HashMap<>());
            entrepriseStructure.put("categories", new HashMap<>());
            entrepriseStructure.put("reparations", new HashMap<>());
            entrepriseStructure.put("maintenances", new HashMap<>());
            entrepriseStructure.put("tickets", new HashMap<>());
            entrepriseStructure.put("stock", new HashMap<>());

            System.out.println("Structure complète: " + entrepriseStructure);

            // Écrire dans la base de données sous entreprises/
            CompletableFuture<Void> future = new CompletableFuture<>();
            CountDownLatch latch = new CountDownLatch(1);

            System.out.println("Écriture dans Firebase: entreprises/" + entrepriseId);
            
            databaseReference.child("entreprises")
                .child(entrepriseId)
                .setValue(entrepriseStructure, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError error, DatabaseReference ref) {
                        if (error != null) {
                            System.err.println("ERREUR Firebase lors de l'écriture: " + error.getMessage());
                            future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                        } else {
                            System.out.println("Entreprise écrite avec succès dans Firebase");
                            future.complete(null);
                        }
                        latch.countDown();
                    }
                });

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                throw new Exception("Timeout lors de l'écriture de l'entreprise (10 secondes)");
            }
            future.get();
            System.out.println("=== initializeEnterpriseStructure: Terminé avec succès ===");

        } catch (Exception e) {
            System.err.println("ERREUR dans initializeEnterpriseStructure: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Erreur lors de l'initialisation de la structure entreprise: " + e.getMessage());
        }
    }

    /**
     * Crée une entreprise dans la collection entreprises/ (séparée de utilisateurs/)
     */
    public String createEnterprise(Map<String, Object> entrepriseData) throws Exception {
        try {
            System.out.println("=== createEnterprise: Début ===");
            System.out.println("Données reçues: " + entrepriseData);
            
            // Générer un ID unique pour l'entreprise
            String entrepriseId = databaseReference.child("entreprises")
                .push()
                .getKey();
            
            System.out.println("ID généré: " + entrepriseId);

            // Initialiser la structure de l'entreprise
            initializeEnterpriseStructure(entrepriseId, entrepriseData);

            System.out.println("=== createEnterprise: Terminé avec succès, ID: " + entrepriseId + " ===");
            return entrepriseId;
        } catch (Exception e) {
            System.err.println("ERREUR dans createEnterprise: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Erreur lors de la création de l'entreprise: " + e.getMessage());
        }
    }

    /**
     * Vérifie si un utilisateur existe déjà dans Realtime Database
     */
    public boolean userExistsInRealtime(String userId) throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CountDownLatch latch = new CountDownLatch(1);

        databaseReference.child("utilisateurs").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                future.complete(snapshot.exists());
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
            return future.get();
        } catch (Exception e) {
            throw new Exception("Timeout ou erreur lors de la vérification");
        }
    }

    /**
     * Initialise l'arborescence pour un utilisateur existant dans Firebase Auth
     * Vérifie d'abord l'existence dans Firebase Authentication avant de créer la structure
     */
    public void initializeExistingUser(String userId, String email, String nom, String nomUtilisateur, String telephone, String role) throws Exception {
        // Vérifier d'abord si l'utilisateur existe dans Firebase Authentication
        // Cette vérification sera faite par le composant qui appelle cette méthode
        
        // Vérifier si l'utilisateur existe déjà dans Realtime Database
        boolean existsInRealtime = userExistsInRealtime(userId);
        
        if (existsInRealtime) {
            // Mettre à jour l'utilisateur existant pour ajouter les champs manquants et supprimer entreprises/
            updateExistingUserStructure(userId);
        } else {
            // Préparer les données
            Map<String, Object> userData = new HashMap<>();
            userData.put("nom", nom != null ? nom : "");
            userData.put("email", email);
            userData.put("nomUtilisateur", nomUtilisateur != null ? nomUtilisateur : email);
            userData.put("telephone", telephone != null ? telephone : "");
            userData.put("role", role != null ? role : "utilisateur");
            userData.put("statut", "actif");

            // Vérifier si c'est le premier utilisateur pour attribuer superadmin
            boolean isFirstUser = isFirstUser();
            
            // Initialiser la structure (sans le nœud entreprises)
            initializeUserStructure(userId, userData, isFirstUser);
        }
    }

    /**
     * Met à jour un utilisateur existant pour ajouter les champs manquants (planning, entreprises)
     * Firebase Realtime Database supprime les objets vides, donc on doit les recréer
     */
    public void updateExistingUserStructure(String userId) throws Exception {
        try {
            CompletableFuture<Void> future = new CompletableFuture<>();
            CountDownLatch latch = new CountDownLatch(1);

            // Vérifier ce qui existe déjà
            databaseReference.child("utilisateurs").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Map<String, Object> updates = new HashMap<>();
                        
                        // Ajouter planning s'il n'existe pas ou est null
                        if (!snapshot.hasChild("planning") || snapshot.child("planning").getValue() == null) {
                            // Créer avec un marqueur pour que Firebase le conserve
                            Map<String, Object> planning = new HashMap<>();
                            planning.put("_empty", false);
                            updates.put("planning", planning);
                        }
                        
                        // Supprimer le nœud entreprises s'il existe (migration vers nouvelle structure)
                        // Les entreprises sont maintenant dans entreprises/ au niveau racine, pas sous utilisateurs/
                        if (snapshot.hasChild("entreprises")) {
                            updates.put("entreprises", null);
                        }
                        
                        if (!updates.isEmpty()) {
                            databaseReference.child("utilisateurs").child(userId).updateChildren(updates, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError error, DatabaseReference ref) {
                                    if (error != null) {
                                        future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                                    } else {
                                        // Note: On garde _keep pour que Firebase conserve la structure
                                        // Firebase supprime les objets complètement vides
                                        future.complete(null);
                                    }
                                    latch.countDown();
                                }
                            });
                        } else {
                            future.complete(null);
                            latch.countDown();
                        }
                    } else {
                        future.complete(null);
                        latch.countDown();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);
            future.get();
        } catch (Exception e) {
            throw new Exception("Erreur lors de la mise à jour de la structure utilisateur: " + e.getMessage());
        }
    }

    /**
     * Récupère tous les utilisateurs depuis Realtime Database
     */
    public List<Map<String, Object>> getAllUsers() throws Exception {
        CompletableFuture<List<Map<String, Object>>> future = new CompletableFuture<>();
        CountDownLatch latch = new CountDownLatch(1);
        List<Map<String, Object>> usersList = new ArrayList<>();

        System.out.println("=== getAllUsers: Début de la récupération ===");

        databaseReference.child("utilisateurs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    System.out.println("=== getAllUsers: onDataChange appelé ===");
                    System.out.println("Snapshot existe: " + snapshot.exists());
                    
                    if (snapshot.exists()) {
                        System.out.println("Nombre d'enfants: " + snapshot.getChildrenCount());
                        
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            Map<String, Object> user = new HashMap<>();
                            String userId = userSnapshot.getKey();
                            System.out.println("User ID trouvé: " + userId);
                            
                            if (userId != null) {
                                user.put("userId", userId);
                                user.put("nom", userSnapshot.child("nom").getValue() != null ? userSnapshot.child("nom").getValue() : "");
                                user.put("prenom", userSnapshot.child("prenom").getValue() != null ? userSnapshot.child("prenom").getValue() : "");
                                user.put("email", userSnapshot.child("email").getValue() != null ? userSnapshot.child("email").getValue() : "");
                                user.put("nomUtilisateur", userSnapshot.child("nomUtilisateur").getValue() != null ? userSnapshot.child("nomUtilisateur").getValue() : "");
                                user.put("role", userSnapshot.child("role").getValue() != null ? userSnapshot.child("role").getValue() : "utilisateur");
                                user.put("statut", userSnapshot.child("statut").getValue() != null ? userSnapshot.child("statut").getValue() : "inactif");
                                user.put("telephone", userSnapshot.child("telephone").getValue() != null ? userSnapshot.child("telephone").getValue() : "");
                                user.put("numeroTelephone", userSnapshot.child("numeroTelephone").getValue() != null ? userSnapshot.child("numeroTelephone").getValue() : "");
                                user.put("dateCreation", userSnapshot.child("dateCreation").getValue());
                                
                                // Récupérer les horaires de travail
                                Object horairesTravail = userSnapshot.child("horairesTravail").getValue();
                                if (horairesTravail != null) {
                                    user.put("horairesTravail", horairesTravail);
                                    System.out.println("Horaires récupérés pour " + userId + ": " + horairesTravail);
                                } else {
                                    System.out.println("Aucun horaire trouvé pour " + userId);
                                }
                                
                                usersList.add(user);
                                System.out.println("Utilisateur ajouté: " + user);
                            }
                        }
                    } else {
                        System.out.println("Aucun utilisateur trouvé dans la base de données");
                    }
                    
                    System.out.println("Nombre total d'utilisateurs récupérés: " + usersList.size());
                    future.complete(usersList);
                } catch (Exception e) {
                    System.err.println("ERREUR dans onDataChange: " + e.getMessage());
                    e.printStackTrace();
                    future.completeExceptionally(e);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("ERREUR onCancelled: " + error.getMessage());
                future.completeExceptionally(new Exception("Erreur Firebase: " + error.getMessage()));
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            if (!completed) {
                System.err.println("TIMEOUT lors de la récupération des utilisateurs");
                throw new Exception("Timeout lors de la récupération des utilisateurs (15 secondes)");
            }
            List<Map<String, Object>> result = future.get();
            System.out.println("=== getAllUsers: Résultat final: " + (result != null ? result.size() : "null") + " utilisateurs ===");
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Interruption lors de la récupération des utilisateurs: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("ERREUR lors de la récupération des utilisateurs: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Erreur lors de la récupération des utilisateurs: " + e.getMessage());
        }
    }

    /**
     * Récupère un utilisateur par son ID
     */
    public Map<String, Object> getUserById(String userId) throws Exception {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        CountDownLatch latch = new CountDownLatch(1);

        databaseReference.child("utilisateurs").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("userId", userId);
                    user.put("nom", snapshot.child("nom").getValue());
                    user.put("prenom", snapshot.child("prenom").getValue());
                    user.put("email", snapshot.child("email").getValue());
                    user.put("nomUtilisateur", snapshot.child("nomUtilisateur").getValue());
                    user.put("role", snapshot.child("role").getValue());
                    user.put("statut", snapshot.child("statut").getValue());
                    user.put("telephone", snapshot.child("telephone").getValue());
                    user.put("numeroTelephone", snapshot.child("numeroTelephone").getValue());
                    user.put("dateCreation", snapshot.child("dateCreation").getValue());
                    
                    // Récupérer les horaires de travail
                    Object horairesTravail = snapshot.child("horairesTravail").getValue();
                    if (horairesTravail != null) {
                        user.put("horairesTravail", horairesTravail);
                        System.out.println("=== getUserById: Horaires récupérés pour " + userId + ": " + horairesTravail);
                    } else {
                        System.out.println("=== getUserById: Aucun horaire trouvé pour " + userId);
                    }
                    
                    future.complete(user);
                } else {
                    future.complete(null);
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
            return future.get();
        } catch (Exception e) {
            throw new Exception("Timeout ou erreur lors de la récupération de l'utilisateur");
        }
    }

    /**
     * Met à jour un utilisateur dans Realtime Database
     */
    public void updateUser(String userId, Map<String, Object> userData) throws Exception {
        try {
            Map<String, Object> updates = new HashMap<>();
            
            // Mettre à jour uniquement les champs fournis
            if (userData.containsKey("nom")) {
                updates.put("nom", userData.get("nom"));
            }
            if (userData.containsKey("prenom")) {
                updates.put("prenom", userData.get("prenom"));
            }
            if (userData.containsKey("email")) {
                updates.put("email", userData.get("email"));
            }
            if (userData.containsKey("nomUtilisateur")) {
                updates.put("nomUtilisateur", userData.get("nomUtilisateur"));
            }
            if (userData.containsKey("role")) {
                String role = (String) userData.get("role");
                // Ne pas permettre de créer un superadmin
                if (!"superadmin".equals(role)) {
                    updates.put("role", role);
                }
            }
            if (userData.containsKey("statut")) {
                updates.put("statut", userData.get("statut"));
            }
            if (userData.containsKey("telephone")) {
                updates.put("telephone", userData.get("telephone"));
            }
            if (userData.containsKey("numeroTelephone")) {
                updates.put("numeroTelephone", userData.get("numeroTelephone"));
                // Mettre à jour aussi telephone pour compatibilité
                if (!userData.containsKey("telephone")) {
                    updates.put("telephone", userData.get("numeroTelephone"));
                }
            }
            if (userData.containsKey("horairesTravail")) {
                Object horaires = userData.get("horairesTravail");
                System.out.println("=== Mise à jour des horaires pour userId " + userId + ": " + horaires);
                updates.put("horairesTravail", horaires);
            } else {
                System.out.println("=== Aucun horaire à mettre à jour pour userId " + userId);
            }
            
            if (!updates.isEmpty()) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                CountDownLatch latch = new CountDownLatch(1);

                databaseReference.child("utilisateurs").child(userId).updateChildren(updates, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError error, DatabaseReference ref) {
                        if (error != null) {
                            future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                        } else {
                            future.complete(null);
                        }
                        latch.countDown();
                    }
                });

                latch.await(10, TimeUnit.SECONDS);
                future.get();
            }
        } catch (Exception e) {
            throw new Exception("Erreur lors de la mise à jour de l'utilisateur: " + e.getMessage());
        }
    }

    /**
     * Récupère toutes les entreprises depuis entreprises/
     */
    public List<Map<String, Object>> getAllEnterprises() throws Exception {
        CompletableFuture<List<Map<String, Object>>> future = new CompletableFuture<>();
        CountDownLatch latch = new CountDownLatch(1);
        List<Map<String, Object>> entreprisesList = new ArrayList<>();

        System.out.println("=== getAllEnterprises: Début de la récupération ===");
        
        databaseReference.child("entreprises").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    System.out.println("=== getAllEnterprises: onDataChange appelé ===");
                    System.out.println("Snapshot existe: " + snapshot.exists());
                    
                    if (snapshot.exists()) {
                        System.out.println("Nombre d'enfants: " + snapshot.getChildrenCount());
                        
                        for (DataSnapshot entrepriseSnapshot : snapshot.getChildren()) {
                            String entrepriseId = entrepriseSnapshot.getKey();
                            System.out.println("Entreprise ID trouvé: " + entrepriseId);
                            
                            // Ignorer les clés spéciales comme "_empty"
                            if (entrepriseId != null && !entrepriseId.startsWith("_")) {
                                Map<String, Object> entreprise = new HashMap<>();
                                entreprise.put("entrepriseId", entrepriseId);
                                entreprise.put("nom", entrepriseSnapshot.child("nom").getValue() != null ? entrepriseSnapshot.child("nom").getValue() : "");
                                // Support ancien format (adresse) et nouveau format (rue, codePostal, ville)
                                if (entrepriseSnapshot.hasChild("rue")) {
                                    entreprise.put("rue", entrepriseSnapshot.child("rue").getValue() != null ? entrepriseSnapshot.child("rue").getValue() : "");
                                    entreprise.put("codePostal", entrepriseSnapshot.child("codePostal").getValue() != null ? entrepriseSnapshot.child("codePostal").getValue() : "");
                                    entreprise.put("ville", entrepriseSnapshot.child("ville").getValue() != null ? entrepriseSnapshot.child("ville").getValue() : "");
                                } else if (entrepriseSnapshot.hasChild("adresse")) {
                                    // Migration: convertir adresse en rue
                                    String adresse = entrepriseSnapshot.child("adresse").getValue() != null ? entrepriseSnapshot.child("adresse").getValue().toString() : "";
                                    entreprise.put("rue", adresse);
                                    entreprise.put("codePostal", "");
                                    entreprise.put("ville", "");
                                } else {
                                    entreprise.put("rue", "");
                                    entreprise.put("codePostal", "");
                                    entreprise.put("ville", "");
                                }
                                entreprise.put("email", entrepriseSnapshot.child("email").getValue() != null ? entrepriseSnapshot.child("email").getValue() : "");
                                entreprise.put("numero", entrepriseSnapshot.child("numero").getValue() != null ? entrepriseSnapshot.child("numero").getValue() : "");
                                entreprise.put("dateCreation", entrepriseSnapshot.child("dateCreation").getValue());
                                entreprisesList.add(entreprise);
                                System.out.println("Entreprise ajoutée: " + entreprise);
                            }
                        }
                    } else {
                        System.out.println("Aucune entreprise trouvée dans la base de données");
                    }
                    
                    System.out.println("Nombre total d'entreprises récupérées: " + entreprisesList.size());
                    future.complete(entreprisesList);
                } catch (Exception e) {
                    System.err.println("ERREUR dans onDataChange: " + e.getMessage());
                    e.printStackTrace();
                    future.completeExceptionally(e);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("ERREUR onCancelled: " + error.getMessage());
                future.completeExceptionally(new Exception("Erreur Firebase: " + error.getMessage()));
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            if (!completed) {
                System.err.println("TIMEOUT lors de la récupération des entreprises");
                throw new Exception("Timeout lors de la récupération des entreprises (15 secondes)");
            }
            List<Map<String, Object>> result = future.get();
            System.out.println("=== getAllEnterprises: Résultat final: " + (result != null ? result.size() : "null") + " entreprises ===");
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Interruption lors de la récupération des entreprises: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("ERREUR lors de la récupération des entreprises: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Erreur lors de la récupération des entreprises: " + e.getMessage());
        }
    }

    /**
     * Récupère toutes les entreprises d'un utilisateur
     * Pour l'instant, retourne toutes les entreprises (on pourra ajouter une liaison plus tard)
     */
    public List<Map<String, Object>> getUserEnterprises(String userId) throws Exception {
        // Pour l'instant, retourner toutes les entreprises
        // On pourra ajouter une table de liaison utilisateurs-entreprises plus tard si nécessaire
        return getAllEnterprises();
    }

    /**
     * Récupère une entreprise par son ID depuis entreprises/
     */
    public Map<String, Object> getEnterpriseById(String entrepriseId) throws Exception {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        CountDownLatch latch = new CountDownLatch(1);

        databaseReference.child("entreprises").child(entrepriseId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Map<String, Object> entreprise = new HashMap<>();
                        entreprise.put("entrepriseId", entrepriseId);
                        entreprise.put("nom", snapshot.child("nom").getValue() != null ? snapshot.child("nom").getValue() : "");
                        // Support ancien format (adresse) et nouveau format (rue, codePostal, ville)
                        if (snapshot.hasChild("rue")) {
                            entreprise.put("rue", snapshot.child("rue").getValue() != null ? snapshot.child("rue").getValue() : "");
                            entreprise.put("codePostal", snapshot.child("codePostal").getValue() != null ? snapshot.child("codePostal").getValue() : "");
                            entreprise.put("ville", snapshot.child("ville").getValue() != null ? snapshot.child("ville").getValue() : "");
                        } else if (snapshot.hasChild("adresse")) {
                            // Migration: convertir adresse en rue
                            String adresse = snapshot.child("adresse").getValue() != null ? snapshot.child("adresse").getValue().toString() : "";
                            entreprise.put("rue", adresse);
                            entreprise.put("codePostal", "");
                            entreprise.put("ville", "");
                        } else {
                            entreprise.put("rue", "");
                            entreprise.put("codePostal", "");
                            entreprise.put("ville", "");
                        }
                        entreprise.put("email", snapshot.child("email").getValue() != null ? snapshot.child("email").getValue() : "");
                        entreprise.put("numero", snapshot.child("numero").getValue() != null ? snapshot.child("numero").getValue() : "");
                        entreprise.put("dateCreation", snapshot.child("dateCreation").getValue());
                        future.complete(entreprise);
                    } else {
                        future.complete(null);
                    }
                    latch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                    latch.countDown();
                }
            });

        try {
            latch.await(10, TimeUnit.SECONDS);
            return future.get();
        } catch (Exception e) {
            throw new Exception("Timeout ou erreur lors de la récupération de l'entreprise");
        }
    }

    /**
     * Récupère l'ensemble des machines pour une entreprise
     */
    public List<Machine> getMachinesForEnterprise(String entrepriseId) throws Exception {
        CompletableFuture<List<Machine>> future = new CompletableFuture<>();
        CountDownLatch latch = new CountDownLatch(1);

        databaseReference.child("entreprises")
            .child(entrepriseId)
            .child("machines")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    try {
                        List<Machine> machines = new ArrayList<>();
                        if (snapshot.exists()) {
                            for (DataSnapshot machineSnapshot : snapshot.getChildren()) {
                                String machineId = machineSnapshot.getKey();
                                if (machineId != null && !machineId.startsWith("_")) {
                                    machines.add(mapMachineSnapshot(machineSnapshot, entrepriseId, machineId));
                                }
                            }
                        }
                        future.complete(machines);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    } finally {
                        latch.countDown();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(new Exception("Erreur Firebase: " + error.getMessage()));
                    latch.countDown();
                }
            });

        try {
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            if (!completed) {
                throw new Exception("Timeout lors de la récupération des machines (15 secondes)");
            }
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Interruption lors de la récupération des machines: " + e.getMessage());
        } catch (Exception e) {
            throw new Exception("Erreur lors de la récupération des machines: " + e.getMessage());
        }
    }

    /**
     * Récupère une machine spécifique pour une entreprise
     */
    public Machine getMachineById(String entrepriseId, String machineId) throws Exception {
        CompletableFuture<Machine> future = new CompletableFuture<>();
        CountDownLatch latch = new CountDownLatch(1);

        databaseReference.child("entreprises")
            .child(entrepriseId)
            .child("machines")
            .child(machineId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    try {
                        if (snapshot.exists()) {
                            future.complete(mapMachineSnapshot(snapshot, entrepriseId, machineId));
                        } else {
                            future.complete(null);
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    } finally {
                        latch.countDown();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(new Exception("Erreur Firebase: " + error.getMessage()));
                    latch.countDown();
                }
            });

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                throw new Exception("Timeout lors de la récupération de la machine (10 secondes)");
            }
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Interruption lors de la récupération de la machine: " + e.getMessage());
        } catch (Exception e) {
            throw new Exception("Erreur lors de la récupération de la machine: " + e.getMessage());
        }
    }

    /**
     * Crée une machine sous une entreprise
     */
    public String createMachine(String entrepriseId, Machine machine) throws Exception {
        try {
            String machineId = databaseReference.child("entreprises")
                .child(entrepriseId)
                .child("machines")
                .push()
                .getKey();

            if (machineId == null) {
                throw new Exception("Impossible de générer un identifiant pour la machine");
            }

            Map<String, Object> machineData = serializeMachine(machine);
            long now = System.currentTimeMillis();
            machineData.put("dateCreation", now);
            machineData.put("dateMiseAJour", now);

            CompletableFuture<Void> future = new CompletableFuture<>();
            CountDownLatch latch = new CountDownLatch(1);

            databaseReference.child("entreprises")
                .child(entrepriseId)
                .child("machines")
                .child(machineId)
                .setValue(machineData, (error, ref) -> {
                    if (error != null) {
                        future.completeExceptionally(new Exception("Erreur Firebase: " + error.getMessage()));
                    } else {
                        future.complete(null);
                    }
                    latch.countDown();
                });

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                throw new Exception("Timeout lors de la création de la machine (10 secondes)");
            }
            future.get();
            return machineId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Interruption lors de la création de la machine: " + e.getMessage());
        }
    }

    /**
     * Met à jour une machine existante
     */
    public void updateMachine(String entrepriseId, String machineId, Machine machine) throws Exception {
        try {
            Map<String, Object> machineData = serializeMachine(machine);
            machineData.put("dateMiseAJour", System.currentTimeMillis());

            CompletableFuture<Void> future = new CompletableFuture<>();
            CountDownLatch latch = new CountDownLatch(1);

            databaseReference.child("entreprises")
                .child(entrepriseId)
                .child("machines")
                .child(machineId)
                .updateChildren(machineData, (error, ref) -> {
                    if (error != null) {
                        future.completeExceptionally(new Exception("Erreur Firebase: " + error.getMessage()));
                    } else {
                        future.complete(null);
                    }
                    latch.countDown();
                });

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                throw new Exception("Timeout lors de la mise à jour de la machine (10 secondes)");
            }
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Interruption lors de la mise à jour de la machine: " + e.getMessage());
        }
    }

    /**
     * Supprime une machine
     */
    public void deleteMachine(String entrepriseId, String machineId) throws Exception {
        CompletableFuture<Void> future = new CompletableFuture<>();
        CountDownLatch latch = new CountDownLatch(1);

        databaseReference.child("entreprises")
            .child(entrepriseId)
            .child("machines")
            .child(machineId)
            .removeValue((error, ref) -> {
                if (error != null) {
                    future.completeExceptionally(new Exception("Erreur Firebase: " + error.getMessage()));
                } else {
                    future.complete(null);
                }
                latch.countDown();
            });

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                throw new Exception("Timeout lors de la suppression de la machine (10 secondes)");
            }
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Interruption lors de la suppression de la machine: " + e.getMessage());
        } catch (Exception e) {
            throw new Exception("Erreur lors de la suppression de la machine: " + e.getMessage());
        }
    }

    private Machine mapMachineSnapshot(DataSnapshot snapshot, String entrepriseId, String machineId) {
        Machine machine = new Machine();
        machine.setMachineId(machineId);
        machine.setEntrepriseId(entrepriseId);
        machine.setNom(snapshot.child("nom").getValue() != null ? snapshot.child("nom").getValue().toString() : "");
        machine.setNumeroSerie(snapshot.child("numeroSerie").getValue() != null ? snapshot.child("numeroSerie").getValue().toString() : "");
        machine.setEmplacement(snapshot.child("emplacement").getValue() != null ? snapshot.child("emplacement").getValue().toString() : "");
        machine.setNotes(snapshot.child("notes").getValue() != null ? snapshot.child("notes").getValue().toString() : "");

        Object rawPhotos = snapshot.child("photos").getValue();
        List<String> photos = new ArrayList<>();
        if (rawPhotos instanceof List<?> list) {
            for (Object value : list) {
                if (value instanceof String photo && !photo.isBlank()) {
                    photos.add(photo);
                }
            }
        } else if (rawPhotos instanceof Map<?, ?> map) {
            List<String> keys = new ArrayList<>();
            for (Object key : map.keySet()) {
                if (key instanceof String stringKey) {
                    keys.add(stringKey);
                }
            }
            keys.sort(String::compareTo);
            for (String key : keys) {
                Object value = map.get(key);
                if (value instanceof String photo && !photo.isBlank()) {
                    photos.add(photo);
                }
            }
        } else if (rawPhotos instanceof String photo && !photo.isBlank()) {
            photos.add(photo);
        }
        machine.setPhotos(photos);

        if (snapshot.child("dateCreation").getValue() instanceof Number numberCreation) {
            machine.setDateCreation(numberCreation.longValue());
        }
        if (snapshot.child("dateMiseAJour").getValue() instanceof Number numberUpdate) {
            machine.setDateMiseAJour(numberUpdate.longValue());
        }
        return machine;
    }

    private Map<String, Object> serializeMachine(Machine machine) {
        Map<String, Object> data = new HashMap<>();
        data.put("nom", machine.getNom());
        data.put("numeroSerie", machine.getNumeroSerie());
        data.put("emplacement", machine.getEmplacement());
        data.put("notes", machine.getNotes());
        data.put("photos", machine.getPhotos());
        return data;
    }

    /**
     * Met à jour une entreprise dans entreprises/
     */
    public void updateEnterprise(String entrepriseId, Map<String, Object> entrepriseData) throws Exception {
        try {
            Map<String, Object> updates = new HashMap<>();
            
            if (entrepriseData.containsKey("nom")) {
                updates.put("nom", entrepriseData.get("nom"));
            }
            if (entrepriseData.containsKey("adresse")) {
                updates.put("adresse", entrepriseData.get("adresse"));
            }
            if (entrepriseData.containsKey("numero")) {
                updates.put("numero", entrepriseData.get("numero"));
            }
            
            if (!updates.isEmpty()) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                CountDownLatch latch = new CountDownLatch(1);

                databaseReference.child("entreprises").child(entrepriseId)
                    .updateChildren(updates, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError error, DatabaseReference ref) {
                            if (error != null) {
                                future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                            } else {
                                future.complete(null);
                            }
                            latch.countDown();
                        }
                    });

                latch.await(10, TimeUnit.SECONDS);
                future.get();
            }
        } catch (Exception e) {
            throw new Exception("Erreur lors de la mise à jour de l'entreprise: " + e.getMessage());
        }
    }

    /**
     * Supprime une entreprise depuis entreprises/
     */
    public void deleteEnterprise(String entrepriseId) throws Exception {
        try {
            CompletableFuture<Void> future = new CompletableFuture<>();
            CountDownLatch latch = new CountDownLatch(1);

            databaseReference.child("entreprises").child(entrepriseId)
                .removeValue(new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError error, DatabaseReference ref) {
                        if (error != null) {
                            future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                        } else {
                            future.complete(null);
                        }
                        latch.countDown();
                    }
                });

            latch.await(10, TimeUnit.SECONDS);
            future.get();
        } catch (Exception e) {
            throw new Exception("Erreur lors de la suppression de l'entreprise: " + e.getMessage());
        }
    }

    /**
     * Copie une entreprise d'un utilisateur vers un autre utilisateur
     */
    public void copyEnterpriseToUser(String sourceUserId, String entrepriseId, String targetUserId) throws Exception {
        try {
            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
            CountDownLatch latch = new CountDownLatch(1);

            // Récupérer l'entreprise source
            databaseReference.child("utilisateurs").child(sourceUserId)
                .child("entreprises").child(entrepriseId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> entrepriseData = (Map<String, Object>) snapshot.getValue();
                            future.complete(entrepriseData);
                        } else {
                            future.complete(null);
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                        latch.countDown();
                    }
                });

            latch.await(5, TimeUnit.SECONDS);
            Map<String, Object> entrepriseData = future.get();

            if (entrepriseData != null) {
                // Copier l'entreprise vers le nouvel utilisateur avec un nouvel ID
                String newEntrepriseId = databaseReference.child("utilisateurs")
                    .child(targetUserId)
                    .child("entreprises")
                    .push()
                    .getKey();

                CompletableFuture<Void> copyFuture = new CompletableFuture<>();
                CountDownLatch copyLatch = new CountDownLatch(1);

                databaseReference.child("utilisateurs").child(targetUserId)
                    .child("entreprises").child(newEntrepriseId)
                    .setValue(entrepriseData, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError error, DatabaseReference ref) {
                            if (error != null) {
                                copyFuture.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                            } else {
                                copyFuture.complete(null);
                            }
                            copyLatch.countDown();
                        }
                    });

                copyLatch.await(5, TimeUnit.SECONDS);
                copyFuture.get();
            }
        } catch (Exception e) {
            throw new Exception("Erreur lors de la copie de l'entreprise: " + e.getMessage());
        }
    }

    /**
     * Supprime un utilisateur de Realtime Database
     * Note: La suppression dans Firebase Authentication doit être faite séparément par le contrôleur
     */
    public void deleteUser(String userId) throws Exception {
        try {
            CompletableFuture<Void> future = new CompletableFuture<>();
            CountDownLatch latch = new CountDownLatch(1);

            databaseReference.child("utilisateurs").child(userId).removeValue(new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError error, DatabaseReference ref) {
                    if (error != null) {
                        future.completeExceptionally(new Exception("Erreur: " + error.getMessage()));
                    } else {
                        future.complete(null);
                    }
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);
            future.get();
        } catch (Exception e) {
            throw new Exception("Erreur lors de la suppression de l'utilisateur: " + e.getMessage());
        }
    }

    /**
     * Supprime un utilisateur de Realtime Database ET de Firebase Authentication
     */
    public void deleteUserFromAuthAndRealtime(String userId, com.maintenance.maintenance.service.FirebaseAuthService firebaseAuthService) throws Exception {
        try {
            // Supprimer d'abord de Realtime Database
            deleteUser(userId);
            
            // Ensuite supprimer de Firebase Authentication
            if (firebaseAuthService.userExistsInAuth(userId)) {
                firebaseAuthService.deleteUser(userId);
            }
        } catch (Exception e) {
            throw new Exception("Erreur lors de la suppression complète de l'utilisateur: " + e.getMessage());
        }
    }
}

