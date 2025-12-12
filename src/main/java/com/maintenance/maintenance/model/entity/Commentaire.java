package com.maintenance.maintenance.model.entity;

import java.util.Objects;

public class Commentaire {
    private String texte;
    private String auteurId;
    private String auteurNom;
    private Long dateCreation;
    private String imageUrl;
    
    public Commentaire() {
        this.dateCreation = System.currentTimeMillis();
    }
    
    public Commentaire(String texte, String auteurId, String auteurNom) {
        this.texte = texte;
        this.auteurId = auteurId;
        this.auteurNom = auteurNom;
        this.dateCreation = System.currentTimeMillis();
    }
    
    public String getTexte() {
        return texte;
    }
    
    public void setTexte(String texte) {
        this.texte = texte;
    }
    
    public String getAuteurId() {
        return auteurId;
    }
    
    public void setAuteurId(String auteurId) {
        this.auteurId = auteurId;
    }
    
    public String getAuteurNom() {
        return auteurNom;
    }
    
    public void setAuteurNom(String auteurNom) {
        this.auteurNom = auteurNom;
    }
    
    public Long getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(Long dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Commentaire)) return false;
        Commentaire that = (Commentaire) o;
        return Objects.equals(texte, that.texte) &&
               Objects.equals(auteurId, that.auteurId) &&
               Objects.equals(dateCreation, that.dateCreation);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(texte, auteurId, dateCreation);
    }
}

