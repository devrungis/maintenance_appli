package com.maintenance.maintenance.model.entity;

import java.util.Objects;

public class Stock {

    private String stockId;
    private String entrepriseId;
    private String machineId; // Référence à la machine
    private String machineNom; // Nom de la machine pour affichage
    private String nomProduit; // Nom du produit/pièce en stock
    private String reference; // Référence du produit
    private Integer quantite; // Quantité en stock
    private String unite; // Unité (pièces, kg, litres, etc.)
    private Double prixUnitaire; // Prix unitaire
    private String emplacement; // Emplacement du stock
    private Integer seuilMinimum; // Seuil d'alerte minimum
    private String fournisseur; // Nom du fournisseur
    private String notes; // Notes complémentaires
    private Long dateCreation;
    private Long dateMiseAJour;

    public Stock() {
    }

    public String getStockId() {
        return stockId;
    }

    public void setStockId(String stockId) {
        this.stockId = stockId;
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

    public String getNomProduit() {
        return nomProduit;
    }

    public void setNomProduit(String nomProduit) {
        this.nomProduit = nomProduit;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public Double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(Double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public String getEmplacement() {
        return emplacement;
    }

    public void setEmplacement(String emplacement) {
        this.emplacement = emplacement;
    }

    public Integer getSeuilMinimum() {
        return seuilMinimum;
    }

    public void setSeuilMinimum(Integer seuilMinimum) {
        this.seuilMinimum = seuilMinimum;
    }

    public String getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(String fournisseur) {
        this.fournisseur = fournisseur;
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
        if (!(o instanceof Stock stock)) return false;
        return Objects.equals(stockId, stock.stockId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockId);
    }
}

