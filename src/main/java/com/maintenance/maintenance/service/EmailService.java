package com.maintenance.maintenance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    /**
     * Envoie un email au super admin pour une alerte de vérification de machine
     */
    public void sendAlerteEmail(String superAdminEmail, String machineNom, String description, Long dateVerification, boolean isRelance) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(superAdminEmail);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH);
            String dateVerifStr = dateFormat.format(new Date(dateVerification));
            
            if (isRelance) {
                message.setSubject("RELANCE - Alerte de vérification de machine - " + machineNom);
                String emailBody = String.format(
                    "Bonjour,\n\n" +
                    "Ceci est une relance concernant la vérification de machine.\n\n" +
                    "Détails de l'alerte :\n" +
                    "- Machine : %s\n" +
                    "- Date de vérification programmée : %s\n" +
                    "- Description : %s\n\n" +
                    "La vérification n'a pas encore été effectuée. Veuillez procéder à la vérification de cette machine.\n\n" +
                    "Cordialement,\n" +
                    "Système de maintenance",
                    machineNom,
                    dateVerifStr,
                    description != null ? description : "Aucune description"
                );
                message.setText(emailBody);
            } else {
                message.setSubject("Alerte de vérification de machine - " + machineNom);
                String emailBody = String.format(
                    "Bonjour,\n\n" +
                    "Une vérification de machine est programmée pour aujourd'hui.\n\n" +
                    "Détails de l'alerte :\n" +
                    "- Machine : %s\n" +
                    "- Date de vérification : %s\n" +
                    "- Description : %s\n\n" +
                    "Veuillez procéder à la vérification de cette machine.\n\n" +
                    "Cordialement,\n" +
                    "Système de maintenance",
                    machineNom,
                    dateVerifStr,
                    description != null ? description : "Aucune description"
                );
                message.setText(emailBody);
            }
            
            message.setFrom("hocinelampro@gmail.com");
            
            mailSender.send(message);
            System.out.println("Email " + (isRelance ? "de relance " : "") + "envoyé avec succès à " + superAdminEmail);
            logger.info("Email {} envoyé avec succès à {}", (isRelance ? "de relance" : ""), superAdminEmail);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
            logger.error("Erreur lors de l'envoi de l'email: {}", e.getMessage(), e);
            throw e; // Re-lancer l'exception pour que le scheduler puisse la gérer
        }
    }

    /**
     * Envoie un email au super admin pour un rappel de vérification de machine
     */
    public void sendRappelEmail(String superAdminEmail, String machineNom, String description, Long dateVerification, boolean isRelance) throws Exception {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(superAdminEmail);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH);
            String dateVerifStr = dateFormat.format(new Date(dateVerification));
            
            if (isRelance) {
                message.setSubject("RELANCE - Rappel de vérification de machine - " + machineNom);
                String emailBody = String.format(
                    "Bonjour,\n\n" +
                    "Ceci est une relance concernant le rappel de vérification de machine.\n\n" +
                    "Détails du rappel :\n" +
                    "- Machine : %s\n" +
                    "- Date de vérification programmée : %s\n" +
                    "- Description : %s\n\n" +
                    "La vérification n'a pas encore été effectuée. Veuillez procéder à la vérification de cette machine.\n\n" +
                    "Cordialement,\n" +
                    "Système de maintenance",
                    machineNom,
                    dateVerifStr,
                    description != null ? description : "Aucune description"
                );
                message.setText(emailBody);
            } else {
                message.setSubject("Rappel de vérification de machine - " + machineNom);
                String emailBody = String.format(
                    "Bonjour,\n\n" +
                    "Un rappel de vérification de machine est programmé pour aujourd'hui.\n\n" +
                    "Détails du rappel :\n" +
                    "- Machine : %s\n" +
                    "- Date de vérification : %s\n" +
                    "- Description : %s\n\n" +
                    "Veuillez procéder à la vérification de cette machine.\n\n" +
                    "Cordialement,\n" +
                    "Système de maintenance",
                    machineNom,
                    dateVerifStr,
                    description != null ? description : "Aucune description"
                );
                message.setText(emailBody);
            }
            
            message.setFrom("hocinelampro@gmail.com");
            
            mailSender.send(message);
            System.out.println("✅ Email de rappel " + (isRelance ? "de relance " : "") + "envoyé avec succès à " + superAdminEmail);
            logger.info("✅ Email de rappel {} envoyé avec succès à {}", (isRelance ? "de relance" : ""), superAdminEmail);
        } catch (org.springframework.mail.MailAuthenticationException e) {
            System.err.println("❌ Erreur d'authentification email: " + e.getMessage());
            logger.error("❌ Erreur d'authentification email: {}", e.getMessage(), e);
            throw new Exception("Erreur d'authentification email. Vérifiez les identifiants dans application.properties.", e);
        } catch (org.springframework.mail.MailSendException e) {
            System.err.println("❌ Erreur d'envoi email: " + e.getMessage());
            logger.error("❌ Erreur d'envoi email: {}", e.getMessage(), e);
            throw new Exception("Erreur d'envoi email. Vérifiez la configuration SMTP.", e);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email de rappel: " + e.getMessage());
            e.printStackTrace();
            logger.error("❌ Erreur lors de l'envoi de l'email de rappel: {}", e.getMessage(), e);
            throw new Exception("Erreur lors de l'envoi de l'email: " + e.getMessage(), e);
        }
    }

    /**
     * Récupère l'email du super admin
     */
    public String getSuperAdminEmail() throws Exception {
        List<Map<String, Object>> users = firebaseRealtimeService.getAllUsers();
        for (Map<String, Object> user : users) {
            String role = (String) user.get("role");
            if (role != null && "superadmin".equalsIgnoreCase(role)) {
                String email = (String) user.get("email");
                if (email != null && !email.isEmpty()) {
                    return email;
                }
            }
        }
        throw new Exception("Aucun super admin trouvé avec un email valide");
    }
}

