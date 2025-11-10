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
            String idToken = firebaseAuthService.authenticateUser(email, password);
            
            if (idToken == null || idToken.isEmpty()) {
                System.err.println("=== Erreur: Token vide ou null ===");
                redirectAttributes.addAttribute("error", "Erreur d'authentification");
                return "redirect:/login";
            }
            
            System.out.println("=== Token reçu: " + idToken.substring(0, Math.min(20, idToken.length())) + "... ===");
            
            // Vérifier le token et récupérer les informations utilisateur
            UserRecord userRecord;
            try {
                userRecord = firebaseAuthService.getUserByEmail(email);
                System.out.println("=== UserRecord récupéré, UID: " + userRecord.getUid() + " ===");
            } catch (FirebaseAuthException e) {
                System.err.println("=== Erreur lors de la récupération du UserRecord: " + e.getMessage() + " ===");
                // Si on ne peut pas récupérer l'utilisateur, on continue quand même avec la session
                userRecord = null;
            }
            
            // Récupérer le rôle depuis Realtime Database
            String role = "utilisateur";
            try {
                Map<String, Object> userData = firebaseRealtimeService.getUserById(userRecord.getUid());
                if (userData != null && userData.get("role") != null) {
                    role = (String) userData.get("role");
                    System.out.println("=== Rôle récupéré depuis Firebase: " + role + " ===");
                }
            } catch (Exception e) {
                System.err.println("=== Erreur lors de la récupération du rôle: " + e.getMessage() + " ===");
                // Si on ne peut pas récupérer le rôle, on utilise la valeur par défaut
            }
            
            // Créer une session
            HttpSession session = request.getSession(true);
            session.setAttribute("firebaseUser", userRecord);
            session.setAttribute("email", email);
            session.setAttribute("userId", userRecord.getUid());
            session.setAttribute("role", role);
            session.setAttribute("idToken", idToken);
            session.setAttribute("authenticated", true);
            
            System.out.println("=== Session créée avec succès ===");
            System.out.println("Session ID: " + session.getId());
            System.out.println("User ID: " + userRecord.getUid());
            System.out.println("Email: " + email);
            System.out.println("Role: " + role);
            System.out.println("=== Redirection vers /dashboard ===");
            
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
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }
}

