package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.entity.Enterprise;
import com.maintenance.maintenance.repository.EnterpriseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class EnterpriseService {

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    /**
     * Récupère toutes les entreprises (depuis JPA et synchronise avec Firebase)
     */
    public List<Enterprise> getAllEnterprises() {
        return enterpriseRepository.findAll();
    }

    /**
     * Récupère une entreprise par son ID
     */
    public Optional<Enterprise> getEnterpriseById(Long id) {
        return enterpriseRepository.findById(id);
    }

    /**
     * Récupère une entreprise par son Firebase ID
     */
    public Optional<Enterprise> getEnterpriseByFirebaseId(String firebaseId) {
        return enterpriseRepository.findByFirebaseId(firebaseId);
    }

    /**
     * Crée une entreprise dans JPA et Firebase
     */
    public Enterprise createEnterprise(Enterprise enterprise) throws Exception {
        System.out.println("=== EnterpriseService.createEnterprise: Début ===");
        
        // Sauvegarder dans JPA d'abord
        enterprise.setDateCreation(LocalDateTime.now());
        enterprise.setDateModification(LocalDateTime.now());
        Enterprise savedEnterprise = enterpriseRepository.save(enterprise);
        System.out.println("Entreprise sauvegardée dans JPA avec ID: " + savedEnterprise.getId());

        // Créer dans Firebase Realtime Database
        try {
            Map<String, Object> entrepriseData = new HashMap<>();
            entrepriseData.put("nom", savedEnterprise.getNom());
            entrepriseData.put("rue", savedEnterprise.getRue() != null ? savedEnterprise.getRue() : "");
            entrepriseData.put("codePostal", savedEnterprise.getCodePostal() != null ? savedEnterprise.getCodePostal() : "");
            entrepriseData.put("ville", savedEnterprise.getVille() != null ? savedEnterprise.getVille() : "");
            entrepriseData.put("email", savedEnterprise.getEmail() != null ? savedEnterprise.getEmail() : "");
            entrepriseData.put("numero", savedEnterprise.getNumero() != null ? savedEnterprise.getNumero() : "");

            String firebaseId = firebaseRealtimeService.createEnterprise(entrepriseData);
            System.out.println("Entreprise créée dans Firebase avec ID: " + firebaseId);

            // Mettre à jour l'entité avec le Firebase ID
            savedEnterprise.setFirebaseId(firebaseId);
            savedEnterprise = enterpriseRepository.save(savedEnterprise);
            System.out.println("Entreprise mise à jour avec Firebase ID: " + firebaseId);

        } catch (Exception e) {
            System.err.println("ERREUR lors de la création dans Firebase: " + e.getMessage());
            e.printStackTrace();
            // Ne pas faire échouer la création si Firebase échoue, on garde l'entité en JPA
        }

        System.out.println("=== EnterpriseService.createEnterprise: Terminé ===");
        return savedEnterprise;
    }

    /**
     * Met à jour une entreprise dans JPA et Firebase
     */
    public Enterprise updateEnterprise(Long id, Enterprise enterpriseDetails) throws Exception {
        System.out.println("=== EnterpriseService.updateEnterprise: Début ===");
        
        Enterprise enterprise = enterpriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée avec l'ID: " + id));

        // Mettre à jour les champs
        enterprise.setNom(enterpriseDetails.getNom());
        enterprise.setRue(enterpriseDetails.getRue());
        enterprise.setCodePostal(enterpriseDetails.getCodePostal());
        enterprise.setVille(enterpriseDetails.getVille());
        enterprise.setEmail(enterpriseDetails.getEmail());
        enterprise.setNumero(enterpriseDetails.getNumero());
        enterprise.setDateModification(LocalDateTime.now());

        // Sauvegarder dans JPA
        Enterprise updatedEnterprise = enterpriseRepository.save(enterprise);
        System.out.println("Entreprise mise à jour dans JPA");

        // Mettre à jour dans Firebase si Firebase ID existe
        if (updatedEnterprise.getFirebaseId() != null && !updatedEnterprise.getFirebaseId().isEmpty()) {
            try {
                Map<String, Object> entrepriseData = new HashMap<>();
                entrepriseData.put("nom", updatedEnterprise.getNom());
                entrepriseData.put("rue", updatedEnterprise.getRue() != null ? updatedEnterprise.getRue() : "");
                entrepriseData.put("codePostal", updatedEnterprise.getCodePostal() != null ? updatedEnterprise.getCodePostal() : "");
                entrepriseData.put("ville", updatedEnterprise.getVille() != null ? updatedEnterprise.getVille() : "");
                entrepriseData.put("email", updatedEnterprise.getEmail() != null ? updatedEnterprise.getEmail() : "");
                entrepriseData.put("numero", updatedEnterprise.getNumero() != null ? updatedEnterprise.getNumero() : "");

                firebaseRealtimeService.updateEnterprise(updatedEnterprise.getFirebaseId(), entrepriseData);
                System.out.println("Entreprise mise à jour dans Firebase");

            } catch (Exception e) {
                System.err.println("ERREUR lors de la mise à jour dans Firebase: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("=== EnterpriseService.updateEnterprise: Terminé ===");
        return updatedEnterprise;
    }

    /**
     * Supprime une entreprise de JPA et Firebase
     */
    public void deleteEnterprise(Long id) throws Exception {
        System.out.println("=== EnterpriseService.deleteEnterprise: Début ===");
        
        Enterprise enterprise = enterpriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée avec l'ID: " + id));

        // Supprimer de Firebase si Firebase ID existe
        if (enterprise.getFirebaseId() != null && !enterprise.getFirebaseId().isEmpty()) {
            try {
                firebaseRealtimeService.deleteEnterprise(enterprise.getFirebaseId());
                System.out.println("Entreprise supprimée de Firebase");
            } catch (Exception e) {
                System.err.println("ERREUR lors de la suppression dans Firebase: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Supprimer de JPA
        enterpriseRepository.delete(enterprise);
        System.out.println("Entreprise supprimée de JPA");

        System.out.println("=== EnterpriseService.deleteEnterprise: Terminé ===");
    }

    /**
     * Synchronise toutes les entreprises depuis Firebase vers JPA
     */
    public void synchronizeFromFirebase() throws Exception {
        System.out.println("=== EnterpriseService.synchronizeFromFirebase: Début ===");
        
        try {
            List<Map<String, Object>> firebaseEnterprises = firebaseRealtimeService.getAllEnterprises();
            System.out.println("Nombre d'entreprises dans Firebase: " + firebaseEnterprises.size());

            for (Map<String, Object> firebaseEnterprise : firebaseEnterprises) {
                String firebaseId = (String) firebaseEnterprise.get("entrepriseId");
                
                // Vérifier si l'entreprise existe déjà dans JPA
                Optional<Enterprise> existingEnterprise = enterpriseRepository.findByFirebaseId(firebaseId);
                
                if (existingEnterprise.isEmpty()) {
                    // Créer l'entreprise dans JPA
                    Enterprise enterprise = new Enterprise();
                    enterprise.setFirebaseId(firebaseId);
                    enterprise.setNom((String) firebaseEnterprise.getOrDefault("nom", ""));
                    // Support ancien format (adresse) et nouveau format (rue, codePostal, ville)
                    if (firebaseEnterprise.containsKey("rue")) {
                        enterprise.setRue((String) firebaseEnterprise.getOrDefault("rue", ""));
                        enterprise.setCodePostal((String) firebaseEnterprise.getOrDefault("codePostal", ""));
                        enterprise.setVille((String) firebaseEnterprise.getOrDefault("ville", ""));
                    } else if (firebaseEnterprise.containsKey("adresse")) {
                        // Migration: convertir adresse en rue
                        String adresse = (String) firebaseEnterprise.getOrDefault("adresse", "");
                        enterprise.setRue(adresse);
                        enterprise.setCodePostal("");
                        enterprise.setVille("");
                    } else {
                        enterprise.setRue("");
                        enterprise.setCodePostal("");
                        enterprise.setVille("");
                    }
                    enterprise.setEmail((String) firebaseEnterprise.getOrDefault("email", ""));
                    enterprise.setNumero((String) firebaseEnterprise.getOrDefault("numero", ""));
                    enterprise.setDateCreation(LocalDateTime.now());
                    
                    enterpriseRepository.save(enterprise);
                    System.out.println("Entreprise synchronisée depuis Firebase: " + firebaseId);
                }
            }

            System.out.println("=== EnterpriseService.synchronizeFromFirebase: Terminé ===");
        } catch (Exception e) {
            System.err.println("ERREUR lors de la synchronisation depuis Firebase: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}

