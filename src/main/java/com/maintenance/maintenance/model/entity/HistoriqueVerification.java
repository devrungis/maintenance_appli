package com.maintenance.maintenance.model.entity;

public class HistoriqueVerification {
    private String historiqueId;
    private String entrepriseId;
    private String machineId;
    private String machineNom;
    private String description;
    private Long dateVerificationProgrammee; // Date programmée initialement
    private Long dateVerificationReelle; // Date réelle de vérification
    private String verifiePar; // ID de l'utilisateur qui a vérifié
    private String verifieParNom; // Nom de l'utilisateur qui a vérifié
    private Long dateCreation;

    // Getters et Setters
    public String getHistoriqueId() {
        return historiqueId;
    }

    public void setHistoriqueId(String historiqueId) {
        this.historiqueId = historiqueId;
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

    public Long getDateVerificationProgrammee() {
        return dateVerificationProgrammee;
    }

    public void setDateVerificationProgrammee(Long dateVerificationProgrammee) {
        this.dateVerificationProgrammee = dateVerificationProgrammee;
    }

    public Long getDateVerificationReelle() {
        return dateVerificationReelle;
    }

    public void setDateVerificationReelle(Long dateVerificationReelle) {
        this.dateVerificationReelle = dateVerificationReelle;
    }

    public String getVerifiePar() {
        return verifiePar;
    }

    public void setVerifiePar(String verifiePar) {
        this.verifiePar = verifiePar;
    }

    public String getVerifieParNom() {
        return verifieParNom;
    }

    public void setVerifieParNom(String verifieParNom) {
        this.verifieParNom = verifieParNom;
    }

    public Long getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Long dateCreation) {
        this.dateCreation = dateCreation;
    }
}

