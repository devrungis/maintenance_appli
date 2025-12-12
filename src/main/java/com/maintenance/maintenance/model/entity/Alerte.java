package com.maintenance.maintenance.model.entity;

public class Alerte {
    private String alerteId;
    private String entrepriseId;
    private String machineId;
    private String machineNom;
    private String description;
    private Long dateVerification; // Timestamp de la date de vérification programmée
    private Boolean envoye; // Indique si l'email a déjà été envoyé
    private Long dateEnvoi; // Timestamp de l'envoi de l'email
    private Boolean activerRelance; // Indique si les relances sont activées
    private Integer nombreRelances; // Nombre de relances à envoyer
    private Integer nombreRelancesEnvoyees; // Nombre de relances déjà envoyées
    private Long dateDerniereRelance; // Timestamp de la dernière relance envoyée
    private Boolean verifie; // Indique si la machine a été vérifiée
    private Long dateVerificationReelle; // Timestamp de la vérification réelle
    private String creePar; // ID de l'utilisateur créateur
    private String creeParNom; // Nom de l'utilisateur créateur
    private Long dateCreation;
    private Long dateModification;

    // Getters et Setters
    public String getAlerteId() {
        return alerteId;
    }

    public void setAlerteId(String alerteId) {
        this.alerteId = alerteId;
    }

    public String getEntrepriseId() {
        return entrepriseId;
    }

    public void setEntrepriseId(String entrepriseId) {
        this.entrepriseId = entrepriseId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getDateVerification() {
        return dateVerification;
    }

    public void setDateVerification(Long dateVerification) {
        this.dateVerification = dateVerification;
    }

    public Boolean getEnvoye() {
        return envoye;
    }

    public void setEnvoye(Boolean envoye) {
        this.envoye = envoye;
    }

    public Long getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(Long dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public Boolean getActiverRelance() {
        return activerRelance;
    }

    public void setActiverRelance(Boolean activerRelance) {
        this.activerRelance = activerRelance;
    }

    public Integer getNombreRelances() {
        return nombreRelances;
    }

    public void setNombreRelances(Integer nombreRelances) {
        this.nombreRelances = nombreRelances;
    }

    public Integer getNombreRelancesEnvoyees() {
        return nombreRelancesEnvoyees;
    }

    public void setNombreRelancesEnvoyees(Integer nombreRelancesEnvoyees) {
        this.nombreRelancesEnvoyees = nombreRelancesEnvoyees;
    }

    public Long getDateDerniereRelance() {
        return dateDerniereRelance;
    }

    public void setDateDerniereRelance(Long dateDerniereRelance) {
        this.dateDerniereRelance = dateDerniereRelance;
    }

    public Boolean getVerifie() {
        return verifie;
    }

    public void setVerifie(Boolean verifie) {
        this.verifie = verifie;
    }

    public Long getDateVerificationReelle() {
        return dateVerificationReelle;
    }

    public void setDateVerificationReelle(Long dateVerificationReelle) {
        this.dateVerificationReelle = dateVerificationReelle;
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
}

