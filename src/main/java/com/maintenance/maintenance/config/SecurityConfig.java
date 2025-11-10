package com.maintenance.maintenance.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private FirebaseAuthenticationFilter firebaseAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("=== SecurityConfig: Configuration de la chaîne de sécurité ===");
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Désactiver COMPLÈTEMENT toutes les authentifications par défaut AVANT d'autoriser les requêtes
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Autoriser les requêtes selon les rôles
            .authorizeHttpRequests(auth -> {
                System.out.println("=== SecurityConfig: Configuration des autorisations ===");
                auth
                    .requestMatchers("/login", "/logout").permitAll()
                    .requestMatchers("/css/**", "/js/**").permitAll()
                    .requestMatchers("/ent/api/**").permitAll() // API accessible à tous (même non authentifiés pour le sélecteur)
                    .requestMatchers("/ent/**").hasRole("SUPERADMIN") // Seul le superadmin peut accéder aux pages de gestion
                    .requestMatchers("/users/create", "/users", "/users/*/delete").hasRole("SUPERADMIN") // Seul le superadmin peut créer/lister/supprimer des utilisateurs
                    .requestMatchers("/users/*/edit").authenticated() // Tout utilisateur authentifié peut modifier son profil (contrôle dans le controller)
                    .requestMatchers("/dashboard", "/machines", "/categories", "/alerts", "/reports", "/calendar", "/inventory", "/tickets").authenticated()
                    .anyRequest().authenticated();
                System.out.println("=== SecurityConfig: Autorisations configurées ===");
            })
            // Ajouter le filtre Firebase
            .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Gérer les exceptions d'authentification avec logs
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    System.out.println("=== SecurityConfig: AuthenticationEntryPoint déclenché ===");
                    System.out.println("URI: " + request.getRequestURI());
                    System.out.println("Exception: " + authException.getMessage());
                    // Ne pas rediriger pour les pages statiques (éviter les boucles)
                    if (!request.getRequestURI().startsWith("/css") && 
                        !request.getRequestURI().startsWith("/js") && 
                        !request.getRequestURI().equals("/favicon.ico")) {
                        response.sendRedirect("/login?error=not_authenticated");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    System.out.println("=== SecurityConfig: AccessDeniedHandler déclenché ===");
                    System.out.println("URI: " + request.getRequestURI());
                    System.out.println("Exception: " + accessDeniedException.getMessage());
                    try {
                        response.sendRedirect("/login?error=access_denied");
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la redirection: " + e.getMessage());
                    }
                })
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())); // Pour H2 console

        System.out.println("=== SecurityConfig: Configuration terminée ===");
        return http.build();
    }
}

