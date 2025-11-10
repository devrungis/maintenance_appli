package com.maintenance.maintenance.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttribute {

    /**
     * Ajoute le rôle de l'utilisateur à tous les modèles pour l'utiliser dans les templates
     */
    @ModelAttribute
    public void addUserRoleToModel(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String role = (String) session.getAttribute("role");
            String userId = (String) session.getAttribute("userId");
            String email = (String) session.getAttribute("email");
            
            model.addAttribute("userRole", role);
            model.addAttribute("currentUserId", userId);
            model.addAttribute("currentUserEmail", email);
            
            System.out.println("=== GlobalModelAttribute: Rôle ajouté au modèle: " + role);
            System.out.println("=== GlobalModelAttribute: UserId ajouté au modèle: " + userId);
            System.out.println("=== GlobalModelAttribute: URI de la requête: " + request.getRequestURI());
            System.out.println("=== GlobalModelAttribute: userRole dans le modèle = " + role);
            System.out.println("=== GlobalModelAttribute: Est superadmin? " + (role != null && role.equals("superadmin")));
        } else {
            model.addAttribute("userRole", null);
            model.addAttribute("currentUserId", null);
            model.addAttribute("currentUserEmail", null);
            System.out.println("=== GlobalModelAttribute: Aucune session, userRole = null");
            System.out.println("=== GlobalModelAttribute: URI de la requête: " + request.getRequestURI());
        }
    }
}

