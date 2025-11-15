package com.maintenance.maintenance.model.dto;

import com.maintenance.maintenance.model.entity.Stock;

public class StockForm {

    private String entrepriseId;
    private String machineId;
    private String nomProduit;
    private String reference;
    private Integer quantite;
    private String unite;
    private Double prixUnitaire;
    private String emplacement;
    private Integer seuilMinimum;
    private String fournisseur;
    private String notes;

    public Stock toStock() {
        Stock stock = new Stock();
        stock.setMachineId(machineId != null ? machineId.trim() : null);
        stock.setNomProduit(nomProduit != null ? nomProduit.trim() : "");
        stock.setReference(reference != null ? reference.trim() : "");
        stock.setQuantite(quantite != null ? quantite : 0);
        stock.setUnite(unite != null ? unite.trim() : "");
        stock.setPrixUnitaire(prixUnitaire);
        stock.setEmplacement(emplacement != null ? emplacement.trim() : "");
        stock.setSeuilMinimum(seuilMinimum);
        stock.setFournisseur(fournisseur != null ? fournisseur.trim() : "");
        stock.setNotes(notes != null ? notes.trim() : "");
        return stock;
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
}

