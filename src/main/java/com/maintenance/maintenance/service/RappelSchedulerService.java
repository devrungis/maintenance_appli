package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.entity.Rappel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RappelSchedulerService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RappelSchedulerService.class);

    @Autowired
    private RappelService rappelService;

    @Autowired
    private EmailService emailService;

    /**
     * S'exécute au démarrage de l'application via CommandLineRunner
     * Cela garantit que toutes les dépendances sont injectées avant l'exécution
     */
    @Override
    public void run(String... args) throws Exception {
        logger.info("🚀 RappelSchedulerService démarré - Vérification automatique des rappels activée");
        logger.info("📅 Le scheduler va vérifier les rappels toutes les 5 minutes");
        logger.info("⚡ Première vérification immédiate dans quelques secondes...");
    }

    /**
     * Vérifie toutes les 5 minutes les rappels à envoyer
     * initialDelay = 60000 pour s'exécuter 1 minute après le démarrage (le temps que tout soit initialisé)
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000) // Toutes les 5 minutes (300000 ms = 5 minutes), démarrage après 1 minute
    public void checkAndSendRappels() {
        try {
            logger.info("🔍 [SCHEDULER] Vérification des rappels à envoyer...");
            List<Rappel> rappelsAEnvoyer = rappelService.getRappelsAEnvoyer();
            
            if (!rappelsAEnvoyer.isEmpty()) {
                logger.info("📧 [SCHEDULER] {} rappel(s) à envoyer trouvé(s)", rappelsAEnvoyer.size());
                String superAdminEmail = emailService.getSuperAdminEmail();
                
                for (Rappel rappel : rappelsAEnvoyer) {
                    try {
                        // Envoyer l'email
                        emailService.sendRappelEmail(
                            superAdminEmail,
                            rappel.getMachineNom() != null ? rappel.getMachineNom() : "Machine inconnue",
                            rappel.getDescription(),
                            rappel.getDateVerification(),
                            false
                        );
                        
                        // Marquer le rappel comme envoyé seulement si l'email a été envoyé avec succès
                        rappel.setEnvoye(true);
                        rappel.setDateEnvoi(System.currentTimeMillis());
                        rappelService.updateRappel(rappel.getEntrepriseId(), rappel.getRappelId(), rappel);
                        
                        logger.info("✅ Rappel envoyé avec succès pour la machine: {} à {}", 
                            rappel.getMachineNom(), superAdminEmail);
                    } catch (Exception e) {
                        logger.error("❌ Erreur lors de l'envoi du rappel pour la machine {}: {}", 
                            rappel.getMachineNom(), e.getMessage(), e);
                        // Ne pas marquer comme envoyé si l'email a échoué
                        // Le scheduler réessayera lors de la prochaine vérification
                    }
                }
            }

            // Vérifier les relances
            logger.info("🔍 [SCHEDULER] Vérification des rappels à relancer...");
            List<Rappel> rappelsARelancer = rappelService.getRappelsARelancer();
            
            if (!rappelsARelancer.isEmpty()) {
                logger.info("📧 [SCHEDULER] {} rappel(s) à relancer trouvé(s)", rappelsARelancer.size());
                String superAdminEmail = emailService.getSuperAdminEmail();
                
                for (Rappel rappel : rappelsARelancer) {
                    try {
                        // Envoyer la relance
                        emailService.sendRappelEmail(
                            superAdminEmail,
                            rappel.getMachineNom() != null ? rappel.getMachineNom() : "Machine inconnue",
                            rappel.getDescription(),
                            rappel.getDateVerification(),
                            true
                        );
                        
                        // Mettre à jour le nombre de relances envoyées seulement si l'email a été envoyé avec succès
                        int nbRelancesEnvoyees = (rappel.getNombreRelancesEnvoyees() != null ? rappel.getNombreRelancesEnvoyees() : 0) + 1;
                        rappel.setNombreRelancesEnvoyees(nbRelancesEnvoyees);
                        rappel.setDateDerniereRelance(System.currentTimeMillis());
                        rappelService.updateRappel(rappel.getEntrepriseId(), rappel.getRappelId(), rappel);
                        
                        logger.info("✅ Relance envoyée avec succès pour la machine: {} ({}/{}) à {}", 
                            rappel.getMachineNom(), nbRelancesEnvoyees, rappel.getNombreRelances(), superAdminEmail);
                    } catch (Exception e) {
                        logger.error("❌ Erreur lors de l'envoi de la relance pour la machine {}: {}", 
                            rappel.getMachineNom(), e.getMessage(), e);
                        // Ne pas mettre à jour le compteur si l'email a échoué
                        // Le scheduler réessayera lors de la prochaine vérification
                    }
                }
            } else {
                logger.debug("ℹ️ [SCHEDULER] Aucun rappel à envoyer ou à relancer pour le moment");
            }
        } catch (Exception e) {
            logger.error("❌ [SCHEDULER] Erreur lors de la vérification des rappels: {}", e.getMessage(), e);
        }
        
        // Log pour confirmer que le scheduler s'est exécuté
        logger.info("✅ [SCHEDULER] Vérification terminée - Prochaine vérification dans 5 minutes");
    }
}

