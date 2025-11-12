package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.entity.Machine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MachineService {

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    public List<Machine> listMachines(String entrepriseId) throws Exception {
        return firebaseRealtimeService.getMachinesForEnterprise(entrepriseId);
    }

    public Machine getMachine(String entrepriseId, String machineId) throws Exception {
        return firebaseRealtimeService.getMachineById(entrepriseId, machineId);
    }

    public String createMachine(String entrepriseId, Machine machine) throws Exception {
        return firebaseRealtimeService.createMachine(entrepriseId, machine);
    }

    public void updateMachine(String entrepriseId, String machineId, Machine machine) throws Exception {
        firebaseRealtimeService.updateMachine(entrepriseId, machineId, machine);
    }

    public void deleteMachine(String entrepriseId, String machineId) throws Exception {
        firebaseRealtimeService.deleteMachine(entrepriseId, machineId);
    }
}

