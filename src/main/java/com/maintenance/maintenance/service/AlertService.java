package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.entity.Alerte;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    public List<Alerte> listAlertes(String entrepriseId) throws Exception {
        return firebaseRealtimeService.getAlertesForEnterprise(entrepriseId);
    }

    public Alerte getAlerte(String entrepriseId, String alerteId) throws Exception {
        return firebaseRealtimeService.getAlerteById(entrepriseId, alerteId);
    }

    public String createAlerte(String entrepriseId, Alerte alerte) throws Exception {
        return firebaseRealtimeService.createAlerte(entrepriseId, alerte);
    }

    public void updateAlerte(String entrepriseId, String alerteId, Alerte alerte) throws Exception {
        firebaseRealtimeService.updateAlerte(entrepriseId, alerteId, alerte);
    }

    public void deleteAlerte(String entrepriseId, String alerteId) throws Exception {
        firebaseRealtimeService.deleteAlerte(entrepriseId, alerteId);
    }

    public List<Alerte> getAlertesAEnvoyer() throws Exception {
        return firebaseRealtimeService.getAlertesAEnvoyer();
    }

    public List<Alerte> getAlertesARelancer() throws Exception {
        return firebaseRealtimeService.getAlertesARelancer();
    }
}

