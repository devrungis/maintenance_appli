package com.maintenance.maintenance.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.ListUsersPage;
import com.google.firebase.auth.UserRecord;
import com.maintenance.maintenance.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class FirebaseAuthService {

    @Autowired
    private FirebaseAuth firebaseAuth;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${firebase.api.key}")
    private String firebaseApiKey;

    private static final String FIREBASE_AUTH_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=";

    public FirebaseToken verifyIdToken(String idToken) throws FirebaseAuthException {
        return firebaseAuth.verifyIdToken(idToken);
    }

    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        return firebaseAuth.getUserByEmail(email);
    }

    /**
     * Authentifie un utilisateur avec email et mot de passe via Firebase REST API
     */
    public String authenticateUser(String email, String password) throws AuthenticationException, Exception {
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);
        requestBody.put("password", password);
        requestBody.put("returnSecureToken", "true");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        // Construire l'URL sans exposer la clé API dans les logs
        String url = FIREBASE_AUTH_URL + firebaseApiKey;
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody == null) {
                throw new Exception("Erreur d'authentification");
            }
            
            // Vérifier les erreurs Firebase
            if (responseBody.containsKey("error")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> error = (Map<String, Object>) responseBody.get("error");
                String message = (String) error.get("message");
                
                if (message != null) {
                    if (message.contains("EMAIL_NOT_FOUND") || message.contains("USER_NOT_FOUND")) {
                        throw new AuthenticationException("Email inexistant");
                    } else if (message.contains("INVALID_PASSWORD") || message.contains("INVALID_CREDENTIAL") || message.contains("INVALID_LOGIN_CREDENTIALS")) {
                        throw new AuthenticationException("Mot de passe incorrect");
                    }
                }
                throw new Exception("Erreur d'authentification");
            }
            
            // Vérifier que le token est présent
            String idToken = (String) responseBody.get("idToken");
            if (idToken == null || idToken.isEmpty()) {
                throw new Exception("Erreur d'authentification");
            }
            
            return idToken;
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Gérer les erreurs HTTP 400 (Bad Request)
            String responseBody = e.getResponseBodyAsString();
            if (responseBody != null && responseBody.contains("EMAIL_NOT_FOUND")) {
                throw new AuthenticationException("Email inexistant");
            } else if (responseBody != null && (responseBody.contains("INVALID_PASSWORD") || responseBody.contains("INVALID_CREDENTIAL") || responseBody.contains("INVALID_LOGIN_CREDENTIALS"))) {
                throw new AuthenticationException("Mot de passe incorrect");
            }
            // Ne pas exposer l'URL avec la clé API dans l'exception
            throw new Exception("Erreur d'authentification");
        } catch (org.springframework.web.client.RestClientException e) {
            // Masquer toute information sensible dans les exceptions RestTemplate
            throw new Exception("Erreur d'authentification");
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            // Ne pas propager le message d'exception qui pourrait contenir l'URL avec la clé API
            throw new Exception("Erreur d'authentification");
        }
    }

    public UserRecord createUser(String email, String password) throws FirebaseAuthException {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
            .setEmail(email)
            .setPassword(password)
            .setEmailVerified(false);

        return firebaseAuth.createUser(request);
    }

    public void deleteUser(String uid) throws FirebaseAuthException {
        firebaseAuth.deleteUser(uid);
    }

    public UserRecord getUserById(String uid) throws FirebaseAuthException {
        return firebaseAuth.getUser(uid);
    }

    public ListUsersPage listAllUsers() throws FirebaseAuthException {
        return firebaseAuth.listUsers(null);
    }

    public UserRecord updateUser(String uid, UserRecord.UpdateRequest updateRequest) throws FirebaseAuthException {
        return firebaseAuth.updateUser(updateRequest);
    }

    /**
     * Met à jour le mot de passe d'un utilisateur
     */
    public void updateUserPassword(String uid, String newPassword) throws FirebaseAuthException {
        UserRecord.UpdateRequest updateRequest = new UserRecord.UpdateRequest(uid)
            .setPassword(newPassword);
        firebaseAuth.updateUser(updateRequest);
    }

    /**
     * Met à jour l'email d'un utilisateur
     */
    public void updateUserEmail(String uid, String newEmail) throws FirebaseAuthException {
        UserRecord.UpdateRequest updateRequest = new UserRecord.UpdateRequest(uid)
            .setEmail(newEmail);
        firebaseAuth.updateUser(updateRequest);
    }

    /**
     * Vérifie si un utilisateur existe dans Firebase Authentication
     */
    public boolean userExistsInAuth(String uid) {
        try {
            firebaseAuth.getUser(uid);
            return true;
        } catch (FirebaseAuthException e) {
            return false;
        }
    }
}

