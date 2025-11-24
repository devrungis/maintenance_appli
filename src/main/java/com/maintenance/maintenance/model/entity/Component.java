package com.maintenance.maintenance.model.entity;

import java.util.ArrayList;
import java.util.List;

public class Component {

    private String componentId;
    private String entrepriseId;
    private String machineId;
    private Long categoryId;
    private String categoryName;
    private String nom;
    private String numeroSerie;
    private String etat; // fonctionnel / non_fonctionnel / en_reparation
    private String notes;
    private List<String> photos = new ArrayList<>();
    private String creePar;
    private Long creeLe;
    private String modifiePar;
    private Long modifieLe;
    private String location; // stockage / en machine
    private String typePeripherique; // standard / secours / neuf

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
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
        this.photos = photos != null ? photos : new ArrayList<>();
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTypePeripherique() {
        return typePeripherique != null ? typePeripherique : "standard";
    }

    public void setTypePeripherique(String typePeripherique) {
        this.typePeripherique = typePeripherique;
    }
}


