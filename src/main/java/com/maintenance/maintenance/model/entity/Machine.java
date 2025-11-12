package com.maintenance.maintenance.model.entity;

import java.util.ArrayList;
import java.util.List;
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

