package com.maintenance.maintenance.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        System.out.println("=== FirebaseAuthenticationFilter: Requête reçue ===");
        System.out.println("URI: " + path);
        System.out.println("Method: " + method);
        
        // Laisser passer la page de login (GET et POST) et les ressources statiques
        if (path.equals("/login") || path.equals("/logout") || path.startsWith("/css/") || path.startsWith("/js/")) {
            System.out.println("=== FirebaseAuthenticationFilter: Ressource statique ou login/logout, passage direct ===");
            filterChain.doFilter(request, response);
            return;
        }
        
        // Vérifier si l'utilisateur est déjà authentifié dans Spring Security
        if (SecurityContextHolder.getContext().getAuthentication() != null 
            && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            System.out.println("=== FirebaseAuthenticationFilter: Utilisateur déjà authentifié dans Spring Security ===");
            System.out.println("Principal: " + SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            System.out.println("Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
            filterChain.doFilter(request, response);
            return;
        }
        
        // Vérifier si l'utilisateur est authentifié via la session
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        System.out.println("=== FirebaseAuthenticationFilter: Vérification de la session ===");
        System.out.println("Session existe: " + (session != null));
        
        if (session != null) {
            Boolean authenticated = (Boolean) session.getAttribute("authenticated");
            String email = (String) session.getAttribute("email");
            String role = (String) session.getAttribute("role");
            String userId = (String) session.getAttribute("userId");
            
            System.out.println("Session - authenticated: " + authenticated);
            System.out.println("Session - email: " + email);
            System.out.println("Session - role: " + role);
            System.out.println("Session - userId: " + userId);
            
            if (authenticated != null && authenticated && email != null && !email.isEmpty()) {
                // Déterminer le rôle Spring Security
                String authority = "ROLE_USER";
                if (role != null && role.equalsIgnoreCase("superadmin")) {
                    authority = "ROLE_SUPERADMIN";
                } else if (role != null && role.equalsIgnoreCase("admin")) {
                    authority = "ROLE_ADMIN";
                }
                
                System.out.println("=== FirebaseAuthenticationFilter: Création de l'authentification Spring Security ===");
                System.out.println("Authority: " + authority);
                
                // Créer une authentification Spring Security
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(authority))
                    );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                System.out.println("=== FirebaseAuthenticationFilter: Authentification créée et ajoutée au contexte ===");
            } else {
                System.out.println("=== FirebaseAuthenticationFilter: Utilisateur non authentifié dans la session ===");
            }
        } else {
            System.out.println("=== FirebaseAuthenticationFilter: Aucune session trouvée ===");
        }
        
        System.out.println("=== FirebaseAuthenticationFilter: Passage au filtre suivant ===");
        filterChain.doFilter(request, response);
    }
}

