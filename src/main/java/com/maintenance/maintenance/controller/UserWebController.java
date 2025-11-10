package com.maintenance.maintenance.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.maintenance.maintenance.service.FirebaseAuthService;
import com.maintenance.maintenance.service.FirebaseRealtimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/users")
public class UserWebController {

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
     * Liste tous les utilisateurs
     */
    @GetMapping
    public String listUsers(Model model, HttpServletRequest request) {
        System.out.println("=== UserWebController.listUsers ===");
        
        // Vérifier que l'utilisateur est superadmin
        if (!isSuperAdmin(request)) {
            System.out.println("=== Accès refusé - Pas superadmin ===");
            return "redirect:/login";
        }

        try {
            List<Map<String, Object>> users = firebaseRealtimeService.getAllUsers();
            System.out.println("=== Nombre d'utilisateurs récupérés: " + (users != null ? users.size() : 0) + " ===");
            
            model.addAttribute("users", users != null ? users : new ArrayList<>());
            return "users/list";
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des utilisateurs: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors de la récupération des utilisateurs: " + e.getMessage());
            model.addAttribute("users", new ArrayList<>());
            return "users/list";
        }
    }

    /**
     * Affiche le formulaire de création d'utilisateur
     */
    @GetMapping("/create")
    public String showCreateForm(Model model, HttpServletRequest request) {
        System.out.println("=== UserWebController.showCreateForm ===");
        
        // Vérifier que l'utilisateur est superadmin
        if (!isSuperAdmin(request)) {
            System.out.println("=== Accès refusé - Pas superadmin ===");
            return "redirect:/login";
        }

        // Liste des jours de la semaine
        String[] jours = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
        model.addAttribute("jours", jours);
        
        return "users/create";
    }

    /**
     * Crée un nouvel utilisateur
     */
    @PostMapping("/create")
    public String createUser(
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String numeroTelephone,
            @RequestParam String role,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        
        System.out.println("=== UserWebController.createUser ===");
        System.out.println("Nom: " + nom + ", Prenom: " + prenom + ", Email: " + email + ", Role: " + role);
        
        // Vérifier que l'utilisateur est superadmin
        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Accès refusé. Seul le superadmin peut créer des utilisateurs.");
            return "redirect:/login";
        }

