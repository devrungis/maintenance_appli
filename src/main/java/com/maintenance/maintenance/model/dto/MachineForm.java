package com.maintenance.maintenance.model.dto;

import com.maintenance.maintenance.model.entity.Machine;

import java.util.ArrayList;
import java.util.List;

public class MachineForm {

    private String entrepriseId;
    private String nom;
    private String numeroSerie;
    private String emplacement;
    private String notes;
    private Long categoryId;
    private List<String> existingPhotos = new ArrayList<>();
    private Boolean operationnel;
    private Boolean enReparation;
    private Boolean enProgrammation;
    private String adresseIP;
    private String machinePrincipaleId;
    private Boolean estMachineSecours;
    private Boolean estMachineEntrepot;

    public Machine toMachine() {
        Machine machine = new Machine();
        machine.setEntrepriseId(entrepriseId != null ? entrepriseId.trim() : null);
        machine.setNom(nom != null ? nom.trim() : "");
        machine.setNumeroSerie(numeroSerie != null ? numeroSerie.trim() : "");
        machine.setEmplacement(emplacement != null ? emplacement.trim() : "");
        machine.setNotes(notes != null ? notes.trim() : "");
        machine.setPhotos(new ArrayList<>(existingPhotos));
        machine.setCategoryId(categoryId);
        machine.setOperationnel(operationnel != null ? operationnel : true); // Par défaut opérationnel
        machine.setEnReparation(enReparation != null ? enReparation : false); // Par défaut pas en réparation
        machine.setEnProgrammation(enProgrammation != null ? enProgrammation : false);
        machine.setAdresseIP(adresseIP != null ? adresseIP.trim() : "");
        machine.setMachinePrincipaleId(machinePrincipaleId != null ? machinePrincipaleId.trim() : null);
        machine.setEstMachineSecours(estMachineSecours != null ? estMachineSecours : false);
        machine.setEstMachineEntrepot(estMachineEntrepot != null ? estMachineEntrepot : false);
        return machine;
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public List<String> getExistingPhotos() {
        return existingPhotos;
    }

    public void setExistingPhotos(List<String> existingPhotos) {
        this.existingPhotos = existingPhotos != null ? new ArrayList<>(existingPhotos) : new ArrayList<>();
    }

    public Boolean getOperationnel() {
        return operationnel;
    }

    public void setOperationnel(Boolean operationnel) {
        this.operationnel = operationnel;
    }

    public Boolean getEnReparation() {
        return enReparation;
    }

    public void setEnReparation(Boolean enReparation) {
        this.enReparation = enReparation;
    }

    public Boolean getEnProgrammation() {
        return enProgrammation;
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
        return estMachineSecours;
    }

    public void setEstMachineSecours(Boolean estMachineSecours) {
        this.estMachineSecours = estMachineSecours;
    }

    public Boolean getEstMachineEntrepot() {
        return estMachineEntrepot;
    }

    public void setEstMachineEntrepot(Boolean estMachineEntrepot) {
        this.estMachineEntrepot = estMachineEntrepot;
    }
}

