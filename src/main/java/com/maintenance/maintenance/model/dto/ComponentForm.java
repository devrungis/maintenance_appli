package com.maintenance.maintenance.model.dto;

import com.maintenance.maintenance.model.entity.Component;

import java.util.ArrayList;
import java.util.List;

public class ComponentForm {

    private String entrepriseId;
    private String machineId;
    private String componentId;
    private Long categoryId;
    private String categoryName;
    private String nom;
    private String numeroSerie;
    private String etat;
    private String notes;
    private List<String> photos = new ArrayList<>();

    public Component toComponent() {
        Component component = new Component();
        component.setComponentId(componentId);
        component.setEntrepriseId(entrepriseId);
        component.setMachineId(machineId);
        component.setCategoryId(categoryId);
        component.setCategoryName(categoryName);
        component.setNom(nom != null ? nom.trim() : "");
        component.setNumeroSerie(numeroSerie != null ? numeroSerie.trim() : "");
        component.setEtat(etat != null ? etat : "fonctionnel");
        component.setNotes(notes != null ? notes.trim() : "");
        component.setPhotos(new ArrayList<>(photos));
        return component;
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

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
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

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos != null ? new ArrayList<>(photos) : new ArrayList<>();
    }
}


