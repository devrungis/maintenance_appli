package com.maintenance.maintenance.controller;

import com.maintenance.maintenance.model.entity.Enterprise;
import com.maintenance.maintenance.service.EnterpriseService;
import com.maintenance.maintenance.service.FirebaseRealtimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ent")
public class EnterpriseController {

    @Autowired
    private EnterpriseService enterpriseService;
    
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
     * Récupère l'ID de l'utilisateur connecté
     */
    private String getCurrentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String userId = (String) session.getAttribute("userId");
            System.out.println("Session userId: " + userId);
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }
            // Vérifier aussi si l'utilisateur est authentifié via Spring Security
            if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null 
                && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                String email = (String) session.getAttribute("email");
                System.out.println("Utilisateur authentifié via Spring Security, email: " + email);
                // Retourner un userId temporaire si l'utilisateur est authentifié mais pas de userId en session
                return email != null ? email : "authenticated_user";
            }
        }
        System.out.println("Aucune session ou userId trouvé");
        return null;
    }

    /**
     * Affiche la liste des entreprises (READ) - Récupère depuis Firebase Realtime Database
     */
    @GetMapping
    public String listEnterprises(Model model, HttpServletRequest request) {
        System.out.println("========================================");
        System.out.println("=== EnterpriseController.listEnterprises ===");
        System.out.println("========================================");
        
        // Vérifier l'authentification Spring Security
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        System.out.println("=== Spring Security Authentication ===");
        System.out.println("Authenticated: " + (auth != null && auth.isAuthenticated()));
        if (auth != null) {
            System.out.println("Principal: " + auth.getPrincipal());
            System.out.println("Authorities: " + auth.getAuthorities());
        }
        
        // Vérifier la session
        HttpSession session = request.getSession(false);
        System.out.println("=== Session HTTP ===");
        System.out.println("Session existe: " + (session != null));
        if (session != null) {
            Boolean authenticated = (Boolean) session.getAttribute("authenticated");
            String userId = (String) session.getAttribute("userId");
            String email = (String) session.getAttribute("email");
            String role = (String) session.getAttribute("role");
            
            System.out.println("Session - authenticated: " + authenticated);
            System.out.println("Session - userId: " + userId);
            System.out.println("Session - email: " + email);
            System.out.println("Session - role: " + role);
            
            // Vérifier si l'utilisateur est superadmin
            if (role == null || !role.equalsIgnoreCase("superadmin")) {
                System.out.println("=== ERREUR: L'utilisateur n'est pas superadmin ===");
                System.out.println("Role actuel: " + role);
                System.out.println("Redirection vers /login");
                return "redirect:/login?error=not_superadmin";
            }
            System.out.println("=== OK: L'utilisateur est superadmin ===");
        } else {
            System.out.println("=== ERREUR: Aucune session trouvée ===");
            System.out.println("Redirection vers /login");
            return "redirect:/login?error=no_session";
        }

        List<Map<String, Object>> firebaseEnterprises = new ArrayList<>();
        
        try {
            // Récupérer directement depuis Firebase Realtime Database avec timeout
            System.out.println("Tentative de récupération des entreprises depuis Firebase...");
            firebaseEnterprises = firebaseRealtimeService.getAllEnterprises();
            System.out.println("Nombre d'entreprises récupérées depuis Firebase: " + (firebaseEnterprises != null ? firebaseEnterprises.size() : 0));
            
            // Afficher les détails de chaque entreprise
            if (firebaseEnterprises != null && !firebaseEnterprises.isEmpty()) {
                for (Map<String, Object> entreprise : firebaseEnterprises) {
                    System.out.println("Entreprise: " + entreprise);
                }
            }
            
        } catch (java.util.concurrent.TimeoutException e) {
            System.err.println("TIMEOUT lors de la récupération des entreprises depuis Firebase");
            model.addAttribute("error", "Délai d'attente dépassé lors de la récupération des entreprises. Veuillez réessayer.");
            firebaseEnterprises = new ArrayList<>();
        } catch (Exception e) {
            System.err.println("ERREUR lors du chargement des entreprises depuis Firebase: " + e.getMessage());
            e.printStackTrace();
            // Toujours afficher la page même en cas d'erreur
            model.addAttribute("error", "Erreur lors du chargement des entreprises: " + (e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
            firebaseEnterprises = new ArrayList<>();
        }
        
        // Toujours ajouter les entreprises au modèle (même si vide)
        List<Map<String, Object>> finalEnterprises = firebaseEnterprises != null ? firebaseEnterprises : new ArrayList<>();
        model.addAttribute("enterprises", finalEnterprises);
        model.addAttribute("enterprise", new Enterprise()); // Pour le formulaire de création
        
        // Log final pour vérifier ce qui est passé au modèle
        System.out.println("=== FIN listEnterprises ===");
        System.out.println("Nombre d'entreprises dans le modèle: " + finalEnterprises.size());
        System.out.println("Type de la liste: " + (finalEnterprises != null ? finalEnterprises.getClass().getName() : "null"));
        if (!finalEnterprises.isEmpty()) {
            System.out.println("Première entreprise dans le modèle: " + finalEnterprises.get(0));
            System.out.println("Clés de la première entreprise: " + (finalEnterprises.get(0) != null ? finalEnterprises.get(0).keySet() : "null"));
        }

        return "enterprises/list";
    }

    /**
     * Affiche le formulaire de création d'entreprise
     */
    @GetMapping("/create")
    public String showCreateForm(Model model, HttpServletRequest request) {
        System.out.println("========================================");
        System.out.println("=== EnterpriseController.showCreateForm ===");
        System.out.println("========================================");
        
        // Vérifier l'authentification Spring Security
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        System.out.println("=== Spring Security Authentication ===");
        System.out.println("Authenticated: " + (auth != null && auth.isAuthenticated()));
        if (auth != null) {
            System.out.println("Principal: " + auth.getPrincipal());
            System.out.println("Authorities: " + auth.getAuthorities());
        }
        
        // Vérifier la session
        HttpSession session = request.getSession(false);
        System.out.println("=== Session HTTP ===");
        System.out.println("Session existe: " + (session != null));
        if (session != null) {
            String userId = (String) session.getAttribute("userId");
            String email = (String) session.getAttribute("email");
            String role = (String) session.getAttribute("role");
            System.out.println("Session - userId: " + userId);
            System.out.println("Session - email: " + email);
            System.out.println("Session - role: " + role);
            
            // Vérifier si l'utilisateur est superadmin
            if (role == null || !role.equalsIgnoreCase("superadmin")) {
                System.out.println("=== ERREUR: L'utilisateur n'est pas superadmin ===");
                return "redirect:/login?error=not_superadmin";
            }
            System.out.println("=== OK: L'utilisateur est superadmin ===");
        } else {
            System.out.println("=== ERREUR: Aucune session trouvée ===");
            return "redirect:/login?error=no_session";
        }

        Enterprise enterprise = new Enterprise();
        model.addAttribute("enterprise", enterprise);
        System.out.println("=== Template retourné: enterprises/create ===");
        
        return "enterprises/create";
    }

    /**
     * Traite la création d'une entreprise (CREATE)
     */
    @PostMapping("/create")
    public String createEnterprise(@RequestParam String nom,
                                   @RequestParam(required = false) String rue,
                                   @RequestParam(required = false) String codePostal,
                                   @RequestParam(required = false) String ville,
                                   @RequestParam String email,
                                   @RequestParam String numero,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        System.out.println("=== EnterpriseController.createEnterprise ===");
        System.out.println("Nom: " + nom);
        System.out.println("Rue: " + rue);
        System.out.println("Code Postal: " + codePostal);
        System.out.println("Ville: " + ville);
        System.out.println("Email: " + email);
        System.out.println("Numéro: " + numero);
        
        // Logging de la session pour debug
        HttpSession session = request.getSession(false);
        if (session != null) {
            String userId = (String) session.getAttribute("userId");
            String userEmail = (String) session.getAttribute("email");
            System.out.println("Session - userId: " + userId + ", email: " + userEmail);
        }

        // Validation
        if (nom == null || nom.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Le nom de l'entreprise est obligatoire");
            redirectAttributes.addFlashAttribute("nom", nom);
            redirectAttributes.addFlashAttribute("rue", rue);
            redirectAttributes.addFlashAttribute("codePostal", codePostal);
            redirectAttributes.addFlashAttribute("ville", ville);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("numero", numero);
            return "redirect:/ent/create";
        }
        
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            redirectAttributes.addFlashAttribute("error", "L'email est obligatoire et doit être valide");
            redirectAttributes.addFlashAttribute("nom", nom);
            redirectAttributes.addFlashAttribute("rue", rue);
            redirectAttributes.addFlashAttribute("codePostal", codePostal);
            redirectAttributes.addFlashAttribute("ville", ville);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("numero", numero);
            return "redirect:/ent/create";
        }
        
        if (numero == null || numero.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Le numéro de téléphone est obligatoire");
            redirectAttributes.addFlashAttribute("nom", nom);
            redirectAttributes.addFlashAttribute("rue", rue);
            redirectAttributes.addFlashAttribute("codePostal", codePostal);
            redirectAttributes.addFlashAttribute("ville", ville);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("numero", numero);
            return "redirect:/ent/create";
        }

        try {
            Enterprise enterprise = new Enterprise();
            enterprise.setNom(nom.trim());
            enterprise.setRue(rue != null ? rue.trim() : null);
            enterprise.setCodePostal(codePostal != null ? codePostal.trim() : null);
            enterprise.setVille(ville != null ? ville.trim() : null);
            enterprise.setEmail(email.trim());
            enterprise.setNumero(numero.trim());
            
            Enterprise createdEnterprise = enterpriseService.createEnterprise(enterprise);
            redirectAttributes.addFlashAttribute("success", "Entreprise créée avec succès (ID: " + createdEnterprise.getId() + ", Firebase ID: " + createdEnterprise.getFirebaseId() + ")");
            System.out.println("Entreprise créée avec succès: " + createdEnterprise);
            
        } catch (Exception e) {
            System.err.println("ERREUR lors de la création: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
            redirectAttributes.addFlashAttribute("nom", nom);
            redirectAttributes.addFlashAttribute("rue", rue);
            redirectAttributes.addFlashAttribute("codePostal", codePostal);
            redirectAttributes.addFlashAttribute("ville", ville);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("numero", numero);
            return "redirect:/ent/create";
        }

        return "redirect:/ent";
    }

    /**
     * Affiche le formulaire de modification d'entreprise (utilise Firebase ID)
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model, HttpServletRequest request) {
        System.out.println("========================================");
        System.out.println("=== EnterpriseController.showEditForm ===");
        System.out.println("========================================");
        System.out.println("Firebase ID: " + id);
        
        // Vérifier l'authentification Spring Security
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        System.out.println("=== Spring Security Authentication ===");
        System.out.println("Authenticated: " + (auth != null && auth.isAuthenticated()));
        if (auth != null) {
            System.out.println("Principal: " + auth.getPrincipal());
            System.out.println("Authorities: " + auth.getAuthorities());
        }
        
        // Vérifier la session
        HttpSession session = request.getSession(false);
        System.out.println("=== Session HTTP ===");
        System.out.println("Session existe: " + (session != null));
        if (session != null) {
            String userId = (String) session.getAttribute("userId");
            String email = (String) session.getAttribute("email");
            String role = (String) session.getAttribute("role");
            System.out.println("Session - userId: " + userId);
            System.out.println("Session - email: " + email);
            System.out.println("Session - role: " + role);
            
            // Vérifier si l'utilisateur est superadmin
            if (role == null || !role.equalsIgnoreCase("superadmin")) {
                System.out.println("=== ERREUR: L'utilisateur n'est pas superadmin ===");
                return "redirect:/login?error=not_superadmin";
            }
            System.out.println("=== OK: L'utilisateur est superadmin ===");
        } else {
            System.out.println("=== ERREUR: Aucune session trouvée ===");
            return "redirect:/login?error=no_session";
        }

        try {
            // Récupérer depuis Firebase Realtime Database
            Map<String, Object> entreprise = firebaseRealtimeService.getEnterpriseById(id);
            System.out.println("Entreprise récupérée: " + entreprise);
            
            if (entreprise == null || entreprise.isEmpty()) {
                return "redirect:/ent?error=Entreprise non trouvée";
            }
            
            model.addAttribute("entreprise", entreprise);
            
        } catch (Exception e) {
            System.err.println("ERREUR lors de la récupération de l'entreprise: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/ent?error=Erreur lors du chargement";
        }

        return "enterprises/edit";
    }

    /**
     * Traite la modification d'une entreprise (UPDATE) - utilise Firebase ID
     */
    @PostMapping("/{id}/edit")
    public String updateEnterprise(@PathVariable String id,
                                   @RequestParam String nom,
                                   @RequestParam(required = false) String rue,
                                   @RequestParam(required = false) String codePostal,
                                   @RequestParam(required = false) String ville,
                                   @RequestParam String email,
                                   @RequestParam String numero,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        System.out.println("=== EnterpriseController.updateEnterprise ===");
        System.out.println("Firebase ID: " + id);
        System.out.println("Nom: " + nom);
        System.out.println("Rue: " + rue);
        System.out.println("Code Postal: " + codePostal);
        System.out.println("Ville: " + ville);
        System.out.println("Email: " + email);
        System.out.println("Numéro: " + numero);
        
        // Logging de la session pour debug
        HttpSession session = request.getSession(false);
        if (session != null) {
            String userId = (String) session.getAttribute("userId");
            String userEmail = (String) session.getAttribute("email");
            System.out.println("Session - userId: " + userId + ", email: " + userEmail);
        }

        // Validation
        if (nom == null || nom.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Le nom de l'entreprise est obligatoire");
            return "redirect:/ent/" + id + "/edit";
        }
        
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            redirectAttributes.addFlashAttribute("error", "L'email est obligatoire et doit être valide");
            return "redirect:/ent/" + id + "/edit";
        }
        
        if (numero == null || numero.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Le numéro de téléphone est obligatoire");
            return "redirect:/ent/" + id + "/edit";
        }

        try {
            // Mettre à jour directement dans Firebase
            Map<String, Object> entrepriseData = new java.util.HashMap<>();
            entrepriseData.put("nom", nom.trim());
            entrepriseData.put("rue", rue != null ? rue.trim() : "");
            entrepriseData.put("codePostal", codePostal != null ? codePostal.trim() : "");
            entrepriseData.put("ville", ville != null ? ville.trim() : "");
            entrepriseData.put("email", email.trim());
            entrepriseData.put("numero", numero.trim());
            
            firebaseRealtimeService.updateEnterprise(id, entrepriseData);
            redirectAttributes.addFlashAttribute("success", "Entreprise mise à jour avec succès");
            System.out.println("Entreprise mise à jour dans Firebase: " + id);
            
        } catch (Exception e) {
            System.err.println("ERREUR lors de la mise à jour: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour: " + e.getMessage());
            return "redirect:/ent/" + id + "/edit";
        }

        return "redirect:/ent";
    }

    /**
     * Supprime une entreprise (DELETE) - utilise Firebase ID
     */
    @PostMapping("/{id}/delete")
    public String deleteEnterprise(@PathVariable String id,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        System.out.println("=== EnterpriseController.deleteEnterprise ===");
        System.out.println("Firebase ID: " + id);
        
        // Logging de la session pour debug
        HttpSession session = request.getSession(false);
        if (session != null) {
            String userId = (String) session.getAttribute("userId");
            String email = (String) session.getAttribute("email");
            System.out.println("Session - userId: " + userId + ", email: " + email);
        }

        try {
            // Supprimer directement depuis Firebase
            firebaseRealtimeService.deleteEnterprise(id);
            redirectAttributes.addFlashAttribute("success", "Entreprise supprimée avec succès");
            System.out.println("Entreprise supprimée de Firebase: " + id);
            
        } catch (Exception e) {
            System.err.println("ERREUR lors de la suppression: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }

        return "redirect:/ent";
    }

    /**
     * Endpoint API REST pour récupérer toutes les entreprises
     * Accessible à tous les utilisateurs connectés (pas de vérification de rôle)
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllEnterprisesApi(HttpServletRequest request) {
        System.out.println("=== EnterpriseController.getAllEnterprisesApi: Début ===");
        System.out.println("=== URI de la requête: " + request.getRequestURI() + " ===");
        
        // Vérifier l'authentification (optionnel, juste pour les logs)
        HttpSession session = request.getSession(false);
        if (session != null) {
            String email = (String) session.getAttribute("email");
            String role = (String) session.getAttribute("role");
            System.out.println("=== Session trouvée - Email: " + email + ", Role: " + role + " ===");
        } else {
            System.out.println("=== Aucune session trouvée (accès anonyme autorisé) ===");
        }
        
        try {
            // Récupérer toutes les entreprises depuis Firebase (accessible à tous)
            List<Map<String, Object>> enterprises = firebaseRealtimeService.getAllEnterprises();
            System.out.println("=== EnterpriseController.getAllEnterprisesApi: Nombre d'entreprises récupérées: " + (enterprises != null ? enterprises.size() : 0) + " ===");
            
            if (enterprises != null && !enterprises.isEmpty()) {
                System.out.println("=== Première entreprise: " + enterprises.get(0) + " ===");
                // Log des noms de toutes les entreprises
                System.out.println("=== Liste des entreprises: " + enterprises.stream()
                    .map(e -> e.get("nom") != null ? e.get("nom").toString() : "Sans nom")
                    .collect(Collectors.joining(", ")) + " ===");
            } else {
                System.out.println("=== Aucune entreprise trouvée dans Firebase ===");
            }
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .header("Access-Control-Allow-Origin", "*")
                    .body(enterprises != null ? enterprises : new ArrayList<>());
        } catch (Exception e) {
            System.err.println("ERREUR lors de la récupération des entreprises via API: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .header("Content-Type", "application/json")
                    .body(new ArrayList<>());
        }
    }

    /**
     * Synchronise les entreprises depuis Firebase
     */
    @PostMapping("/sync")
    public String syncFromFirebase(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        // Logging de la session pour debug
        HttpSession session = request.getSession(false);
        if (session != null) {
            String userId = (String) session.getAttribute("userId");
            String email = (String) session.getAttribute("email");
            String role = (String) session.getAttribute("role");
            System.out.println("Session - userId: " + userId + ", email: " + email + ", role: " + role);
        }

        if (!isSuperAdmin(request)) {
            redirectAttributes.addFlashAttribute("error", "Accès refusé. Seul le superadmin peut synchroniser.");
            return "redirect:/ent";
        }

        try {
            enterpriseService.synchronizeFromFirebase();
            redirectAttributes.addFlashAttribute("success", "Synchronisation depuis Firebase terminée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la synchronisation: " + e.getMessage());
        }

        return "redirect:/ent";
    }
}

