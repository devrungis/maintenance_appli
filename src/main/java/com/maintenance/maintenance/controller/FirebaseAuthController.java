package com.maintenance.maintenance.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.maintenance.maintenance.service.FirebaseAuthService;
import com.maintenance.maintenance.service.FirebaseRealtimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class FirebaseAuthController {

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null && !error.isEmpty()) {
            String errorMessage = error;
            if (error.equals("true")) {
                errorMessage = "Email inexistant ou mot de passe incorrect";
            }
            model.addAttribute("error", errorMessage);
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                       @RequestParam String password,
                       HttpServletRequest request,
                       RedirectAttributes redirectAttributes) {
        
        System.out.println("=== FirebaseAuthController.login ===");
        System.out.println("Email: " + email);
        
        try {
            // Authentifier l'utilisateur avec Firebase
            System.out.println("=== Tentative d'authentification avec Firebase ===");
            FirebaseAuthService.AuthResult authResult = firebaseAuthService.authenticateUser(email, password);
            String idToken = authResult.getIdToken();
            
            if (idToken == null || idToken.isEmpty()) {
                System.err.println("=== Erreur: Token vide ou null ===");
                redirectAttributes.addAttribute("error", "Erreur d'authentification");
                return "redirect:/login";
            }
            
            System.out.println("=== Token reçu: " + idToken.substring(0, Math.min(20, idToken.length())) + "... ===");
            
            // Vérifier le token et récupérer les informations utilisateur
            UserRecord userRecord = null;
            String userId = authResult.getLocalId();
            try {
                if (userId != null && !userId.isEmpty()) {
                    userRecord = firebaseAuthService.getUserById(userId);
                } else {
                    userRecord = firebaseAuthService.getUserByEmail(email);
                }
                if (userRecord != null) {
                    System.out.println("=== UserRecord récupéré, UID: " + userRecord.getUid() + " ===");
                    userId = userRecord.getUid();
                }
            } catch (FirebaseAuthException e) {
                System.err.println("=== Erreur lors de la récupération du UserRecord: " + e.getMessage() + " ===");
                try {
                    FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(idToken);
                    userId = decodedToken.getUid();
                } catch (FirebaseAuthException tokenException) {
                    System.err.println("=== Erreur lors de la vérification du token: " + tokenException.getMessage() + " ===");
                }
            }
            
            // Récupérer le rôle depuis Realtime Database
            String role = "utilisateur";
            try {
                if (userId != null) {
                    Map<String, Object> userData = firebaseRealtimeService.getUserById(userId);
                    if (userData == null) {
                        System.out.println("=== Aucune structure trouvée dans Realtime Database pour " + userId + ", création en cours ===");
                        String displayName = userRecord != null ? userRecord.getDisplayName() : null;
                        String telephone = userRecord != null ? userRecord.getPhoneNumber() : null;
                        String nom = displayName != null ? displayName : "";
                        String nomUtilisateur = authResult.getEmail() != null ? authResult.getEmail().split("@")[0] : "user";
                        firebaseRealtimeService.initializeExistingUser(
                            userId,
                            authResult.getEmail(),
                            nom,
                            nomUtilisateur,
                            telephone,
                            null
                        );
                        userData = firebaseRealtimeService.getUserById(userId);
                    }
                    if (userData != null && userData.get("role") != null) {
                        role = (String) userData.get("role");
                        System.out.println("=== Rôle récupéré depuis Firebase: " + role + " ===");
                    }
                }
            } catch (Exception e) {
                System.err.println("=== Erreur lors de la récupération/initialisation du rôle: " + e.getMessage() + " ===");
                // Si on ne peut pas récupérer le rôle, on utilise la valeur par défaut
            }
            
            if (role == null || role.isBlank()) {
                role = "utilisateur";
            }
            
            if (userId == null || userId.isBlank()) {
                System.err.println("=== Impossible de déterminer l'UID de l'utilisateur ===");
                redirectAttributes.addAttribute("error", "Erreur lors de la connexion");
                return "redirect:/login";
            }
            
            // Nettoyer le contexte Spring Security AVANT de manipuler les sessions
            SecurityContextHolder.clearContext();
            
            // Invalider toute session existante pour créer une session propre
            // Essayer plusieurs fois pour s'assurer que toutes les sessions sont nettoyées
            for (int i = 0; i < 3; i++) {
                HttpSession oldSession = request.getSession(false);
                if (oldSession != null) {
                    try {
                        // Nettoyer tous les attributs avant d'invalider
                        oldSession.removeAttribute("authenticated");
                        oldSession.removeAttribute("email");
                        oldSession.removeAttribute("userId");
                        oldSession.removeAttribute("role");
                        oldSession.removeAttribute("idToken");
                        oldSession.removeAttribute("firebaseUser");
                        oldSession.removeAttribute("lastSelectedEntrepriseId");
                        oldSession.removeAttribute("sessionCreated");
                        oldSession.invalidate();
                        System.out.println("=== Ancienne session invalidée (tentative " + (i + 1) + ") ===");
                    } catch (IllegalStateException e) {
                        // Session déjà invalidée, c'est bon
                        System.out.println("=== Session déjà invalidée (tentative " + (i + 1) + ") ===");
                        break;
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'invalidation de l'ancienne session (tentative " + (i + 1) + "): " + e.getMessage());
                    }
                } else {
                    break; // Plus de session à invalider
                }
            }
            
            // Attendre un peu pour que l'invalidation soit complète
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Créer une nouvelle session propre
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(1800); // 30 minutes
            
            // S'assurer que la nouvelle session est vraiment nouvelle
            String newSessionId = session.getId();
            System.out.println("=== Nouvelle session créée avec ID: " + newSessionId + " ===");
            
            // Définir tous les attributs de session dans l'ordre
            session.setAttribute("authenticated", true);
            session.setAttribute("email", authResult.getEmail());
            session.setAttribute("userId", userId);
            session.setAttribute("role", role);
            session.setAttribute("idToken", idToken);
            if (userRecord != null) {
                session.setAttribute("firebaseUser", userRecord);
            }
            
            // Forcer la sauvegarde de la session avec timestamp
            long sessionCreatedTime = System.currentTimeMillis();
            session.setAttribute("sessionCreated", sessionCreatedTime);
            
            // Forcer l'écriture de la session
            try {
                session.setAttribute("_forceWrite", sessionCreatedTime);
            } catch (Exception e) {
                System.err.println("Erreur lors de l'écriture forcée de la session: " + e.getMessage());
            }
            
            System.out.println("=== Session créée avec succès ===");
            System.out.println("Session ID: " + session.getId());
            System.out.println("User ID: " + userId);
            System.out.println("Email: " + authResult.getEmail());
            System.out.println("Role: " + role);
            System.out.println("Authenticated: " + session.getAttribute("authenticated"));
            System.out.println("Session Created Time: " + sessionCreatedTime);
            System.out.println("=== Redirection vers /dashboard ===");
            
            // Attendre un peu pour que la session soit bien sauvegardée avant la redirection
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return "redirect:/dashboard";
            
        } catch (com.maintenance.maintenance.exception.AuthenticationException e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/login";
        } catch (FirebaseAuthException e) {
            String errorCode = e.getErrorCode().toString();
            String errorMessage;
            
            if (errorCode.contains("USER_NOT_FOUND") || errorCode.contains("user-not-found")) {
                errorMessage = "Email inexistant";
            } else if (errorCode.contains("INVALID_PASSWORD") || errorCode.contains("wrong-password")) {
                errorMessage = "Mot de passe incorrect";
            } else {
                errorMessage = "Erreur d'authentification";
            }
            
            redirectAttributes.addAttribute("error", errorMessage);
            return "redirect:/login";
        } catch (Exception e) {
            String errorMessage = "Erreur lors de la connexion";
            String exceptionMessage = e.getMessage();
            if (exceptionMessage != null) {
                if (exceptionMessage.contains("Email inexistant")) {
                    errorMessage = "Email inexistant";
                } else if (exceptionMessage.contains("Mot de passe incorrect")) {
                    errorMessage = "Mot de passe incorrect";
                }
            }
            redirectAttributes.addAttribute("error", errorMessage);
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logoutGet(HttpServletRequest request) {
        try {
            // Nettoyer le contexte Spring Security
            SecurityContextHolder.clearContext();
            
            // Invalider toutes les sessions possibles
            for (int i = 0; i < 3; i++) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    try {
                        // Nettoyer tous les attributs avant d'invalider
                        session.removeAttribute("authenticated");
                        session.removeAttribute("email");
                        session.removeAttribute("userId");
                        session.removeAttribute("role");
                        session.removeAttribute("idToken");
                        session.removeAttribute("firebaseUser");
                        session.removeAttribute("lastSelectedEntrepriseId");
                        session.removeAttribute("sessionCreated");
                        session.removeAttribute("_forceWrite");
                        session.invalidate();
                        System.out.println("=== Session invalidée avec succès (tentative " + (i + 1) + ") ===");
                    } catch (IllegalStateException e) {
                        // Session déjà invalidée, c'est bon
                        System.out.println("=== Session déjà invalidée (tentative " + (i + 1) + ") ===");
                        break;
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'invalidation de la session (tentative " + (i + 1) + "): " + e.getMessage());
                    }
                } else {
                    break; // Plus de session à invalider
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du logout: " + e.getMessage());
        }
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        try {
            // Nettoyer le contexte Spring Security
            SecurityContextHolder.clearContext();
            
            // Invalider toutes les sessions possibles
            for (int i = 0; i < 3; i++) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    try {
                        // Nettoyer tous les attributs avant d'invalider
                        session.removeAttribute("authenticated");
                        session.removeAttribute("email");
                        session.removeAttribute("userId");
                        session.removeAttribute("role");
                        session.removeAttribute("idToken");
                        session.removeAttribute("firebaseUser");
                        session.removeAttribute("lastSelectedEntrepriseId");
                        session.removeAttribute("sessionCreated");
                        session.removeAttribute("_forceWrite");
                        session.invalidate();
                        System.out.println("=== Session invalidée avec succès (tentative " + (i + 1) + ") ===");
                    } catch (IllegalStateException e) {
                        // Session déjà invalidée, c'est bon
                        System.out.println("=== Session déjà invalidée (tentative " + (i + 1) + ") ===");
                        break;
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'invalidation de la session (tentative " + (i + 1) + "): " + e.getMessage());
                    }
                } else {
                    break; // Plus de session à invalider
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du logout: " + e.getMessage());
        }
        return "redirect:/login";
    }
}

