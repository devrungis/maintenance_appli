package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.entity.Alerte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(AlertSchedulerService.class);

    @Autowired
    private AlertService alertService;

    @Autowired
    private EmailService emailService;

    /**
     * Vérifie toutes les heures les alertes à envoyer
     */
    @Scheduled(fixedRate = 3600000) // Toutes les heures (3600000 ms = 1 heure)
    public void checkAndSendAlertes() {
        try {
            logger.info("Vérification des alertes à envoyer...");
            List<Alerte> alertesAEnvoyer = alertService.getAlertesAEnvoyer();
            
            if (!alertesAEnvoyer.isEmpty()) {
                logger.info("{} alerte(s) à envoyer trouvée(s)", alertesAEnvoyer.size());
                String superAdminEmail = emailService.getSuperAdminEmail();
                
                for (Alerte alerte : alertesAEnvoyer) {
                    try {
                        // Envoyer l'email
                        emailService.sendAlerteEmail(
                            superAdminEmail,
                            alerte.getMachineNom() != null ? alerte.getMachineNom() : "Machine inconnue",
                            alerte.getDescription(),
                            alerte.getDateVerification(),
                            false
                        );
                        
                        // Marquer l'alerte comme envoyée
                        alerte.setEnvoye(true);
                        alerte.setDateEnvoi(System.currentTimeMillis());
                        alertService.updateAlerte(alerte.getEntrepriseId(), alerte.getAlerteId(), alerte);
                        
                        logger.info("Alerte envoyée pour la machine: {}", alerte.getMachineNom());
                    } catch (Exception e) {
                        logger.error("Erreur lors de l'envoi de l'alerte pour la machine {}: {}", 
                            alerte.getMachineNom(), e.getMessage());
                    }
                }
            }

            // Vérifier les relances
            logger.info("Vérification des alertes à relancer...");
            List<Alerte> alertesARelancer = alertService.getAlertesARelancer();
            
            if (!alertesARelancer.isEmpty()) {
                logger.info("{} alerte(s) à relancer trouvée(s)", alertesARelancer.size());
                String superAdminEmail = emailService.getSuperAdminEmail();
                
                for (Alerte alerte : alertesARelancer) {
                    try {
                        // Envoyer la relance
                        emailService.sendAlerteEmail(
                            superAdminEmail,
                            alerte.getMachineNom() != null ? alerte.getMachineNom() : "Machine inconnue",
                            alerte.getDescription(),
                            alerte.getDateVerification(),
                            true
                        );
                        
                        // Mettre à jour le nombre de relances envoyées
                        int nbRelancesEnvoyees = (alerte.getNombreRelancesEnvoyees() != null ? alerte.getNombreRelancesEnvoyees() : 0) + 1;
                        alerte.setNombreRelancesEnvoyees(nbRelancesEnvoyees);
                        alerte.setDateDerniereRelance(System.currentTimeMillis());
                        alertService.updateAlerte(alerte.getEntrepriseId(), alerte.getAlerteId(), alerte);
                        
                        logger.info("Relance envoyée pour la machine: {} ({}/{})", 
                            alerte.getMachineNom(), nbRelancesEnvoyees, alerte.getNombreRelances());
                    } catch (Exception e) {
                        logger.error("Erreur lors de l'envoi de la relance pour la machine {}: {}", 
                            alerte.getMachineNom(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification des alertes: {}", e.getMessage());
        }
    }
}

