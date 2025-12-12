package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.entity.Rappel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RappelService {

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    public List<Rappel> listRappels(String entrepriseId) throws Exception {
        return firebaseRealtimeService.getRappelsForEnterprise(entrepriseId);
    }

    public Rappel getRappel(String entrepriseId, String rappelId) throws Exception {
        return firebaseRealtimeService.getRappelById(entrepriseId, rappelId);
    }

    public String createRappel(String entrepriseId, Rappel rappel) throws Exception {
        return firebaseRealtimeService.createRappel(entrepriseId, rappel);
    }

    public void updateRappel(String entrepriseId, String rappelId, Rappel rappel) throws Exception {
        firebaseRealtimeService.updateRappel(entrepriseId, rappelId, rappel);
    }

    public void deleteRappel(String entrepriseId, String rappelId) throws Exception {
        firebaseRealtimeService.deleteRappel(entrepriseId, rappelId);
    }

    public List<Rappel> getRappelsAEnvoyer() throws Exception {
        return firebaseRealtimeService.getRappelsAEnvoyer();
    }

    public List<Rappel> getRappelsARelancer() throws Exception {
        return firebaseRealtimeService.getRappelsARelancer();
    }
}

