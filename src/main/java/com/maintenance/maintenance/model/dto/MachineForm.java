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
    private List<String> existingPhotos = new ArrayList<>();

    public Machine toMachine() {
        Machine machine = new Machine();
        machine.setNom(nom != null ? nom.trim() : "");
        machine.setNumeroSerie(numeroSerie != null ? numeroSerie.trim() : "");
        machine.setEmplacement(emplacement != null ? emplacement.trim() : "");
        machine.setNotes(notes != null ? notes.trim() : "");
        machine.setPhotos(new ArrayList<>(existingPhotos));
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

    public List<String> getExistingPhotos() {
        return existingPhotos;
    }

    public void setExistingPhotos(List<String> existingPhotos) {
        this.existingPhotos = existingPhotos != null ? new ArrayList<>(existingPhotos) : new ArrayList<>();
    }
}

