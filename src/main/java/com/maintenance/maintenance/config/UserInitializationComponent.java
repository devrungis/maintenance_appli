package com.maintenance.maintenance.config;

import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.ListUsersPage;
import com.maintenance.maintenance.service.FirebaseAuthService;
import com.maintenance.maintenance.service.FirebaseRealtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2) // S'exécute après DatabaseMigrationComponent
public class UserInitializationComponent implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(UserInitializationComponent.class);

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Démarrage de la synchronisation automatique des utilisateurs...");
        
        try {
            synchronizeAllUsers();
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation automatique des utilisateurs: " + e.getMessage(), e);
        }
    }

    /**
     * Synchronise tous les utilisateurs de Firebase Authentication avec Realtime Database
     */
    private void synchronizeAllUsers() throws Exception {
        try {
            ListUsersPage page = firebaseAuthService.listAllUsers();
            int syncedCount = 0;
            int skippedCount = 0;
            int errorCount = 0;

            while (page != null) {
                for (ExportedUserRecord user : page.getValues()) {
                    try {
                        String uid = user.getUid();
                        String email = user.getEmail();
                        
                        // Vérifier que l'utilisateur existe bien dans Firebase Authentication
                        if (!firebaseAuthService.userExistsInAuth(uid)) {
                            logger.warn("Utilisateur {} non trouvé dans Firebase Auth, ignoré", uid);
                            skippedCount++;
                            continue;
                        }
                        
                        // Vérifier si l'utilisateur existe déjà dans Realtime Database
                        if (!firebaseRealtimeService.userExistsInRealtime(uid)) {
                            // Récupérer les informations de l'utilisateur
                            String nom = user.getDisplayName() != null ? user.getDisplayName() : "";
                            String telephone = user.getPhoneNumber() != null ? user.getPhoneNumber() : "";
                            String nomUtilisateur = email != null ? email.split("@")[0] : "user";
                            
                            // Initialiser l'arborescence (sans le nœud entreprises)
                            firebaseRealtimeService.initializeExistingUser(
                                uid,
                                email,
                                nom,
                                nomUtilisateur,
                                telephone,
                                null // Le rôle sera déterminé automatiquement (superadmin si premier)
                            );
                            
                            syncedCount++;
                            logger.info("Arborescence créée pour l'utilisateur: {} ({})", email, uid);
                        } else {
                            // Mettre à jour la structure existante (supprimer entreprises/ si présent)
                            firebaseRealtimeService.updateExistingUserStructure(uid);
                            skippedCount++;
                            logger.debug("Utilisateur {} déjà présent dans Realtime Database, structure mise à jour", uid);
                        }
                    } catch (Exception e) {
                        errorCount++;
                        logger.warn("Erreur lors de la synchronisation de l'utilisateur {}: {}", 
                            user.getUid(), e.getMessage());
                    }
                }
                
                // Passer à la page suivante
                try {
                    if (page.hasNextPage()) {
                        page = page.getNextPage();
                    } else {
                        page = null;
                    }
                } catch (Exception e) {
                    // Pas de page suivante ou erreur
                    page = null;
                }
            }
            
            logger.info("Synchronisation terminée: {} créé(s), {} ignoré(s), {} erreur(s)", 
                syncedCount, skippedCount, errorCount);
            
        } catch (FirebaseAuthException e) {
            logger.error("Erreur lors de la récupération des utilisateurs: " + e.getMessage(), e);
            throw new Exception("Erreur lors de la synchronisation: " + e.getMessage());
        }
    }
}

