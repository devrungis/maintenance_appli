package com.maintenance.maintenance.config;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Composant de migration pour nettoyer la structure de base de données
 * Supprime le nœud entreprises/ sous utilisateurs/ (migration vers entreprises/ au niveau racine)
 */
@Component
@Order(1) // S'exécute avant UserInitializationComponent
public class DatabaseMigrationComponent implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationComponent.class);

    @Autowired
    private DatabaseReference databaseReference;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Démarrage de la migration de la structure de base de données...");
        
        try {
            migrateDatabaseStructure();
        } catch (Exception e) {
            logger.error("Erreur lors de la migration: " + e.getMessage(), e);
        }
    }

    /**
     * Migre la structure : supprime entreprises/ sous utilisateurs/
     */
    private void migrateDatabaseStructure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        databaseReference.child("utilisateurs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    if (snapshot.exists()) {
                        int migratedCount = 0;
                        
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String userId = userSnapshot.getKey();
                            if (userId != null && userSnapshot.hasChild("entreprises")) {
                                // Supprimer le nœud entreprises
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("entreprises", null);
                                
                                databaseReference.child("utilisateurs").child(userId)
                                    .updateChildren(updates, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(DatabaseError error, DatabaseReference ref) {
                                            if (error != null) {
                                                logger.warn("Erreur lors de la migration pour l'utilisateur {}: {}", 
                                                    userId, error.getMessage());
                                            } else {
                                                logger.debug("Nœud entreprises supprimé pour l'utilisateur: {}", userId);
                                            }
                                        }
                                    });
                                
                                migratedCount++;
                            }
                        }
                        
                        if (migratedCount > 0) {
                            logger.info("Migration terminée: {} utilisateur(s) migré(s)", migratedCount);
                        } else {
                            logger.info("Aucune migration nécessaire (structure déjà à jour)");
                        }
                    } else {
                        logger.info("Aucun utilisateur trouvé, pas de migration nécessaire");
                    }
                } catch (Exception e) {
                    logger.error("Erreur lors de la migration: " + e.getMessage(), e);
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logger.error("Erreur Firebase lors de la migration: " + error.getMessage());
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                logger.warn("Timeout lors de la migration (30 secondes)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interruption lors de la migration: " + e.getMessage());
        }
    }
}

