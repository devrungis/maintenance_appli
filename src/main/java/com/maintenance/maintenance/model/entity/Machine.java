package com.maintenance.maintenance.model.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Machine {

    private String machineId;
    private String entrepriseId;
    private String nom;
    private String numeroSerie;
    private List<String> photos = new ArrayList<>();
    private String emplacement;
    private String notes;
    private Long dateCreation;
    private Long dateMiseAJour;
    private Long categoryId;
    private String categoryName;
    private String categoryDescription;
    private Boolean operationnel; // true = opérationnel, false = non opérationnel
    private Boolean enReparation; // true = en réparation, false = pas en réparation
    private Boolean enProgrammation; // true = en cours de programmation (matériel neuf)
    private String adresseIP; // Adresse IP de la machine
    private String machinePrincipaleId; // ID de la machine principale si c'est une machine de secours
    private Boolean estMachineSecours; // true = machine de secours, false = machine principale
    private Boolean estMachineEntrepot; // true = machine entrepôt (neuve, non programmée), false = machine normale
    private String creePar; // Utilisateur qui a créé la machine
    private Long creeLe; // Date de création (timestamp)
    private String modifiePar; // Utilisateur qui a modifié la machine
    private Long modifieLe; // Date de modification (timestamp)
    private String supprimePar; // Utilisateur qui a supprimé la machine
    private Long supprimeLe; // Date de suppression (timestamp)
    private Boolean supprime; // true si la machine est supprimée (soft delete)
    private Map<String, Object> champsPersonnalises = new HashMap<>(); // Champs personnalisés dynamiques

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getEntrepriseId() {
        return entrepriseId;
    }

    public void setEntrepriseId(String entrepriseId) {
        this.entrepriseId = entrepriseId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos != null ? photos : new ArrayList<>();
    }

    public String getEmplacement() {
        return emplacement;
    }

    public void setEmplacement(String emplacement) {
        this.emplacement = emplacement;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Long dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Long getDateMiseAJour() {
        return dateMiseAJour;
    }

    public void setDateMiseAJour(Long dateMiseAJour) {
        this.dateMiseAJour = dateMiseAJour;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    public Boolean getOperationnel() {
        return operationnel != null ? operationnel : true; // Par défaut opérationnel
    }

    public void setOperationnel(Boolean operationnel) {
        this.operationnel = operationnel;
    }

    public Boolean getEnReparation() {
        return enReparation != null ? enReparation : false; // Par défaut pas en réparation
    }

    public void setEnReparation(Boolean enReparation) {
        this.enReparation = enReparation;
    }

    public Boolean getEnProgrammation() {
        return enProgrammation != null ? enProgrammation : false;
    }

    public void setEnProgrammation(Boolean enProgrammation) {
        this.enProgrammation = enProgrammation;
    }

    public String getAdresseIP() {
        return adresseIP;
    }

    public void setAdresseIP(String adresseIP) {
        this.adresseIP = adresseIP;
    }

    public String getMachinePrincipaleId() {
        return machinePrincipaleId;
    }

    public void setMachinePrincipaleId(String machinePrincipaleId) {
        this.machinePrincipaleId = machinePrincipaleId;
    }

    public Boolean getEstMachineSecours() {
        return estMachineSecours != null ? estMachineSecours : false;
    }

    public void setEstMachineSecours(Boolean estMachineSecours) {
        this.estMachineSecours = estMachineSecours;
    }

    public Boolean getEstMachineEntrepot() {
        return estMachineEntrepot != null ? estMachineEntrepot : false;
    }

    public void setEstMachineEntrepot(Boolean estMachineEntrepot) {
        this.estMachineEntrepot = estMachineEntrepot;
    }

    public String getCreePar() {
        return creePar;
    }

    public void setCreePar(String creePar) {
        this.creePar = creePar;
    }

    public Long getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(Long creeLe) {
        this.creeLe = creeLe;
    }

    public String getModifiePar() {
        return modifiePar;
    }

    public void setModifiePar(String modifiePar) {
        this.modifiePar = modifiePar;
    }

    public Long getModifieLe() {
        return modifieLe;
    }

    public void setModifieLe(Long modifieLe) {
        this.modifieLe = modifieLe;
    }

    public String getSupprimePar() {
        return supprimePar;
    }

    public void setSupprimePar(String supprimePar) {
        this.supprimePar = supprimePar;
    }

    public Long getSupprimeLe() {
        return supprimeLe;
    }

    public void setSupprimeLe(Long supprimeLe) {
        this.supprimeLe = supprimeLe;
    }

    public Boolean getSupprime() {
        return supprime != null ? supprime : false;
    }

    public void setSupprime(Boolean supprime) {
        this.supprime = supprime;
    }

    public Map<String, Object> getChampsPersonnalises() {
        return champsPersonnalises != null ? champsPersonnalises : new HashMap<>();
    }

    public void setChampsPersonnalises(Map<String, Object> champsPersonnalises) {
        this.champsPersonnalises = champsPersonnalises != null ? champsPersonnalises : new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Machine machine)) return false;
        return Objects.equals(machineId, machine.machineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machineId);
    }
}

