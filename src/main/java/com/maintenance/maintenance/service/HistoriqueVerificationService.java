package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.entity.HistoriqueVerification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoriqueVerificationService {

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    public List<HistoriqueVerification> listHistoriqueVerifications(String entrepriseId) throws Exception {
        return firebaseRealtimeService.getHistoriqueVerificationsForEnterprise(entrepriseId);
    }
}

