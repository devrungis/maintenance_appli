package com.maintenance.maintenance.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.ListUsersPage;
import com.google.firebase.auth.UserRecord;
import com.maintenance.maintenance.service.FirebaseAuthService;
import com.maintenance.maintenance.service.FirebaseRealtimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    /**
     * Vérifie si l'utilisateur connecté est superadmin
     */
    private boolean isSuperAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String role = (String) session.getAttribute("role");
            return "superadmin".equals(role);
        }
        return false;
    }

    /**
     * Endpoint pour vérifier le rôle de l'utilisateur connecté
     */
    @GetMapping("/check-role")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkRole(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            String role = (String) session.getAttribute("role");
            String userId = (String) session.getAttribute("userId");
            response.put("success", true);
            response.put("role", role != null ? role : "utilisateur");
            response.put("userId", userId);
            response.put("isSuperAdmin", "superadmin".equals(role));
        } else {
            response.put("success", false);
            response.put("error", "Non connecté");
            response.put("role", "utilisateur");
            response.put("isSuperAdmin", false);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère tous les utilisateurs depuis Realtime Database
     * Accessible uniquement par le superadmin
     */
    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllUsers(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        if (!isSuperAdmin(request)) {
            response.put("success", false);
            response.put("error", "Accès refusé. Seul le superadmin peut accéder à cette fonctionnalité.");
            return ResponseEntity.status(403).body(response);
        }
        
        try {
            List<Map<String, Object>> usersList = firebaseRealtimeService.getAllUsers();
            response.put("success", true);
            response.put("users", usersList);
            response.put("count", usersList.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erreur lors de la récupération des utilisateurs: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Récupère un utilisateur par son ID
     */
    @GetMapping("/{userId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String userId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        if (!isSuperAdmin(request)) {
            response.put("success", false);
            response.put("error", "Accès refusé. Seul le superadmin peut accéder à cette fonctionnalité.");
            return ResponseEntity.status(403).body(response);
        }
        
        try {
            Map<String, Object> user = firebaseRealtimeService.getUserById(userId);
            if (user != null) {
                response.put("success", true);
                response.put("user", user);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Utilisateur non trouvé");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Crée un nouvel utilisateur avec initialisation de la structure Realtime Database complète
     * Accessible uniquement par le superadmin
     */
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> userData, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        if (!isSuperAdmin(request)) {
            response.put("success", false);
            response.put("error", "Accès refusé. Seul le superadmin peut créer des utilisateurs.");
            return ResponseEntity.status(403).body(response);
        }
        
        try {
            String email = (String) userData.get("email");
            String password = (String) userData.get("password");
            String nom = (String) userData.getOrDefault("nom", "");
            String nomUtilisateur = (String) userData.getOrDefault("nomUtilisateur", email);
            String telephone = (String) userData.getOrDefault("telephone", "");
            String role = (String) userData.getOrDefault("role", "utilisateur");
            
            // Le superadmin ne peut pas créer un autre superadmin via l'interface
            // Seul le premier utilisateur peut être superadmin
            if ("superadmin".equals(role)) {
                response.put("success", false);
                response.put("error", "Le rôle superadmin ne peut pas être attribué manuellement.");
                return ResponseEntity.badRequest().body(response);
            }

            // 2. Créer l'utilisateur dans Firebase Authentication
            UserRecord userRecord = firebaseAuthService.createUser(email, password);
            String userId = userRecord.getUid();

            // 3. Préparer les données utilisateur pour Realtime Database
            Map<String, Object> realtimeUserData = new HashMap<>();
            realtimeUserData.put("nom", nom);
            realtimeUserData.put("email", email);
            realtimeUserData.put("nomUtilisateur", nomUtilisateur);
            realtimeUserData.put("motDePasse", password); // Note: devrait être hashé
            realtimeUserData.put("role", role);
            realtimeUserData.put("statut", "actif");
            realtimeUserData.put("telephone", telephone);

            // 4. Initialiser la structure Realtime Database complète (isFirstUser = false car créé par superadmin)
            firebaseRealtimeService.initializeUserStructure(userId, realtimeUserData, false);

            response.put("success", true);
            response.put("userId", userId);
            response.put("role", role);
            response.put("message", "Utilisateur créé avec succès");
            
            return ResponseEntity.ok(response);
            
        } catch (FirebaseAuthException e) {
            response.put("success", false);
            response.put("error", "Erreur lors de la création de l'utilisateur: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erreur lors de l'initialisation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Crée une entreprise et initialise sa structure
     */
    @PostMapping("/{userId}/enterprises/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createEnterprise(
            @PathVariable String userId,
            @RequestBody Map<String, Object> entrepriseData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entrepriseId = firebaseRealtimeService.createEnterprise(entrepriseData);
            response.put("success", true);
            response.put("entrepriseId", entrepriseId);
            response.put("message", "Entreprise créée avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Initialise l'arborescence pour un utilisateur existant dans Firebase Authentication
     * Utile pour synchroniser les utilisateurs existants
     */
    @PostMapping("/sync/{userId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> syncExistingUser(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Récupérer les informations de l'utilisateur depuis Firebase Authentication
            UserRecord userRecord = firebaseAuthService.getUserByEmail(userId);
            
            // Si userId est un email, récupérer l'UID
            String uid = userRecord.getUid();
            String email = userRecord.getEmail();
            String nom = userRecord.getDisplayName() != null ? userRecord.getDisplayName() : "";
            String telephone = userRecord.getPhoneNumber() != null ? userRecord.getPhoneNumber() : "";
            
            // Initialiser l'arborescence
            firebaseRealtimeService.initializeExistingUser(
                uid, 
                email, 
                nom, 
                email.split("@")[0], // Utiliser la partie avant @ comme nomUtilisateur
                telephone,
                null // Le rôle sera déterminé automatiquement (superadmin si premier)
            );
            
            response.put("success", true);
            response.put("userId", uid);
            response.put("message", "Arborescence initialisée avec succès");
            return ResponseEntity.ok(response);
            
        } catch (com.google.firebase.auth.FirebaseAuthException e) {
            // Si userId est un UID, essayer directement
            try {
                firebaseAuthService.getUserByEmail(userId);
                // Si ça passe, userId est un email
                return syncExistingUser(userId);
            } catch (Exception ex) {
                // userId est probablement un UID
                try {
                    UserRecord userRecord = firebaseAuthService.getUserById(userId);
                    String email = userRecord.getEmail();
                    String nom = userRecord.getDisplayName() != null ? userRecord.getDisplayName() : "";
                    String telephone = userRecord.getPhoneNumber() != null ? userRecord.getPhoneNumber() : "";
                    
                    firebaseRealtimeService.initializeExistingUser(
                        userId,
                        email,
                        nom,
                        email.split("@")[0],
                        telephone,
                        null
                    );
                    
                    response.put("success", true);
                    response.put("userId", userId);
                    response.put("message", "Arborescence initialisée avec succès");
                    return ResponseEntity.ok(response);
                } catch (Exception ex2) {
                    response.put("success", false);
                    response.put("error", "Utilisateur non trouvé: " + ex2.getMessage());
                    return ResponseEntity.badRequest().body(response);
                }
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Synchronise tous les utilisateurs existants dans Firebase Authentication
     * Initialise leur arborescence dans Realtime Database
     */
    @PostMapping("/sync-all")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> syncAllUsers() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Lister tous les utilisateurs (première page)
            ListUsersPage page = firebaseAuthService.listAllUsers();
            List<String> syncedUsers = new ArrayList<>();
            List<String> skippedUsers = new ArrayList<>();
            
            while (page != null) {
                for (ExportedUserRecord user : page.getValues()) {
                    try {
                        String uid = user.getUid();
                        String email = user.getEmail();
                        
                        // Vérifier si l'utilisateur existe déjà dans Realtime Database
                        if (!firebaseRealtimeService.userExistsInRealtime(uid)) {
                            String nom = user.getDisplayName() != null ? user.getDisplayName() : "";
                            String telephone = user.getPhoneNumber() != null ? user.getPhoneNumber() : "";
                            
                            firebaseRealtimeService.initializeExistingUser(
                                uid,
                                email,
                                nom,
                                email != null ? email.split("@")[0] : "user",
                                telephone,
                                null
                            );
                            syncedUsers.add(uid);
                        } else {
                            skippedUsers.add(uid);
                        }
                    } catch (Exception e) {
                        skippedUsers.add(user.getUid() + " (erreur: " + e.getMessage() + ")");
                    }
                }
                
                page = page.getNextPage();
            }
            
            response.put("success", true);
            response.put("synced", syncedUsers.size());
            response.put("skipped", skippedUsers.size());
            response.put("syncedUsers", syncedUsers);
            response.put("skippedUsers", skippedUsers);
            response.put("message", String.format("%d utilisateur(s) synchronisé(s), %d ignoré(s)", syncedUsers.size(), skippedUsers.size()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erreur lors de la synchronisation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Met à jour un utilisateur
     * Accessible uniquement par le superadmin
     */
    @PutMapping("/{userId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String userId,
            @RequestBody Map<String, Object> userData,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        if (!isSuperAdmin(request)) {
            response.put("success", false);
            response.put("error", "Accès refusé. Seul le superadmin peut modifier des utilisateurs.");
            return ResponseEntity.status(403).body(response);
        }
        
        try {
            // Mettre à jour dans Firebase Authentication si email ou password changent
            if (userData.containsKey("email") || userData.containsKey("password")) {
                UserRecord.UpdateRequest updateRequest = new UserRecord.UpdateRequest(userId);
                if (userData.containsKey("email")) {
                    updateRequest.setEmail((String) userData.get("email"));
                }
                if (userData.containsKey("password")) {
                    updateRequest.setPassword((String) userData.get("password"));
                }
                firebaseAuthService.updateUser(userId, updateRequest);
            }
            
            // Mettre à jour dans Realtime Database
            firebaseRealtimeService.updateUser(userId, userData);
            
            response.put("success", true);
            response.put("message", "Utilisateur mis à jour avec succès");
            return ResponseEntity.ok(response);
            
        } catch (FirebaseAuthException e) {
            response.put("success", false);
            response.put("error", "Erreur lors de la mise à jour: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Supprime un utilisateur
     * Accessible uniquement par le superadmin
     */
    @DeleteMapping("/{userId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String userId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        if (!isSuperAdmin(request)) {
            response.put("success", false);
            response.put("error", "Accès refusé. Seul le superadmin peut supprimer des utilisateurs.");
            return ResponseEntity.status(403).body(response);
        }
        
        try {
            // Supprimer de Firebase Authentication
            firebaseAuthService.deleteUser(userId);
            
            // Supprimer de Realtime Database
            firebaseRealtimeService.deleteUser(userId);
            
            response.put("success", true);
            response.put("message", "Utilisateur supprimé avec succès");
            return ResponseEntity.ok(response);
            
        } catch (FirebaseAuthException e) {
            response.put("success", false);
            response.put("error", "Erreur lors de la suppression: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

