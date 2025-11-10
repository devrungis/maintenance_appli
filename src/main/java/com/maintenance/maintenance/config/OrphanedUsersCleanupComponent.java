package com.maintenance.maintenance.config;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.maintenance.maintenance.service.FirebaseAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Composant pour nettoyer les utilisateurs orphelins
 * Supprime les utilisateurs qui sont dans Realtime Database mais pas dans Firebase Authentication
 */
@Component
@Order(3) // S'exécute après UserInitializationComponent
public class OrphanedUsersCleanupComponent implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(OrphanedUsersCleanupComponent.class);

    @Autowired
    private DatabaseReference databaseReference;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Démarrage du nettoyage des utilisateurs orphelins...");
        
        try {
            cleanupOrphanedUsers();
        } catch (Exception e) {
            logger.error("Erreur lors du nettoyage des utilisateurs orphelins: " + e.getMessage(), e);
        }
    }

    /**
     * Nettoie les utilisateurs qui sont dans Realtime Database mais pas dans Firebase Authentication
     */
    private void cleanupOrphanedUsers() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> orphanedUsers = new ArrayList<>();
        
        databaseReference.child("utilisateurs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    if (snapshot.exists()) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String userId = userSnapshot.getKey();
                            if (userId != null) {
                                // Vérifier si l'utilisateur existe dans Firebase Authentication
                                if (!firebaseAuthService.userExistsInAuth(userId)) {
                                    orphanedUsers.add(userId);
                                }
                            }
                        }
                        
                        // Supprimer les utilisateurs orphelins
                        if (!orphanedUsers.isEmpty()) {
                            logger.info("Trouvé {} utilisateur(s) orphelin(s) à supprimer", orphanedUsers.size());
                            
                            for (String userId : orphanedUsers) {
                                try {
                                    databaseReference.child("utilisateurs").child(userId).removeValue(
                                        new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(DatabaseError error, DatabaseReference ref) {
                                                if (error != null) {
                                                    logger.warn("Erreur lors de la suppression de l'utilisateur orphelin {}: {}", 
                                                        userId, error.getMessage());
                                                } else {
                                                    logger.info("Utilisateur orphelin supprimé: {}", userId);
                                                }
                                            }
                                        }
                                    );
                                } catch (Exception e) {
                                    logger.warn("Erreur lors de la suppression de l'utilisateur orphelin {}: {}", 
                                        userId, e.getMessage());
                                }
                            }
                            
                            logger.info("Nettoyage terminé: {} utilisateur(s) orphelin(s) supprimé(s)", orphanedUsers.size());
                        } else {
                            logger.info("Aucun utilisateur orphelin trouvé");
                        }
                    } else {
                        logger.info("Aucun utilisateur trouvé dans Realtime Database");
                    }
                } catch (Exception e) {
                    logger.error("Erreur lors du nettoyage: " + e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logger.error("Erreur Firebase lors du nettoyage: " + error.getMessage());
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                logger.warn("Timeout lors du nettoyage (30 secondes)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interruption lors du nettoyage: " + e.getMessage());
        }
    }
}

