package com.maintenance.maintenance.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Ticket {
    
    private String ticketId;
    private String entrepriseId;
    private String titre;
    private String description;
    private String statut; // "a_faire", "en_cours", "termine", "archive"
    private String priorite; // "basse", "normale", "haute", "urgente"
    private String machineId; // ID de la machine associée (optionnel)
    private String machineNom; // Nom de la machine (pour affichage)
    private String assigneA; // ID de l'utilisateur assigné
    private String assigneANom; // Nom de l'utilisateur assigné (pour affichage)
    private String creePar; // ID de l'utilisateur créateur
    private String creeParNom; // Nom de l'utilisateur créateur (pour affichage)
    private Long dateCreation; // Timestamp
    private Long dateModification; // Timestamp
    private Long dateTerminaison; // Timestamp (quand terminé)
    private Long dateArchivage; // Timestamp (quand archivé)
    private Long dateEcheance; // Timestamp (date prévue de résolution)
    private List<Commentaire> commentaires; // Liste des commentaires
    private String categorie; // Catégorie du ticket (optionnel)
    
    public Ticket() {
        this.commentaires = new ArrayList<>();
        this.statut = "a_faire";
        this.priorite = "normale";
        this.dateCreation = System.currentTimeMillis();
        this.dateModification = System.currentTimeMillis();
    }
    
    public String getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
    
    public String getEntrepriseId() {
        return entrepriseId;
    }
    
    public void setEntrepriseId(String entrepriseId) {
        this.entrepriseId = entrepriseId;
    }
    
    public String getTitre() {
        return titre;
    }
    
    public void setTitre(String titre) {
        this.titre = titre;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStatut() {
        return statut != null ? statut : "a_faire";
    }
    
    public void setStatut(String statut) {
        this.statut = statut;
        this.dateModification = System.currentTimeMillis();
        
        // Mettre à jour les dates selon le statut
        if ("termine".equals(statut) && this.dateTerminaison == null) {
            this.dateTerminaison = System.currentTimeMillis();
        }
        if ("archive".equals(statut) && this.dateArchivage == null) {
            this.dateArchivage = System.currentTimeMillis();
        }
    }
    
    public String getPriorite() {
        return priorite != null ? priorite : "normale";
    }
    
    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }
    
    public String getMachineId() {
        return machineId;
    }
    
    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }
    
    public String getMachineNom() {
        return machineNom;
    }
    
    public void setMachineNom(String machineNom) {
        this.machineNom = machineNom;
    }
    
    public String getAssigneA() {
        return assigneA;
    }
    
    public void setAssigneA(String assigneA) {
        this.assigneA = assigneA;
    }
    
    public String getAssigneANom() {
        return assigneANom;
    }
    
    public void setAssigneANom(String assigneANom) {
        this.assigneANom = assigneANom;
    }
    
    public String getCreePar() {
        return creePar;
    }
    
    public void setCreePar(String creePar) {
        this.creePar = creePar;
    }
    
    public String getCreeParNom() {
        return creeParNom;
    }
    
    public void setCreeParNom(String creeParNom) {
        this.creeParNom = creeParNom;
    }
    
    public Long getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(Long dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public Long getDateModification() {
        return dateModification;
    }
    
    public void setDateModification(Long dateModification) {
        this.dateModification = dateModification;
    }
    
    public Long getDateTerminaison() {
        return dateTerminaison;
    }
    
    public void setDateTerminaison(Long dateTerminaison) {
        this.dateTerminaison = dateTerminaison;
    }
    
    public Long getDateArchivage() {
        return dateArchivage;
    }
    
    public void setDateArchivage(Long dateArchivage) {
        this.dateArchivage = dateArchivage;
    }
    
    public Long getDateEcheance() {
        return dateEcheance;
    }
    
    public void setDateEcheance(Long dateEcheance) {
        this.dateEcheance = dateEcheance;
    }
    
    public List<Commentaire> getCommentaires() {
        return commentaires != null ? commentaires : new ArrayList<>();
    }
    
    public void setCommentaires(List<Commentaire> commentaires) {
        this.commentaires = commentaires != null ? commentaires : new ArrayList<>();
    }
    
    public void addCommentaire(Commentaire commentaire) {
        if (this.commentaires == null) {
            this.commentaires = new ArrayList<>();
        }
        this.commentaires.add(commentaire);
        this.dateModification = System.currentTimeMillis();
    }
    
    public String getCategorie() {
        return categorie;
    }
    
    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket ticket)) return false;
        return Objects.equals(ticketId, ticket.ticketId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(ticketId);
    }
}