        // Validation
        if (nom == null || nom.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Le nom est obligatoire.");
            return "redirect:/users/create";
        }
        if (prenom == null || prenom.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Le prénom est obligatoire.");
            return "redirect:/users/create";
        }
        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "L'email est obligatoire.");
            return "redirect:/users/create";
        }
        if (password == null || password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Le mot de passe doit contenir au moins 6 caractères.");
            return "redirect:/users/create";
        }
        if (numeroTelephone == null || numeroTelephone.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Le numéro de téléphone est obligatoire.");
            return "redirect:/users/create";
        }
        if (role == null || (!role.equals("admin") && !role.equals("technicien") && !role.equals("superadmin"))) {
            redirectAttributes.addFlashAttribute("error", "Le rôle doit être admin, technicien ou superadmin.");
            return "redirect:/users/create";
        }

        // Le superadmin ne peut pas créer un autre superadmin via l'interface
        if ("superadmin".equals(role)) {
            redirectAttributes.addFlashAttribute("error", "Le rôle superadmin ne peut pas être attribué manuellement.");
            return "redirect:/users/create";
        }

        try {
            // 1. Créer l'utilisateur dans Firebase Authentication
            UserRecord userRecord = firebaseAuthService.createUser(email, password);
            String userId = userRecord.getUid();
            System.out.println("=== Utilisateur créé dans Firebase Auth avec ID: " + userId + " ===");

            // 2. Construire les horaires de travail à partir de tous les paramètres
            Map<String, Object> horairesTravail = new HashMap<>();
            String[] jours = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
            
            System.out.println("=== Paramètres reçus pour création ===");
            for (String jour : jours) {
                Map<String, Object> horaireJour = new HashMap<>();
                
                // Les paramètres arrivent avec le préfixe "heureDebut_", "heureFin_", "actif_"
                String debutKey = "heureDebut_" + jour;
                String finKey = "heureFin_" + jour;
                String actifKey = "actif_" + jour;
                
                String debut = allParams.getOrDefault(debutKey, "");
                String fin = allParams.getOrDefault(finKey, "");
                boolean isActif = allParams.containsKey(actifKey) && "true".equals(allParams.get(actifKey));
                
                System.out.println(jour + ": debut=" + debut + ", fin=" + fin + ", actif=" + isActif);
                
                horaireJour.put("heureDebut", debut);
                horaireJour.put("heureFin", fin);
                horaireJour.put("actif", isActif);
                horairesTravail.put(jour, horaireJour);
            }

            // 3. Préparer les données utilisateur pour Realtime Database
            Map<String, Object> realtimeUserData = new HashMap<>();
            realtimeUserData.put("nom", nom);
            realtimeUserData.put("prenom", prenom);
            realtimeUserData.put("email", email);
            realtimeUserData.put("nomUtilisateur", email.split("@")[0]); // Utiliser la partie avant @ comme nomUtilisateur
            realtimeUserData.put("motDePasse", password); // Note: devrait être hashé
            realtimeUserData.put("role", role);
            realtimeUserData.put("statut", "actif");
            realtimeUserData.put("telephone", numeroTelephone);
            realtimeUserData.put("numeroTelephone", numeroTelephone);
            realtimeUserData.put("horairesTravail", horairesTravail);

            // 4. Initialiser la structure Realtime Database
            firebaseRealtimeService.initializeUserStructure(userId, realtimeUserData, false);

            System.out.println("=== Utilisateur créé avec succès ===");
            redirectAttributes.addFlashAttribute("success", "Utilisateur créé avec succès.");
            return "redirect:/users";
            
        } catch (FirebaseAuthException e) {
            System.err.println("Erreur Firebase Auth: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création de l'utilisateur: " + e.getMessage());
            return "redirect:/users/create";
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
            return "redirect:/users/create";
        }
    }

    /**
     * Affiche le formulaire de modification d'utilisateur
     */
    @GetMapping("/{userId}/edit")
    public String showEditForm(@PathVariable String userId, Model model, HttpServletRequest request) {
        System.out.println("=== UserWebController.showEditForm pour userId: " + userId + " ===");
        
        HttpSession session = request.getSession(false);
        String currentUserId = (String) (session != null ? session.getAttribute("userId") : null);
        String currentUserRole = (String) (session != null ? session.getAttribute("role") : null);
        
        System.out.println("=== Vérification des permissions ===");
        System.out.println("Current user ID: " + currentUserId);
        System.out.println("Current user role: " + currentUserRole);
        System.out.println("Target user ID: " + userId);
        
        // Permettre si :
        // 1. L'utilisateur est superadmin (peut modifier n'importe qui)
        // 2. OU l'utilisateur modifie son propre compte
        boolean isSuperAdmin = "superadmin".equals(currentUserRole);
        boolean isOwnProfile = userId.equals(currentUserId);
        
        if (!isSuperAdmin && !isOwnProfile) {
            System.out.println("=== Accès refusé - Ni superadmin ni modification de son propre profil ===");
            return "redirect:/login";
        }

        try {
            Map<String, Object> user = firebaseRealtimeService.getUserById(userId);
            if (user == null) {
                model.addAttribute("error", "Utilisateur non trouvé.");
                return "redirect:/users";
            }
            
            String targetUserRole = (String) user.get("role");
            System.out.println("=== Rôle de l'utilisateur cible: " + targetUserRole);
            
            // SEUL un utilisateur non-superadmin ne peut pas modifier un compte superadmin
            // Le superadmin PEUT modifier d'autres superadmins
            if (!isSuperAdmin && "superadmin".equals(targetUserRole)) {
                System.out.println("=== Accès refusé - Tentative de modification d'un superadmin par un non-superadmin ===");
                model.addAttribute("error", "Vous n'avez pas la permission de modifier ce compte.");
                return "redirect:/dashboard";
            }
            
            System.out.println("=== Accès autorisé pour modification ===");

            // Liste des jours de la semaine
            String[] jours = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
            model.addAttribute("jours", jours);
            model.addAttribute("user", user);
            model.addAttribute("isOwnProfile", isOwnProfile);
            model.addAttribute("isSuperAdmin", isSuperAdmin);
            
            System.out.println("=== Utilisateur récupéré pour édition ===");
            System.out.println("Horaires: " + user.get("horairesTravail"));
            
            return "users/edit";
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
            return "redirect:/users";
        }
    }

    /**
     * Met à jour un utilisateur
     */
    @PostMapping("/{userId}/edit")
    public String updateUser(
            @PathVariable String userId,
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam String numeroTelephone,
            @RequestParam String role,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        
        System.out.println("=== UserWebController.updateUser pour userId: " + userId + " ===");
        
        HttpSession session = request.getSession(false);
        String currentUserId = (String) (session != null ? session.getAttribute("userId") : null);
        String currentUserRole = (String) (session != null ? session.getAttribute("role") : null);
        
        System.out.println("=== Vérification des permissions pour mise à jour ===");
        System.out.println("Current user ID: " + currentUserId);
        System.out.println("Current user role: " + currentUserRole);
        System.out.println("Target user ID: " + userId);
        
        // Permettre si :
        // 1. L'utilisateur est superadmin (peut modifier n'importe qui)
        // 2. OU l'utilisateur modifie son propre compte (mais ne peut pas changer son rôle)
        boolean isSuperAdmin = "superadmin".equals(currentUserRole);
        boolean isOwnProfile = userId.equals(currentUserId);
        
        if (!isSuperAdmin && !isOwnProfile) {
            System.out.println("=== Accès refusé - Ni superadmin ni modification de son propre profil ===");
            redirectAttributes.addFlashAttribute("error", "Accès refusé.");
            return "redirect:/login";
        }
        
        // Vérifier le rôle de l'utilisateur cible
        String targetUserRole = null;
        Map<String, Object> existingUser = null;
        try {
            existingUser = firebaseRealtimeService.getUserById(userId);
            if (existingUser != null) {
                targetUserRole = (String) existingUser.get("role");
                System.out.println("=== Rôle de l'utilisateur cible: " + targetUserRole);
                
                // SEUL un utilisateur non-superadmin ne peut pas modifier un compte superadmin
                // Le superadmin PEUT modifier d'autres superadmins
                if (!isSuperAdmin && "superadmin".equals(targetUserRole)) {
                    System.out.println("=== Accès refusé - Tentative de modification d'un superadmin par un non-superadmin ===");
                    redirectAttributes.addFlashAttribute("error", "Vous n'avez pas la permission de modifier ce compte.");
                    return "redirect:/dashboard";
                }
                
                // Si c'est son propre profil et qu'il n'est pas superadmin, il ne peut pas changer son rôle
                if (isOwnProfile && !isSuperAdmin) {
                    role = targetUserRole; // Forcer le rôle actuel
                    System.out.println("=== Rôle forcé à: " + role + " (propre profil, non-superadmin) ===");
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Utilisateur non trouvé.");
                return "redirect:/users";
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la récupération de l'utilisateur.");
            return "redirect:/users";
        }

        try {
            // L'utilisateur existant a déjà été récupéré plus haut dans la vérification des permissions
            System.out.println("=== Début de la mise à jour de l'utilisateur ===");
            
            // 1. Mettre à jour dans Firebase Authentication si email ou password changent
            if (password != null && !password.trim().isEmpty()) {
                if (password.length() < 6) {
                    redirectAttributes.addFlashAttribute("error", "Le mot de passe doit contenir au moins 6 caractères.");
                    return "redirect:/users/" + userId + "/edit";
                }
                System.out.println("=== Mise à jour du mot de passe ===");
                firebaseAuthService.updateUserPassword(userId, password);
            }
            
            // Mettre à jour l'email si nécessaire
            String existingEmail = (String) existingUser.get("email");
            if (!email.equals(existingEmail)) {
                System.out.println("=== Mise à jour de l'email: " + existingEmail + " -> " + email + " ===");
                firebaseAuthService.updateUserEmail(userId, email);
            }

            // 2. Récupérer les horaires de travail existants
            Map<String, Object> horairesTravail = new HashMap<>();
            @SuppressWarnings("unchecked")
            Map<String, Object> existingHoraires = (Map<String, Object>) existingUser.get("horairesTravail");
            
            // Si les horaires existent, les copier d'abord
            if (existingHoraires != null) {
                for (Map.Entry<String, Object> entry : existingHoraires.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> existingHoraire = (Map<String, Object>) entry.getValue();
                    Map<String, Object> horaireJour = new HashMap<>();
                    horaireJour.put("heureDebut", existingHoraire.getOrDefault("heureDebut", ""));
                    horaireJour.put("heureFin", existingHoraire.getOrDefault("heureFin", ""));
                    horaireJour.put("actif", existingHoraire.getOrDefault("actif", false));
                    horairesTravail.put(entry.getKey(), horaireJour);
                }
            }
            
            // 3. Mettre à jour les horaires à partir de tous les paramètres
            String[] jours = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"};
            
            System.out.println("=== Paramètres reçus pour mise à jour ===");
            for (String jour : jours) {
                Map<String, Object> horaireJour;
                if (horairesTravail.containsKey(jour)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> existing = (Map<String, Object>) horairesTravail.get(jour);
                    horaireJour = new HashMap<>(existing);
                } else {
                    horaireJour = new HashMap<>();
                    horaireJour.put("heureDebut", "");
                    horaireJour.put("heureFin", "");
                    horaireJour.put("actif", false);
                }
                
                // Les paramètres arrivent avec le préfixe "heureDebut_", "heureFin_", "actif_"
                String debutKey = "heureDebut_" + jour;
                String finKey = "heureFin_" + jour;
                String actifKey = "actif_" + jour;
                
                // Mettre à jour heureDebut uniquement si une valeur non vide est fournie
                if (allParams.containsKey(debutKey)) {
                    String debut = allParams.get(debutKey);
                    // Ne mettre à jour que si la valeur n'est pas null ni vide
                    if (debut != null && !debut.trim().isEmpty()) {
                        horaireJour.put("heureDebut", debut);
                    }
                    // Si vide, on garde la valeur existante (déjà dans horaireJour)
                }
                
                // Mettre à jour heureFin uniquement si une valeur non vide est fournie
                if (allParams.containsKey(finKey)) {
                    String fin = allParams.get(finKey);
                    // Ne mettre à jour que si la valeur n'est pas null ni vide
                    if (fin != null && !fin.trim().isEmpty()) {
                        horaireJour.put("heureFin", fin);
                    }
                    // Si vide, on garde la valeur existante (déjà dans horaireJour)
                }
                
                // Pour actif, toujours mettre à jour car les checkboxes cochées envoient "true"
                // et les checkboxes non cochées n'envoient rien
                boolean isActif = allParams.containsKey(actifKey) && "true".equals(allParams.get(actifKey));
                horaireJour.put("actif", isActif);
                System.out.println(jour + ": debut=" + horaireJour.get("heureDebut") + ", fin=" + horaireJour.get("heureFin") + ", actif=" + isActif);
                
                horairesTravail.put(jour, horaireJour);
            }
            System.out.println("=== Fin de la mise à jour des horaires ===");

            // 3. Préparer les données de mise à jour
            Map<String, Object> userData = new HashMap<>();
            userData.put("nom", nom);
            userData.put("prenom", prenom);
            userData.put("email", email);
            userData.put("telephone", numeroTelephone);
            userData.put("numeroTelephone", numeroTelephone);
            userData.put("role", role);
            userData.put("horairesTravail", horairesTravail);

            // 4. Mettre à jour dans Realtime Database
            firebaseRealtimeService.updateUser(userId, userData);

            System.out.println("=== Utilisateur mis à jour avec succès ===");
            
            // Si l'utilisateur a modifié son propre profil, rediriger vers le dashboard
            // Sinon (superadmin modifiant quelqu'un d'autre), rediriger vers la liste des utilisateurs
            if (isOwnProfile) {
                System.out.println("=== Redirection vers /dashboard (propre profil) ===");
                redirectAttributes.addFlashAttribute("success", "Votre profil a été mis à jour avec succès.");
                return "redirect:/dashboard";
            } else {
                System.out.println("=== Redirection vers /users (modification d'un autre utilisateur) ===");
                redirectAttributes.addFlashAttribute("success", "Utilisateur mis à jour avec succès.");
                return "redirect:/users";
            }
            
        } catch (FirebaseAuthException e) {
            System.err.println("Erreur Firebase Auth: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
            return "redirect:/users/" + userId + "/edit";
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
            return "redirect:/users/" + userId + "/edit";
        }
    }

    /**
     * Supprime un utilisateur
     */
    @PostMapping("/{userId}/delete")
    public String deleteUser(@PathVariable String userId, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        System.out.println("=== UserWebController.deleteUser pour userId: " + userId + " ===");
        
        HttpSession session = request.getSession(false);
        String currentUserId = (String) (session != null ? session.getAttribute("userId") : null);
        String currentUserRole = (String) (session != null ? session.getAttribute("role") : null);
        
        // Vérifier que l'utilisateur est superadmin
        if (!"superadmin".equals(currentUserRole)) {
            redirectAttributes.addFlashAttribute("error", "Accès refusé. Seul le superadmin peut supprimer des utilisateurs.");
            return "redirect:/login";
        }
        
        // Empêcher le superadmin de supprimer son propre compte
        if (userId.equals(currentUserId)) {
            System.out.println("=== Tentative de suppression du propre compte superadmin bloquée ===");
            redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas supprimer votre propre compte superadmin.");
            return "redirect:/users";
        }

        try {
            // Vérifier si l'utilisateur à supprimer est un superadmin
            Map<String, Object> userToDelete = firebaseRealtimeService.getUserById(userId);
            if (userToDelete != null && "superadmin".equals(userToDelete.get("role"))) {
                System.out.println("=== Tentative de suppression d'un superadmin bloquée ===");
                redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas supprimer un compte superadmin.");
                return "redirect:/users";
            }
            
            // Supprimer de Firebase Authentication
            firebaseAuthService.deleteUser(userId);
            
            // Supprimer de Realtime Database
            firebaseRealtimeService.deleteUser(userId);
            
            System.out.println("=== Utilisateur supprimé avec succès ===");
            redirectAttributes.addFlashAttribute("success", "Utilisateur supprimé avec succès.");
            return "redirect:/users";
            
        } catch (FirebaseAuthException e) {
            System.err.println("Erreur Firebase Auth: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
            return "redirect:/users";
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
            return "redirect:/users";
        }
    }
}

