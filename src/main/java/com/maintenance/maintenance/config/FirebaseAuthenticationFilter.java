package com.maintenance.maintenance.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

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
        if (path.equals("/login") || path.equals("/logout") || path.startsWith("/css/") || path.startsWith("/js/") 
            || path.startsWith("/media/") || path.equals("/favicon.ico")) {
            System.out.println("=== FirebaseAuthenticationFilter: Ressource statique ou login/logout, passage direct ===");
            filterChain.doFilter(request, response);
            return;
        }
        
        // Ne pas laisser passer directement - toujours créer l'authentification Spring Security
        
        // Vérifier si l'utilisateur est authentifié via la session (priorité sur Spring Security)
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        System.out.println("=== FirebaseAuthenticationFilter: Vérification de la session ===");
        System.out.println("Session existe: " + (session != null));
        
        boolean shouldAuthenticate = false;
        String email = null;
        String role = null;
        String userId = null;
        
        if (session != null) {
            try {
                // Vérifier si la session est expirée en vérifiant le timestamp de création
                Long sessionCreated = (Long) session.getAttribute("sessionCreated");
                long currentTime = System.currentTimeMillis();
                long sessionMaxAge = 1800 * 1000; // 30 minutes en millisecondes
                
                // Si la session a été créée il y a plus de 30 minutes, elle est considérée comme expirée
                boolean sessionExpired = false;
                if (sessionCreated != null) {
                    long sessionAge = currentTime - sessionCreated;
                    if (sessionAge > sessionMaxAge) {
                        sessionExpired = true;
                        System.out.println("=== FirebaseAuthenticationFilter: Session expirée (âge: " + (sessionAge / 1000) + " secondes) ===");
                    }
                }
                
                // Vérifier aussi si la session est invalide côté serveur
                try {
                    session.getAttribute("authenticated"); // Tester si la session est valide
                } catch (IllegalStateException e) {
                    sessionExpired = true;
                    System.out.println("=== FirebaseAuthenticationFilter: Session invalide côté serveur ===");
                }
                
                // Si la session est expirée, l'invalider immédiatement
                if (sessionExpired) {
                    try {
                        session.invalidate();
                        System.out.println("=== FirebaseAuthenticationFilter: Session expirée invalidée ===");
                        session = null; // Réinitialiser pour éviter de l'utiliser
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'invalidation de la session expirée: " + e.getMessage());
                        session = null;
                    }
                } else {
                    // Session valide, récupérer les attributs
                    Boolean authenticated = (Boolean) session.getAttribute("authenticated");
                    email = (String) session.getAttribute("email");
                    role = (String) session.getAttribute("role");
                    userId = (String) session.getAttribute("userId");
                    
                    System.out.println("Session - authenticated: " + authenticated);
                    System.out.println("Session - email: " + email);
                    System.out.println("Session - role: " + role);
                    System.out.println("Session - userId: " + userId);
                    
                    if (authenticated != null && authenticated && email != null && !email.isEmpty()) {
                        shouldAuthenticate = true;
                        // Rafraîchir la session pour éviter l'expiration
                        session.setAttribute("authenticated", true);
                        session.setAttribute("email", email);
                        if (role != null) session.setAttribute("role", role);
                        if (userId != null) session.setAttribute("userId", userId);
                        // Mettre à jour le timestamp pour prolonger la session
                        session.setAttribute("sessionCreated", currentTime);
                    } else {
                        System.out.println("=== FirebaseAuthenticationFilter: Utilisateur non authentifié dans la session ===");
                        // Ne pas invalider la session si elle vient d'être créée (éviter les problèmes de timing après login)
                        if (sessionCreated != null && (currentTime - sessionCreated) < 5000) {
                            System.out.println("=== FirebaseAuthenticationFilter: Session récemment créée, ne pas invalider ===");
                        } else {
                            // Nettoyer la session si elle est corrompue et qu'elle n'est pas récente
                            if (authenticated == null || email == null || email.isEmpty()) {
                                try {
                                    session.invalidate();
                                    System.out.println("=== FirebaseAuthenticationFilter: Session invalide nettoyée ===");
                                    session = null;
                                } catch (Exception e) {
                                    System.err.println("Erreur lors de l'invalidation de la session: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (IllegalStateException e) {
                // La session a été invalidée
                System.out.println("=== FirebaseAuthenticationFilter: Session invalidée, nettoyage ===");
                try {
                    if (session != null) {
                        session.invalidate();
                    }
                } catch (Exception ex) {
                    // Ignorer les erreurs d'invalidation
                }
                session = null;
            }
        } else {
            System.out.println("=== FirebaseAuthenticationFilter: Aucune session trouvée ===");
        }
        
        // Créer l'authentification Spring Security si nécessaire
        if (shouldAuthenticate && email != null && !email.isEmpty() && session != null) {
            // Déterminer le rôle Spring Security
            String authority = "ROLE_USER";
            if (role != null && role.equalsIgnoreCase("superadmin")) {
                authority = "ROLE_SUPERADMIN";
            } else if (role != null && role.equalsIgnoreCase("admin")) {
                authority = "ROLE_ADMIN";
            }
            
            System.out.println("=== FirebaseAuthenticationFilter: Création de l'authentification Spring Security ===");
            System.out.println("Authority: " + authority);
            System.out.println("Email: " + email);
            System.out.println("Role: " + role);
            
            // Créer une authentification Spring Security (toujours recréer pour s'assurer qu'elle est à jour)
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(authority))
                );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // FORCER la mise à jour du contexte Spring Security
            SecurityContextHolder.clearContext();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            
            // Sauvegarder le SecurityContext dans la session HTTP pour que Spring Security le reconnaisse
            // Utiliser la session existante si elle est valide, sinon en créer une nouvelle
            jakarta.servlet.http.HttpSession httpSession = session != null ? session : request.getSession(true);
            try {
                securityContextRepository.saveContext(context, request, response);
                System.out.println("=== FirebaseAuthenticationFilter: Authentification créée et ajoutée au contexte ===");
                System.out.println("Principal: " + SecurityContextHolder.getContext().getAuthentication().getPrincipal());
                System.out.println("Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                System.out.println("Is Authenticated: " + SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
                System.out.println("SecurityContext sauvegardé dans la session: " + httpSession.getId());
            } catch (IllegalStateException e) {
                // La session a été invalidée entre temps, créer une nouvelle session
                System.err.println("=== FirebaseAuthenticationFilter: Session invalidée pendant la sauvegarde, création d'une nouvelle ===");
                httpSession = request.getSession(true);
                // Recréer les attributs de session
                httpSession.setAttribute("authenticated", true);
                httpSession.setAttribute("email", email);
                if (role != null) httpSession.setAttribute("role", role);
                if (userId != null) httpSession.setAttribute("userId", userId);
                httpSession.setAttribute("sessionCreated", System.currentTimeMillis());
                securityContextRepository.saveContext(context, request, response);
            }
        } else {
            // Nettoyer le contexte Spring Security si pas d'authentification valide
            SecurityContextHolder.clearContext();
            System.out.println("=== FirebaseAuthenticationFilter: Contexte Spring Security nettoyé (pas d'authentification valide) ===");
            System.out.println("shouldAuthenticate: " + shouldAuthenticate);
            System.out.println("email: " + email);
            System.out.println("session: " + (session != null ? "existe" : "null"));
        }
        
        // Vérifier si l'utilisateur est maintenant authentifié dans Spring Security
        if (SecurityContextHolder.getContext().getAuthentication() != null 
            && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            System.out.println("=== FirebaseAuthenticationFilter: Utilisateur authentifié dans Spring Security ===");
            System.out.println("Principal: " + SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            System.out.println("Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        } else {
            System.out.println("=== FirebaseAuthenticationFilter: Utilisateur NON authentifié ===");
        }
        
        System.out.println("=== FirebaseAuthenticationFilter: Passage au filtre suivant ===");
        filterChain.doFilter(request, response);
    }
}

